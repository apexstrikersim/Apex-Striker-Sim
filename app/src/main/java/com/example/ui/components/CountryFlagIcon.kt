package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CountryFlagIcon(country: String, modifier: Modifier = Modifier) {
    val flagShape = RoundedCornerShape(2.dp)
    val flagModifier = modifier
        .size(width = 20.dp, height = 14.dp)
        .clip(flagShape)
        .border(0.5.dp, Color.White.copy(alpha = 0.25f), flagShape)

    Canvas(modifier = flagModifier) {
        when (country) {
            "England" -> {
                drawRect(color = Color.White)
                val crossThickness = size.height * 0.28f
                drawRect(
                    color = Color(0xFFCE1124),
                    topLeft = Offset(0f, size.height / 2 - crossThickness / 2),
                    size = Size(size.width, crossThickness)
                )
                drawRect(
                    color = Color(0xFFCE1124),
                    topLeft = Offset(size.width / 2 - crossThickness / 2, 0f),
                    size = Size(crossThickness, size.height)
                )
            }
            "Spain" -> {
                val stripeH = size.height / 4f
                drawRect(
                    color = Color(0xFFC60B1E),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, stripeH)
                )
                drawRect(
                    color = Color(0xFFFFC107),
                    topLeft = Offset(0f, stripeH),
                    size = Size(size.width, stripeH * 2f)
                )
                drawRect(
                    color = Color(0xFFC60B1E),
                    topLeft = Offset(0f, stripeH * 3f),
                    size = Size(size.width, stripeH)
                )
            }
            "France" -> {
                val stripeW = size.width / 3f
                drawRect(
                    color = Color(0xFF002395),
                    topLeft = Offset(0f, 0f),
                    size = Size(stripeW, size.height)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(stripeW, 0f),
                    size = Size(stripeW, size.height)
                )
                drawRect(
                    color = Color(0xFFED2939),
                    topLeft = Offset(stripeW * 2f, 0f),
                    size = Size(stripeW, size.height)
                )
            }
            "Germany" -> {
                val stripeH = size.height / 3f
                drawRect(
                    color = Color(0xFF000000),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, stripeH)
                )
                drawRect(
                    color = Color(0xFFDD0000),
                    topLeft = Offset(0f, stripeH),
                    size = Size(size.width, stripeH)
                )
                drawRect(
                    color = Color(0xFFFFCC00),
                    topLeft = Offset(0f, stripeH * 2f),
                    size = Size(size.width, stripeH)
                )
            }
            "Italy" -> {
                val stripeW = size.width / 3f
                drawRect(
                    color = Color(0xFF009246),
                    topLeft = Offset(0f, 0f),
                    size = Size(stripeW, size.height)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(stripeW, 0f),
                    size = Size(stripeW, size.height)
                )
                drawRect(
                    color = Color(0xFFCE2B37),
                    topLeft = Offset(stripeW * 2f, 0f),
                    size = Size(stripeW, size.height)
                )
            }
            else -> {
                drawRect(color = Color.Gray)
            }
        }
    }
}

@Composable
fun EnglandFlagIcon(modifier: Modifier = Modifier) {
    CountryFlagIcon(country = "England", modifier = modifier)
}
