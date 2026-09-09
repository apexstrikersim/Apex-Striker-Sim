package com.example.ui.dialogs

import androidx.compose.runtime.Composable
import com.example.data.PlayerEntity
import com.example.ui.CareerViewModel
import com.example.ui.YouthDrillType
import com.example.ui.YouthTrainingDialog

@Composable
fun TrainingDialog(
    viewModel: CareerViewModel,
    player: PlayerEntity,
    onDismiss: () -> Unit
) {
    YouthTrainingDialog(
        player = player,
        title = "SENIOR TRAINING DRILLS",
        subtitle = "Select Training Drill",
        onCompleteTraining = { drillType, qualityScore ->
            val grade = when {
                qualityScore >= 0.85f -> "S"
                qualityScore >= 0.65f -> "A"
                qualityScore >= 0.40f -> "B"
                else -> "C"
            }
            val focus = when (drillType) {
                YouthDrillType.SHOOTING -> "Finishing"
                YouthDrillType.PASSING -> "Passing"
                YouthDrillType.PACE -> "Pace"
                YouthDrillType.TECHNICAL -> "Technique"
                YouthDrillType.PHYSICAL -> "Physical"
            }
            viewModel.trainPlayer(focus, grade)
        },
        onDismiss = onDismiss
    )
}
