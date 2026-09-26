package com.crownos.connect.ui.screens.device

import com.crownos.connect.protocol.Feature
import com.crownos.connect.ui.kit.CrownIcons

data class FeatureInfo(val title: String, val description: String, val icon: Int)

val Feature.info: FeatureInfo
    get() = when (this) {
        Feature.Mirror -> FeatureInfo("Screen sharing", "Show either screen on the other device", CrownIcons.ScreenShare)
        Feature.Camera -> FeatureInfo("Webcam", "Use this phone's camera as a webcam", CrownIcons.Webcam)
        Feature.Mic -> FeatureInfo("Microphone", "Use this phone as a microphone", CrownIcons.Mic)
        Feature.Monitor -> FeatureInfo("Second display", "Extend the computer's desktop onto this screen", CrownIcons.Monitor)
        Feature.Unicursor -> FeatureInfo("Shared cursor", "Move the computer's pointer onto this phone", CrownIcons.MousePointer2)
        Feature.Calls -> FeatureInfo("Calls", "Answer and place phone calls from the computer", CrownIcons.Phone)
        Feature.Clipboard -> FeatureInfo("Clipboard", "Copy on one device, paste on the other", CrownIcons.Clipboard)
        Feature.Notifications -> FeatureInfo("Notifications", "Show and reply to this phone's notifications", CrownIcons.Bell)
        Feature.Battery -> FeatureInfo("Battery", "Share battery levels both ways", CrownIcons.BatteryFull)
        Feature.Hotspot -> FeatureInfo("Hotspot", "Turn the hotspot on and off remotely", CrownIcons.RadioTower)
        Feature.Files -> FeatureInfo("Files", "Receive files sent from the computer", CrownIcons.FileDown)
    }
