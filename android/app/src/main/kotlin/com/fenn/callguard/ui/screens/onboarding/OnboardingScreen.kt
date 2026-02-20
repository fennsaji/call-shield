package com.fenn.callguard.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fenn.callguard.R
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
)

private val pages = listOf(
    OnboardingPage(Icons.Filled.Shield, R.string.onboarding_title_1, R.string.onboarding_body_1),
    OnboardingPage(Icons.Filled.Lock, R.string.onboarding_title_2, R.string.onboarding_body_2),
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var isCompleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0A0F1E), Color(0xFF1A2D6B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(72.dp))

            AnimatedContent(
                targetState = currentPage,
                label = "onboarding_page",
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(300)) { it * direction } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(tween(300)) { -it * direction } + fadeOut(tween(200)))
                },
            ) { page ->
                PageContent(pages[page])
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp),
            ) {
                // Pill-shape dot indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    pages.indices.forEach { index ->
                        val isActive = index == currentPage
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .then(
                                    if (isActive) Modifier.width(24.dp)
                                    else Modifier.size(8.dp)
                                )
                                .animateContentSize(spring(dampingRatio = 0.7f))
                                .background(
                                    color = if (isActive) primary
                                    else Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp),
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (currentPage < pages.lastIndex) {
                            currentPage++
                        } else if (!isCompleting) {
                            isCompleting = true
                            scope.launch {
                                viewModel.markOnboardingComplete()
                                onComplete()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                    ),
                ) {
                    Text(
                        if (currentPage < pages.lastIndex)
                            stringResource(R.string.onboarding_next)
                        else
                            stringResource(R.string.onboarding_get_started),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                // PRD §3.1: Onboarding must NOT be skippable — no skip button.
            }
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Radial glow behind icon
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(primary.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    )
            )
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = primary,
            )
        }
        Spacer(Modifier.height(36.dp))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = Color.White,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.65f),
        )
    }
}
