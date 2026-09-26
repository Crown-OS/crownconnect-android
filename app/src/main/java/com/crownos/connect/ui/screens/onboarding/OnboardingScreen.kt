package com.crownos.connect.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.crownos.connect.permissions.AppPermission
import com.crownos.connect.ui.kit.ButtonVariant
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.LucideIcon
import com.crownos.connect.ui.layout.HeroWizard
import com.crownos.connect.ui.screens.permissions.PermissionsViewModel
import com.crownos.connect.ui.screens.permissions.RuntimePermissionsCard
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType

private enum class OnboardingStep(val title: String, val subtitle: String, val heroIcon: Int) {
    Welcome(
        "Your phone, part of CrownOS",
        "Share screens, use this phone as a webcam and microphone, and keep the clipboard and notifications in sync.",
        CrownIcons.MonitorSmartphone,
    ),
    Access("A few permissions", "CrownConnect only uses them while you are paired with a computer.", CrownIcons.ShieldCheck),
    Pair("Pair your computer", "Open Cross-device in CrownOS Settings and choose Pair a device.", CrownIcons.QrCode),
}

@Composable
fun OnboardingScreen(onFinished: (pairNow: Boolean) -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = OnboardingStep.entries[index]
    val permissions = hiltViewModel<PermissionsViewModel>()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissions.refresh() }

    HeroWizard(
        title = step.title,
        subtitle = step.subtitle,
        steps = OnboardingStep.entries.size,
        completed = index + 1,
        hero = { LucideIcon(step.heroIcon, size = 72.dp, strokeWidth = 1.4f, color = CrownTheme.palette.text.onAccent) },
        action = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step == OnboardingStep.Pair) {
                    CrownButton("Pair a computer", { onFinished(true) }, Modifier.fillMaxWidth(), variant = ButtonVariant.Primary)
                    CrownButton("Later", { onFinished(false) }, Modifier.fillMaxWidth(), variant = ButtonVariant.Ghost)
                } else {
                    CrownButton("Continue", { index += 1 }, Modifier.fillMaxWidth(), variant = ButtonVariant.Primary)
                }
            }
        },
    ) {
        when (step) {
            OnboardingStep.Welcome -> BasicText(
                "Everything travels over your own network, end-to-end encrypted, with nothing in the cloud.",
                style = CrownType.paragraph.copy(color = CrownTheme.palette.text.body),
            )
            OnboardingStep.Access -> RuntimePermissionsCard(AppPermission.essential.filter { it.applies }, permissions, title = "Needed")
            OnboardingStep.Pair -> Unit
        }
    }
}
