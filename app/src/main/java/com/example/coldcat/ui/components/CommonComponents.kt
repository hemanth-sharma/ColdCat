package com.example.coldcat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coldcat.ui.theme.*

/**
 * Banner shown at the top of all settings screens when a block is active.
 * Prevents the user from making changes.
 */
@Composable
fun ActiveBlockBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CatRedDim)
            .border(1.dp, CatRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = CatRed,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = "Block is Active",
                color = CatWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Settings are locked until the block period ends.",
                color = CatGray,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Section header with optional count badge.
 */
@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title.uppercase(),
            color = CatGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CatCard)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = CatGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Empty state placeholder.
 */
@Composable
fun EmptyState(
    emoji: String,
    message: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = CatWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = CatGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/**
 * Card container with standard ColdCat styling.
 */
@Composable
fun CatCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CatCard)
            .border(1.dp, CatBorder, RoundedCornerShape(16.dp))
            .padding(0.dp),
        content = content
    )
}

/**
 * Standard divider with ColdCat styling.
 */
@Composable
fun CatDivider() {
    HorizontalDivider(
        color = CatBorder,
        thickness = 1.dp
    )
}

/**
 * Permission setup card shown when accessibility / VPN is not granted.
 */
@Composable
fun PermissionCard(
    icon: String,
    title: String,
    description: String,
    buttonText: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isGranted) CatGreenDim else CatCard)
            .border(
                1.dp,
                if (isGranted) CatGreen.copy(alpha = 0.4f) else CatBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = icon, fontSize = 32.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CatWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = description,
                color = CatGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (isGranted) {
            Text(text = "✓", color = CatGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        } else {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = CatRed),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(text = buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}