package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.*

import com.example.ui.components.bounceClick

@Composable
fun MainMenuScreen(viewModel: CareerViewModel) {
    var activeLegalDialog by remember { mutableStateOf<String?>(null) } // "TOS" or "PRIVACY"
    val hasActiveSave by viewModel.hasActiveSave.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SportsDarkBg,
                        Color(0xFF0F1419),
                        DarkBackground
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Identity Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Apex Striker Career Icon",
                    modifier = Modifier
                        .size(96.dp)
                        .background(Color(0xFF1E242B), RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .border(2.dp, PitchGreen, RoundedCornerShape(20.dp))
                )

                Text(
                    text = "APEX STRIKER CAREER",
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = PitchGreen,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Generational Football Career Simulator",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = SportsCardBg,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Text(
                        text = "v2.4.1",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrophyGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Central Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { viewModel.startNewCareerFlow() },
                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .bounceClick()
                ) {
                    Text("NEW CAREER", fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
                }

                Button(
                    onClick = { viewModel.continueLastCareer() },
                    enabled = hasActiveSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrophyGold,
                        contentColor = Color.Black,
                        disabledContainerColor = DarkSlate.copy(alpha = 0.5f),
                        disabledContentColor = TextSecondary.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .bounceClick()
                ) {
                    Text("CONTINUE", fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.openLoadSlotPicker() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PitchGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .bounceClick()
                ) {
                    Text("LOAD GAME", fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
                }
            }

            // Legal footer
            val legalText = buildAnnotatedString {
                append("By playing, you agree to our ")
                pushStringAnnotation(tag = "TOS", annotation = "terms")
                withStyle(SpanStyle(color = PitchGreen, textDecoration = TextDecoration.Underline)) {
                    append("Terms of Service")
                }
                pop()
                append(" and ")
                pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                withStyle(SpanStyle(color = PitchGreen, textDecoration = TextDecoration.Underline)) {
                    append("Privacy Policy")
                }
                pop()
            }

            ClickableText(
                text = legalText,
                style = TextStyle(color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center),
                onClick = { offset ->
                    legalText.getStringAnnotations(offset, offset).firstOrNull()?.let { annotation ->
                        if (annotation.tag == "TOS") {
                            activeLegalDialog = "TOS"
                        } else if (annotation.tag == "PRIVACY") {
                            activeLegalDialog = "PRIVACY"
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    activeLegalDialog?.let { type ->
        val title = if (type == "TOS") "Terms of Service" else "Privacy Policy"
        val assetFile = if (type == "TOS") "terms_of_service.txt" else "privacy_policy.txt"
        LegalTextDialog(
            title = title,
            assetFileName = assetFile,
            onDismiss = { activeLegalDialog = null }
        )
    }
}
