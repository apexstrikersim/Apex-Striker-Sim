package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerEntity
import com.example.data.YouthAcademyEntity
import com.example.ui.theme.*

@Composable
fun StreetClubTab(onOpenTraining: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = PitchGreen,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Grassroots Unaffiliated Player",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You do not belong to an official club or academy yet. Practice individual drills to boost your physical and technical attributes.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onOpenTraining,
                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)
                ) {
                    Text("Practice Drills", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun YouthClubTab(
    player: PlayerEntity,
    academies: List<YouthAcademyEntity>,
    onOpenTraining: () -> Unit
) {
    val myAcademy = academies.find { it.id == player.currentAcademyId }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PitchGreen.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "CURRENT ACADEMY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchGreen,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = myAcademy?.academyName ?: "Youth Academy",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Parent Club Tier: ${myAcademy?.parentReputation ?: "Pro"} Academy",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Academy Rival:", fontSize = 13.sp, color = TextSecondary)
                    Text("${myAcademy?.youthRivalName ?: "Rivals"} (${myAcademy?.youthRivalOvr ?: 60} OVR)", fontSize = 13.sp, color = GoldStar, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onOpenTraining,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)
        ) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Youth Development Drills", color = DarkBackground, fontWeight = FontWeight.Bold)
        }
    }
}
