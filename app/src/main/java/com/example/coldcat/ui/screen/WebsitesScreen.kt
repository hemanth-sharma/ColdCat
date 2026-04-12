package com.example.coldcat.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coldcat.data.BlockedWebsite
import com.example.coldcat.ui.MainViewModel
import com.example.coldcat.ui.components.*
import com.example.coldcat.ui.theme.*

// Popular sites for quick-add
private val QUICK_ADD_SITES = listOf(
    "youtube.com", "reddit.com", "twitter.com", "instagram.com",
    "facebook.com", "tiktok.com", "netflix.com", "twitch.tv"
)

@Composable
fun WebsitesScreen(viewModel: MainViewModel) {
    val blockedWebsites by viewModel.blockedWebsites.collectAsState()
    val isBlockActive by viewModel.isBlockActive.collectAsState()

    var inputDomain by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf("") }

    val blockedDomains = blockedWebsites.map { it.domain }.toSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CatBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Blocked Websites",
            color = CatWhite,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            letterSpacing = (-0.5).sp
        )

        // Lock banner
        if (isBlockActive) {
            ActiveBlockBanner()
        }

        // Input field
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = inputDomain,
                onValueChange = {
                    inputDomain = it
                    inputError = ""
                },
                placeholder = { Text("e.g. youtube.com", color = CatGray) },
                label = { Text("Domain", color = CatGray) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBlockActive,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CatRed,
                    unfocusedBorderColor = CatBorder,
                    focusedTextColor = CatWhite,
                    unfocusedTextColor = CatWhite,
                    cursorColor = CatRed,
                    focusedContainerColor = CatCard,
                    unfocusedContainerColor = CatCard,
                    disabledBorderColor = CatGrayDim,
                    focusedLabelColor = CatRed,
                    unfocusedLabelColor = CatGray
                ),
                isError = inputError.isNotEmpty(),
                supportingText = if (inputError.isNotEmpty()) {
                    { Text(inputError, color = CatRed) }
                } else null,
                leadingIcon = {
                    Icon(Icons.Default.Language, contentDescription = null, tint = CatGray)
                },
                singleLine = true
            )

            Button(
                onClick = {
                    val normalized = normalizeDomain(inputDomain)
                    when {
                        normalized.isBlank() -> inputError = "Please enter a domain"
                        !isValidDomain(normalized) -> inputError = "Invalid domain format"
                        normalized in blockedDomains -> inputError = "Domain already blocked"
                        else -> {
                            viewModel.addBlockedWebsite(normalized)
                            inputDomain = ""
                            inputError = ""
                        }
                    }
                },
                enabled = !isBlockActive,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CatRed,
                    disabledContainerColor = CatGrayDim
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Block Domain", fontWeight = FontWeight.Bold)
            }
        }

        // Quick-add chips
        if (!isBlockActive) {
            SectionHeader(title = "Quick Add")
            QuickAddChips(
                sites = QUICK_ADD_SITES,
                blockedDomains = blockedDomains,
                onAdd = { viewModel.addBlockedWebsite(it) },
                onRemove = { domain ->
                    blockedWebsites.find { it.domain == domain }?.let {
                        viewModel.removeBlockedWebsite(it)
                    }
                }
            )
        }

        // Blocked sites list
        SectionHeader(title = "Blocked Domains", count = blockedWebsites.size)

        if (blockedWebsites.isEmpty()) {
            EmptyState(
                emoji = "🌐",
                message = "No websites blocked",
                subtitle = "Add domains above or use Quick Add for popular sites."
            )
        } else {
            CatCard {
                blockedWebsites.forEachIndexed { idx, site ->
                    BlockedSiteRow(
                        site = site,
                        onRemove = { viewModel.removeBlockedWebsite(site) },
                        locked = isBlockActive
                    )
                    if (idx < blockedWebsites.size - 1) CatDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun QuickAddChips(
    sites: List<String>,
    blockedDomains: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    // Wrap chips in rows
    val chunked = sites.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { site ->
                    val isAdded = site in blockedDomains
                    FilterChip(
                        selected = isAdded,
                        onClick = {
                            if (isAdded) onRemove(site) else onAdd(site)
                        },
                        label = {
                            Text(
                                text = site,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CatRedDim,
                            selectedLabelColor = CatWhite,
                            containerColor = CatCard,
                            labelColor = CatGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isAdded,
                            selectedBorderColor = CatRed.copy(alpha = 0.5f),
                            borderColor = CatBorder
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedSiteRow(
    site: BlockedWebsite,
    onRemove: () -> Unit,
    locked: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CatRedDim),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🌐", fontSize = 16.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = site.domain,
                color = CatWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = "and all subdomains",
                color = CatGray,
                fontSize = 11.sp
            )
        }

        if (!locked) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = CatGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Icon(Icons.Default.Lock, contentDescription = null, tint = CatGrayDim, modifier = Modifier.size(16.dp))
        }
    }
}

private fun normalizeDomain(input: String): String {
    return input.lowercase()
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')
        .split("/")[0]
        .trim()
}

private fun isValidDomain(domain: String): Boolean {
    // Basic check: has at least one dot, no spaces, valid chars
    return domain.contains(".") &&
            !domain.contains(" ") &&
            domain.length > 3 &&
            domain.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9\\-._]+[a-zA-Z0-9]$"))
}