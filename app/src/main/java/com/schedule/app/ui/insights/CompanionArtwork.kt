package com.schedule.app.ui.insights

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.schedule.app.R
import kotlin.math.sin

enum class CompanionCharacter(val displayName: String) {
    LULU("Lulu"),
    NAI("Nai")
}

enum class CompanionPose {
    IDLE, BLINK, PAT, LAUGH, REST, CHEER
}

enum class CompanionReaction {
    IDLE, PAT, TICKLE, REST, WAKE;

    val pose: CompanionPose
        get() = when (this) {
            IDLE -> CompanionPose.IDLE
            PAT -> CompanionPose.PAT
            TICKLE -> CompanionPose.LAUGH
            REST -> CompanionPose.REST
            WAKE -> CompanionPose.CHEER
        }
}

object CompanionArtworkDefaults {
    val canvasSize: Dp = 224.dp
}

fun getCompanionDrawable(character: CompanionCharacter, pose: CompanionPose): Int {
    return when (character) {
        CompanionCharacter.LULU -> when (pose) {
            CompanionPose.IDLE -> R.drawable.companion_lulu_idle
            CompanionPose.BLINK -> R.drawable.companion_lulu_blink
            CompanionPose.PAT -> R.drawable.companion_lulu_pat
            CompanionPose.LAUGH -> R.drawable.companion_lulu_laugh
            CompanionPose.CHEER -> R.drawable.companion_lulu_cheer
            CompanionPose.REST -> R.drawable.companion_lulu_rest
        }
        CompanionCharacter.NAI -> when (pose) {
            CompanionPose.IDLE -> R.drawable.companion_nai_idle
            CompanionPose.BLINK -> R.drawable.companion_nai_blink
            CompanionPose.PAT -> R.drawable.companion_nai_pat
            CompanionPose.LAUGH -> R.drawable.companion_nai_laugh
            CompanionPose.CHEER -> R.drawable.companion_nai_cheer
            CompanionPose.REST -> R.drawable.companion_nai_rest
        }
    }
}

/**
 * Native motion adds breathing and idle blink effect matching iOS CompanionArtwork.swift
 */
@Composable
fun CompanionArtwork(
    character: CompanionCharacter,
    pose: CompanionPose,
    modifier: Modifier = Modifier,
    canvasSize: Dp = CompanionArtworkDefaults.canvasSize
) {
    val time by produceState(initialValue = 0.0) {
        val startNano = System.nanoTime()
        while (true) {
            withFrameNanos { nowNano ->
                value = (nowNano - startNano) / 1_000_000_000.0
            }
        }
    }

    val breathPeriod = if (pose == CompanionPose.REST) 2.5 else 1.8
    val breath = sin(time * Math.PI / breathPeriod).toFloat()
    val isBlink = pose == CompanionPose.IDLE && ((time % 4.7) > 4.53)
    val displayPose = if (isBlink) CompanionPose.BLINK else pose
    val swayAngle = if (pose == CompanionPose.REST) 0f else (sin(time * Math.PI / 3.6) * 0.65).toFloat()

    val drawableId = getCompanionDrawable(character, displayPose)

    Image(
        painter = painterResource(id = drawableId),
        contentDescription = character.displayName,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(canvasSize)
            .graphicsLayer {
                scaleX = 1f - breath * 0.004f
                scaleY = 1f + breath * 0.008f
                rotationZ = swayAngle
                transformOrigin = TransformOrigin(0.5f, 1.0f)
            }
    )
}
