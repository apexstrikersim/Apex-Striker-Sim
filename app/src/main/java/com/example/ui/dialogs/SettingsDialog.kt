package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameStateEntity
import com.example.ui.CareerViewModel
import com.example.ui.components.bounceClick
import com.example.ui.components.AnimatedSaveButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SettingsDialog(viewModel: CareerViewModel, gameState: GameStateEntity?) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var clickCount by remember { mutableStateOf(0) }
    var showDevTools by remember(gameState?.isDevMode) { mutableStateOf(gameState?.isDevMode ?: false) }
    var showPinPrompt by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf<String?>(if (gameState?.isDevMode == true) "dev" else null) }
    var expandedDevPlayerStats by remember { mutableStateOf(false) }
    var expandedDevSocialStats by remember { mutableStateOf(false) }

    val playerState by viewModel.playerFlow.collectAsStateWithLifecycle()
    val player = playerState
    val isGodModeActive = player?.isGodMode == true
    var showGodModeConfirmationDialog by remember { mutableStateOf(false) }

    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsStateWithLifecycle()
    val audioVolume by viewModel.audioVolume.collectAsStateWithLifecycle()

    // Dev values
    var finVal by remember { mutableStateOf("80") }
    var pacVal by remember { mutableStateOf("80") }
    var pasVal by remember { mutableStateOf("80") }
    var phyVal by remember { mutableStateOf("80") }
    var techVal by remember { mutableStateOf("80") }

    // Social sliders (test features)
    var moraleSlider by remember(player?.morale) { mutableStateOf(player?.morale?.toFloat() ?: 50f) }
    var fanRepSlider by remember(player?.fanReputation) { mutableStateOf(player?.fanReputation?.toFloat() ?: 50f) }
    var managerTrustSlider by remember(player?.managerTrust) { mutableStateOf(player?.managerTrust?.toFloat() ?: 50f) }
    var rivalRelSlider by remember(player?.rivalRelationship) { mutableStateOf(player?.rivalRelationship?.toFloat() ?: 50f) }

    var simSeasonsSlider by remember(isGodModeActive) { mutableFloatStateOf(if (isGodModeActive) 10f else 1f) }

    Dialog(onDismissRequest = {
        keyboardController?.hide()
        focusManager.clearFocus()
        viewModel.showSettings(false)
    }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .border(1.dp, PitchGreen, RoundedCornerShape(20.dp))
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "GAME OPTIONS", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PitchGreen)
                        IconButton(onClick = { viewModel.showSettings(false) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                    Divider(color = BorderColor)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // SECTION 1: GENERAL
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (expandedSection == "general") PitchGreen.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedSection = if (expandedSection == "general") null else "general"
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GENERAL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (expandedSection == "general") PitchGreen else TextPrimary
                            )
                            Icon(
                                imageVector = if (expandedSection == "general") Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expandedSection == "general") "Collapse" else "Expand",
                                tint = if (expandedSection == "general") PitchGreen else TextSecondary
                            )
                        }

                        if (expandedSection == "general") {
                            Divider(color = BorderColor.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Auto-save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Saves automatically on advancement", fontSize = 11.sp, color = TextSecondary)
                                }
                                Switch(
                                    checked = gameState?.autoSave ?: true,
                                    onCheckedChange = { viewModel.toggleAutoSave(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PitchGreen)
                                )
                            }
                            Divider(color = BorderColor.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.showSettings(false)
                                    viewModel.openLoadSlotPicker()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PitchGreen),
                                border = BorderStroke(1.dp, PitchGreen)
                            ) {
                                Text("SWITCH SAVE SLOT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.returnToMainMenu()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TrophyGold),
                                border = BorderStroke(1.dp, TrophyGold)
                            ) {
                                Text("MAIN MENU", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // SECTION 2: AUDIO & HAPTICS
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (expandedSection == "audio") PitchGreen.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedSection = if (expandedSection == "audio") null else "audio"
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AUDIO & HAPTICS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (expandedSection == "audio") PitchGreen else TextPrimary
                            )
                            Icon(
                                imageVector = if (expandedSection == "audio") Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expandedSection == "audio") "Collapse" else "Expand",
                                tint = if (expandedSection == "audio") PitchGreen else TextSecondary
                            )
                        }

                        if (expandedSection == "audio") {
                            Divider(color = BorderColor.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Mute Audio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Mute Audio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Mute all sound effects and whistles", fontSize = 11.sp, color = TextSecondary)
                                }
                                Switch(
                                    checked = isMuted,
                                    onCheckedChange = { viewModel.toggleMute(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PitchGreen)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))

                            // Haptic Feedback
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Haptic Feedback", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Vibrations on tab switch and scout reports", fontSize = 11.sp, color = TextSecondary)
                                }
                                Switch(
                                    checked = isHapticEnabled,
                                    onCheckedChange = { viewModel.toggleHaptic(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PitchGreen)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Audio Volume
                            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Audio Volume", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "${(audioVolume * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PitchGreen)
                                }
                                Slider(
                                    value = audioVolume,
                                    onValueChange = { viewModel.setAudioVolume(it) },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PitchGreen,
                                        activeTrackColor = PitchGreen,
                                        inactiveTrackColor = DarkSlate
                                    )
                                )
                            }
                        }
                    }
                }

                // SECTION 3: SAVE DATA
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (expandedSection == "save") PitchGreen.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedSection = if (expandedSection == "save") null else "save"
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SAVE DATA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (expandedSection == "save") PitchGreen else TextPrimary
                            )
                            Icon(
                                imageVector = if (expandedSection == "save") Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expandedSection == "save") "Collapse" else "Expand",
                                tint = if (expandedSection == "save") PitchGreen else TextSecondary
                            )
                        }

                        if (expandedSection == "save") {
                            Divider(color = BorderColor.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Manual Save
                            AnimatedSaveButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("save_game_button"),
                                onSaveClick = {
                                    viewModel.syncSlotMetadata()
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Reset Career
                            var isConfirmingReset by remember { mutableStateOf(false) }
                            var countdown by remember { mutableStateOf(3) }

                            LaunchedEffect(isConfirmingReset) {
                                if (isConfirmingReset) {
                                    while (countdown > 0) {
                                        delay(1000)
                                        countdown--
                                    }
                                } else {
                                    countdown = 3
                                }
                            }

                            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                if (!isConfirmingReset) {
                                    Button(
                                        onClick = { isConfirmingReset = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MutedRed.copy(alpha = 0.2f), contentColor = MutedRed),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("reset_career_button")
                                    ) {
                                        Text(text = "RESET CAREER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.resetGame() },
                                        enabled = countdown == 0,
                                        colors = ButtonDefaults.buttonColors(containerColor = MutedRed, contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Text(
                                            text = if (countdown > 0) "HOLD CONFIRM ($countdown)" else "CONFIRM RESET",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 4: DEVELOPER TOOLS
                if (showDevTools) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (expandedSection == "dev") TrophyGold.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedSection = if (expandedSection == "dev") null else "dev"
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🛠️ DEVELOPER TOOLS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (expandedSection == "dev") TrophyGold else TrophyGold.copy(alpha = 0.8f)
                                )
                                Icon(
                                    imageVector = if (expandedSection == "dev") Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (expandedSection == "dev") "Collapse" else "Expand",
                                    tint = if (expandedSection == "dev") TrophyGold else TextSecondary
                                )
                            }

                            if (expandedSection == "dev") {
                                Divider(color = BorderColor.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    // God Mode Action Button
                                    Button(
                                        onClick = { showGodModeConfirmationDialog = true },
                                        enabled = !isGodModeActive,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = TrophyGold,
                                            contentColor = Color.Black,
                                            disabledContainerColor = TrophyGold.copy(alpha = 0.4f),
                                            disabledContentColor = Color.Black.copy(alpha = 0.6f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("activate_god_mode_button")
                                    ) {
                                        Text(
                                            text = if (isGodModeActive) "⚡ GOD MODE ACTIVE" else "⚡ ACTIVATE GOD MODE",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    if (showGodModeConfirmationDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showGodModeConfirmationDialog = false },
                                            title = { Text("Activate God Mode?", fontWeight = FontWeight.Bold, color = TrophyGold) },
                                            text = {
                                                Text(
                                                    "God Mode bypasses retirement at age 41, halts age-based stat decline, and uncaps season simulation up to 100 seasons per tap. This action cannot be undone for this career save.",
                                                    fontSize = 13.sp,
                                                    color = TextPrimary
                                                )
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        viewModel.activateGodMode()
                                                        showGodModeConfirmationDialog = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = TrophyGold, contentColor = Color.Black)
                                                ) {
                                                    Text("ENABLE GOD MODE", fontWeight = FontWeight.Bold)
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showGodModeConfirmationDialog = false }) {
                                                    Text("CANCEL", color = TextSecondary)
                                                }
                                            },
                                            containerColor = SportsCardBg
                                        )
                                    }

                                    // Auto-simulate slider and button at developer tools root level
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "Simulate Seasons", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(
                                                text = "${simSeasonsSlider.toInt()} Season${if (simSeasonsSlider.toInt() > 1) "s" else ""}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TrophyGold
                                            )
                                        }
                                        Slider(
                                            value = simSeasonsSlider,
                                            onValueChange = { simSeasonsSlider = it },
                                            valueRange = if (isGodModeActive) 10f..100f else 1f..10f,
                                            steps = 8,
                                            colors = SliderDefaults.colors(
                                                thumbColor = TrophyGold,
                                                activeTrackColor = TrophyGold
                                            ),
                                            modifier = Modifier.testTag("dev_auto_sim_slider")
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { viewModel.devSimulateSeason(simSeasonsSlider.toInt()) },
                                            colors = ButtonDefaults.buttonColors(containerColor = TrophyGold, contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("dev_auto_sim_season_button")
                                        ) {
                                            Text(
                                                text = "⚡ AUTO-SIMULATE ${simSeasonsSlider.toInt()} SEASON${if (simSeasonsSlider.toInt() > 1) "S" else ""}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Sub-section A: Player Stats
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, if (expandedDevPlayerStats) TrophyGold.copy(alpha = 0.4f) else BorderColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expandedDevPlayerStats = !expandedDevPlayerStats }
                                                .padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Player Stats",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (expandedDevPlayerStats) TrophyGold else TextPrimary
                                            )
                                            Icon(
                                                imageVector = if (expandedDevPlayerStats) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (expandedDevPlayerStats) "Collapse Player Stats" else "Expand Player Stats",
                                                tint = if (expandedDevPlayerStats) TrophyGold else TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        if (expandedDevPlayerStats) {
                                            Divider(color = BorderColor.copy(alpha = 0.3f))
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    OutlinedTextField(
                                                        value = finVal,
                                                        onValueChange = { input -> finVal = input.filter { it.isDigit() }.take(3) },
                                                        label = { Text("FIN") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f), singleLine = true
                                                    )
                                                    OutlinedTextField(
                                                        value = pacVal,
                                                        onValueChange = { input -> pacVal = input.filter { it.isDigit() }.take(3) },
                                                        label = { Text("PAC") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f), singleLine = true
                                                    )
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    OutlinedTextField(
                                                        value = pasVal,
                                                        onValueChange = { input -> pasVal = input.filter { it.isDigit() }.take(3) },
                                                        label = { Text("PAS") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f), singleLine = true
                                                    )
                                                    OutlinedTextField(
                                                        value = phyVal,
                                                        onValueChange = { input -> phyVal = input.filter { it.isDigit() }.take(3) },
                                                        label = { Text("PHY") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f), singleLine = true
                                                    )
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    OutlinedTextField(
                                                        value = techVal,
                                                        onValueChange = { input -> techVal = input.filter { it.isDigit() }.take(3) },
                                                        label = { Text("TEC") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f), singleLine = true
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Button(
                                                    onClick = {
                                                        val f = finVal.toIntOrNull() ?: 80
                                                        val pa = pacVal.toIntOrNull() ?: 80
                                                        val ps = pasVal.toIntOrNull() ?: 80
                                                        val ph = phyVal.toIntOrNull() ?: 80
                                                        val t = techVal.toIntOrNull() ?: 80
                                                        viewModel.devUpdateStats(f, pa, ps, ph, t)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = TrophyGold, contentColor = Color.Black),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(text = "FORCE SET STATS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }

                                    // Sub-section B: Social Stats
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, if (expandedDevSocialStats) PitchGreen.copy(alpha = 0.4f) else BorderColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expandedDevSocialStats = !expandedDevSocialStats }
                                                .padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Social Stats",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (expandedDevSocialStats) PitchGreen else TextPrimary
                                            )
                                            Icon(
                                                imageVector = if (expandedDevSocialStats) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (expandedDevSocialStats) "Collapse Social Stats" else "Expand Social Stats",
                                                tint = if (expandedDevSocialStats) PitchGreen else TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        if (expandedDevSocialStats) {
                                            Divider(color = BorderColor.copy(alpha = 0.3f))
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(text = "Morale", fontSize = 11.sp, color = TextPrimary)
                                                            Text(text = "${moraleSlider.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PitchGreen)
                                                        }
                                                        Slider(
                                                            value = moraleSlider,
                                                            onValueChange = { moraleSlider = it },
                                                            valueRange = 0f..100f,
                                                            modifier = Modifier.testTag("dev_morale_slider")
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(text = "Fan Rep", fontSize = 11.sp, color = TextPrimary)
                                                            Text(text = "${fanRepSlider.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TrophyGold)
                                                        }
                                                        Slider(
                                                            value = fanRepSlider,
                                                            onValueChange = { fanRepSlider = it },
                                                            valueRange = 0f..100f,
                                                            modifier = Modifier.testTag("dev_fan_reputation_slider")
                                                        )
                                                    }
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(text = "Manager Trust", fontSize = 11.sp, color = TextPrimary)
                                                            Text(text = "${managerTrustSlider.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SubLineYellow)
                                                        }
                                                        Slider(
                                                            value = managerTrustSlider,
                                                            onValueChange = { managerTrustSlider = it },
                                                            valueRange = 0f..100f,
                                                            modifier = Modifier.testTag("dev_manager_trust_slider")
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(text = "Rival Rel", fontSize = 11.sp, color = TextPrimary)
                                                            Text(text = "${rivalRelSlider.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (rivalRelSlider < 30f) MutedRed else PitchGreen)
                                                        }
                                                        Slider(
                                                            value = rivalRelSlider,
                                                            onValueChange = { rivalRelSlider = it },
                                                            valueRange = 0f..100f,
                                                            modifier = Modifier.testTag("dev_rival_relationship_slider")
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Button(
                                                    onClick = {
                                                        viewModel.devUpdateSocialStats(
                                                            morale = moraleSlider.toInt(),
                                                            fanRep = fanRepSlider.toInt(),
                                                            managerTrust = managerTrustSlider.toInt(),
                                                            rivalRel = rivalRelSlider.toInt()
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreen, contentColor = Color.Black),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().testTag("dev_save_social_button")
                                                ) {
                                                    Text(text = "FORCE SET SOCIAL STATS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Version string
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Version 2.4.0 (Striker Edition)",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (showDevTools) {
                                    expandedSection = if (expandedSection == "dev") null else "dev"
                                } else {
                                    clickCount++
                                    if (clickCount >= 5) {
                                        showPinPrompt = true
                                    }
                                }
                            }
                    )
                }
            }
        }
    }

    if (showPinPrompt) {
        AlertDialog(
            onDismissRequest = {
                keyboardController?.hide()
                focusManager.clearFocus()
                showPinPrompt = false
                clickCount = 0
                pinInput = ""
                pinError = false
            },
            title = {
                Text(text = "Developer PIN Required", color = PitchGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(text = "Enter 4-digit PIN to access Dev Tools:", fontSize = 13.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= 4) pinInput = digitsOnly
                        },
                        singleLine = true,
                        isError = pinError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        placeholder = { Text("••••", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PitchGreen,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("dev_pin_input")
                    )
                    if (pinError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Incorrect PIN.", color = MutedRed, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput == "2323") {
                            showDevTools = true
                            viewModel.setDevMode(true)
                            expandedSection = "dev"
                            showPinPrompt = false
                            pinInput = ""
                            pinError = false
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreen, contentColor = Color.Black)
                ) {
                    Text("UNLOCK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinPrompt = false
                        clickCount = 0
                        pinInput = ""
                        pinError = false
                    }
                ) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = SportsCardBg
        )
    }
}
