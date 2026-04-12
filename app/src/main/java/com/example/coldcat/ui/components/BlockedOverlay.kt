package com.example.coldcat.ui.screen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coldcat.MainActivity
import com.example.coldcat.data.AppDatabase
import com.example.coldcat.util.AppUtils
import com.example.coldcat.util.TimeUtils
import kotlinx.coroutines.*

class BlockedOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        const val PACKAGE_MARKER = "coldcat.overlay.marker"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedPkg = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        val appName = AppUtils.getAppName(applicationContext, blockedPkg)

        setContent {
            BlockedScreen(
                appName = appName,
                onGoHome = {
                    // Navigate to home screen (ColdCat), not blocked app
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                    finish()
                }
            )
        }
    }

    override fun onBackPressed() {
        // Prevent back press from returning to blocked app
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

@Composable
fun BlockedScreen(
    appName: String,
    onGoHome: () -> Unit
) {
    val catEmojis = listOf("🐱", "😾", "🙀", "😼")
    var emojiIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1200)
            emojiIndex = (emojiIndex + 1) % catEmojis.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = catEmojis[emojiIndex],
                fontSize = 80.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BLOCKED",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF4444),
                letterSpacing = 8.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = appName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "is blocked during your focus session.",
                fontSize = 15.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Stay focused. You got this. 💪",
                fontSize = 14.sp,
                color = Color(0xFF555555),
                textAlign = TextAlign.Center,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF222222)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp)
            ) {
                Text(
                    text = "Go to Home",
                    color = Color(0xFF888888),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}