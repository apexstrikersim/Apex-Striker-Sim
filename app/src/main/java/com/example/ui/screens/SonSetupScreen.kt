package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.FictionalData
import com.example.data.PlayerEntity
import com.example.ui.CareerViewModel
import com.example.ui.theme.*

@Composable
fun SonSetupScreen(viewModel: CareerViewModel, father: PlayerEntity?) {
    if (father == null) return
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val fatherLastName = father.lastName.ifBlank {
        father.name.split(" ").drop(1).joinToString(" ").ifBlank { father.name }
    }
    var sonFirstName by remember { mutableStateOf("") }
    var sonLastName by remember { mutableStateOf(fatherLastName) }

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

                HorizontalDivider(color = BorderColor)

                Text(
                    text = "Your son will inherit starting stat advantages, scaled directly by your father's peak achievements and total trophies won!",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sonFirstName,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isLetter() || it == '-' || it == '\'' }
                            if (filtered.length <= 16) sonFirstName = filtered
                        },
                        label = { Text("First Name", color = TextSecondary) },
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
                            .weight(1f)
                            .testTag("son_first_name_input")
                    )

                    IconButton(
                        onClick = {
                            sonFirstName = FictionalData.generateRandomFirstName(father.academyCountry)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSlate)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .testTag("randomize_son_name_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Randomize Name",
                            tint = PitchGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = sonLastName,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isLetter() || it == '-' || it == '\'' || it == ' ' }
                        if (filtered.length <= 20) sonLastName = filtered
                    },
                    label = { Text("Last Name (Family)", color = TextSecondary) },
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

                val isSonValid = sonFirstName.trim().isNotBlank() && sonLastName.trim().isNotBlank()
                Button(
                    onClick = {
                        if (isSonValid) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            val fName = sonFirstName.trim()
                            val lName = sonLastName.trim()
                            viewModel.createSon(
                                sonName = "$fName $lName",
                                firstName = fName,
                                lastName = lName
                            )
                        }
                    },
                    enabled = isSonValid,
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
