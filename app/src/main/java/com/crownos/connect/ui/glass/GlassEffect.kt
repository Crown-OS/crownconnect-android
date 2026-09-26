package com.crownos.connect.ui.glass

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi

internal object GlassEffect {
    const val VIBRANCY = 1.3f
    const val SHEEN_ALPHA = 0.06f
    const val NOISE = 0.01f

    val supportsBlur: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    private val supportsShader: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @RequiresApi(Build.VERSION_CODES.S)
    fun create(geometry: GlassGeometry): RenderEffect {
        val blur = RenderEffect.createBlurEffect(geometry.blurRadius, geometry.blurRadius, Shader.TileMode.CLAMP)
        return if (supportsShader) finish(geometry, blur) else vibrancy(blur)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun vibrancy(blur: RenderEffect): RenderEffect {
        val saturation = ColorMatrix().apply { setSaturation(VIBRANCY) }
        return RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(saturation), blur)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun finish(geometry: GlassGeometry, blur: RenderEffect): RenderEffect {
        val shader = RuntimeShader(FINISH_SHADER).apply {
            setFloatUniform("glassSize", geometry.width, geometry.height)
            setFloatUniform("glassOrigin", geometry.padding, geometry.padding)
            setFloatUniform("cornerRadius", geometry.cornerRadius)
            setFloatUniform("rim", geometry.rim)
            setFloatUniform("saturation", VIBRANCY)
            setFloatUniform("sheen", SHEEN_ALPHA)
            setFloatUniform("noise", NOISE)
        }
        return RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"), blur)
    }

    private const val FINISH_SHADER = """
        uniform shader content;
        uniform float2 glassSize;
        uniform float2 glassOrigin;
        uniform float cornerRadius;
        uniform float rim;
        uniform float saturation;
        uniform float sheen;
        uniform float noise;

        const half3 LUMA = half3(0.2126, 0.7152, 0.0722);
        const float RIM_VIBRANCY = 0.45;
        const float INNER_SHADOW = 0.28;
        const float SPECULAR = 0.30;
        const float AMBIENT = 0.35;

        float hash(float2 p) {
            return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 coord) {
            float2 halfSize = glassSize * 0.5;
            float2 p = coord - glassOrigin - halfSize;
            float r = min(cornerRadius, min(halfSize.x, halfSize.y));
            float2 q = abs(p) - halfSize + r;
            float d = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
            float2 axis = (q.x > 0.0 || q.y > 0.0) ? normalize(max(q, 0.0) + 1e-5)
                                                  : (q.x > q.y ? float2(1.0, 0.0) : float2(0.0, 1.0));
            float2 normal = axis * sign(p + 1e-5);

            float bevel = rim > 0.0 ? exp(-max(-d, 0.0) / rim) : 0.0;
            float highlight = bevel * bevel;
            float facing = mix(AMBIENT, 1.0, abs(dot(normal, normalize(float2(-1.0, -1.0)))));

            half3 color = content.eval(coord - normal * (bevel * rim)).rgb;
            half luma = dot(color, LUMA);
            color = max(mix(half3(luma), color, saturation * (1.0 + RIM_VIBRANCY * bevel)), 0.0);
            color = mix(color, half3(1.0), sheen);
            color *= 1.0 - INNER_SHADOW * facing * bevel * (1.0 - highlight);
            color += SPECULAR * facing * highlight;
            color += (hash(coord) - 0.5) * noise;

            float coverage = 1.0 - smoothstep(-0.5, 0.5, d);
            return half4(color * coverage, coverage);
        }
    """
}

internal data class GlassGeometry(
    val width: Float,
    val height: Float,
    val padding: Float,
    val cornerRadius: Float,
    val rim: Float,
    val blurRadius: Float,
)
