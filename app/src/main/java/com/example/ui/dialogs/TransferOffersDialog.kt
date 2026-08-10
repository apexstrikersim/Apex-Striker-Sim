package com.example.ui.dialogs

import com.example.ui.components.ClubCrestIcon

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransferOffer
import com.example.ui.CareerViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TransferOffersDialog(viewModel: CareerViewModel) {
    val offers by viewModel.transferOffers.collectAsStateWithLifecycle()
    var offerBeingReviewed by remember { mutableStateOf<TransferOffer?>(null) }

    Dialog(onDismissRequest = { viewModel.dismissTransferDialog() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PitchGreen, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "INCOMING TRANSFER DEALS", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PitchGreen)
                    IconButton(onClick = { viewModel.dismissTransferDialog() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = BorderColor)

                if (offers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No clubs are offering transfers right now.", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(offers) { offer ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = offer.clubName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PitchGreen)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SportsDarkBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = offer.clubReputation, fontSize = 11.sp, color = TrophyGold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "Deal Duration: ${offer.contractYears} Years", fontSize = 12.sp, color = TextPrimary)
                                    Text(text = "Season expectation: ${offer.targetGplusA} G+A", fontSize = 12.sp, color = TextPrimary)
                                    Text(text = "Rival: ${offer.rivalStrikerName} (${offer.rivalStrikerOvr} OVR)", fontSize = 11.sp, color = TextSecondary)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = { offerBeingReviewed = offer },
                                        colors = ButtonDefaults.buttonColors(containerColor = PitchGreen, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
                                    ) {
                                        Text(text = "REVIEW OFFER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    offerBeingReviewed?.let { offer ->
        ContractSigningDialog(
            offer = offer,
            playerName = viewModel.playerFlow.collectAsStateWithLifecycle().value?.name ?: "",
            onReject = {
                viewModel.rejectTransferOffer(offer)
                offerBeingReviewed = null
            },
            onSigned = {
                viewModel.acceptTransfer(offer)
                offerBeingReviewed = null
            }
        )
    }
}

@Composable
fun ClubCrestBadge(clubId: Int, clubName: String, size: Dp = 46.dp, modifier: Modifier = Modifier) {
    ClubCrestIcon(clubId = clubId, clubName = clubName, size = size, modifier = modifier)
}

@Composable
fun ContractSigningDialog(
    offer: TransferOffer,
    playerName: String,
    onReject: () -> Unit,
    onSigned: () -> Unit
) {
    var isSigning by remember { mutableStateOf(false) }
    var isSigned by remember { mutableStateOf(false) }
    val signatureProgress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    val paperColor = Color(0xFFF3EFE4)
    val inkColor = Color(0xFF1A1D2E)
    val goldRule = Color(0xFFB8985A)

    Dialog(
        onDismissRequest = { if (!isSigning) onReject() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .border(2.dp, goldRule, RoundedCornerShape(4.dp))
                .padding(4.dp)
                .border(1.dp, goldRule.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(4.dp)),
                colors = CardDefaults.cardColors(containerColor = paperColor),
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 26.dp, vertical = 22.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { if (!isSigning) onReject() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = inkColor.copy(alpha = 0.5f))
                        }
                    }

                    ClubCrestBadge(clubId = offer.clubId, clubName = offer.clubName, size = 46.dp)

                    Spacer(Modifier.height(6.dp))
                    Text("OFFICIAL CONTRACT", fontFamily = DisplayFontFamily, fontWeight = FontWeight.Black, fontSize = 22.sp, color = inkColor)
                    Text(offer.clubName, fontStyle = FontStyle.Italic, fontSize = 16.sp, color = inkColor.copy(alpha = 0.8f))
                    Text("${offer.clubReputation} CLUB", fontSize = 11.sp, letterSpacing = 1.5.sp, color = inkColor.copy(alpha = 0.55f))

                    Spacer(Modifier.height(14.dp))
                    Divider(color = goldRule.copy(alpha = 0.6f), thickness = 1.dp)
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "This document serves as a binding professional agreement between ${offer.clubName} (\"The Club\") and $playerName (\"The Player\").",
                        fontSize = 14.sp,
                        color = inkColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                            .border(1.dp, goldRule.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ContractRow("AGREEMENT LENGTH", "${offer.contractYears} YEARS", inkColor)
                        ContractRow("SEASON TARGET", "${offer.targetGplusA} G+A", inkColor)
                        ContractRow("KEY RIVAL", "${offer.rivalStrikerName} (${offer.rivalStrikerOvr} OVR)", inkColor)
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Signed on this day, by the aforementioned parties.", fontStyle = FontStyle.Italic, fontSize = 12.sp, color = inkColor.copy(alpha = 0.6f))
                    Spacer(Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Board of Directors",
                                fontFamily = SignatureFontFamily,
                                fontSize = 20.sp,
                                color = inkColor.copy(alpha = 0.85f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Divider(color = inkColor.copy(alpha = 0.3f), thickness = 1.dp)
                            Text("CLUB REPRESENTATIVE", fontSize = 9.sp, letterSpacing = 0.8.sp, color = inkColor.copy(alpha = 0.5f))
                        }

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable(enabled = !isSigning && !isSigned) {
                                        isSigning = true
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        coroutineScope.launch {
                                            signatureProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
                                            isSigned = true
                                            kotlinx.coroutines.delay(400)
                                            onSigned()
                                        }
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                if (signatureProgress.value == 0f) {
                                    Text("Tap to sign", fontSize = 11.sp, color = inkColor.copy(alpha = 0.4f))
                                } else {
                                    val visibleChars = (playerName.length * signatureProgress.value).toInt().coerceIn(0, playerName.length)
                                    Text(
                                        text = playerName.take(visibleChars),
                                        fontFamily = SignatureFontFamily,
                                        fontSize = 22.sp,
                                        color = inkColor
                                    )
                                }
                            }
                            Divider(color = inkColor.copy(alpha = 0.3f), thickness = 1.dp)
                            Text("PLAYER SIGNATURE", fontSize = 9.sp, letterSpacing = 0.8.sp, color = inkColor.copy(alpha = 0.5f))
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    OutlinedButton(
                        onClick = onReject,
                        enabled = !isSigning,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MutedRed)
                    ) {
                        Text("REJECT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContractRow(label: String, value: String, inkColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = inkColor.copy(alpha = 0.55f), letterSpacing = 0.5.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = inkColor)
    }
}
