package com.evil0ctopus.octobuddy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evil0ctopus.octobuddy.R
import com.evil0ctopus.octobuddy.data.Achievement
import com.evil0ctopus.octobuddy.data.Cosmetic
import com.evil0ctopus.octobuddy.data.CosmeticSlot
import com.evil0ctopus.octobuddy.data.DailyChallenge
import com.evil0ctopus.octobuddy.data.EquippedCosmetics
import com.evil0ctopus.octobuddy.data.PetProgress
import com.evil0ctopus.octobuddy.ui.theme.Brand
import kotlinx.coroutines.delay

@Composable
fun PetScreen(viewModel: PetViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    var showRenameDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun maybeHaptic(strong: Boolean) {
        if (!state.hapticsEnabled) return
        haptics.performHapticFeedback(
            if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
        )
    }

    LaunchedEffect(state.levelUpEpoch, state.evolveEpoch) {
        if ((state.levelUpEpoch > 0 || state.evolveEpoch > 0) && state.hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(state.newAchievement) {
        if (state.newAchievement != null) {
            if (state.hapticsEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            delay(3200)
            viewModel.dismissAchievementToast()
        }
    }

    if (showRenameDialog) {
        RenamePetDialog(
            currentName = state.petName,
            onDismiss = { showRenameDialog = false },
            onConfirm = {
                viewModel.renamePet(it)
                showRenameDialog = false
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.settings_reset)) },
            text = { Text(stringResource(R.string.settings_reset_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    viewModel.resetPet()
                }) {
                    Text(stringResource(R.string.settings_reset_confirm), color = Brand.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        InteractiveCyberOceanBackground(
            modifier = Modifier.fillMaxSize(),
            mood = state.mood,
            energy = state.energy,
            careBurstEpoch = state.careBurstEpoch,
            lastAction = state.lastAction,
            onBackgroundTap = { maybeHaptic(false) },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.openDaily() }) {
                        Icon(
                            imageVector = Icons.Filled.Today,
                            contentDescription = stringResource(R.string.daily_challenges),
                            tint = Brand.Cyan,
                        )
                    }
                    IconButton(onClick = { viewModel.openAchievements() }) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = stringResource(R.string.achievements),
                            tint = Brand.CopperBright,
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = state.petName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Brand.Cyan,
                            modifier = Modifier.clickable { showRenameDialog = true },
                        )
                        IconButton(
                            onClick = { showRenameDialog = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.content_desc_edit_name),
                                tint = Brand.Cyan,
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.openUnlocks() }) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = stringResource(R.string.unlocks),
                            tint = Brand.CopperBright,
                        )
                    }
                    IconButton(onClick = { viewModel.openSettings() }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.content_desc_settings),
                            tint = Brand.FoamDim,
                        )
                    }
                }

                Text(
                    text = stringResource(
                        R.string.rank_level_line,
                        state.rankTitle,
                        state.level,
                        state.stage.displayName,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = Brand.CyanSoft,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                StreakChip(streak = state.careStreak, dailyDone = state.daily.completedCount, dailyTotal = state.daily.challenges.size)
                Spacer(modifier = Modifier.height(6.dp))
                GlossyXpBar(
                    progress = state.xpProgress,
                    caption = if (state.level >= PetProgress.MAX_LEVEL) {
                        stringResource(R.string.xp_maxed, state.xp.toInt())
                    } else {
                        stringResource(
                            R.string.xp_to_next,
                            state.xpToNext.toInt(),
                            PetProgress.XP_PER_LEVEL.toInt(),
                        )
                    },
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = Brand.Foam,
                )
            }

            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxWidth(),
            ) {
                BrandPetView(
                    stage = state.stage,
                    actionEpoch = state.actionEpoch,
                    lastAction = state.lastAction,
                    evolveEpoch = state.evolveEpoch,
                    hunger = state.hunger,
                    mood = state.mood,
                    energy = state.energy,
                    equipped = state.equipped,
                    onTap = {
                        maybeHaptic(false)
                        viewModel.tapPet()
                    },
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .size(270.dp),
                )
                SpeechBubble(
                    text = state.speechText,
                    modifier = Modifier.padding(top = 0.dp),
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Brand.NavyCard.copy(alpha = 0.84f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NeedBar(stringResource(R.string.need_hunger), state.hunger / 100f, Brand.Ok, Brand.Warn, Brand.Danger)
                    NeedBar(stringResource(R.string.need_mood), state.mood / 100f, Brand.Cyan, Brand.CyanDim, Brand.Copper)
                    NeedBar(stringResource(R.string.need_energy), state.energy / 100f, Brand.CopperBright, Brand.Copper, Brand.Danger)

                    if (state.hunger < 30f || state.mood < 30f || state.energy < 30f) {
                        Text(
                            text = stringResource(R.string.empty_needs_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Brand.Warn,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CareButton(stringResource(R.string.feed), Modifier.weight(1f), Brand.Cyan, Brand.NavyDeep) {
                            maybeHaptic(true); viewModel.feed()
                        }
                        CareButton(stringResource(R.string.play), Modifier.weight(1f), Brand.Copper, Brand.NavyDeep) {
                            maybeHaptic(true); viewModel.play()
                        }
                        CareButton(stringResource(R.string.rest), Modifier.weight(1f), Brand.CyanDim, Brand.Foam) {
                            maybeHaptic(true); viewModel.rest()
                        }
                    }
                    Text(
                        text = stringResource(R.string.tap_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.FoamDim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (state.ready && !state.onboardingComplete) {
            WelcomeSheet(onStart = { viewModel.completeOnboarding(it) })
        }
        if (state.showSettings) {
            SettingsSheet(
                hapticsEnabled = state.hapticsEnabled,
                onHapticsChange = { viewModel.setHapticsEnabled(it) },
                onReset = { showResetConfirm = true },
                onClose = { viewModel.closeSettings() },
            )
        }
        if (state.showAchievements) {
            AchievementsSheet(
                mask = state.achievementsMask,
                onClose = { viewModel.closeAchievements() },
            )
        }
        if (state.showDaily) {
            DailySheet(
                challenges = state.daily.challenges,
                streak = state.careStreak,
                onClose = { viewModel.closeDaily() },
            )
        }
        if (state.showUnlocks) {
            UnlocksSheet(
                mask = state.achievementsMask,
                level = state.level,
                streak = state.careStreak,
                equipped = state.equipped,
                onEquip = { viewModel.equipCosmetic(it) },
                onClose = { viewModel.closeUnlocks() },
            )
        }

        AnimatedVisibility(
            visible = state.leveledTo != null && state.evolvedToStage == null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            state.leveledTo?.let { lvl ->
                FanfareOverlay(
                    title = stringResource(R.string.level_up_title),
                    body = stringResource(
                        R.string.level_up_body,
                        state.petName,
                        lvl,
                        PetProgress.rankTitle(lvl),
                    ),
                    onDismiss = { viewModel.dismissLevelUp() },
                )
            }
        }

        AnimatedVisibility(
            visible = state.evolvedToStage != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            state.evolvedToStage?.let { stage ->
                FanfareOverlay(
                    title = stringResource(R.string.evolve_title),
                    body = stringResource(R.string.evolve_body, state.petName, stage.displayName),
                    onDismiss = {
                        viewModel.dismissEvolve()
                        viewModel.dismissLevelUp()
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = state.newAchievement != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp),
        ) {
            state.newAchievement?.let { a ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Brand.NavyCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .border(1.dp, Brand.Copper, RoundedCornerShape(16.dp)),
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆 ${a.title}", color = Brand.CopperBright, style = MaterialTheme.typography.titleMedium)
                        Text(a.description, color = Brand.FoamDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakChip(streak: Int, dailyDone: Int, dailyTotal: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Brand.NavyDeep.copy(alpha = 0.75f))
                .border(1.dp, Brand.Copper.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = if (streak > 0) Brand.CopperBright else Brand.FoamDim,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.streak_label, streak),
                style = MaterialTheme.typography.labelLarge,
                color = if (streak > 0) Brand.CopperBright else Brand.FoamDim,
            )
        }
        if (dailyTotal > 0) {
            Text(
                text = stringResource(R.string.daily_progress_chip, dailyDone, dailyTotal),
                style = MaterialTheme.typography.labelLarge,
                color = if (dailyDone >= dailyTotal) Brand.Ok else Brand.CyanSoft,
            )
        }
    }
}

@Composable
private fun GlossyXpBar(progress: Float, caption: String) {
    val p = progress.coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(Brand.NavyDeep.copy(alpha = 0.85f))
                .border(1.dp, Brand.CyanDim.copy(alpha = 0.45f), RoundedCornerShape(50)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(p)
                    .height(14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Brand.Copper, Brand.CopperBright, Brand.Cyan),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(p)
                    .height(5.dp)
                    .align(Alignment.TopStart)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.35f)),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(caption, style = MaterialTheme.typography.bodySmall, color = Brand.FoamDim)
    }
}

@Composable
private fun CareButton(
    label: String,
    modifier: Modifier = Modifier,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) { Text(label) }
}

@Composable
private fun NeedBar(label: String, value: Float, high: Color, mid: Color, low: Color) {
    val v = value.coerceIn(0f, 1f)
    val barColor = when {
        v >= 0.6f -> high
        v >= 0.3f -> mid
        else -> low
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = Brand.Foam)
            Text("${(v * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = barColor)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brand.NavyDeep.copy(alpha = 0.65f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(v)
                    .height(11.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.75f), barColor))),
            )
        }
    }
}

@Composable
private fun RenamePetDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var draft by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_pet)) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(PetViewModel.MAX_PET_NAME_LENGTH) },
                singleLine = true,
                label = { Text(stringResource(R.string.pet_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brand.Cyan,
                    cursorColor = Brand.Cyan,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }, enabled = draft.trim().isNotEmpty()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun WelcomeSheet(onStart: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.NavyDeep.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Brand.NavyCard),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brand.Cyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Text("🐙", style = MaterialTheme.typography.headlineMedium) }
                Text(
                    stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Brand.Cyan,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.welcome_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.FoamDim,
                    textAlign = TextAlign.Center,
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(PetViewModel.MAX_PET_NAME_LENGTH) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.welcome_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand.Cyan,
                        cursorColor = Brand.Cyan,
                    ),
                )
                Button(
                    onClick = { onStart(draft) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand.Cyan,
                        contentColor = Brand.NavyDeep,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(stringResource(R.string.welcome_start)) }
            }
        }
    }
}

@Composable
private fun SettingsSheet(
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.NavyDeep.copy(alpha = 0.65f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Brand.NavyCard),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, color = Brand.Cyan)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_haptics), style = MaterialTheme.typography.titleMedium, color = Brand.Foam)
                        Text(
                            stringResource(if (hapticsEnabled) R.string.settings_haptics_on else R.string.settings_haptics_off),
                            style = MaterialTheme.typography.bodySmall,
                            color = Brand.FoamDim,
                        )
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = onHapticsChange,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Brand.CyanDim,
                            checkedThumbColor = Brand.Cyan,
                        ),
                    )
                }
                FilledTonalButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Brand.Danger.copy(alpha = 0.2f),
                        contentColor = Brand.Danger,
                    ),
                ) { Text(stringResource(R.string.settings_reset)) }
                TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.settings_close))
                }
            }
        }
    }
}

@Composable
private fun AchievementsSheet(mask: Long, onClose: () -> Unit) {
    val unlocked = Achievement.unlocked(mask).size
    val total = Achievement.all().size
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.NavyDeep.copy(alpha = 0.65f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Brand.NavyCard),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(20.dp),
            ) {
                Text(
                    stringResource(R.string.achievements_title, unlocked, total),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Brand.CopperBright,
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(Achievement.all()) { a ->
                        val on = Achievement.isUnlocked(mask, a)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) Brand.Cyan.copy(alpha = 0.12f) else Brand.NavyDeep.copy(alpha = 0.45f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (on) "🏅" else "🔒", modifier = Modifier.width(28.dp))
                            Column {
                                Text(
                                    a.title,
                                    color = if (on) Brand.Foam else Brand.FoamDim,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(a.description, color = Brand.FoamDim, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.settings_close))
                }
            }
        }
    }
}

@Composable
private fun DailySheet(
    challenges: List<DailyChallenge>,
    streak: Int,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.NavyDeep.copy(alpha = 0.65f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Brand.NavyCard),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(20.dp),
            ) {
                Text(
                    stringResource(R.string.daily_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Brand.Cyan,
                )
                Text(
                    stringResource(R.string.streak_detail, streak),
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.CopperBright,
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(challenges) { ch ->
                        DailyChallengeRow(ch)
                    }
                }
                TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.settings_close))
                }
            }
        }
    }
}

@Composable
private fun DailyChallengeRow(ch: DailyChallenge) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (ch.complete) Brand.Ok.copy(alpha = 0.12f)
                else Brand.NavyDeep.copy(alpha = 0.45f),
            )
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (ch.complete) "✓ ${ch.title}" else ch.title,
                color = if (ch.complete) Brand.Ok else Brand.Foam,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${ch.progress}/${ch.target}",
                color = Brand.FoamDim,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(ch.description, color = Brand.FoamDim, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Brand.NavyDeep),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ch.fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (ch.complete) Brand.Ok else Brand.Cyan),
            )
        }
    }
}

@Composable
private fun UnlocksSheet(
    mask: Long,
    level: Int,
    streak: Int,
    equipped: EquippedCosmetics,
    onEquip: (Cosmetic) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.NavyDeep.copy(alpha = 0.65f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Brand.NavyCard),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(20.dp),
            ) {
                Text(
                    stringResource(R.string.unlocks_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Brand.CopperBright,
                )
                Text(
                    stringResource(R.string.unlocks_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.FoamDim,
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    items(CosmeticSlot.entries.toList()) { slot ->
                        Text(
                            slot.name,
                            color = Brand.Cyan,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Cosmetic.forSlot(slot).forEach { c ->
                            val unlocked = c.isUnlocked(mask, level, streak)
                            val selected = when (slot) {
                                CosmeticSlot.Frame -> equipped.frameId == c.id
                                CosmeticSlot.Effect -> equipped.effectId == c.id
                                CosmeticSlot.Head -> equipped.headId == c.id
                                CosmeticSlot.Accent -> equipped.accentId == c.id
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            selected -> Brand.Cyan.copy(alpha = 0.18f)
                                            unlocked -> Brand.NavyDeep.copy(alpha = 0.5f)
                                            else -> Brand.NavyDeep.copy(alpha = 0.3f)
                                        },
                                    )
                                    .clickable(enabled = unlocked) { onEquip(c) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    when {
                                        selected -> "★"
                                        unlocked -> "◇"
                                        else -> "🔒"
                                    },
                                    modifier = Modifier.width(28.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        c.title,
                                        color = if (unlocked) Brand.Foam else Brand.FoamDim,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(c.description, color = Brand.FoamDim, style = MaterialTheme.typography.bodySmall)
                                }
                                if (selected) {
                                    Text("ON", color = Brand.Cyan, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.settings_close))
                }
            }
        }
    }
}

@Composable
private fun FanfareOverlay(title: String, body: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.NavyDeep.copy(alpha = 0.78f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Brand.NavyCard),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("✨", style = MaterialTheme.typography.headlineMedium)
                Text(title, style = MaterialTheme.typography.headlineMedium, color = Brand.Cyan)
                Text(body, style = MaterialTheme.typography.titleMedium, color = Brand.Foam, textAlign = TextAlign.Center)
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Brand.Cyan, contentColor = Brand.NavyDeep),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(stringResource(R.string.evolve_dismiss)) }
            }
        }
    }
}
