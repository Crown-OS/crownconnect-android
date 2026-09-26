package com.crownos.connect.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.crownos.connect.util.lerpPremultiplied as mix

@Immutable
data class SurfaceColors(
    val background: Color,
    val raised: Color,
    val sunken: Color,
    val border: Color,
    val hairline: Color,
    val hover: Color,
    val shadow: Color,
) {
    fun lerp(to: SurfaceColors, t: Float) = SurfaceColors(
        mix(background, to.background, t), mix(raised, to.raised, t), mix(sunken, to.sunken, t),
        mix(border, to.border, t), mix(hairline, to.hairline, t), mix(hover, to.hover, t),
        mix(shadow, to.shadow, t),
    )
}

@Immutable
data class TextColors(
    val primary: Color,
    val body: Color,
    val muted: Color,
    val disabled: Color,
    val onAccent: Color,
    val icon: Color,
    val iconMuted: Color,
) {
    fun lerp(to: TextColors, t: Float) = TextColors(
        mix(primary, to.primary, t), mix(body, to.body, t), mix(muted, to.muted, t),
        mix(disabled, to.disabled, t), mix(onAccent, to.onAccent, t), mix(icon, to.icon, t),
        mix(iconMuted, to.iconMuted, t),
    )
}

@Immutable
data class ControlColors(
    val track: Color,
    val knob: Color,
    val knobShadow: Color,
    val toggleOff: Gradient,
) {
    fun lerp(to: ControlColors, t: Float) = ControlColors(
        mix(track, to.track, t), mix(knob, to.knob, t), mix(knobShadow, to.knobShadow, t),
        toggleOff.lerp(to.toggleOff, t),
    )
}

@Immutable
data class StatusColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val neutral: Color,
    val dangerBackground: Color,
    val onDanger: Color,
) {
    fun lerp(to: StatusColors, t: Float) = StatusColors(
        mix(success, to.success, t), mix(warning, to.warning, t), mix(danger, to.danger, t),
        mix(neutral, to.neutral, t), mix(dangerBackground, to.dangerBackground, t),
        mix(onDanger, to.onDanger, t),
    )
}

@Immutable
data class PopoverColors(
    val background: Color,
    val border: Color,
    val text: Color,
    val mutedText: Color,
    val hoverBackground: Color,
    val triggerBackground: Color,
) {
    fun lerp(to: PopoverColors, t: Float) = PopoverColors(
        mix(background, to.background, t), mix(border, to.border, t), mix(text, to.text, t),
        mix(mutedText, to.mutedText, t), mix(hoverBackground, to.hoverBackground, t),
        mix(triggerBackground, to.triggerBackground, t),
    )
}

@Immutable
data class RadioColors(
    val unselectedBackground: Color,
    val unselectedBorder: Color,
    val hole: Color,
    val text: Color,
) {
    fun lerp(to: RadioColors, t: Float) = RadioColors(
        mix(unselectedBackground, to.unselectedBackground, t),
        mix(unselectedBorder, to.unselectedBorder, t), mix(hole, to.hole, t), mix(text, to.text, t),
    )
}

@Immutable
data class MenuColors(
    val background: Color,
    val border: Color,
    val text: Color,
    val shortcutText: Color,
    val disabledText: Color,
    val separator: Color,
    val selectedText: Color,
) {
    fun lerp(to: MenuColors, t: Float) = MenuColors(
        mix(background, to.background, t), mix(border, to.border, t), mix(text, to.text, t),
        mix(shortcutText, to.shortcutText, t), mix(disabledText, to.disabledText, t),
        mix(separator, to.separator, t), mix(selectedText, to.selectedText, t),
    )
}

@Immutable
data class SidebarColors(
    val background: Color,
    val separator: Color,
    val text: Color,
    val selectedText: Color,
    val subitemText: Color,
    val brandText: Color,
    val icon: Color,
    val selectedIcon: Color,
    val hoverBackground: Color,
    val selectedBackground: Color,
    val selectedBorder: Color,
) {
    fun lerp(to: SidebarColors, t: Float) = SidebarColors(
        mix(background, to.background, t), mix(separator, to.separator, t), mix(text, to.text, t),
        mix(selectedText, to.selectedText, t), mix(subitemText, to.subitemText, t),
        mix(brandText, to.brandText, t), mix(icon, to.icon, t), mix(selectedIcon, to.selectedIcon, t),
        mix(hoverBackground, to.hoverBackground, t), mix(selectedBackground, to.selectedBackground, t),
        mix(selectedBorder, to.selectedBorder, t),
    )
}

@Immutable
data class GlassColors(
    val tintTop: Color,
    val tintBottom: Color,
    val highlight: Color,
    val border: Color,
    val label: Color,
) {
    fun lerp(to: GlassColors, t: Float) = GlassColors(
        mix(tintTop, to.tintTop, t), mix(tintBottom, to.tintBottom, t), mix(highlight, to.highlight, t),
        mix(border, to.border, t), mix(label, to.label, t),
    )
}

@Immutable
data class CrownPalette(
    val isDark: Boolean,
    val surface: SurfaceColors,
    val text: TextColors,
    val control: ControlColors,
    val status: StatusColors,
    val popover: PopoverColors,
    val radio: RadioColors,
    val menu: MenuColors,
    val sidebar: SidebarColors,
    val glass: GlassColors,
) {
    fun lerp(to: CrownPalette, t: Float) = CrownPalette(
        isDark = if (t < 0.5f) isDark else to.isDark,
        surface = surface.lerp(to.surface, t),
        text = text.lerp(to.text, t),
        control = control.lerp(to.control, t),
        status = status.lerp(to.status, t),
        popover = popover.lerp(to.popover, t),
        radio = radio.lerp(to.radio, t),
        menu = menu.lerp(to.menu, t),
        sidebar = sidebar.lerp(to.sidebar, t),
        glass = glass.lerp(to.glass, t),
    )

    companion object {
        val Light = CrownPalette(
            isDark = false,
            surface = SurfaceColors(
                background = Color(0xEEF3F3F3),
                raised = Color(0xFFFFFFFF),
                sunken = Color(0xFFF2F2F6),
                border = Color(0x121B1B2A),
                hairline = Color(0x0C1B1B2A),
                hover = Color(0x0C2A1F45),
                shadow = Color(0x0D14141F),
            ),
            text = TextColors(
                primary = Color(0xFF17151F),
                body = Color(0xFF403E4A),
                muted = Color(0xFF8B8996),
                disabled = Color(0xFFC2C0CB),
                onAccent = Color(0xFFFFFFFF),
                icon = Color(0xFF3D3B47),
                iconMuted = Color(0xFF78768A),
            ),
            control = ControlColors(
                track = Color(0xFFE7E5F1),
                knob = Color(0xFFFFFFFF),
                knobShadow = Color(0x0D000000),
                toggleOff = Gradient(Color(0xFFEDEBF6), Color(0xFFD9D6E8)),
            ),
            status = StatusColors(
                success = Color(0xFF159C4E),
                warning = Color(0xFFB45409),
                danger = Color(0xFFC22727),
                neutral = Color(0xFF8A8A8F),
                dangerBackground = Color(0xFFDC2A2A),
                onDanger = Color(0xFFFFFFFF),
            ),
            popover = PopoverColors(
                background = Color(0xFFFFFFFF),
                border = Color(0xFFE7E5F1),
                text = Color(0xFF1B1926),
                mutedText = Color(0xFF78768A),
                hoverBackground = Color(0xFFF1F0F7),
                triggerBackground = Color(0xFFFFFFFF),
            ),
            radio = RadioColors(
                unselectedBackground = Color(0xFFFFFFFF),
                unselectedBorder = Color(0xFFD9D6E8),
                hole = Color(0xFFFFFFFF),
                text = Color(0xFF1B1926),
            ),
            menu = MenuColors(
                background = Color(0xFFF9F8FD),
                border = Color(0xFFE7E5F1),
                text = Color(0xFF1E1C29),
                shortcutText = Color(0xFF8E8C99),
                disabledText = Color(0xFFC2C0CB),
                separator = Color(0xFFE7E5F1),
                selectedText = Color(0xFFFFFFFF),
            ),
            sidebar = SidebarColors(
                background = Color(0x00F3F3F3),
                separator = Color(0xFFEAE8F2),
                text = Color(0xFF646464),
                selectedText = Color(0xFF545454),
                subitemText = Color(0xFF676473),
                brandText = Color(0xFF17151F),
                icon = Color(0xFF646464),
                selectedIcon = Color(0xFF545454),
                hoverBackground = Color(0x082A1F45),
                selectedBackground = Color(0x80B0B0B0),
                selectedBorder = Color(0x002A1F45),
            ),
            glass = GlassColors(
                tintTop = Color(0xB3F5F5F7),
                tintBottom = Color(0xC2DEDEE3),
                highlight = Color(0x8CFFFFFF),
                border = Color(0x73FFFFFF),
                label = Color(0xFF1C1C21),
            ),
        )

        val Dark = CrownPalette(
            isDark = true,
            surface = SurfaceColors(
                background = Color(0xCC0F0F10),
                raised = Color(0xFF171718),
                sunken = Color(0xFF121212),
                border = Color(0xFF27272A),
                hairline = Color(0xFF232326),
                hover = Color(0x10FFFFFF),
                shadow = Color(0x59000000),
            ),
            text = TextColors(
                primary = Color(0xFFFAFAFA),
                body = Color(0xFFD4D4D8),
                muted = Color(0xFF9F9F9F),
                disabled = Color(0xFF5A5A60),
                onAccent = Color(0xFFFFFFFF),
                icon = Color(0xFFD4D4D8),
                iconMuted = Color(0xFF8A8A93),
            ),
            control = ControlColors(
                track = Color(0xFF3A3A3D),
                knob = Color(0xFFFAFAFA),
                knobShadow = Color(0x73000000),
                toggleOff = Gradient(Color(0xFF3F3F46), Color(0xFF2A2A2E)),
            ),
            status = StatusColors(
                success = Color(0xFF4ADE80),
                warning = Color(0xFFFBBF24),
                danger = Color(0xFFF87171),
                neutral = Color(0xFF8A8A93),
                dangerBackground = Color(0xFFC12B2B),
                onDanger = Color(0xFFFFFFFF),
            ),
            popover = PopoverColors(
                background = Color(0xFF1D1D1D),
                border = Color(0xFF282828),
                text = Color(0xFFFAFAFA),
                mutedText = Color(0xFF9F9F9F),
                hoverBackground = Color(0xFF2A2A2A),
                triggerBackground = Color(0xFF1C1C1C),
            ),
            radio = RadioColors(
                unselectedBackground = Color(0xFF1C1C1C),
                unselectedBorder = Color(0xFF3E3E3E),
                hole = Color(0xFF1D1D1D),
                text = Color(0xFFFAFAFA),
            ),
            menu = MenuColors(
                background = Color(0xFF1D1D1D),
                border = Color(0xFF2A2A2A),
                text = Color(0xFFFAFAFA),
                shortcutText = Color(0xFF8E8E93),
                disabledText = Color(0xFF5A5A60),
                separator = Color(0xFF282828),
                selectedText = Color(0xFFFFFFFF),
            ),
            sidebar = SidebarColors(
                background = Color(0x00131314),
                separator = Color(0xFF262628),
                text = Color(0xFFA1A1AA),
                selectedText = Color(0xFFFAFAFA),
                subitemText = Color(0xFF8A8A93),
                brandText = Color(0xFFFAFAFA),
                icon = Color(0xFF8A8A93),
                selectedIcon = Color(0xFFFAFAFA),
                hoverBackground = Color(0x0EFFFFFF),
                selectedBackground = Color(0xFF232326),
                selectedBorder = Color(0x1CFFFFFF),
            ),
            glass = GlassColors(
                tintTop = Color(0xE0333338),
                tintBottom = Color(0xB8212126),
                highlight = Color(0x1AFFFFFF),
                border = Color(0x2EFFFFFF),
                label = Color(0xFFF0F0F5),
            ),
        )
    }
}
