package com.example.coldcat.ui.screen

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.coldcat.data.BlockedApp
import com.example.coldcat.ui.MainViewModel
import com.example.coldcat.ui.components.*
import com.example.coldcat.ui.theme.*
import com.example.coldcat.util.InstalledApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val blockedApps by viewModel.blockedApps.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val isBlockActive by viewModel.isBlockActive.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAppPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context)
    }

    val blockedPackages = blockedApps.map { it.packageName }.toSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CatBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Blocked Apps",
            color = CatWhite,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            letterSpacing = (-0.5).sp
        )

        // Lock banner
        if (isBlockActive) {
            ActiveBlockBanner()
        }

        // Add button
        Button(
            onClick = { showAppPicker = true },
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
            Text("Add App", fontWeight = FontWeight.Bold)
        }

        // Blocked apps list
        SectionHeader(title = "Blocked", count = blockedApps.size)

        if (blockedApps.isEmpty()) {
            EmptyState(
                emoji = "📱",
                message = "No apps blocked",
                subtitle = "Tap 'Add App' to select apps to block during your focus sessions."
            )
        } else {
            CatCard {
                blockedApps.forEachIndexed { idx, app ->
                    BlockedAppRow(
                        app = app,
                        icon = try {
                            context.packageManager.getApplicationIcon(app.packageName)
                        } catch (e: Exception) { null },
                        onRemove = { viewModel.removeBlockedApp(app) },
                        locked = isBlockActive
                    )
                    if (idx < blockedApps.size - 1) CatDivider()
                }
            }
        }
    }

    // App picker bottom sheet
    if (showAppPicker) {
        ModalBottomSheet(
            onDismissRequest = { showAppPicker = false },
            containerColor = CatDarkSurface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(CatBorder)
                )
            }
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Select Apps to Block",
                    color = CatWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...", color = CatGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CatRed,
                        unfocusedBorderColor = CatBorder,
                        focusedTextColor = CatWhite,
                        unfocusedTextColor = CatWhite,
                        cursorColor = CatRed,
                        focusedContainerColor = CatCard,
                        unfocusedContainerColor = CatCard
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CatGray)
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingApps) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CatRed)
                    }
                } else {
                    val filtered = installedApps.filter {
                        searchQuery.isBlank() ||
                                it.appName.contains(searchQuery, ignoreCase = true) ||
                                it.packageName.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppPickerRow(
                                app = app,
                                isBlocked = app.packageName in blockedPackages,
                                onToggle = { isAdding ->
                                    if (isAdding) {
                                        viewModel.addBlockedApp(
                                            BlockedApp(
                                                packageName = app.packageName,
                                                appName = app.appName
                                            )
                                        )
                                    } else {
                                        viewModel.removeBlockedApp(
                                            BlockedApp(
                                                packageName = app.packageName,
                                                appName = app.appName
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedAppRow(
    app: BlockedApp,
    icon: Drawable?,
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
        AppIconView(icon = icon, size = 40)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.appName, color = CatWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(text = app.packageName, color = CatGray, fontSize = 11.sp)
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

@Composable
private fun AppPickerRow(
    app: InstalledApp,
    isBlocked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isBlocked) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppIconView(icon = app.icon, size = 42)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.appName, color = CatWhite, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(text = app.packageName, color = CatGray, fontSize = 11.sp)
        }
        Checkbox(
            checked = isBlocked,
            onCheckedChange = { onToggle(it) },
            colors = CheckboxDefaults.colors(
                checkedColor = CatRed,
                uncheckedColor = CatGrayDim,
                checkmarkColor = CatWhite
            )
        )
    }
}

@Composable
fun AppIconView(icon: Drawable?, size: Int) {
    if (icon != null) {
        try {
            val bitmap = icon.toBitmap(size * 2, size * 2)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(size.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } catch (e: Exception) {
            DefaultAppIcon(size)
        }
    } else {
        DefaultAppIcon(size)
    }
}

@Composable
private fun DefaultAppIcon(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CatCard),
        contentAlignment = Alignment.Center
    ) {
        Text("📱", fontSize = (size / 2.2).sp)
    }
}