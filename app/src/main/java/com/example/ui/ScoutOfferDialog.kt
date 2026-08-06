package com.example.ui

import com.example.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.YouthScoutOffer
import com.example.data.YouthToSeniorOffer
import kotlinx.coroutines.launch

@Composable
fun YouthScoutOfferDialog(
    offers: List<YouthScoutOffer>,
    onAccept: (YouthScoutOffer) -> Unit,
    onReject: (YouthScoutOffer) -> Unit,
    onDeclineAll: () -> Unit = {},
    onDismiss: () -> Unit
) {
    if (offers.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var offerList by remember(offers) { mutableStateOf(offers) }
    val pagerState = rememberPagerState(pageCount = { offerList.size })
    val coroutineScope = rememberCoroutineScope()

    if (offerList.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PitchGreen.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ACADEMY SCOUT OFFER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchGreen,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Youth Academy Invitations",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (offerList.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val currentOffer = offerList.getOrNull(page) ?: return@HorizontalPager

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardBackground)
                                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentOffer.academyName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PitchGreen.copy(alpha = 0.2f))
                                        .border(1.dp, PitchGreen, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = currentOffer.parentClubReputation,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PitchGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Required OVR: ${currentOffer.minScoutOvr}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Rival: ${currentOffer.youthRivalName} (${currentOffer.youthRivalOvr} OVR)",
                                    fontSize = 12.sp,
                                    color = GoldStar
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = currentOffer.scoutReport,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onReject(currentOffer)
                                        val updated = offerList.filter { it.academyId != currentOffer.academyId }
                                        offerList = updated
                                        if (updated.isEmpty()) {
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                    Text("Decline", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        onAccept(currentOffer)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                    Text("Accept", fontWeight = FontWeight.Bold, color = DarkBackground)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Swiping & Pager Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Offer", tint = if (pagerState.currentPage > 0) PitchGreen else TextSecondary.copy(alpha = 0.3f))
                        }

                        Text(
                            text = "Offer ${pagerState.currentPage + 1} of ${offerList.size} (Swipe to compare)",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < offerList.size - 1) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                }
                            },
                            enabled = pagerState.currentPage < offerList.size - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Offer", tint = if (pagerState.currentPage < offerList.size - 1) PitchGreen else TextSecondary.copy(alpha = 0.3f))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary dismiss all / close button
                    OutlinedButton(
                        onClick = onDeclineAll,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text("Decline All & Close", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun SeniorScoutOfferDialog(
    offers: List<YouthToSeniorOffer>,
    onAccept: (YouthToSeniorOffer) -> Unit,
    onReject: (YouthToSeniorOffer) -> Unit,
    onDeclineAll: () -> Unit = {},
    onDismiss: () -> Unit
) {
    if (offers.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var offerList by remember(offers) { mutableStateOf(offers) }
    val pagerState = rememberPagerState(pageCount = { offerList.size })
    val coroutineScope = rememberCoroutineScope()

    if (offerList.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldStar.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PROFESSIONAL CONTRACT OFFER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldStar,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Turn Professional (Age 16+)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (offerList.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val currentOffer = offerList.getOrNull(page) ?: return@HorizontalPager

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardBackground)
                                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentOffer.clubName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GoldStar.copy(alpha = 0.2f))
                                        .border(1.dp, GoldStar, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = currentOffer.clubReputation,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldStar
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Contract: ${currentOffer.contractYears} yrs",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Target: ${currentOffer.targetGplusA} G+A",
                                    fontSize = 12.sp,
                                    color = PitchGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = currentOffer.scoutReport,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onReject(currentOffer)
                                        val updated = offerList.filter { it.clubId != currentOffer.clubId }
                                        offerList = updated
                                        if (updated.isEmpty()) {
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                    Text("Decline", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        onAccept(currentOffer)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldStar)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                    Text("Sign Pro", fontWeight = FontWeight.Bold, color = DarkBackground)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Swiping & Pager Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Offer", tint = if (pagerState.currentPage > 0) GoldStar else TextSecondary.copy(alpha = 0.3f))
                        }

                        Text(
                            text = "Offer ${pagerState.currentPage + 1} of ${offerList.size} (Swipe to compare)",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < offerList.size - 1) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                }
                            },
                            enabled = pagerState.currentPage < offerList.size - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Offer", tint = if (pagerState.currentPage < offerList.size - 1) GoldStar else TextSecondary.copy(alpha = 0.3f))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary dismiss all / close button
                    OutlinedButton(
                        onClick = onDeclineAll,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text("Decline All & Close", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
