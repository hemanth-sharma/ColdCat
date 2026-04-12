package com.example.coldcat.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.example.coldcat.data.AppDatabase
import com.example.coldcat.util.TimeUtils
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Local VPN service that intercepts DNS queries and returns NXDOMAIN
 * (or 0.0.0.0) for blocked domains during active block periods.
 *
 * Architecture:
 * - Establishes a local TUN interface
 * - Reads all outgoing DNS packets (UDP port 53)
 * - If the queried domain is in the block list AND block is active → returns blocked response
 * - Otherwise → forwards to real DNS (8.8.8.8) and relays response back
 */
class VpnBlockerService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var blockedDomains = setOf<String>()
    private var schedules = listOf<com.example.coldcat.data.BlockSchedule>()

    companion object {
        var isRunning = false
        const val ACTION_START = "com.example.coldcat.VPN_START"
        const val ACTION_STOP = "com.example.coldcat.VPN_STOP"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return
        try {
            val builder = Builder()
                .setSession("ColdCat VPN")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .setMtu(1500)

            vpnInterface = builder.establish() ?: return
            isRunning = true

            // Start observing blocked domains
            scope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                db.blockDao().getAllBlockedWebsites().collect { sites ->
                    blockedDomains = sites.map { normalizeDomain(it.domain) }.toSet()
                }
            }
            scope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                db.blockDao().getAllSchedules().collect { list ->
                    schedules = list
                }
            }

            // Start packet processing loop
            scope.launch(Dispatchers.IO) {
                runPacketLoop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
        }
    }

    private fun stopVpn() {
        isRunning = false
        scope.coroutineContext.cancelChildren()
        vpnInterface?.close()
        vpnInterface = null
    }

    /**
     * Main packet processing loop.
     * Reads IP packets from TUN, inspects DNS queries, blocks or forwards.
     */
    private fun runPacketLoop() {
        val vpnFd = vpnInterface ?: return
        val inputStream = FileInputStream(vpnFd.fileDescriptor)
        val outputStream = FileOutputStream(vpnFd.fileDescriptor)

        val buffer = ByteArray(32767)

        while (isRunning) {
            try {
                val length = inputStream.read(buffer)
                if (length <= 0) {
                    Thread.sleep(10)
                    continue
                }

                val packet = buffer.copyOf(length)

                // Only process IPv4 UDP packets to port 53 (DNS)
                if (!isIPv4UdpDns(packet)) {
                    // Forward non-DNS packets normally (just write back to TUN)
                    // For a strict DNS-only blocker, we let non-DNS through
                    outputStream.write(packet, 0, length)
                    continue
                }

                val dnsPayload = extractDnsPayload(packet) ?: continue
                val queriedDomain = parseDnsQuery(dnsPayload)

                if (queriedDomain != null &&
                    isDomainBlocked(queriedDomain) &&
                    TimeUtils.isAnyScheduleActive(schedules)
                ) {
                    // Return NXDOMAIN response
                    val blockedResponse = buildBlockedDnsResponse(dnsPayload)
                    val responsePacket = wrapInUdpIpPacket(packet, blockedResponse)
                    outputStream.write(responsePacket)
                } else {
                    // Forward to real DNS
                    val realResponse = forwardDnsQuery(dnsPayload)
                    if (realResponse != null) {
                        val responsePacket = wrapInUdpIpPacket(packet, realResponse)
                        outputStream.write(responsePacket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) Thread.sleep(50)
            }
        }
    }

    private fun isDomainBlocked(domain: String): Boolean {
        val normalized = normalizeDomain(domain)
        return blockedDomains.any { blocked ->
            normalized == blocked || normalized.endsWith(".$blocked")
        }
    }

    private fun normalizeDomain(domain: String): String =
        domain.lowercase().trimEnd('.')

    /**
     * Checks if packet is IPv4, UDP, destination port 53
     */
    private fun isIPv4UdpDns(packet: ByteArray): Boolean {
        if (packet.size < 28) return false
        val ipVersion = (packet[0].toInt() and 0xFF) shr 4
        if (ipVersion != 4) return false
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return false // 17 = UDP
        val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
        if (packet.size < ipHeaderLen + 8) return false
        val dstPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or
                (packet[ipHeaderLen + 3].toInt() and 0xFF)
        return dstPort == 53
    }

    private fun extractDnsPayload(packet: ByteArray): ByteArray? {
        val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
        val udpHeaderLen = 8
        val payloadStart = ipHeaderLen + udpHeaderLen
        if (packet.size <= payloadStart) return null
        return packet.copyOfRange(payloadStart, packet.size)
    }

    /**
     * Parses the queried domain name from a DNS query packet.
     */
    private fun parseDnsQuery(dns: ByteArray): String? {
        return try {
            // DNS header is 12 bytes, question starts at byte 12
            var pos = 12
            val domainParts = mutableListOf<String>()
            while (pos < dns.size) {
                val len = dns[pos].toInt() and 0xFF
                if (len == 0) break
                pos++
                if (pos + len > dns.size) break
                domainParts.add(String(dns, pos, len))
                pos += len
            }
            if (domainParts.isEmpty()) null else domainParts.joinToString(".")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Builds a DNS NXDOMAIN response from the original query.
     */
    private fun buildBlockedDnsResponse(query: ByteArray): ByteArray {
        val response = query.copyOf()
        // Set QR bit (response), keep OPCODE, set RA, set RCODE = 3 (NXDOMAIN)
        response[2] = (0x81).toByte()  // QR=1, OPCODE=0, AA=0, TC=0, RD=1
        response[3] = (0x83).toByte()  // RA=1, RCODE=3 (NXDOMAIN)
        // Clear answer/authority/additional counts
        response[6] = 0; response[7] = 0
        response[8] = 0; response[9] = 0
        response[10] = 0; response[11] = 0
        return response
    }

    /**
     * Forwards DNS query to 8.8.8.8 and returns the response.
     */
    private fun forwardDnsQuery(query: ByteArray): ByteArray? {
        return try {
            val socket = DatagramSocket()
            protect(socket)
            socket.soTimeout = 3000
            val sendPacket = DatagramPacket(query, query.size, InetAddress.getByName("8.8.8.8"), 53)
            socket.send(sendPacket)
            val responseBuffer = ByteArray(4096)
            val receivePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(receivePacket)
            socket.close()
            responseBuffer.copyOf(receivePacket.length)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Wraps a DNS response payload back into a UDP/IP packet addressed
     * back to the original requester (swaps src/dst).
     */
    private fun wrapInUdpIpPacket(originalRequest: ByteArray, dnsResponse: ByteArray): ByteArray {
        val ipHeaderLen = (originalRequest[0].toInt() and 0x0F) * 4
        val udpPayloadLen = dnsResponse.size
        val udpTotalLen = 8 + udpPayloadLen
        val ipTotalLen = ipHeaderLen + udpTotalLen

        val result = ByteArray(ipTotalLen)

        // Copy original IP header, then swap src/dst addresses
        System.arraycopy(originalRequest, 0, result, 0, ipHeaderLen)
        // Swap source and destination IP
        System.arraycopy(originalRequest, 12, result, 16, 4) // orig src -> new dst
        System.arraycopy(originalRequest, 16, result, 12, 4) // orig dst -> new src
        // Update total length
        result[2] = (ipTotalLen shr 8).toByte()
        result[3] = (ipTotalLen and 0xFF).toByte()
        // Zero checksum (will be recalculated or ignored by kernel)
        result[10] = 0; result[11] = 0

        // UDP header: swap ports
        result[ipHeaderLen] = originalRequest[ipHeaderLen + 2]     // dst port -> src port
        result[ipHeaderLen + 1] = originalRequest[ipHeaderLen + 3]
        result[ipHeaderLen + 2] = originalRequest[ipHeaderLen]     // src port -> dst port
        result[ipHeaderLen + 3] = originalRequest[ipHeaderLen + 1]
        result[ipHeaderLen + 4] = (udpTotalLen shr 8).toByte()
        result[ipHeaderLen + 5] = (udpTotalLen and 0xFF).toByte()
        result[ipHeaderLen + 6] = 0; result[ipHeaderLen + 7] = 0 // UDP checksum (optional for IPv4)

        // DNS payload
        System.arraycopy(dnsResponse, 0, result, ipHeaderLen + 8, udpPayloadLen)

        // Recalculate IP checksum
        val checksum = calculateIpChecksum(result, ipHeaderLen)
        result[10] = (checksum shr 8).toByte()
        result[11] = (checksum and 0xFF).toByte()

        return result
    }

    private fun calculateIpChecksum(header: ByteArray, headerLen: Int): Int {
        var sum = 0
        var i = 0
        while (i < headerLen) {
            val word = ((header[i].toInt() and 0xFF) shl 8) or (header[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }

    override fun onRevoke() {
        stopVpn()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        scope.cancel()
    }
}