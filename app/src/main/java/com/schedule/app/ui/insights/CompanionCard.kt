package com.schedule.app.ui.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.ui.components.cardBackground
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.ui.theme.control
import com.schedule.app.util.CompanionDialogue
import com.schedule.app.util.UpcomingEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var touchSequence by remember { mutableIntStateOf(0) }
    var lastTouch by remember { mutableLongStateOf(0L) }
    var lastSpoken by remember { mutableStateOf<Long?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    
    val speechList = remember(events, nextStep) {
        CompanionDialogue.lines(events, nextStep)
    }
    var currentSpeech by remember { mutableStateOf(speechList.firstOrNull() ?: "今天也可以先迈出很小的一步。") }

    val coroutineScope = rememberCoroutineScope()

    fun speak(random: Boolean) {
        val lines = CompanionDialogue.lines(events, nextStep)
        val alternatives = lines.filter { it != currentSpeech }
        currentSpeech = if (random) {
            alternatives.randomOrNull() ?: lines.firstOrNull() ?: "今天也可以先迈出很小的一步。"
        } else {
            lines.firstOrNull() ?: "今天也可以先迈出很小的一步。"
        }
        lastSpoken = System.currentTimeMillis()
    }

    fun interact(action: CompanionReaction) {
        val now = System.currentTimeMillis()
        if (now - lastTouch < 180) return
        lastTouch = now
        isResting = action == CompanionReaction.REST
        reaction = action
        touchSequence++
        when (action) {
            CompanionReaction.TICKLE -> currentSpeech = "哈哈，好痒！稍微休息一下也很好。"
            CompanionReaction.REST -> currentSpeech = "靠着你休息一小会儿，真舒服。"
            CompanionReaction.WAKE -> currentSpeech = "醒啦，我继续陪着你。"
            else -> {
                if (touchSequence % 3 == 0) {
                    speak(random = true)
                } else {
                    currentSpeech = "收到摸摸，陪你慢慢把事情做好。"
                }
            }
        }
        lastSpoken = now
    }

    // Touch phase animations (matching iOS phaseAnimator)
    val touchOffsetY = remember { Animatable(0f) }
    val touchSway = remember { Animatable(0f) }
    val touchScaleX = remember { Animatable(1f) }
    val touchScaleY = remember { Animatable(1f) }

    LaunchedEffect(touchSequence) {
        if (touchSequence > 0) {
            val amount = if (reaction == CompanionReaction.TICKLE) 5.0f else 2.0f
            val bounceY = when {
                reaction == CompanionReaction.REST -> 3f
                reaction == CompanionReaction.WAKE -> -12f
                else -> -8f
            }

            // Phase 1 (120ms)
            launch { touchOffsetY.animateTo(bounceY, tween(120, easing = FastOutSlowInEasing)) }
            launch { touchSway.animateTo(-amount, tween(120, easing = FastOutSlowInEasing)) }
            launch { touchScaleX.animateTo(1.035f, tween(120, easing = FastOutSlowInEasing)) }
            launch { touchScaleY.animateTo(0.965f, tween(120, easing = FastOutSlowInEasing)) }
            delay(120)

            // Phase 2 (120ms)
            launch { touchOffsetY.animateTo(0f, tween(120, easing = FastOutSlowInEasing)) }
            launch { touchSway.animateTo(amount, tween(120, easing = FastOutSlowInEasing)) }
            launch { touchScaleX.animateTo(1f, tween(120, easing = FastOutSlowInEasing)) }
            launch { touchScaleY.animateTo(1f, tween(120, easing = FastOutSlowInEasing)) }
            delay(120)

            // Phase 3 (120ms)
            launch { touchOffsetY.animateTo(-3f, tween(120, easing = FastOutSlowInEasing)) }
            launch { touchSway.animateTo(-amount * 0.45f, tween(120, easing = FastOutSlowInEasing)) }
            delay(120)

            // Phase 4 (120ms)
            launch { touchOffsetY.animateTo(0f, tween(120, easing = FastOutSlowInEasing)) }
            launch { touchSway.animateTo(0f, tween(120, easing = FastOutSlowInEasing)) }
        }
    }

    // Return reaction to IDLE after 1.8s
    LaunchedEffect(touchSequence) {
        if (reaction != CompanionReaction.IDLE && !isResting) {
            delay(1800)
            reaction = CompanionReaction.IDLE
        }
    }

    // Auto speak loop every 35s
    LaunchedEffect(Unit) {
        while (true) {
            delay(35000)
            if (!isResting && CompanionDialogue.canSpeak(lastSpoken)) {
                speak(random = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(cornerRadius = AppTheme.Radius.hero)
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
                                touchSequence++
                                currentSpeech = "我是 ${char.displayName}，今天也一起慢慢来。"
                                lastSpoken = System.currentTimeMillis()
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

        // Stage (280dp)
        val stageHeight = 280.dp
        val density = LocalDensity.current
        val headBoundaryPx = with(density) { (stageHeight - CompanionArtworkDefaults.canvasSize * 0.43f).toPx() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stageHeight)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            AppTheme.colors.highlight.copy(alpha = if (isDark) 0.10f else 0.45f),
                            AppTheme.colors.accent.copy(alpha = 0.07f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (isResting) {
                                interact(CompanionReaction.WAKE)
                            } else {
                                val isHead = offset.y < headBoundaryPx
                                interact(if (isHead) CompanionReaction.PAT else CompanionReaction.TICKLE)
                            }
                        },
                        onLongPress = {
                            interact(if (isResting) CompanionReaction.WAKE else CompanionReaction.REST)
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Ground shadow ellipse (220dp x 44dp)
            val accentColor = AppTheme.colors.accent
            Canvas(
                modifier = Modifier
                    .size(width = 220.dp, height = 44.dp)
                    .offset(y = 4.dp)
            ) {
                drawOval(color = accentColor.copy(alpha = 0.12f))
            }

            // Decorative sparkle
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.55f),
                modifier = Modifier
                    .size(16.dp)
                    .offset(x = (-100).dp, y = (-168).dp)
            )

            // Decorative leaf
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(18.dp)
                    .offset(x = 116.dp, y = (-36).dp)
                    .graphicsLayer { rotationZ = 24f }
            )

            // Mascot artwork with animated transforms
            CompanionArtwork(
                character = character,
                pose = if (isResting) CompanionPose.REST else reaction.pose,
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .graphicsLayer {
                        translationY = with(density) { touchOffsetY.value.dp.toPx() }
                        scaleX = touchScaleX.value
                        scaleY = touchScaleY.value
                        rotationZ = touchSway.value
                        transformOrigin = TransformOrigin(0.5f, 1.0f)
                    }
            )
        }

        // Speech Bubble
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 55.dp)
                .background(
                    color = AppTheme.colors.subtleSurface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(13.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = currentSpeech,
                style = AppTheme.typography.control,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Upcoming events inside card
        UpcomingSummary(events = events, onSelect = onSelect)
    }
}
