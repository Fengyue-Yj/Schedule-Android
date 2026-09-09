package com.schedule.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.models.CourseEntity

@Composable
fun CourseBlock(course: CourseEntity, isDark: Boolean) {
    val baseColor = course.displayColor(isDark)
    val opacity = if (isDark) 0.95f else 0.85f
    val bgColor = baseColor.copy(alpha = opacity)
    val borderColor = Color.White.copy(alpha = if (isDark) 0.22f else 0.5f)
    val cornerRadius = 10.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .background(color = bgColor, shape = RoundedCornerShape(cornerRadius))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(cornerRadius))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = course.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color.White else Color.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
        if (course.classroom.isNotBlank()) {
            Text(
                text = course.classroom,
                fontSize = 10.sp,
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
