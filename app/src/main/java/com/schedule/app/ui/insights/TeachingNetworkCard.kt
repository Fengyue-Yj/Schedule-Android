package com.schedule.app.ui.insights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedule.app.teaching.TeachingStore
import com.schedule.app.ui.components.cardBackground
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.ui.theme.cardTitle

@Composable
fun TeachingNetworkCard(
    onNavigateToTeaching: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { TeachingStore.getInstance(context) }
    val isSignedIn by store.isSignedIn.collectAsState()
    val snapshot by store.snapshot.collectAsState()

    val subtitle = if (isSignedIn) {
        val unreadCount = snapshot.items.count { it.readKey !in snapshot.readKeys }
        "$unreadCount unread notices · assignments & materials"
    } else {
        "Connect notices, assignments and course files"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(cornerRadius = AppTheme.Radius.card)
            .clickable(onClick = onNavigateToTeaching),
        shape = RoundedCornerShape(AppTheme.Radius.card),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.Spacing.card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = AppTheme.colors.accent,
                modifier = Modifier.size(28.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "PKU Teaching Network",
                    style = AppTheme.typography.cardTitle,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
