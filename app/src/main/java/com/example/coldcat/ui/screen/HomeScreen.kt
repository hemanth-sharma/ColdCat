package com.example.coldcat.ui.screen

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coldcat.service.AccessibilityBlockerService
import com.example.coldcat.service.VpnBlockerService
import com.example.coldcat.ui.MainViewModel
import com.example.coldcat.ui.components.*
import com.example.coldcat.ui.theme.*
import com.example.coldcat.util.TimeUtils

@Composable
fun HomeScreen(viewModel: MainViewModel, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val blockedApps by viewModel.blockedApps.collectAsState()
    val blockedWebsites by viewModel.blockedWebsites.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val isBlockActive by viewModel.isBlockActive.collectAsState()

    // Re-check permissions on every recompose (user may have granted them)
    var accessibilityGranted by remember { mutableStateOf(false) }
    var vpnGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        accessibilityGranted = isAccessibilityServiceEnabled(context)
        vpnGranted = VpnService.prepare(context) == null
    }

    // Re-check every time screen comes to focus
    DisposableEffect(Unit) {
        accessibilityGranted = isAccessibilityServiceEnabled(context)
        vpnGranted = VpnService.prepare(context) == null
        onDispose { }
    }

    // Pulsing animation for active state indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val statusColor by animateColorAsState(
        targetValue = if (isBlockActive) CatRed else CatGreen,
        label = "statusColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CatBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "🐱 ColdCat",
                    color = CatWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Digital focus enforcer",
                    color = CatGray,
                    fontSize = 13.sp
                )
            }
            // Live status dot
            Box(
                modifier = Modifier
                    .size(if (isBlockActive) (12 * pulseScale).dp else 12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
        }

        // ── Status Card ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isBlockActive) CatRedDim else CatCard)
                .border(
                    1.dp,
                    if (isBlockActive) CatRed.copy(alpha = 0.5f) else CatBorder,
                    RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isBlockActive) "🔒" else "✅",
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isBlockActive) "Block Active" else "No Active Block",
                    color = if (isBlockActive) CatRed else CatGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = if (isBlockActive) {
                        val mins = TimeUtils.minutesUntilBlockEnds(schedules)
                        if (mins > 60) "Ends in ${mins / 60}h ${mins % 60}m"
                        else if (mins > 0) "Ends in ${mins}m"
                        else "Ending soon..."
                    } else {
                        "All apps and websites are accessible"
                    },
                    color = CatGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // ── Quick Stats ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatChip(
                modifier = Modifier.weight(1f),
                icon = "📱",
                value = blockedApps.size.toString(),
                label = "Apps",
                onClick = { onNavigate("apps") }
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = "🌐",
                value = blockedWebsites.size.toString(),
                label = "Websites",
                onClick = { onNavigate("websites") }
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = "⏰",
                value = schedules.count { it.isEnabled }.toString(),
                label = "Schedules",
                onClick = { onNavigate("schedule") }
            )
        }

        // ── Permissions Setup ────────────────────────────────────
        SectionHeader(title = "Setup", modifier = Modifier.padding(top = 4.dp))

        PermissionCard(
            icon = "♿",
            title = "Accessibility Service",
            description = "Required for app blocking",
            buttonText = "Enable",
            isGranted = accessibilityGranted,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        )

        PermissionCard(
            icon = "🔒",
            title = "VPN Permission",
            description = "Required for website blocking",
            buttonText = "Enable",
            isGranted = vpnGranted,
            onClick = {
                val prepareIntent = VpnService.prepare(context)
                if (prepareIntent != null) {
                    // MainActivity handles this activity result
                    context.startActivity(prepareIntent.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } else {
                    vpnGranted = true
                }
            }
        )

        // ── Active Schedules ─────────────────────────────────────
        if (schedules.isNotEmpty()) {
            SectionHeader(title = "Active Schedules", count = schedules.size)
            CatCard {
                schedules.forEachIndexed { idx, schedule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (TimeUtils.isWithinSchedule(schedule)) CatRed
                                    else if (schedule.isEnabled) CatGreen
                                    else CatGrayDim
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = TimeUtils.formatSchedule(schedule),
                            color = CatWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (!schedule.isEnabled) {
                            Text(text = "OFF", color = CatGrayDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (idx < schedules.size - 1) CatDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Bottom nav clearance
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CatCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CatBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 22.sp)
            Text(
                text = value,
                color = CatWhite,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
            Text(
                text = label,
                color = CatGray,
                fontSize = 11.sp
            )
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabledServices.any {
        it.resolveInfo.serviceInfo.packageName == context.packageName
    }
}