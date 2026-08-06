package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SocialPostEntity
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.MutedBlue
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.SportsCardBg
import com.example.ui.theme.SportsDarkBg
import com.example.ui.theme.SubLineYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrophyGold
import kotlinx.coroutines.launch

@Composable
fun SocialMediaScreen(viewModel: CareerViewModel) {
    val player by viewModel.playerFlow.collectAsStateWithLifecycle()
    val clubs by viewModel.clubsFlow.collectAsStateWithLifecycle()
    val socialPosts by viewModel.socialPostsFlow.collectAsStateWithLifecycle()
    val isPlayerOnly by viewModel.isSocialFeedPlayerOnly.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentClub = remember(clubs, player) {
        clubs.find { it.id == player?.currentClubId }
    }

    val sortedPosts = remember(socialPosts) {
        socialPosts.sortedByDescending { it.sequenceIndex }
    }

    val displayedPosts = remember(sortedPosts, isPlayerOnly, player, currentClub) {
        if (isPlayerOnly) {
            val clubId = player?.currentClubId ?: 0
            sortedPosts.filter { post ->
                post.isAboutPlayerOrClub || (clubId != 0 && post.relatedClubId == clubId)
            }
        } else {
            sortedPosts
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SportsDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Pills Bar (Clean, rounded 8dp container, high-contrast PitchGreen selection)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSlate)
                    .border(1.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("All Posts" to false, "Player & Club" to true).forEach { (label, isMeVal) ->
                    val selected = isPlayerOnly == isMeVal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) PitchGreen else Color.Transparent)
                            .clickable { viewModel.setSocialFeedPlayerOnly(isMeVal) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.Black else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (displayedPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SportsCardBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No posts in your feed yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Advance your career to see match reactions, pundits, and social media buzz!",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedPosts, key = { it.id }) { post ->
                        SocialPostCard(
                            post = post,
                            playerInitials = player?.name?.take(2)?.uppercase() ?: "ME",
                            onReplySelected = { replyIndex ->
                                viewModel.submitSocialPostReply(post, replyIndex) { statMessage ->
                                    if (statMessage.isNotBlank()) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(statMessage)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SocialPostCard(
    post: SocialPostEntity,
    playerInitials: String,
    onReplySelected: (Int) -> Unit
) {
    var isReplying by remember { mutableStateOf(false) }

    val accentColor = when (post.postType) {
        "PUNDIT", "CLUB_NEWS" -> PitchGreen
        "INFLUENCER" -> MutedBlue
        "MILESTONE", "ON_THIS_DAY" -> TrophyGold
        "TREND", "FUN_FACT" -> SubLineYellow
        else -> TextSecondary
    }

    val badgeLabel = when (post.postType) {
        "CLUB_NEWS" -> "CLUB NEWS"
        "ON_THIS_DAY" -> "ON THIS DAY"
        "FUN_FACT" -> "FUN FACT"
        else -> post.postType
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SportsCardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSlate)
                    .border(1.5.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = post.authorInitials,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.authorName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = post.authorHandle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Post Body
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = post.content,
            fontSize = 13.sp,
            color = TextPrimary,
            lineHeight = 18.sp
        )

        // Footer Row
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "Likes",
                modifier = Modifier.size(14.dp),
                tint = TextSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatLikeCount(post.likeCount),
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.weight(1f))

            if (post.isReplyable && !post.hasReplied) {
                Text(
                    text = if (isReplying) "Cancel" else "Reply",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchGreen,
                    modifier = Modifier.clickable { isReplying = !isReplying }
                )
            }
        }

        // Has Replied Preview
        if (post.hasReplied) {
            val chosenText = when (post.selectedReplyIndex) {
                1 -> post.reply1Text
                2 -> post.reply2Text
                3 -> post.reply3Text
                else -> null
            }
            if (!chosenText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSlate.copy(alpha = 0.6f))
                        .border(1.dp, PitchGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(DarkSlate)
                            .border(1.dp, PitchGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = playerInitials,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = chosenText,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Reply Options Expansion
        if (isReplying && !post.hasReplied) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val replies = listOfNotNull(
                    post.reply1Text?.takeIf { it.isNotBlank() }?.let { 1 to it },
                    post.reply2Text?.takeIf { it.isNotBlank() }?.let { 2 to it },
                    post.reply3Text?.takeIf { it.isNotBlank() }?.let { 3 to it }
                )
                replies.forEach { (index, text) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSlate)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .clickable {
                                isReplying = false
                                onReplySelected(index)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = text,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun formatLikeCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fK", count / 1_000f)
        else -> count.toString()
    }
}
