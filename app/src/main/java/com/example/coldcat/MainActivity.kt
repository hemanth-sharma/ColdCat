package com.example.coldcat

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coldcat.service.BlockEnforcementService
import com.example.coldcat.service.VpnBlockerService
import com.example.coldcat.ui.MainViewModel
import com.example.coldcat.ui.screen.*
import com.example.coldcat.ui.theme.*

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val NAV_ITEMS = listOf(
    NavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavItem("apps", "Apps", Icons.Filled.PhoneAndroid, Icons.Outlined.PhoneAndroid),
    NavItem("websites", "Sites", Icons.Filled.Language, Icons.Outlined.Language),
    NavItem("schedule", "Schedule", Icons.Filled.Schedule, Icons.Outlined.Schedule)
)

class MainActivity : ComponentActivity() {

    // Exposed so HomeScreen composable can read and react to VPN grant state
    val vpnGrantedState = mutableStateOf(false)

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            vpnGrantedState.value = true
            startVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            startForegroundService(Intent(this, BlockEnforcementService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        vpnGrantedState.value = (VpnService.prepare(this) == null)

        setContent {
            ColdCatTheme {
                ColdCatApp(
                    vpnGranted = vpnGrantedState,
                    onRequestVpnPermission = { requestVpnPermission() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vpnGrantedState.value = (VpnService.prepare(this) == null)
    }

    fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            vpnGrantedState.value = true
            startVpnService()
        }
    }

    fun startVpnService() {
        try {
            val intent = Intent(this, VpnBlockerService::class.java)
                .apply { action = VpnBlockerService.ACTION_START }
            startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun ColdCatApp(
    vpnGranted: MutableState<Boolean>,
    onRequestVpnPermission: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    var selectedRoute by remember { mutableStateOf("home") }

    Scaffold(
        containerColor = CatBlack,
        bottomBar = {
            ColdCatBottomNav(
                selectedRoute = selectedRoute,
                onNavigate = { selectedRoute = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedRoute) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    vpnGranted = vpnGranted.value,
                    onNavigate = { selectedRoute = it },
                    onRequestVpnPermission = onRequestVpnPermission
                )
                "apps"     -> AppsScreen(viewModel = viewModel)
                "websites" -> WebsitesScreen(viewModel = viewModel)
                "schedule" -> ScheduleScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ColdCatBottomNav(selectedRoute: String, onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CatBlack)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CatDarkSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NAV_ITEMS.forEach { item ->
                NavBarItem(item = item, isSelected = selectedRoute == item.route, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
private fun NavBarItem(item: NavItem, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) CatRedDim else androidx.compose.ui.graphics.Color.Transparent, label = "navBg"
    )
    val iconColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) CatRed else CatGrayDim, label = "navIcon"
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(bgColor).size(56.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label, tint = iconColor, modifier = Modifier.size(22.dp)
            )
            Text(
                text = item.label, color = iconColor, fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}