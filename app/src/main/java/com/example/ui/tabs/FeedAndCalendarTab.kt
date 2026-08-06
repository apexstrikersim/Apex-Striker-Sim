package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CareerViewModel
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.SportsDarkBg
import com.example.ui.theme.TextSecondary

@Composable
fun FeedAndCalendarTab(viewModel: CareerViewModel) {
    var showCalendar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SportsDarkBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSlate)
                .padding(2.dp)
        ) {
            listOf("FEED" to false, "CALENDAR" to true).forEach { (label, isCalVal) ->
                val selected = showCalendar == isCalVal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) PitchGreen else Color.Transparent)
                        .clickable { showCalendar = isCalVal },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) SportsDarkBg else TextSecondary
                    )
                }
            }
        }

        if (showCalendar) {
            CalendarTab(viewModel = viewModel)
        } else {
            SocialFeedTab(viewModel = viewModel)
        }
    }
}
