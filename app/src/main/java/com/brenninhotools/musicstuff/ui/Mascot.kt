package com.brenninhotools.musicstuff.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun ImageVector.Builder.layer(data: String, fill: Color?, stroke: Color? = null, width: Float = 0f) {
    addPath(
        pathData = addPathNodes(data),
        fill = fill?.let(::SolidColor),
        stroke = stroke?.let(::SolidColor),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}

val MascotIcon: ImageVector by lazy {
    ImageVector.Builder(
        defaultWidth = 108.dp,
        defaultHeight = 108.dp,
        viewportWidth = 108f,
        viewportHeight = 108f,
    ).apply {
        addGroup(
            pivotX = 54f,
            pivotY = 54f,
            scaleX = 0.78f,
            scaleY = 0.78f,
            translationX = 1.6f,
            translationY = -0.6f,
        )
        layer(
            "M42.2,36.5C41.8,34 43,32.6 45,32.1L75,25.4C77.6,24.8 79.2,26 79.2,28.4L78.6,64C79,70 73,75.5 65.5,76.6C59,77.6 55.4,74 56.3,69.6C57.2,65 63,61.5 68,61C70,60.8 71.8,61 72.9,61.6L73.2,43.4L48,49L47.6,68C47.8,75 42,81 34.5,82.2C28,83.2 24.6,79.8 25.4,75.4C26.3,70.6 32,67 37,66.6C39,66.4 40.9,66.7 42,67.4Z",
            fill = Color(0xFFFFFFFF),
            stroke = Color(0xFFFFFFFF),
            width = 7.2f,
        )
        layer(
            "M42.2,36.5C41.8,34 43,32.6 45,32.1L75,25.4C77.6,24.8 79.2,26 79.2,28.4L78.6,64C79,70 73,75.5 65.5,76.6C59,77.6 55.4,74 56.3,69.6C57.2,65 63,61.5 68,61C70,60.8 71.8,61 72.9,61.6L73.2,43.4L48,49L47.6,68C47.8,75 42,81 34.5,82.2C28,83.2 24.6,79.8 25.4,75.4C26.3,70.6 32,67 37,66.6C39,66.4 40.9,66.7 42,67.4Z",
            fill = Color(0xFF1B0B3A),
            stroke = Color(0xFF1B0B3A),
            width = 3.6f,
        )
        layer(
            "M42.2,36.5C41.8,34 43,32.6 45,32.1L75,25.4C77.6,24.8 79.2,26 79.2,28.4L78.6,64C79,70 73,75.5 65.5,76.6C59,77.6 55.4,74 56.3,69.6C57.2,65 63,61.5 68,61C70,60.8 71.8,61 72.9,61.6L73.2,43.4L48,49L47.6,68C47.8,75 42,81 34.5,82.2C28,83.2 24.6,79.8 25.4,75.4C26.3,70.6 32,67 37,66.6C39,66.4 40.9,66.7 42,67.4Z",
            fill = Color(0xFFFFD23F),
        )
        layer(
            "M47.6,68C47.8,75 42,81 34.5,82.2C30.5,82.8 27.6,81.8 26.2,79.6C31.5,80.2 37.5,77.5 41,73.8C43,71.6 43.8,69.4 42.9,67.8L44.2,49.8L48,49Z",
            fill = Color(0xFFFF9A1F),
        )
        layer(
            "M78.6,64C79,70 73,75.5 65.5,76.6C61.5,77.2 58.6,76.2 57.2,74C62.5,74.6 68.5,71.9 72,68.2C74,66 74.8,63.8 73.9,62.2L74.4,43.2L79.1,37Z",
            fill = Color(0xFFFF9A1F),
        )
        layer(
            "M48,49L73.2,43.4L79.1,38L79.1,34.5L73.2,39.8L48,45.4Z",
            fill = Color(0xFFFF9A1F),
        )
        layer(
            "M46.5,34.2L52,33",
            fill = null,
            stroke = Color(0xFFFFF4B8),
            width = 1.8f,
        )
        layer(
            "M71,28.8L74.4,28",
            fill = null,
            stroke = Color(0xFFFFF4B8),
            width = 1.8f,
        )
        layer(
            "M31,72.8C32.6,71 35,69.8 37,69.6",
            fill = null,
            stroke = Color(0xFFFFF4B8),
            width = 1.8f,
        )
        layer(
            "M62,67.2C63.6,65.4 66,64.2 68,64",
            fill = null,
            stroke = Color(0xFFFFF4B8),
            width = 1.8f,
        )
        layer(
            "M42.2,36.5C41.8,34 43,32.6 45,32.1L75,25.4C77.6,24.8 79.2,26 79.2,28.4L78.6,64C79,70 73,75.5 65.5,76.6C59,77.6 55.4,74 56.3,69.6C57.2,65 63,61.5 68,61C70,60.8 71.8,61 72.9,61.6L73.2,43.4L48,49L47.6,68C47.8,75 42,81 34.5,82.2C28,83.2 24.6,79.8 25.4,75.4C26.3,70.6 32,67 37,66.6C39,66.4 40.9,66.7 42,67.4Z",
            fill = null,
            stroke = Color(0xFF1B0B3A),
            width = 2.2f,
        )
        layer(
            "M53.0,37a3.6,4.3 0 1 1 7.2,0a3.6,4.3 0 1 1 -7.2,0Z",
            fill = Color(0xFFFFFFFF),
            stroke = Color(0xFF1B0B3A),
            width = 1.3f,
        )
        layer(
            "M62.800000000000004,34.8a3.6,4.3 0 1 1 7.2,0a3.6,4.3 0 1 1 -7.2,0Z",
            fill = Color(0xFFFFFFFF),
            stroke = Color(0xFF1B0B3A),
            width = 1.3f,
        )
        layer(
            "M55.8,37.6a1.7,2.3 0 1 1 3.4,0a1.7,2.3 0 1 1 -3.4,0Z",
            fill = Color(0xFF1B0B3A),
        )
        layer(
            "M65.6,35.4a1.7,2.3 0 1 1 3.4,0a1.7,2.3 0 1 1 -3.4,0Z",
            fill = Color(0xFF1B0B3A),
        )
        layer(
            "M58.2,43.8C60.8,44.9 64.2,44.2 66,41.9",
            fill = null,
            stroke = Color(0xFF1B0B3A),
            width = 1.5f,
        )
        clearGroup()
        layer(
            "M30,38.2Q30.288,42.712 34.8,43Q30.288,43.288 30,47.8Q29.712,43.288 25.2,43Q29.712,42.712 30,38.2Z",
            fill = Color(0xFFFF4FA3),
            stroke = Color(0xFF1B0B3A),
            width = 1.5f,
        )
        layer(
            "M36,31.4Q36.156,33.844 38.6,34Q36.156,34.156 36,36.6Q35.844,34.156 33.4,34Q35.844,33.844 36,31.4Z",
            fill = Color(0xFF3DE3E0),
            stroke = Color(0xFF1B0B3A),
            width = 1.2f,
        )
        layer(
            "M76,72.8Q76.192,75.808 79.2,76Q76.192,76.192 76,79.2Q75.808,76.192 72.8,76Q75.808,75.808 76,72.8Z",
            fill = Color(0xFF3DE3E0),
            stroke = Color(0xFF1B0B3A),
            width = 1.3f,
        )
    }.build()
}
