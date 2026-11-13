package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.L
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.onGoogleSignInClicked
import com.example.ui.theme.KonarkSaffron
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.PeacockTealLight
import com.example.util.DateUtils
import kotlinx.coroutines.delay

/**
 * Screen 1: Branded Splash Screen with App Name, Custom Logo, and Today's Day/Month/Year.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.82f) }
    val today = remember { DateUtils.today() }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(durationMillis = 550))
        delay(950)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PeacockTeal, PeacockTealLight, Color(0xFF0A474F))
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .scale(scale.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_sikshya_launcher),
                contentDescription = "ଓଡ଼ିଆ ଶିକ୍ଷା ସାଥୀ Logo",
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "ଓଡ଼ିଆ ଶିକ୍ଷା ସାଥୀ",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Odia Sikshya Sathi",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFDE68A),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "NEP 2020 (5+3+3+4) • ପଞ୍ଚପଦୀ ପାଠ୍ୟ ଯୋଜନା ଓ ଶିକ୍ଷକ ସମୁଦାୟ",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.14f)
            ) {
                Text(
                    text = DateUtils.formatDate(today.day, today.month, today.year, AppLanguage.ODIA),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFDE68A),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private data class OnboardingSlide(
    val titleOdia: String,
    val titleEn: String,
    val descOdia: String,
    val descEn: String,
    val icon: ImageVector
)

/**
 * Screen 2: Odia-language Onboarding & Google Sign-In Screen (Jetpack Credential Manager).
 */
@Composable
fun OnboardingAuthScreen(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { },
            scope = scope
        )
    }

    val slides = remember {
        listOf(
            OnboardingSlide(
                titleOdia = "NEP 2020 ୫+୩+୩+୪ ପାଠ୍ୟକ୍ରମ",
                titleEn = "NEP 2020 5+3+3+4 Structure",
                descOdia = "ମୂଳଦୁଆ, ପ୍ରସ୍ତୁତି, ମଧ୍ୟମ ଏବଂ ମାଧ୍ୟମିକ ସ୍ତର ପାଇଁ ଓଡ଼ିଶା SCERT ଓ ବିଦ୍ୟାଳୟ ବୋର୍ଡ ଅନୁସାରେ ବିଷୟବସ୍ତୁ।",
                descEn = "Organized across Foundational, Preparatory, Middle, and Secondary stages aligned with Odisha SCERT.",
                icon = Icons.AutoMirrored.Filled.MenuBook
            ),
            OnboardingSlide(
                titleOdia = "AI ପଞ୍ଚପଦୀ ପାଠ୍ୟ ଯୋଜନା ନିର୍ମାତା",
                titleEn = "AI Panchapadi Lesson Generator",
                descOdia = "ଆଦିତି, ବୋଧ, ଅଭ୍ୟାସ, ପ୍ରୟୋଗ ଓ ପ୍ରସାର — ୫ଟି ପଦକ୍ଷେପରେ ସହଜ ଓଡ଼ିଆ ପାଠ୍ୟ ଯୋଜନା ପ୍ରସ୍ତୁତ କରନ୍ତୁ ଓ PDF ଡାଉନଲୋଡ୍ କରନ୍ତୁ।",
                descEn = "Generate, customize, regenerate sections, and export printable PDFs in Aditi, Bodha, Abhyasa, Prayoga & Prasara.",
                icon = Icons.Default.AutoAwesome
            ),
            OnboardingSlide(
                titleOdia = "ଓଡ଼ିଶା ଶିକ୍ଷକ ସମୁଦାୟ",
                titleEn = "Odisha Educator Community",
                descOdia = "୩୦ଟି ଜିଲ୍ଲାର ଶିକ୍ଷକଙ୍କ ସହ ପାଠ୍ୟ ଯୋଜନା, TLM ଏବଂ ଶ୍ରେଣୀ କକ୍ଷ କାର୍ଯ୍ୟକଳାପ ଅଂଶୀଦାର କରନ୍ତୁ।",
                descEn = "Collaborate in subject and district groups across all 30 districts of Odisha with privacy controls.",
                icon = Icons.Default.Groups
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar with Language switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_sikshya_launcher),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = L(language, "ଓଡ଼ିଆ ଶିକ୍ଷା ସାଥୀ", "Odia Sikshya Sathi"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedButton(
                    onClick = onToggleLanguage,
                    modifier = Modifier.testTag("onboarding_lang_toggle")
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == AppLanguage.ODIA) "English" else "ଓଡ଼ିଆ")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Editorial Illustration
            Image(
                painter = painterResource(id = R.drawable.img_odia_hero_banner),
                contentDescription = L(language, "ଓଡ଼ିଆ ଶିକ୍ଷକ ସହଯୋଗ", "Odisha teachers collaborating"),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Onboarding Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val item = slides[page]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = L(language, item.titleOdia, item.titleEn),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = L(language, item.descOdia, item.descEn),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pager Dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(slides.size) { idx ->
                    val active = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Interactive Google Sign-In Button
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PeacockTeal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(L(language, "ସାଇନ୍ ଇନ୍ ହେଉଛି...", "Signing in..."))
                } else {
                    Text(
                        text = L(
                            language,
                            "ଗୁଗୁଲ୍ ସହ ସାଇନ୍ ଇନ୍ କରନ୍ତୁ (Sign in with Google)",
                            "Sign in with Google"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = L(
                    language,
                    "ଶିକ୍ଷକ, ଶିକ୍ଷାର୍ଥୀ ଓ ପ୍ରଶାସକଙ୍କ ପାଇଁ ସୁରକ୍ଷିତ ପ୍ରବେଶ। ଛାତ୍ରଛାତ୍ରୀଙ୍କ ବ୍ୟକ୍ତିଗତ ତଥ୍ୟ ସର୍ବସାଧାରଣରେ ପ୍ରଦର୍ଶିତ ହୁଏ ନାହିଁ।",
                    "Secure sign-in for educators, learners & admins. Student & private data are never publicly exposed."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
