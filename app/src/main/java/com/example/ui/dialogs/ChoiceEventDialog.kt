package com.example.ui.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.GameStateEntity
import com.example.ui.CareerViewModel
import com.example.ui.theme.*

@Composable
fun ChoiceEventDialog(viewModel: CareerViewModel, gameState: GameStateEntity) {
    val prompt = gameState.activeChoicePrompt ?: return
    val option1 = gameState.activeChoiceOption1 ?: "Option 1"
    val option2 = gameState.activeChoiceOption2 ?: "Option 2"
    val option3 = gameState.activeChoiceOption3

    Dialog(onDismissRequest = { /* Force decision, cannot dismiss */ }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, PitchGreen, RoundedCornerShape(20.dp))
                .testTag("choice_event_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡 CAREER DECISION",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PitchGreen,
                        letterSpacing = 1.sp
                    )
                }

                Divider(color = BorderColor)

                Text(
                    text = prompt,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.resolveChoice(1) }
                        .testTag("choice_option_1")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1️⃣ $option1",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PitchGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.resolveChoice(2) }
                        .testTag("choice_option_2")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2️⃣ $option2",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PitchGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (!option3.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSlate),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.resolveChoice(3) }
                            .testTag("choice_option_3")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "3️⃣ $option3",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PitchGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
