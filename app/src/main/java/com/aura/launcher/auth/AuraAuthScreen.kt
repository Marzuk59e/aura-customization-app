package com.aura.launcher.auth

import android.util.Patterns

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.blur
import androidx.fragment.app.FragmentActivity
import com.aura.launcher.core.theme.*
import com.aura.launcher.domain.model.DeviceSecurityCapability
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Basic device-native email pattern check — matches the design's "Enter a valid email" rule. */
private fun isValidEmail(input: String): Boolean =
    input.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(input).matches()

/**
 * Approved "crystal" login/signup screen for Aura Launcher.
 *
 * Replaces the old WelcomeScreen + separate LoginScreen/SignUpScreen with a single
 * flip-card screen, matching the approved web prototype (loginpage/index.html).
 *
 * Notes on scope:
 *  - "Continue with Google" is still UI-only for now — no backend wired up yet.
 *  - Forgot-password now uses real secure device authentication (BiometricPrompt +
 *    Android Keystore + backend-verified signature) instead of a simulated OTP —
 *    see ForgotPasswordDialog and DeviceAuthViewModel.
 *  - "Continue as guest" is kept (existing app feature, not present in the web design).
 */
@Composable
fun AuraAuthScreen(
    authViewModel: AuthViewModel,
    deviceAuthViewModel: DeviceAuthViewModel,
    activity: androidx.fragment.app.FragmentActivity,
    onAuthSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var isSignUpMode by rememberSaveable { mutableStateOf(false) }
    val uiState by authViewModel.uiState.collectAsState()

    var showSuccessOverlay by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var pendingDeviceUser by remember { mutableStateOf<com.aura.launcher.domain.model.User?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            showSuccessOverlay = true
            toastMessage = "Signed in — welcome back."
            delay(1300)
            showSuccessOverlay = false
            val user = (uiState as AuthUiState.Success).user
            val offerDeviceSetup = !user.isGuest && deviceAuthViewModel.shouldOfferRegistration(user.id)
            authViewModel.resetState()
            if (offerDeviceSetup) {
                pendingDeviceUser = user
            } else {
                onAuthSuccess()
            }
        }
    }

    // Blurs out when the forgot-password overlay is open. Kept as a separate
    // layer (instead of blurring individual children) so ForgotPasswordDialog
    // — drawn as a sibling below, on top of this Box — stays perfectly sharp
    // while everything behind it softens. No-ops on API < 31 (Modifier.blur
    // silently draws unblurred there); the dark scrim in ForgotPasswordDialog
    // still fully covers the screen either way, so the "bicchiri" bright
    // strip under the keyboard is gone regardless of blur support.
    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (showForgotDialog) 20.dp else 0.dp,
        animationSpec = tween(300),
        label = "authBackgroundBlur"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(backgroundBlurRadius)
        ) {
            AuraAuthBackdrop(modifier = Modifier.fillMaxSize())

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(top = 56.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AuthFlipCard(
                    isSignUpMode = isSignUpMode,
                    onToggleMode = { isSignUpMode = it },
                    authViewModel = authViewModel,
                    uiState = uiState,
                    onForgotPassword = { showForgotDialog = true },
                    onGoogleClick = { toastMessage = "Google Sign-In is coming soon" }
                )

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(onClick = { authViewModel.continueAsGuest() }) {
                    Text("Continue as Guest", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        }

        AnimatedVisibility(
            visible = showSuccessOverlay,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            LoginSuccessOverlay(
                displayName = (uiState as? AuthUiState.Success)?.user?.displayName ?: "there",
                visible = showSuccessOverlay
            )
        }

        if (showForgotDialog) {
            ForgotPasswordDialog(
                activity = activity,
                deviceAuthViewModel = deviceAuthViewModel,
                onDismiss = { showForgotDialog = false },
                onToast = { toastMessage = it },
                onDeviceSignIn = { signInToken ->
                    showForgotDialog = false
                    // Reuses the LaunchedEffect(uiState) above for the success
                    // overlay, the device-registration offer, and onAuthSuccess —
                    // exactly the same finish line as a normal email/password login.
                    authViewModel.completeDeviceSignIn(signInToken)
                }
            )
        }

        pendingDeviceUser?.let { user ->
            DeviceRegistrationPromptDialog(
                activity = activity,
                deviceAuthViewModel = deviceAuthViewModel,
                user = user,
                onDone = {
                    pendingDeviceUser = null
                    onAuthSuccess()
                }
            )
        }

        AuraToast(
            message = toastMessage,
            onConsumed = { toastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
private fun AuthFlipCard(
    isSignUpMode: Boolean,
    onToggleMode: (Boolean) -> Unit,
    authViewModel: AuthViewModel,
    uiState: AuthUiState,
    onForgotPassword: () -> Unit,
    onGoogleClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isSignUpMode) 180f else 0f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "authFlip"
    )

    // Both faces stay composed at all times (never added/removed), stacked on
    // top of each other. Only alpha + pointer-input are toggled, so the flip
    // never has to build/tear down the (heavier) SignUp form mid-animation —
    // that swap was what caused the ~1s stutter right around the midpoint.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
    ) {
        val loginVisible = rotation <= 90f

        // Small crossfade band around the 90° midpoint instead of a hard 1/0
        // cut — a single-frame alpha jump right when both faces are near
        // edge-on (thin sliver) is what was showing up as a flash/flicker.
        val fadeWidthDeg = 12f
        val loginAlpha = (((90f + fadeWidthDeg) - rotation) / (2f * fadeWidthDeg)).coerceIn(0f, 1f)
        val signUpAlpha = 1f - loginAlpha

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 18f * density
                    alpha = loginAlpha
                    // Once fully faded out, shove it far off-screen so its hit-test
                    // rect no longer overlaps the visible face at all (see note below).
                    if (loginAlpha <= 0f) translationX = OFFSCREEN_PX
                }
                .hiddenFaceSemantics(hidden = loginAlpha <= 0f)
        ) {
            AuthCardSurface {
                LoginCardContent(
                    authViewModel = authViewModel,
                    uiState = uiState,
                    onNavigateToSignUp = { onToggleMode(true) },
                    onForgotPassword = onForgotPassword,
                    onGoogleClick = onGoogleClick
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = rotation - 180f
                    cameraDistance = 18f * density
                    alpha = signUpAlpha
                    if (signUpAlpha <= 0f) translationX = OFFSCREEN_PX
                }
                .hiddenFaceSemantics(hidden = signUpAlpha <= 0f)
        ) {
            AuthCardSurface {
                SignUpCardContent(
                    authViewModel = authViewModel,
                    uiState = uiState,
                    onNavigateToLogin = { onToggleMode(false) }
                )
            }
        }
    }
}

/** Large px offset used to physically move a fully-hidden face off the visible/touchable area. */
private const val OFFSCREEN_PX = 100_000f

/**
 * Marks a face as invisible to accessibility once it's fully faded out (alpha 0).
 *
 * NOTE — this replaces an older version of this modifier that used
 * `pointerInput(...) { awaitPointerEvent(pass = PointerEventPass.Initial) { consume() } }`
 * to swallow touches on the hidden face. That was the actual cause of the
 * "Create Account button does nothing" bug: PointerEventPass.Initial runs for
 * *every* pointer-input node that a touch lands inside, top-down, before the
 * Main pass that Button/clickable use even starts — and because both the
 * Login and SignUp boxes sit stacked in the exact same place (that's how the
 * flip's 3D overlap works), a tap on the visible face's button also landed
 * inside the hidden face's bounds and got eaten by its Initial-pass consumer
 * first, regardless of which one was actually on top or visible.
 *
 * The fix here doesn't intercept any pointer events at all: it just moves the
 * fully-transparent face's `graphicsLayer` translationX far outside the
 * screen (see OFFSCREEN_PX above) once its alpha has reached 0, so its
 * hit-test rect stops overlapping the visible face entirely. It only does this
 * once alpha is exactly 0 (i.e. outside the small crossfade band around 90°),
 * so it can't reintroduce the earlier flash/flicker glitch.
 */
private fun Modifier.hiddenFaceSemantics(hidden: Boolean): Modifier {
    if (!hidden) return this
    return this.semantics { invisibleToUser() }
}

@Composable
private fun AuthCardSurface(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurfaceGlass)
            .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 28.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun BrandHeader(tagline: String) {
    AuraGemIcon(size = 56.dp)
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Aura Launcher",
        style = MaterialTheme.typography.headlineSmall,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = tagline,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
    )
    Spacer(modifier = Modifier.height(22.dp))
}

/** Mirrors the exact <svg viewBox="0 0 108 108"> gem from the approved design
 *  (loginpage/index.html): three nested rhombi, center (54,54). */
@Composable
private fun AuraGemIcon(size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val scale = this.size.width / 108f
        fun rhombus(halfWidth: Float, halfHeight: Float): androidx.compose.ui.graphics.Path {
            val cx = 54f * scale
            val cy = 54f * scale
            val hw = halfWidth * scale
            val hh = halfHeight * scale
            return androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - hh)
                lineTo(cx + hw, cy)
                lineTo(cx, cy + hh)
                lineTo(cx - hw, cy)
                close()
            }
        }
        drawPath(rhombus(halfWidth = 18f, halfHeight = 34f), color = AuraCyan)
        drawPath(rhombus(halfWidth = 8f, halfHeight = 22f), color = AuraPurple)
        drawPath(rhombus(halfWidth = 3.5f, halfHeight = 12f), color = Color.White)
    }
}

@Composable
private fun AuraAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = errorText?.let {
            { Text(it, color = AuraDanger, style = MaterialTheme.typography.bodySmall) }
        },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = AuraCyan) },
        trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
            {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = TextSecondary
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AuraCyan,
            unfocusedBorderColor = DarkBorder,
            errorBorderColor = AuraDanger,
            errorLeadingIconColor = AuraDanger,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = AuraCyan
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Hero CTA button (Sign in / Create account / Send code / Verify code).
 * Mirrors the design's `.btn-login`: cyan→purple gradient, a press-scale
 * "spring back" feel, a light sheen sweep on every tap, and a crossfade
 * between label / spinner / success-check instead of a hard swap.
 */
@Composable
private fun AuraAuthButton(
    label: String,
    uiState: AuthUiState,
    onClick: () -> Unit
) {
    val isLoading = uiState is AuthUiState.Loading
    val isSuccess = uiState is AuthUiState.Success

    val normalBrush = authCtaNormalBrush()
    val successBrush = authCtaSuccessBrush()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "authButtonScale"
    )

    val sheenOffset = remember { Animatable(-1f) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            sheenOffset.snapTo(-1f)
            sheenOffset.animateTo(2f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        }
    }

    val onDarkText = AuthCtaOnDarkText

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSuccess) successBrush else normalBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isLoading && !isSuccess,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(maxWidth * 0.4f)
                .graphicsLayer {
                    translationX = sheenOffset.value * maxWidth.toPx()
                    rotationZ = -12f
                }
                .background(
                    Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent))
                )
        )

        Crossfade(
            targetState = when {
                isSuccess -> AuthButtonVisualState.Success
                isLoading -> AuthButtonVisualState.Loading
                else -> AuthButtonVisualState.Label
            },
            modifier = Modifier.matchParentSize(),
            label = "authButtonContent"
        ) { state ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                when (state) {
                    AuthButtonVisualState.Loading -> CircularProgressIndicator(
                        color = onDarkText,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    AuthButtonVisualState.Success -> Icon(Icons.Default.Check, contentDescription = null, tint = onDarkText)
                    AuthButtonVisualState.Label -> Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = onDarkText)
                }
            }
        }
    }
}

private enum class AuthButtonVisualState { Label, Loading, Success }

/** Shared visual tokens for every gradient CTA button in this screen (the hero
 * Sign in/Create account button and the smaller forgot-password buttons) —
 * previously each button redefined these same values independently. Plain
 * functions, not top-level vals: AuraCyan/AuraPurple/AuraSuccess are
 * `var ... by mutableStateOf(...)` (VibeSync can change them at runtime from
 * the wallpaper palette), so these need to re-read the current color on every
 * call rather than freeze whatever was active at class-load time. */
private fun authCtaNormalBrush() = Brush.linearGradient(listOf(AuraCyan, Color(0xFF4DE8FF), AuraPurple))
private fun authCtaSuccessBrush() = Brush.linearGradient(listOf(AuraSuccess, Color(0xFF34D399)))
private val AuthCtaOnDarkText = Color(0xFF04050A)

@Composable
private fun GoogleButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text("Continue with Google")
    }
}

@Composable
private fun DividerRow() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
        Text(
            "or",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
    }
}

@Composable
private fun LoginCardContent(
    authViewModel: AuthViewModel,
    uiState: AuthUiState,
    onNavigateToSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleClick: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }

    fun validateAndSubmit() {
        emailError = if (email.isBlank()) "Enter your email or username" else null
        passwordError = if (password.isBlank()) "Enter your password" else null
        if (emailError == null && passwordError == null) {
            authViewModel.login(email, password)
        }
    }

    BrandHeader(tagline = "Your home screen, tuned to you.")

    AuraAuthTextField(
        value = email,
        onValueChange = { email = it; if (emailError != null) emailError = null },
        label = "Email or username",
        leadingIcon = Icons.Default.Email,
        isError = emailError != null || uiState is AuthUiState.Error,
        errorText = emailError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() })
    )
    Spacer(modifier = Modifier.height(14.dp))
    AuraAuthTextField(
        value = password,
        onValueChange = { password = it; if (passwordError != null) passwordError = null },
        label = "Password",
        leadingIcon = Icons.Default.Lock,
        isPassword = true,
        passwordVisible = passwordVisible,
        onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
        isError = passwordError != null || uiState is AuthUiState.Error,
        errorText = passwordError,
        modifier = Modifier.focusRequester(passwordFocusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            validateAndSubmit()
        })
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                colors = CheckboxDefaults.colors(checkedColor = AuraCyan, uncheckedColor = TextMuted)
            )
            Text("Remember me", color = TextSecondary, fontSize = 13.sp)
        }
        TextButton(onClick = onForgotPassword) {
            Text("Forgot password?", color = AuraCyan, fontSize = 13.sp)
        }
    }

    if (uiState is AuthUiState.Error) {
        Spacer(modifier = Modifier.height(8.dp))
        Text((uiState as AuthUiState.Error).message, color = AuraDanger, style = MaterialTheme.typography.bodySmall)
    }

    Spacer(modifier = Modifier.height(18.dp))

    AuraAuthButton(
        label = "Sign in",
        uiState = uiState,
        onClick = { validateAndSubmit() }
    )

    Spacer(modifier = Modifier.height(16.dp))
    DividerRow()
    Spacer(modifier = Modifier.height(16.dp))
    GoogleButton(onClick = onGoogleClick)

    Spacer(modifier = Modifier.height(18.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("New to Aura?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onNavigateToSignUp) {
            Text("Create an account", color = AuraCyan, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SignUpCardContent(
    authViewModel: AuthViewModel,
    uiState: AuthUiState,
    onNavigateToLogin: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    fun validateAndSubmit() {
        nameError = if (name.isBlank()) "Tell us what to call you" else null
        emailError = if (!isValidEmail(email)) "Enter a valid email" else null
        passwordError = if (password.isBlank()) "Password must not be empty" else null
        confirmError = when {
            confirmPassword.isBlank() -> "Confirm your password"
            confirmPassword != password -> "Passwords don't match"
            else -> null
        }
        if (nameError == null && emailError == null && passwordError == null && confirmError == null) {
            authViewModel.signUp(email, name, password, confirmPassword)
        }
    }

    BrandHeader(tagline = "Set up your space in a few seconds.")

    AuraAuthTextField(
        value = name,
        onValueChange = { name = it; if (nameError != null) nameError = null },
        label = "Full name",
        leadingIcon = Icons.Default.Person,
        isError = nameError != null || uiState is AuthUiState.Error,
        errorText = nameError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() })
    )
    Spacer(modifier = Modifier.height(14.dp))
    AuraAuthTextField(
        value = email,
        onValueChange = { email = it; if (emailError != null) emailError = null },
        label = "Email",
        leadingIcon = Icons.Default.Email,
        isError = emailError != null || uiState is AuthUiState.Error,
        errorText = emailError,
        modifier = Modifier.focusRequester(emailFocusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() })
    )
    Spacer(modifier = Modifier.height(14.dp))
    AuraAuthTextField(
        value = password,
        onValueChange = { password = it; if (passwordError != null) passwordError = null },
        label = "Password",
        leadingIcon = Icons.Default.Lock,
        isPassword = true,
        passwordVisible = passwordVisible,
        onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
        isError = passwordError != null || uiState is AuthUiState.Error,
        errorText = passwordError,
        modifier = Modifier.focusRequester(passwordFocusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { confirmPasswordFocusRequester.requestFocus() })
    )
    Spacer(modifier = Modifier.height(14.dp))
    AuraAuthTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it; if (confirmError != null) confirmError = null },
        label = "Confirm password",
        leadingIcon = Icons.Default.Lock,
        isPassword = true,
        passwordVisible = passwordVisible,
        isError = confirmError != null || uiState is AuthUiState.Error,
        errorText = confirmError,
        modifier = Modifier.focusRequester(confirmPasswordFocusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            validateAndSubmit()
        })
    )

    if (uiState is AuthUiState.Error) {
        Spacer(modifier = Modifier.height(8.dp))
        Text((uiState as AuthUiState.Error).message, color = AuraDanger, style = MaterialTheme.typography.bodySmall)
    }

    Spacer(modifier = Modifier.height(18.dp))

    AuraAuthButton(
        label = "Create account",
        uiState = uiState,
        onClick = { validateAndSubmit() }
    )

    Spacer(modifier = Modifier.height(18.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Already have an account?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onNavigateToLogin) {
            Text("Sign in", color = AuraCyan, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LoginSuccessOverlay(displayName: String?, visible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "successRing")
    val ringProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing)),
        label = "successRingProgress"
    )

    Box(contentAlignment = Alignment.Center) {
        // Soft radial glow behind everything.
        Box(
            modifier = Modifier
                .size(340.dp)
                .background(
                    Brush.radialGradient(listOf(AuraSuccess.copy(alpha = 0.16f), Color.Transparent))
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(DarkBg.copy(alpha = 0.6f))
                .padding(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Pulsing ring
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            val scale = 1f + ringProgress * 0.5f
                            scaleX = scale
                            scaleY = scale
                            alpha = (1f - ringProgress) * 0.6f
                        }
                        .border(2.dp, AuraSuccess, CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AuraSuccess, AuraCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    var checkVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(visible) { if (visible) { delay(180); checkVisible = true } else checkVisible = false }
                    val checkScale by animateFloatAsState(
                        targetValue = if (checkVisible) 1f else 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "checkPop"
                    )
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer { scaleX = checkScale; scaleY = checkScale; alpha = checkScale }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var titleVisible by remember { mutableStateOf(false) }
            var subtitleVisible by remember { mutableStateOf(false) }
            LaunchedEffect(visible) {
                if (visible) {
                    delay(300); titleVisible = true
                    delay(120); subtitleVisible = true
                } else {
                    titleVisible = false
                    subtitleVisible = false
                }
            }

            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }
            ) {
                Text("Successfully logged in", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            AnimatedVisibility(
                visible = subtitleVisible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }
            ) {
                Row {
                    Text("Welcome back, ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        displayName ?: "back",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuraCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Forgot-password modal. Never asks for an email up front: the account is
 * found locally (TrustedAccountSummary, from whichever device keys are
 * already registered on this install) and the person goes straight to their
 * platform's native BiometricPrompt (fingerprint / face / device PIN -
 * Android decides which, never this app). Up to
 * DeviceAuthViewModel.MAX_LOGIN_ATTEMPTS tries; a verified signature signs
 * the person straight in (no "set a new password" step at all). If no local
 * account is found, or every try is used up, it falls back to emailing a
 * normal Firebase reset link.
 */
@Composable
private fun ForgotPasswordDialog(
    activity: FragmentActivity,
    deviceAuthViewModel: DeviceAuthViewModel,
    onDismiss: () -> Unit,
    onToast: (String) -> Unit,
    onDeviceSignIn: (signInToken: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val verifyState by deviceAuthViewModel.verifyState.collectAsState()
    val attemptsUsed by deviceAuthViewModel.attemptsUsed.collectAsState()

    // The email this session is currently working with — set automatically
    // once local accounts are known (one match) or the person taps one
    // (several matches). Stays blank if zero local accounts were found,
    // which is when the fallback screen needs its own email field.
    var email by remember { mutableStateOf("") }

    var fallbackEmailInput by remember { mutableStateOf("") }
    var fallbackEmailError by remember { mutableStateOf<String?>(null) }
    var fallbackSending by remember { mutableStateOf(false) }
    var fallbackSent by remember { mutableStateOf(false) }
    var fallbackError by remember { mutableStateOf<String?>(null) }

    // Kick off local-account detection once, when the dialog first appears.
    LaunchedEffect(Unit) {
        deviceAuthViewModel.startForgotPassword()
    }

    // Reset device-auth state whenever this dialog is torn down, so a stale
    // Success/Failed from a previous attempt never leaks into a fresh open.
    DisposableEffect(Unit) {
        onDispose { deviceAuthViewModel.resetVerifyState() }
    }

    // As soon as the backend hands back a challenge, immediately trigger the
    // native biometric prompt — no separate "continue" tap needed.
    LaunchedEffect(verifyState) {
        when (val s = verifyState) {
            is DeviceVerifyUiState.VerifiedReady ->
                deviceAuthViewModel.authenticateAndLogin(
                    activity = activity,
                    userId = s.userId,
                    email = email,
                    deviceId = s.deviceId,
                    challenge = s.challenge
                )
            is DeviceVerifyUiState.LoggedIn -> onDeviceSignIn(s.signInToken)
            else -> Unit
        }
    }

    val emailRegex = remember { Regex("^[^\\s@]+@[^\\s@]+\\.[a-zA-Z]{2,24}$") }

    fun retry() {
        deviceAuthViewModel.checkDeviceTrust(email)
    }

    fun pickAccount(accountEmail: String) {
        email = accountEmail
        deviceAuthViewModel.pickAccount(accountEmail)
    }

    fun sendFallbackEmail() {
        if (fallbackSending || fallbackSent) return
        val targetEmail = email.ifBlank {
            val typed = fallbackEmailInput.trim()
            if (!emailRegex.matches(typed)) {
                fallbackEmailError = "Enter a valid email"
                return
            }
            typed
        }
        fallbackEmailError = null
        fallbackSending = true
        fallbackError = null
        deviceAuthViewModel.sendFallbackEmail(targetEmail) { success, message ->
            fallbackSending = false
            if (success) fallbackSent = true else fallbackError = message ?: "Couldn't send the reset email. Please try again."
        }
    }

    // Drawn as a normal composable inside the screen's own Box - NOT a
    // platform Dialog() window. A Dialog gets its own Android window, and
    // that window's dim only covers the window's own bounds; when the
    // keyboard opens, the window shrinks (adjustResize) and the part of the
    // screen that falls outside the shrunk window keeps showing the Sign In
    // page underneath at full brightness, undimmed. Rendering the scrim as
    // fillMaxSize() content in the same composition sidesteps that entirely
    // - it always covers the full screen no matter what the keyboard does.
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = verifyState,
                    transitionSpec = {
                        (fadeIn(tween(400, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(
                                animationSpec = tween(400, easing = FastOutSlowInEasing),
                                initialOffsetX = { it / 6 }
                            )).togetherWith(fadeOut(tween(150)))
                    },
                    label = "forgotStepContent"
                ) { state ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ModalBadge(
                            symbol = when (state) {
                                is DeviceVerifyUiState.ChooseAccount -> BadgeSymbol.Question
                                is DeviceVerifyUiState.LoggedIn -> BadgeSymbol.Lock
                                else -> BadgeSymbol.Fingerprint
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        when (state) {
                            DeviceVerifyUiState.CheckingLocalAccounts, DeviceVerifyUiState.CheckingTrust, DeviceVerifyUiState.Idle -> {
                                Text("Checking this device", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    "One moment while we check your account recovery settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                CircularProgressIndicator(color = AuraCyan, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            is DeviceVerifyUiState.ChooseAccount -> {
                                Text("Which account?", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    "This device has more than one account set up.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                state.accounts.forEach { account ->
                                    val masked = remember(account.email) {
                                        val at = account.email.indexOf('@')
                                        if (at <= 1) account.email
                                        else account.email.first() + "***" + account.email.substring(at)
                                    }
                                    Surface(
                                        onClick = { pickAccount(account.email) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = DarkSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(masked, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                                Text(account.deviceLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            }
                                        }
                                    }
                                }
                            }

                            is DeviceVerifyUiState.VerifiedReady, DeviceVerifyUiState.Authenticating, DeviceVerifyUiState.VerifyingWithBackend -> {
                                Text(
                                    if (state == DeviceVerifyUiState.VerifyingWithBackend) "Confirming..." else "Verify it's you",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    "Use your device's fingerprint, face, or PIN to sign in.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                CircularProgressIndicator(color = AuraCyan, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            is DeviceVerifyUiState.LoggedIn -> {
                                // onDeviceSignIn(...) already fired from the LaunchedEffect above;
                                // this only shows for the brief moment before the dialog closes.
                                Text("Verified", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Spacer(modifier = Modifier.height(20.dp))
                                CircularProgressIndicator(color = AuraSuccess, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            DeviceVerifyUiState.NotTrustedDevice -> {
                                val needsEmail = email.isBlank()
                                Text(
                                    if (needsEmail) "No device recovery set up" else "This device isn't registered",
                                    style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center
                                )
                                Text(
                                    if (attemptsUsed >= DeviceAuthViewModel.MAX_LOGIN_ATTEMPTS)
                                        "That didn't match after a few tries. We can email you a reset link instead."
                                    else
                                        "We couldn't verify this as a trusted device for account recovery. We can email you a reset link instead.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (fallbackSent) {
                                    Text(
                                        "Reset link sent - check your inbox.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AuraSuccess,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    GradientCtaButton(
                                        label = "Done",
                                        isLoading = false,
                                        isSuccess = false,
                                        enabled = true,
                                        onClick = onDismiss
                                    )
                                } else {
                                    if (needsEmail) {
                                        AuraAuthTextField(
                                            value = fallbackEmailInput,
                                            onValueChange = { fallbackEmailInput = it; fallbackEmailError = null },
                                            label = "Email address",
                                            leadingIcon = Icons.Default.Email,
                                            isError = fallbackEmailError != null,
                                            errorText = fallbackEmailError,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    fallbackError?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF6B6B), textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    GradientCtaButton(
                                        label = "Email me a reset link",
                                        isLoading = fallbackSending,
                                        isSuccess = false,
                                        enabled = !needsEmail || fallbackEmailInput.isNotBlank(),
                                        onClick = { sendFallbackEmail() }
                                    )
                                }
                            }

                            DeviceVerifyUiState.NoSecurityConfigured -> {
                                Text("Set up device security first", style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
                                Text(
                                    "This device doesn't have a screen lock or fingerprint set up yet, so it can't be used for account recovery.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                GradientCtaButton(
                                    label = "Open security settings",
                                    isLoading = false,
                                    isSuccess = false,
                                    enabled = true,
                                    onClick = {
                                        try {
                                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                                        } catch (e: Exception) {
                                            onToast("Couldn't open settings on this device.")
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SecondaryCtaButton(
                                    label = "Email me a reset link instead",
                                    onClick = { sendFallbackEmail() }
                                )
                                if (fallbackSent) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Reset link sent - check your inbox.", style = MaterialTheme.typography.bodySmall, color = AuraSuccess, textAlign = TextAlign.Center)
                                }
                            }

                            DeviceVerifyUiState.KeyInvalidated -> {
                                Text("Device security has changed", style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
                                Text(
                                    "Your device security settings have changed since this device was registered. Please use the email reset link instead.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (fallbackSent) {
                                    Text("Reset link sent - check your inbox.", style = MaterialTheme.typography.bodySmall, color = AuraSuccess, textAlign = TextAlign.Center)
                                } else {
                                    GradientCtaButton(
                                        label = "Email me a reset link",
                                        isLoading = fallbackSending,
                                        isSuccess = false,
                                        enabled = true,
                                        onClick = { sendFallbackEmail() }
                                    )
                                }
                            }

                            is DeviceVerifyUiState.Failed -> {
                                Text("Device verification failed", style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
                                Text(
                                    state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Attempt $attemptsUsed of ${DeviceAuthViewModel.MAX_LOGIN_ATTEMPTS}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                GradientCtaButton(
                                    label = "Try again",
                                    isLoading = false,
                                    isSuccess = false,
                                    enabled = true,
                                    onClick = { retry() }
                                )
                            }

                            DeviceVerifyUiState.Cancelled -> {
                                Text("Verification cancelled", style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
                                Text(
                                    "No changes were made. You can try again whenever you're ready.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Attempt $attemptsUsed of ${DeviceAuthViewModel.MAX_LOGIN_ATTEMPTS}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                GradientCtaButton(
                                    label = "Try again",
                                    isLoading = false,
                                    isSuccess = false,
                                    enabled = true,
                                    onClick = { retry() }
                                )
                            }

                            DeviceVerifyUiState.ResetSuccess -> {
                                // Unused by this dialog now — a verified signature
                                // signs the person in directly (see LoggedIn above).
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Shown once, right after a normal (non-guest) login, on a device that has
 * true biometric hardware enrolled and no device key yet. Purely opt-in —
 * skipping it is always one tap away and never blocks getting into the app.
 * Reuses the same scrim/card/badge visual language as ForgotPasswordDialog.
 */
@Composable
private fun DeviceRegistrationPromptDialog(
    activity: FragmentActivity,
    deviceAuthViewModel: DeviceAuthViewModel,
    user: com.aura.launcher.domain.model.User,
    onDone: () -> Unit
) {
    val registerState by deviceAuthViewModel.registerState.collectAsState()
    val deviceLabel = remember { "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim() }

    LaunchedEffect(registerState) {
        when (registerState) {
            DeviceRegisterUiState.Registered,
            DeviceRegisterUiState.Skipped,
            DeviceRegisterUiState.NotEligible -> {
                delay(if (registerState == DeviceRegisterUiState.Registered) 1100 else 0)
                deviceAuthViewModel.resetRegisterState()
                onDone()
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ModalBadge(
                    symbol = if (registerState == DeviceRegisterUiState.Registered) BadgeSymbol.Success else BadgeSymbol.Fingerprint
                )
                Spacer(modifier = Modifier.height(14.dp))

                when (registerState) {
                    DeviceRegisterUiState.Registering, DeviceRegisterUiState.Prompting -> {
                        Text("Setting up device recovery", style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = AuraCyan, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    DeviceRegisterUiState.Registered -> {
                        Text("Device recovery enabled", style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
                        Text(
                            "You can now use this device's security to reset your password if you ever forget it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is DeviceRegisterUiState.Failed -> {
                        Text("Couldn't enable device recovery", style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
                        Text(
                            (registerState as DeviceRegisterUiState.Failed).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SecondaryCtaButton(label = "Skip for now", onClick = { deviceAuthViewModel.skipRegistration() })
                    }
                    else -> {
                        Text("Enable device recovery?", style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
                        Text(
                            "Use fingerprint, face unlock, or your device PIN to reset your password later — no email codes needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GradientCtaButton(
                            label = "Enable",
                            isLoading = false,
                            isSuccess = false,
                            enabled = true,
                            onClick = { deviceAuthViewModel.registerDevice(activity, user.id, user.email, deviceLabel) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SecondaryCtaButton(label = "Not now", onClick = { deviceAuthViewModel.skipRegistration() })
                    }
                }
            }
        }
    }
}

/** Smaller gradient CTA used inside the forgot-password dialog (Send code / Verify code). */
@Composable
private fun GradientCtaButton(
    label: String,
    isLoading: Boolean,
    isSuccess: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val normalBrush = authCtaNormalBrush()
    val successBrush = authCtaSuccessBrush()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ctaScale"
    )
    val onDarkText = AuthCtaOnDarkText

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.5f }
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSuccess) successBrush else normalBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading && !isSuccess,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = when {
                isSuccess -> AuthButtonVisualState.Success
                isLoading -> AuthButtonVisualState.Loading
                else -> AuthButtonVisualState.Label
            },
            label = "ctaContent"
        ) { state ->
            when (state) {
                AuthButtonVisualState.Loading -> CircularProgressIndicator(color = onDarkText, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                AuthButtonVisualState.Success -> Icon(Icons.Default.Check, contentDescription = null, tint = onDarkText)
                AuthButtonVisualState.Label -> Text(label, fontWeight = FontWeight.Bold, color = onDarkText)
            }
        }
    }
}

private enum class BadgeSymbol { Question, Lock, Fingerprint, Success }

/** Outline/subtle-fill secondary action, used for "Continue to dashboard" next to the
 * gradient "Update password" CTA — matches `.btn-secondary` in the web prototype
 * (a real bordered button, not a bare text link, so it reads as a genuine second choice). */
@Composable
private fun SecondaryCtaButton(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "secondaryCtaScale"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.08f else 0.04f,
        animationSpec = tween(150),
        label = "secondaryCtaBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextSecondary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

/**
 * The rotating conic ring + pulsing centre mark used at the top of the
 * forgot-password modal (matches `.modal__badge` in the approved design —
 * a spinning cyan→purple→pink ring with a gradient "?" or lock icon
 * breathing in the middle, not a plain filled circle).
 */
@Composable
private fun ModalBadge(symbol: BadgeSymbol) {
    val infiniteTransition = rememberInfiniteTransition(label = "modalBadge")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "modalBadgeRing"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "modalBadgePulse"
    )
    val ringBrush = remember { Brush.sweepGradient(listOf(AuraCyan, AuraPurple, AuraPink, AuraCyan)) }

    // Success ring pulses (scale + fade out, looping) instead of rotating —
    // matches `successRingPulse` in the web prototype's CSS.
    val successPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)),
        label = "modalBadgeSuccessPulse"
    )
    val successPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)),
        label = "modalBadgeSuccessPulseAlpha"
    )

    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        if (symbol == BadgeSymbol.Success) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { scaleX = successPulse; scaleY = successPulse; alpha = successPulseAlpha }
                    .border(2.dp, AuraSuccess, CircleShape)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(2.dp, AuraSuccess.copy(alpha = 0.5f), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { rotationZ = ringRotation }
                    .border(3.dp, ringBrush, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DarkSurface)
        )
        Box(
            modifier = Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse },
            contentAlignment = Alignment.Center
        ) {
            when (symbol) {
                BadgeSymbol.Question -> Text(
                    "?",
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(AuraCyan, AuraPurple)),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                BadgeSymbol.Lock -> Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = AuraCyan,
                    modifier = Modifier.size(24.dp)
                )
                BadgeSymbol.Fingerprint -> Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = AuraCyan,
                    modifier = Modifier.size(26.dp)
                )
                BadgeSymbol.Success -> {
                    var checkVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { checkVisible = true }
                    val checkScale by animateFloatAsState(
                        targetValue = if (checkVisible) 1f else 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "modalBadgeCheckPop"
                    )
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = AuraSuccess,
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer { scaleX = checkScale; scaleY = checkScale; alpha = checkScale }
                    )
                }
            }
        }
    }
}

@Composable
private fun AuraToast(
    message: String?,
    onConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(2200)
            onConsumed()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        Surface(
            color = DarkSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Text(
                text = message.orEmpty(),
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Background scene: gradient glows, slow orbit rings, a faceted "crystal" hero and
 * a handful of floating shards — a native-Android equivalent of the web prototype's
 * particle canvas + 3D crystal (no cursor/pointer on phones, so the motion is
 * ambient/looping instead of pointer-driven, per touch-device guidance).
 */
@Composable
private fun AuraAuthBackdrop(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "auraBackdrop")

    // NOTE: kept as State<Float> (no `by`) on purpose. Reading `.value` only
    // inside graphicsLayer{}/Canvas{} draw-phase lambdas below means each tick
    // only invalidates drawing — not composition/layout — which is what keeps
    // this smooth. Destructuring with `by` here previously forced the whole
    // function to recompose (and the shard Boxes to re-layout) every frame.
    val crystalFloatAnim = infinite.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "crystalFloat"
    )
    val facetRotationAnim = infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "facetRotation"
    )
    val ringRotationAnim = infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing)),
        label = "ringRotation"
    )
    val particleTimeAnim = infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "particleTime"
    )

    val particles = remember {
        val random = Random(42)
        List(28) {
            ParticleSpec(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = random.nextFloat() * 2.2f + 0.6f,
                speed = random.nextFloat() * 0.6f + 0.3f,
                isCyan = random.nextBoolean()
            )
        }
    }

    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(AuraPurple.copy(alpha = 0.10f), DarkBg),
                center = Offset(0.3f, 0.15f),
                radius = 1400f
            )
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // .value read here (draw phase) — does not recompose the composable.
            val ringRotation = ringRotationAnim.value
            val particleTime = particleTimeAnim.value

            // ambient glows
            drawCircle(
                color = AuraPurple.copy(alpha = 0.10f),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * 0.1f, size.height * 0.05f)
            )
            drawCircle(
                color = AuraCyan.copy(alpha = 0.08f),
                radius = size.minDimension * 0.4f,
                center = Offset(size.width * 0.95f, size.height * 0.9f)
            )

            // orbit rings
            val ringCenter = Offset(size.width * 0.5f, size.height * 0.4f)
            rotate(degrees = ringRotation, pivot = ringCenter) {
                drawCircle(
                    color = AuraCyan.copy(alpha = 0.14f),
                    radius = size.minDimension * 0.42f,
                    center = ringCenter,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
            rotate(degrees = -ringRotation * 1.5f, pivot = ringCenter) {
                drawCircle(
                    color = AuraPurple.copy(alpha = 0.16f),
                    radius = size.minDimension * 0.30f,
                    center = ringCenter,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }

            // drifting particles
            particles.forEach { p ->
                val progress = (p.y - particleTime * p.speed).let { v -> ((v % 1f) + 1f) % 1f }
                val cx = p.x * size.width
                val cy = progress * size.height
                drawCircle(
                    color = if (p.isCyan) AuraCyan.copy(alpha = 0.35f) else AuraPurple.copy(alpha = 0.35f),
                    radius = p.radius.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }

        // Floating shards orbiting the crystal.
        // Position/rotation are set inside graphicsLayer{} (translationX/Y, rotationZ)
        // instead of Modifier.offset(), so the Box's own layout is computed once and
        // only the compositing layer is touched every frame — no per-frame relayout.
        val shardHues = listOf(AuraCyan, AuraPink, AuraPurple, AuraCyan, AuraPink)
        Box(modifier = Modifier.fillMaxSize()) {
            shardHues.forEachIndexed { index, hue ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size((28 + index * 2).dp)
                        .graphicsLayer {
                            val facetRotation = facetRotationAnim.value
                            val crystalFloat = crystalFloatAnim.value
                            val angle = (index / shardHues.size.toFloat()) * 2 * Math.PI.toFloat() + (facetRotation * 0.01f)
                            val radius = 130f
                            val offsetX = cos(angle) * radius
                            val offsetY = (sin(angle) * radius) * 0.6f + crystalFloat * 0.4f - 40f
                            translationX = offsetX.dp.toPx()
                            translationY = offsetY.dp.toPx()
                            rotationZ = 45f
                        }
                        .clip(RoundedCornerShape(6.dp))
                        .background(hue.copy(alpha = 0.35f))
                        .border(1.dp, hue.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                )
            }

            // The crystal hero
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(120.dp)
                    .graphicsLayer {
                        translationY = (-40f + crystalFloatAnim.value).dp.toPx()
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(120.dp)
                        .graphicsLayer { rotationZ = 45f + facetRotationAnim.value * 0.15f }
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(listOf(AuraCyan.copy(alpha = 0.55f), Color.Transparent))
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(76.dp)
                        .graphicsLayer { rotationZ = 45f }
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(listOf(AuraPurple, AuraPurple.copy(alpha = 0.15f)))
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .graphicsLayer { rotationZ = 45f }
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                )
            }
        }
    }
}

private data class ParticleSpec(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val isCyan: Boolean
)