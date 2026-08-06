package com.example.ui.tabs

import androidx.compose.runtime.Composable
import com.example.ui.CareerViewModel
import com.example.ui.SocialMediaScreen

@Composable
fun SocialFeedTab(viewModel: CareerViewModel) {
    SocialMediaScreen(viewModel = viewModel)
}
