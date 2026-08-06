package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CareerViewModel
import com.example.ui.components.CountryFlagIcon
import com.example.ui.components.JerseyNumberIcon
import com.example.ui.components.RepeatingStepperButton
import com.example.ui.theme.*

@Composable
fun SetupScreen(viewModel: CareerViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf("") }
    var selectedBirthCountry by remember { mutableStateOf("England") }
    var preferredFoot by remember { mutableStateOf("Right") }
    var squadNumber by remember { mutableIntStateOf(9) }
    var backgroundStory by remember { mutableStateOf("Street Cages") }
    var isInputFocused by remember { mutableStateOf(false) }

    val countries = listOf("England", "Spain", "France", "Germany", "Italy")
    val feet = listOf("Left", "Right", "Both")
    val stories = listOf("Street Cages", "School Team", "Family Club")

    fun getCountryCode(country: String): String {
        return when (country) {
            "England" -> "ENG"
            "Spain" -> "ESP"
            "France" -> "FRA"
            "Germany" -> "GER"
            "Italy" -> "ITA"
            else -> country.take(3).uppercase()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SportsDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("setup_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.returnToMainMenu()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Main Menu",
                    tint = PitchGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "NEW CAREER",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(40.dp))
        }

        // 2. HEADER
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SportsCardBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = "Sports Soccer",
                    tint = PitchGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CREATE YOUR PLAYER",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DisplayFontFamily,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Every legend starts somewhere",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // 3. PLAYER IDENTITY CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PLAYER IDENTITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = PitchGreen
                )

                // Name input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSlate)
                        .border(
                            width = 1.dp,
                            color = if (isInputFocused || name.isNotEmpty()) PitchGreen else BorderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Person Icon",
                            tint = if (isInputFocused || name.isNotEmpty()) PitchGreen else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = name,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isLetter() || it == ' ' || it == '-' || it == '\'' }
                                if (filtered.length <= 24) name = filtered
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Words
                            ),
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isInputFocused = it.isFocused }
                                .testTag("name_input"),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (name.isEmpty()) {
                                        Text(
                                            text = "Character Name",
                                            color = TextSecondary,
                                            style = TextStyle(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                // Foot & Squad Number Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Foot
                    Column(
                        modifier = Modifier.weight(0.52f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "PREFERRED FOOT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSlate)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            feet.forEach { foot ->
                                val isSel = preferredFoot == foot
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) PitchGreen else Color.Transparent)
                                        .clickable { preferredFoot = foot },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = foot,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) SportsDarkBg else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Squad Number (Jersey Picker)
                    Column(
                        modifier = Modifier.weight(0.48f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "SQUAD NUMBER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            RepeatingStepperButton(
                                label = "-",
                                color = PitchGreen,
                                onStep = { if (squadNumber > 1) squadNumber-- },
                                modifier = Modifier.background(DarkSlate, CircleShape),
                                size = 32.dp
                            )
                            JerseyNumberIcon(
                                number = squadNumber,
                                modifier = Modifier.size(width = 52.dp, height = 58.dp)
                            )
                            RepeatingStepperButton(
                                label = "+",
                                color = PitchGreen,
                                onStep = { if (squadNumber < 99) squadNumber++ },
                                modifier = Modifier.background(DarkSlate, CircleShape),
                                size = 32.dp
                            )
                        }
                    }
                }
            }
        }

        // 4. NATIONALITY & ORIGIN CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "NATIONALITY & ORIGIN",
                    style = MaterialTheme.typography.labelSmall,
                    color = PitchGreen
                )
                Text(
                    text = "Select your heritage & birthplace",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    countries.forEach { country ->
                        val isSelected = selectedBirthCountry == country
                        val code = getCountryCode(country)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PitchGreen else DarkSlate)
                                .then(
                                    if (!isSelected) Modifier.border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                    else Modifier
                                )
                                .clickable { selectedBirthCountry = country },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CountryFlagIcon(
                                    country = country,
                                    modifier = Modifier.size(width = 24.dp, height = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = code,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SportsDarkBg else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. BACKGROUND STORY CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "WHERE DID YOU LEARN THE GAME?",
                    style = MaterialTheme.typography.labelSmall,
                    color = PitchGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stories.forEach { story ->
                        val isSel = backgroundStory == story
                        val icon = when (story) {
                            "Street Cages" -> Icons.Default.LocationCity
                            "School Team" -> Icons.Default.School
                            else -> Icons.Default.Home
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) PitchGreen else DarkSlate)
                                .clickable { backgroundStory = story },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = story,
                                    tint = if (isSel) SportsDarkBg else TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = story,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) SportsDarkBg else TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                val talentHintText = when (backgroundStory) {
                    "Street Cages" -> "Raw, unpolished, and fearless — your talent ceiling is unknown until scouts see you play."
                    "School Team" -> "Structured fundamentals give you a head start — your true potential shows once training begins."
                    else -> "Guided from a young age — your technique is sharp, but your ceiling is still a mystery."
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PitchGreen.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Hint Icon",
                        tint = PitchGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = talentHintText,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // 6. CREATE CAREER BUTTON
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.createCharacter(
                        name = name.trim(),
                        birth = selectedBirthCountry,
                        academy = selectedBirthCountry,
                        preferredFoot = preferredFoot,
                        squadNumber = squadNumber,
                        backgroundStory = backgroundStory
                    )
                }
            },
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PitchGreen,
                contentColor = SportsDarkBg,
                disabledContainerColor = PitchGreen.copy(alpha = 0.3f),
                disabledContentColor = SportsDarkBg.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("create_career_button")
        ) {
            Text(
                text = "CREATE CAREER",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
