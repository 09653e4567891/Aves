package com.example.shader

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import com.example.model.GlassConfig

object PrismalShader {

    // Apple iOS & VisionOS Precision Liquid Glass Optical Shader
    const val AGSL_SOURCE = """
        uniform float2 uResolution;
        uniform float uTime;
        uniform float uRefraction;
        uniform float uChromaticAberration;
        uniform float uViscosity;
        uniform float uFresnelGlow;
        uniform float4 uTintColor;
        uniform float2 uTouch;
        uniform float uTouchRadius;
        uniform shader uContent;

        vec4 main(vec2 fragCoord) {
            vec2 uv = fragCoord / uResolution;
            vec2 centered = uv - vec2(0.5);
            
            // Optical bevel curvature (lens bending at pill borders)
            float distFromCenter = length(centered * 2.0);
            float bevelZone = smoothstep(0.2, 1.0, distFromCenter);
            vec2 lensNormal = normalize(centered + vec2(0.0001));
            
            // Apple refractive displacement (Snell's law bevel lens)
            float iorDelta = (uRefraction - 1.0);
            vec2 refractionDisp = lensNormal * pow(bevelZone, 1.8) * iorDelta * 0.028;

            // Touch ripple displacement (subtle elastic glass shockwave)
            if (uTouchRadius > 0.0) {
                vec2 delta = fragCoord - uTouch;
                float d = length(delta);
                if (d < uTouchRadius) {
                    float ring = sin(d * 0.15 - uTime * 10.0) * (1.0 - d / uTouchRadius);
                    refractionDisp += normalize(delta + vec2(0.001)) * ring * 0.02 * uViscosity;
                }
            }

            // Chromatic dispersion (RGB wavelength split on bevel edges)
            float dispersion = uChromaticAberration * 0.016 * bevelZone;
            vec2 uvR = uv - refractionDisp * (1.0 + dispersion);
            vec2 uvG = uv - refractionDisp;
            vec2 uvB = uv - refractionDisp * (1.0 - dispersion);

            float r = uContent.eval(clamp(uvR, vec2(0.0), vec2(1.0)) * uResolution).r;
            float g = uContent.eval(clamp(uvG, vec2(0.0), vec2(1.0)) * uResolution).g;
            float b = uContent.eval(clamp(uvB, vec2(0.0), vec2(1.0)) * uResolution).b;
            float a = uContent.eval(clamp(uv, vec2(0.0), vec2(1.0)) * uResolution).a;

            // Apple Cut-Crystal Specular Rim Lighting (Top-Left light source)
            vec2 lightDir = normalize(vec2(-0.707, -0.707));
            float rimDot = dot(lensNormal, lightDir);
            float topRim = pow(clamp(rimDot, 0.0, 1.0), 3.5) * bevelZone * uFresnelGlow * 0.35;
            float bottomShadow = clamp(-rimDot * 0.12, 0.0, 0.18) * bevelZone;

            // Apple Dynamic Glass Sheen Glint (diagonal glint sweep)
            float glintPos = fract(uTime * 0.08) * 3.5 - 0.8;
            float distToGlint = (uv.x + uv.y) - glintPos;
            float glint = exp(-distToGlint * distToGlint * 45.0) * 0.20 * bevelZone;

            // Surface frosted tint blend
            vec3 col = vec3(r, g, b);
            col = mix(col, uTintColor.rgb, uTintColor.a * 0.6);
            col += vec3(topRim + glint);
            col -= vec3(bottomShadow);

            return vec4(clamp(col, 0.0, 1.0), a);
        }
    """

    fun createRuntimeShader(
        config: GlassConfig,
        width: Float,
        height: Float,
        timeSeconds: Float,
        touchX: Float = 0f,
        touchY: Float = 0f,
        touchRadius: Float = 0f
    ): RuntimeShader? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return try {
            val shader = RuntimeShader(AGSL_SOURCE)
            shader.setFloatUniform("uResolution", width.coerceAtLeast(1f), height.coerceAtLeast(1f))
            shader.setFloatUniform("uTime", timeSeconds)
            shader.setFloatUniform("uRefraction", config.refraction)
            shader.setFloatUniform("uChromaticAberration", config.chromaticAberration)
            shader.setFloatUniform("uViscosity", config.viscosity)
            shader.setFloatUniform("uFresnelGlow", config.fresnelGlow)

            // RGBA tint
            val hex = config.tintColorHex
            val a = ((hex shr 24) and 0xFF) / 255f
            val r = ((hex shr 16) and 0xFF) / 255f
            val g = ((hex shr 8) and 0xFF) / 255f
            val b = (hex and 0xFF) / 255f
            shader.setFloatUniform("uTintColor", r, g, b, a)

            shader.setFloatUniform("uTouch", touchX, touchY)
            shader.setFloatUniform("uTouchRadius", touchRadius)
            shader
        } catch (_: Throwable) {
            null
        }
    }

    fun createRenderEffect(
        config: GlassConfig,
        width: Float,
        height: Float,
        timeSeconds: Float,
        touchX: Float = 0f,
        touchY: Float = 0f,
        touchRadius: Float = 0f
    ): RenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val blurEffect = RenderEffect.createBlurEffect(
            config.blurRadius.coerceAtLeast(1f),
            config.blurRadius.coerceAtLeast(1f),
            Shader.TileMode.CLAMP
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val runtimeShader = createRuntimeShader(config, width, height, timeSeconds, touchX, touchY, touchRadius)
            if (runtimeShader != null) {
                val shaderEffect = RenderEffect.createRuntimeShaderEffect(runtimeShader, "uContent")
                return RenderEffect.createChainEffect(shaderEffect, blurEffect)
            }
        }
        return blurEffect
    }
}
