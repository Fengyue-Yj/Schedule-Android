package com.schedule.app.ui.insights

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.ui.components.cardBackground
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.util.CompanionDialogue
import com.schedule.app.util.UpcomingEvent
import kotlinx.coroutines.delay

enum class CompanionCharacter(val displayName: String) {
    LULU("Lulu"),
    NAI("Nai")
}

enum class CompanionReaction {
    IDLE, PAT, TICKLE, REST, WAKE
}

@Composable
fun CompanionCard(
    events: List<UpcomingEvent> = emptyList(),
    nextStep: String? = null,
    onSelect: (UpcomingEvent) -> Unit = {},
    onHide: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    var character by remember { mutableStateOf(CompanionCharacter.LULU) }
    var reaction by remember { mutableStateOf(CompanionReaction.IDLE) }
    var isResting by remember { mutableStateOf(false) }
    var touchCount by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    
    val speechList = remember(events, nextStep) {
        CompanionDialogue.lines(events, nextStep)
    }
    var currentSpeech by remember { mutableStateOf(speechList.firstOrNull() ?: "今天也可以先迈出很小的一步。") }

    // Idle breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // Bounce reaction animation
    val bounceAnim = remember { Animatable(0f) }
    LaunchedEffect(touchCount) {
        if (touchCount > 0) {
            bounceAnim.snapTo(0f)
            bounceAnim.animateTo(
                targetValue = -12f,
                animationSpec = tween(120, easing = EaseOutQuad)
            )
            bounceAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
    }

    // Reset reaction after short delay
    LaunchedEffect(reaction) {
        if (reaction != CompanionReaction.IDLE && !isResting) {
            delay(1800)
            reaction = CompanionReaction.IDLE
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(cornerRadius = AppTheme.Radius.hero),
        shape = RoundedCornerShape(AppTheme.Radius.hero),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = AppTheme.colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${character.displayName}’s Corner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "A little company, at your pace",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Companion Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        CompanionCharacter.entries.forEach { char ->
                            DropdownMenuItem(
                                text = { Text(char.displayName) },
                                onClick = {
                                    character = char
                                    isResting = false
                                    reaction = CompanionReaction.WAKE
                                    currentSpeech = "我是 ${char.displayName}，今天也一起慢慢来。"
                                    menuExpanded = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Hide Companion") },
                            onClick = {
                                menuExpanded = false
                                onHide()
                            }
                        )
                    }
                }
            }

            // Interactive Stage (280dp)
            val stageGradient = Brush.linearGradient(
                colors = listOf(
                    AppTheme.colors.highlight.copy(alpha = if (isDark) 0.15f else 0.45f),
                    AppTheme.colors.accent.copy(alpha = 0.08f)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(stageGradient, RoundedCornerShape(22.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                touchCount++
                                if (isResting) {
                                    isResting = false
                                    reaction = CompanionReaction.WAKE
                                    currentSpeech = "醒啦，我继续陪着你。"
                                } else {
                                    val isHead = offset.y < 130
                                    if (isHead) {
                                        reaction = CompanionReaction.PAT
                                        currentSpeech = if (touchCount % 3 == 0) {
                                            speechList.randomOrNull() ?: "收到摸摸，陪你慢慢把事情做好。"
                                        } else {
                                            "收到摸摸，陪你慢慢把事情做好。"
                                        }
                                    } else {
                                        reaction = CompanionReaction.TICKLE
                                        currentSpeech = "哈哈，好痒！稍微休息一下也很好。"
                                    }
                                }
                            },
                            onLongPress = {
                                touchCount++
                                if (isResting) {
                                    isResting = false
                                    reaction = CompanionReaction.WAKE
                                    currentSpeech = "醒啦，我继续陪着你。"
                                } else {
                                    isResting = true
                                    reaction = CompanionReaction.REST
                                    currentSpeech = "靠着你休息一小会儿，真舒服。"
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Ground shadow ellipse
                Canvas(modifier = Modifier.size(200.dp, 40.dp).align(Alignment.BottomCenter).offset(y = (-16).dp)) {
                    drawOval(color = Color(0x1F298A5C))
                }

                // Companion Cute Mascot Artwork Canvas
                val mainColor = if (character == CompanionCharacter.LULU) {
                    if (isDark) Color(0xFF6EBD94) else Color(0xFF3D9169)
                } else {
                    if (isDark) Color(0xFFFFB74D) else Color(0xFFFFA726)
                }

                Canvas(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            translationY = bounceAnim.value
                            scaleX = if (isResting) 1f else breathScale
                            scaleY = if (isResting) 0.96f else breathScale
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f + 10f

                    // Ears
                    if (character == CompanionCharacter.LULU) {
                        // Cat-like ears
                        drawCircle(color = mainColor, radius = 24f, center = Offset(cx - 46f, cy - 54f))
                        drawCircle(color = Color(0xFFFFCDD2), radius = 14f, center = Offset(cx - 46f, cy - 54f))
                        drawCircle(color = mainColor, radius = 24f, center = Offset(cx + 46f, cy - 54f))
                        drawCircle(color = Color(0xFFFFCDD2), radius = 14f, center = Offset(cx + 46f, cy - 54f))
                    } else {
                        // Round bear ears
                        drawCircle(color = mainColor, radius = 26f, center = Offset(cx - 50f, cy - 50f))
                        drawCircle(color = Color(0xFFFFE0B2), radius = 14f, center = Offset(cx - 50f, cy - 50f))
                        drawCircle(color = mainColor, radius = 26f, center = Offset(cx + 50f, cy - 50f))
                        drawCircle(color = Color(0xFFFFE0B2), radius = 14f, center = Offset(cx + 50f, cy - 50f))
                    }

                    // Main round body
                    drawCircle(color = mainColor, radius = 64f, center = Offset(cx, cy))

                    // Belly patch
                    drawCircle(color = Color.White.copy(alpha = 0.35f), radius = 34f, center = Offset(cx, cy + 18f))

                    // Face: eyes & mouth based on state
                    if (isResting || reaction == CompanionReaction.REST) {
                        // Sleeping eyes: gentle curved lines (- -)
                        drawLine(color = Color(0xFF1B3D2F), start = Offset(cx - 28f, cy - 6f), end = Offset(cx - 14f, cy - 6f), strokeWidth = 5f)
                        drawLine(color = Color(0xFF1B3D2F), start = Offset(cx + 14f, cy - 6f), end = Offset(cx + 28f, cy - 6f), strokeWidth = 5f)
                        // Sleeping smile
                        drawCircle(color = Color(0xFF1B3D2F), radius = 3f, center = Offset(cx, cy + 8f))
                    } else if (reaction == CompanionReaction.TICKLE) {
                        // Happy squint eyes (> <)
                        drawLine(color = Color(0xFF1B3D2F), start = Offset(cx - 28f, cy - 12f), end = Offset(cx - 16f, cy - 6f), strokeWidth = 5f)
                        drawLine(color = Color(0xFF1B3D2F), start = Offset(cx - 28f, cy), end = Offset(cx - 16f, cy - 6f), strokeWidth = 5f)
                        drawLine(color = Color(0xFF1B3D2F), start = Offset(cx + 28f, cy - 12f), end = Offset(cx + 16f, cy - 6f), strokeWidth = 5f)
                        drawLine(color = Color(0xFF1B3D2F), start = Offset(cx + 28f, cy), end = Offset(cx + 16f, cy - 6f), strokeWidth = 5f)
                        // Laughing open mouth
                        drawArc(
                            color = Color(0xFFE57373),
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(cx - 12f, cy + 4f),
                            size = androidx.compose.ui.geometry.Size(24f, 18f)
                        )
                    } else if (reaction == CompanionReaction.PAT || reaction == CompanionReaction.WAKE) {
                        // Wide happy eyes
                        drawCircle(color = Color(0xFF1B3D2F), radius = 8f, center = Offset(cx - 22f, cy - 8f))
                        drawCircle(color = Color.White, radius = 3f, center = Offset(cx - 24f, cy - 10f))
                        drawCircle(color = Color(0xFF1B3D2F), radius = 8f, center = Offset(cx + 22f, cy - 8f))
                        drawCircle(color = Color.White, radius = 3f, center = Offset(cx + 20f, cy - 10f))
                        // Cute smile
                        drawArc(
                            color = Color(0xFF1B3D2F),
                            startAngle = 20f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(cx - 10f, cy + 2f),
                            size = androidx.compose.ui.geometry.Size(20f, 12f),
                            style = Stroke(width = 4f)
                        )
                    } else {
                        // Standard idle eyes with shine
                        drawCircle(color = Color(0xFF1B3D2F), radius = 7f, center = Offset(cx - 22f, cy - 8f))
                        drawCircle(color = Color.White, radius = 2.5f, center = Offset(cx - 24f, cy - 10f))
                        drawCircle(color = Color(0xFF1B3D2F), radius = 7f, center = Offset(cx + 22f, cy - 8f))
                        drawCircle(color = Color.White, radius = 2.5f, center = Offset(cx + 20f, cy - 10f))
                        // Gentle smile
                        drawArc(
                            color = Color(0xFF1B3D2F),
                            startAngle = 20f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(cx - 8f, cy + 4f),
                            size = androidx.compose.ui.geometry.Size(16f, 10f),
                            style = Stroke(width = 3.5f)
                        )
                    }

                    // Blush cheeks
                    drawCircle(color = Color(0x44FF4081), radius = 9f, center = Offset(cx - 38f, cy + 4f))
                    drawCircle(color = Color(0x44FF4081), radius = 9f, center = Offset(cx + 38f, cy + 4f))
                }
            }

            // Speech Bubble
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = currentSpeech,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Upcoming events inside card
            UpcomingSummary(events = events, onSelect = onSelect)
        }
    }
}
