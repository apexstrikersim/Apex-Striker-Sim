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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
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
import com.example.data.FaceDescriptor
import com.example.data.FictionalData
import com.example.data.flagEmoji
import com.example.data.nationFlagEmoji
import com.example.ui.CareerViewModel
import com.example.ui.components.JerseyNumberIcon
import com.example.ui.components.RepeatingStepperButton
import com.example.ui.theme.*

private val LEAGUE_ACADEMY_COUNTRIES = listOf("England", "Spain", "France", "Germany", "Italy")
fun randomAcademyCountry(): String = LEAGUE_ACADEMY_COUNTRIES.random()

@Composable
fun SetupScreen(viewModel: CareerViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedBirthCountry by remember { mutableStateOf("England") }
    var birthCountrySearchQuery by remember { mutableStateOf("") }
    var isBirthCountryDropdownExpanded by remember { mutableStateOf(false) }

    val filteredNations = remember(birthCountrySearchQuery) {
        val alphabeticalNations = com.example.data.ALL_NATIONS.sortedBy { it.name }
        if (birthCountrySearchQuery.isBlank()) alphabeticalNations
        else alphabeticalNations.filter { it.name.contains(birthCountrySearchQuery, ignoreCase = true) }
    }

    var preferredFoot by remember { mutableStateOf("Right") }
    var squadNumber by remember { mutableIntStateOf(9) }
    var backgroundStory by remember { mutableStateOf("Street Cages") }
    var isFirstNameFocused by remember { mutableStateOf(false) }
    var isLastNameFocused by remember { mutableStateOf(false) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLAYER IDENTITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = PitchGreen
                    )
                    IconButton(
                        onClick = {
                            val randomCountry = randomAcademyCountry()
                            firstName = FictionalData.generateRandomFirstName(randomCountry)
                            lastName = FictionalData.generateRandomLastName(randomCountry)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSlate)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .testTag("randomize_name_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Randomize Name",
                            tint = PitchGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // First Name and Last Name inputs row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // First Name input
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSlate)
                            .border(
                                width = 1.dp,
                                color = if (isFirstNameFocused || firstName.isNotEmpty()) PitchGreen else BorderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = firstName,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isLetter() || it == '-' || it == '\'' }
                                    if (filtered.length <= 16) firstName = filtered
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
                                    .fillMaxWidth()
                                    .onFocusChanged { isFirstNameFocused = it.isFocused }
                                    .testTag("first_name_input"),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (firstName.isEmpty()) {
                                            Text(
                                                text = "First Name",
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

                    // Last Name input
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSlate)
                            .border(
                                width = 1.dp,
                                color = if (isLastNameFocused || lastName.isNotEmpty()) PitchGreen else BorderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = lastName,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isLetter() || it == '-' || it == '\'' || it == ' ' }
                                    if (filtered.length <= 20) lastName = filtered
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
                                    .fillMaxWidth()
                                    .onFocusChanged { isLastNameFocused = it.isFocused }
                                    .testTag("last_name_input"),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (lastName.isEmpty()) {
                                            Text(
                                                text = "Last Name",
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
                    text = "Select your birth country",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSlate)
                        .border(1.dp, if (isBirthCountryDropdownExpanded) PitchGreen else BorderColor, RoundedCornerShape(12.dp))
                        .clickable { isBirthCountryDropdownExpanded = !isBirthCountryDropdownExpanded }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("birth_country_picker"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BIRTH COUNTRY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${nationFlagEmoji(selectedBirthCountry)} $selectedBirthCountry",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Icon(
                            imageVector = if (isBirthCountryDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Country Dropdown",
                            tint = PitchGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (isBirthCountryDropdownExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSlate)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search Input
                        OutlinedTextField(
                            value = birthCountrySearchQuery,
                            onValueChange = { birthCountrySearchQuery = it },
                            placeholder = { Text("Search 200 nations...", fontSize = 13.sp, color = TextSecondary) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = PitchGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (birthCountrySearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { birthCountrySearchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PitchGreen,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = PitchGreen
                            ),
                            textStyle = TextStyle(fontSize = 14.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("birth_country_search_input")
                        )

                        // Scrollable list
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (filteredNations.isEmpty()) {
                                    Text(
                                        text = "No nations found",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                } else {
                                    filteredNations.forEach { nation ->
                                        val isChosen = nation.name == selectedBirthCountry
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isChosen) PitchGreen.copy(alpha = 0.15f) else Color.Transparent)
                                                .clickable {
                                                    selectedBirthCountry = nation.name
                                                    isBirthCountryDropdownExpanded = false
                                                    birthCountrySearchQuery = ""
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${nation.flagEmoji} ${nation.name}",
                                                fontSize = 14.sp,
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isChosen) PitchGreen else TextPrimary
                                            )
                                            Text(
                                                text = "${nation.code} · ${nation.tier.name}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
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
        val isNameValid = firstName.trim().isNotBlank() && lastName.trim().isNotBlank()
        Button(
            onClick = {
                if (isNameValid) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val fName = firstName.trim()
                    val lName = lastName.trim()
                    val fullName = "$fName $lName"
                    val assignedAcademyCountry = randomAcademyCountry()
                    val generatedFace = FaceDescriptor.random(getCountryCode(assignedAcademyCountry)).serialize()
                    viewModel.createCharacter(
                        name = fullName,
                        birth = selectedBirthCountry,
                        academy = assignedAcademyCountry,
                        preferredFoot = preferredFoot,
                        squadNumber = squadNumber,
                        backgroundStory = backgroundStory,
                        faceDescriptor = generatedFace,
                        firstName = fName,
                        lastName = lName
                    )
                }
            },
            enabled = isNameValid,
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
