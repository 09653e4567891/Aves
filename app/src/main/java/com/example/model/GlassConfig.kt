package com.example.model

data class GlassConfig(
    val isEnabled: Boolean = true,
    val refraction: Float = 1.25f, // 1.0 (air) to 2.2 (diamond/crystal)
    val chromaticAberration: Float = 0.22f, // 0.0 to 1.0 (Prism dispersion)
    val viscosity: Float = 0.15f, // 0.0 to 1.0
    val blurRadius: Float = 32.0f, // 0.0 to 60.0 dp
    val fresnelGlow: Float = 2.6f, // 1.0 to 5.0 (specular rim gloss)
    val tintColorHex: Long = 0x18FFFFFF, // pristine Apple crystal tint
    val touchRippleEnabled: Boolean = true,
    val replaceSystemBlur: Boolean = true,
    val injectAvesHeaders: Boolean = true,
    val injectAvesNav: Boolean = true,
    val presetName: String = "Apple Liquid Glass"
) {
    companion object {
        val PRESETS = listOf(
            GlassConfig(
                presetName = "Apple Liquid Glass",
                refraction = 1.25f,
                chromaticAberration = 0.22f,
                viscosity = 0.15f,
                blurRadius = 32.0f,
                fresnelGlow = 2.6f,
                tintColorHex = 0x18FFFFFF
            ),
            GlassConfig(
                presetName = "VisionOS Acrylic",
                refraction = 1.45f,
                chromaticAberration = 0.35f,
                viscosity = 0.25f,
                blurRadius = 26.0f,
                fresnelGlow = 3.2f,
                tintColorHex = 0x2290D5FF
            ),
            GlassConfig(
                presetName = "Cyber Neon Glass",
                refraction = 1.85f,
                chromaticAberration = 0.85f,
                viscosity = 0.55f,
                blurRadius = 20.0f,
                fresnelGlow = 4.5f,
                tintColorHex = 0x28FF007F
            ),
            GlassConfig(
                presetName = "Deep Ocean Fluid",
                refraction = 1.38f,
                chromaticAberration = 0.50f,
                viscosity = 0.70f,
                blurRadius = 28.0f,
                fresnelGlow = 2.9f,
                tintColorHex = 0x300088CC
            ),
            GlassConfig(
                presetName = "Frosted Obsidian",
                refraction = 1.50f,
                chromaticAberration = 0.15f,
                viscosity = 0.10f,
                blurRadius = 40.0f,
                fresnelGlow = 1.8f,
                tintColorHex = 0x4010141E
            )
        )
    }
}
