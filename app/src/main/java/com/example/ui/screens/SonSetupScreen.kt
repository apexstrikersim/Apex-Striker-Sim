package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerEntity
import com.example.ui.CareerViewModel
import com.example.ui.theme.*

@Composable
fun SonSetupScreen(viewModel: CareerViewModel, father: PlayerEntity?) {
    if (father == null) return
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var sonName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("son_setup_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChildCare,
                    contentDescription = "Son",
                    tint = PitchGreen,
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = "GENERATIONAL LEGACY",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchGreen,
                    letterSpacing = 1.sp
                )

                Divider(color = BorderColor)

                Text(
                    text = "Your son will inherit starting stat advantages, scaled directly by your father's peak achievements and total trophies won!",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                OutlinedTextField(
                    value = sonName,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isLetter() || it == ' ' || it == '-' || it == '\'' }
                        if (filtered.length <= 24) sonName = filtered
                    },
                    label = { Text("Son's Name", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PitchGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("son_name_input")
                )

                Button(
                    onClick = {
                        if (sonName.isNotBlank()) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.createSon(sonName.trim())
                        }
                    },
                    enabled = sonName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("begin_son_legacy_button")
                ) {
                    Text(text = "BEGIN GENERATION ${father.generation + 1}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
