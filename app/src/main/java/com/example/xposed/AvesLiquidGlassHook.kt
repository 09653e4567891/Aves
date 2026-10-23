package com.example.xposed

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.view.SurfaceView
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.example.model.GlassConfig
import com.example.shader.PrismalShader
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class AvesLiquidGlassHook : IXposedHookLoadPackage {

    companion object {
        const val PROVIDER_URI = "content://com.aistudio.liquidglassaves.wpxktq.provider/settings"
        private val TARGET_PACKAGES = setOf(
            "deckers.thibault.aves",
            "deckers.thibault.aves.debug",
            "deckers.thibault.aves.libre"
        )
        const val SELF_PACKAGE = "com.aistudio.liquidglassaves.wpxktq"

        @Volatile
        private var cachedConfig: GlassConfig? = null
        @Volatile
        private var lastConfigFetchTime: Long = 0L
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 1. Self-hook check for companion management app
        if (lpparam.packageName == SELF_PACKAGE || lpparam.packageName == "com.example") {
            try {
                val statusClass = XposedHelpers.findClass("com.example.util.ModuleStatus", lpparam.classLoader)
                XposedHelpers.findAndHookMethod(
                    statusClass,
                    "isModuleActive",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = true
                        }
                    }
                )
                XposedBridge.log("AvesLiquidGlass: Self activation hook installed in ${lpparam.packageName}")
            } catch (e: Throwable) {
                XposedBridge.log("AvesLiquidGlass: Self check hook note: ${e.message}")
            }
        }

        val isTarget = TARGET_PACKAGES.contains(lpparam.packageName) || lpparam.packageName.contains("thibault.aves")
        if (!isTarget) {
            return
        }

        XposedBridge.log("AvesLiquidGlass: Target Aves Gallery detected: ${lpparam.packageName}. Initializing Liquid Glass UI Engine...")

        try {
            // 2. Intercept RenderEffect.createBlurEffect to replace flat Gaussian blur with Apple Liquid Glass
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                hookRenderEffectCreation()
            }

            // 3. Intercept View.setRenderEffect on UI elements
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                hookViewSetRenderEffect()
            }

            // 4. Intercept Window.setBackgroundBlurRadius for sheets, popups, and dialogs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                hookWindowBlur()
            }

            // 5. Hook Activity lifecycle to enable window blur behind on Aves windows
            hookActivityLifecycle()

        } catch (t: Throwable) {
            XposedBridge.log("AvesLiquidGlass: Failed to hook Aves Gallery: ${t.message}")
        }
    }

    /**
     * Intercepts RenderEffect.createBlurEffect so every blurred UI element in Aves
     * gets transformed into an authentic Apple Liquid Glass optical shader effect.
     */
    private fun hookRenderEffectCreation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            // Overload 1: createBlurEffect(float radiusX, float radiusY, Shader.TileMode edgeTreatment)
            XposedHelpers.findAndHookMethod(
                RenderEffect::class.java,
                "createBlurEffect",
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Shader.TileMode::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val originalBlur = param.result as? RenderEffect ?: return
                        val config = getOrFetchConfig(null)
                        if (!config.isEnabled || !config.replaceSystemBlur) return

                        val enhanced = wrapWithLiquidGlass(originalBlur, config)
                        if (enhanced != null) {
                            param.result = enhanced
                        }
                    }
                }
            )

            // Overload 2: createBlurEffect(float radiusX, float radiusY, RenderEffect inputEffect, Shader.TileMode edgeTreatment)
            XposedHelpers.findAndHookMethod(
                RenderEffect::class.java,
                "createBlurEffect",
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                RenderEffect::class.java,
                Shader.TileMode::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val originalBlur = param.result as? RenderEffect ?: return
                        val config = getOrFetchConfig(null)
                        if (!config.isEnabled || !config.replaceSystemBlur) return

                        val enhanced = wrapWithLiquidGlass(originalBlur, config)
                        if (enhanced != null) {
                            param.result = enhanced
                        }
                    }
                }
            )
            XposedBridge.log("AvesLiquidGlass: Hooked RenderEffect.createBlurEffect successfully")
        } catch (t: Throwable) {
            XposedBridge.log("AvesLiquidGlass: RenderEffect hook note: ${t.message}")
        }
    }

    /**
     * Chains the AGSL Liquid Glass runtime shader (refraction + chromatic dispersion + specular rim)
     * onto an existing blur RenderEffect.
     */
    private fun wrapWithLiquidGlass(blurEffect: RenderEffect, config: GlassConfig): RenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return try {
            val runtimeShader = PrismalShader.createRuntimeShader(
                config = config,
                width = 1080f,
                height = 2400f,
                timeSeconds = 1.0f
            ) ?: return null

            val liquidShaderEffect = RenderEffect.createRuntimeShaderEffect(runtimeShader, "uContent")
            RenderEffect.createChainEffect(liquidShaderEffect, blurEffect)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Intercepts View.setRenderEffect on individual UI elements (app bars, buttons, sheets)
     * to upgrade their visual style without touching SurfaceView.
     */
    private fun hookViewSetRenderEffect() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "setRenderEffect",
                RenderEffect::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        // CRITICAL: NEVER apply to SurfaceView or decorView root to avoid crashes
                        if (view is SurfaceView) return
                        val viewIdName = try { view.resources?.getResourceEntryName(view.id) } catch (_: Throwable) { null }
                        if (viewIdName == "content") return

                        val currentEffect = param.args[0] as? RenderEffect ?: return
                        val config = getOrFetchConfig(view.context)
                        if (!config.isEnabled || !config.replaceSystemBlur) return

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val w = view.width.toFloat().coerceAtLeast(300f)
                            val h = view.height.toFloat().coerceAtLeast(100f)
                            val runtimeShader = PrismalShader.createRuntimeShader(config, w, h, 1.0f)
                            if (runtimeShader != null) {
                                try {
                                    val liquidEffect = RenderEffect.createRuntimeShaderEffect(runtimeShader, "uContent")
                                    param.args[0] = RenderEffect.createChainEffect(liquidEffect, currentEffect)
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                }
            )
            XposedBridge.log("AvesLiquidGlass: Hooked View.setRenderEffect successfully")
        } catch (t: Throwable) {
            XposedBridge.log("AvesLiquidGlass: View.setRenderEffect hook note: ${t.message}")
        }
    }

    /**
     * Hooks Window.setBackgroundBlurRadius so popups, bottom sheets, and dialogs
     * in Aves use the customized Liquid Glass blur radius.
     */
    private fun hookWindowBlur() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            XposedHelpers.findAndHookMethod(
                Window::class.java,
                "setBackgroundBlurRadius",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val window = param.thisObject as? Window ?: return
                        val config = getOrFetchConfig(window.context)
                        if (config.isEnabled && config.replaceSystemBlur) {
                            val requested = param.args[0] as? Int ?: 0
                            if (requested > 0) {
                                param.args[0] = (config.blurRadius * 1.5f).toInt().coerceAtLeast(requested)
                                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                            }
                        }
                    }
                }
            )
            XposedBridge.log("AvesLiquidGlass: Hooked Window.setBackgroundBlurRadius successfully")
        } catch (t: Throwable) {
            XposedBridge.log("AvesLiquidGlass: Window blur hook note: ${t.message}")
        }
    }

    /**
     * Hooks Aves activity lifecycle to ensure window blur-behind is supported on Android 12+.
     */
    private fun hookActivityLifecycle() {
        try {
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        val config = getOrFetchConfig(activity)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && config.isEnabled && config.replaceSystemBlur) {
                            try {
                                activity.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                            } catch (_: Throwable) {}
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("AvesLiquidGlass: Activity lifecycle hook note: ${t.message}")
        }
    }

    /**
     * Retrieves the latest GlassConfig from cache, ContentProvider, or local SharedPreferences.
     */
    private fun getOrFetchConfig(context: Context?): GlassConfig {
        val now = System.currentTimeMillis()
        val cached = cachedConfig
        if (cached != null && (now - lastConfigFetchTime < 2500L)) {
            return cached
        }

        if (context == null) {
            return cached ?: GlassConfig()
        }

        val freshConfig = fetchConfigInternal(context)
        cachedConfig = freshConfig
        lastConfigFetchTime = now
        return freshConfig
    }

    private fun fetchConfigInternal(context: Context): GlassConfig {
        // 1. Try ContentProvider via call()
        try {
            val bundle = context.contentResolver.call(
                Uri.parse(PROVIDER_URI),
                "getConfig", null, null
            )
            if (bundle != null && bundle.containsKey("isEnabled")) {
                return GlassConfig(
                    isEnabled = bundle.getBoolean("isEnabled", true),
                    refraction = bundle.getFloat("refraction", 1.25f),
                    chromaticAberration = bundle.getFloat("chromaticAberration", 0.22f),
                    viscosity = bundle.getFloat("viscosity", 0.15f),
                    blurRadius = bundle.getFloat("blurRadius", 32.0f),
                    fresnelGlow = bundle.getFloat("fresnelGlow", 2.6f),
                    tintColorHex = bundle.getLong("tintColorHex", 0x18FFFFFF),
                    touchRippleEnabled = bundle.getBoolean("touchRippleEnabled", true),
                    replaceSystemBlur = bundle.getBoolean("replaceSystemBlur", true),
                    injectAvesHeaders = bundle.getBoolean("injectAvesHeaders", true),
                    injectAvesNav = bundle.getBoolean("injectAvesNav", true),
                    presetName = bundle.getString("presetName", "Apple Liquid Glass") ?: "Apple Liquid Glass"
                )
            }
        } catch (_: Throwable) {}

        // 2. Try query()
        try {
            val cursor: Cursor? = context.contentResolver.query(
                Uri.parse(PROVIDER_URI),
                null, null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    return GlassConfig(
                        isEnabled = it.getInt(it.getColumnIndexOrThrow("isEnabled")) == 1,
                        refraction = it.getFloat(it.getColumnIndexOrThrow("refraction")),
                        chromaticAberration = it.getFloat(it.getColumnIndexOrThrow("chromaticAberration")),
                        viscosity = it.getFloat(it.getColumnIndexOrThrow("viscosity")),
                        blurRadius = it.getFloat(it.getColumnIndexOrThrow("blurRadius")),
                        fresnelGlow = it.getFloat(it.getColumnIndexOrThrow("fresnelGlow")),
                        tintColorHex = it.getLong(it.getColumnIndexOrThrow("tintColorHex")),
                        touchRippleEnabled = it.getInt(it.getColumnIndexOrThrow("touchRippleEnabled")) == 1,
                        replaceSystemBlur = it.getInt(it.getColumnIndexOrThrow("replaceSystemBlur")) == 1,
                        injectAvesHeaders = it.getInt(it.getColumnIndexOrThrow("injectAvesHeaders")) == 1,
                        injectAvesNav = it.getInt(it.getColumnIndexOrThrow("injectAvesNav")) == 1,
                        presetName = it.getString(it.getColumnIndexOrThrow("presetName")) ?: "Apple Liquid Glass"
                    )
                }
            }
        } catch (_: Throwable) {}

        // 3. Try SharedPreferences
        try {
            val sp = context.getSharedPreferences("aves_liquid_glass_prefs", Context.MODE_PRIVATE)
            return GlassConfig(
                isEnabled = sp.getBoolean("isEnabled", true),
                refraction = sp.getFloat("refraction", 1.25f),
                chromaticAberration = sp.getFloat("chromaticAberration", 0.22f),
                viscosity = sp.getFloat("viscosity", 0.15f),
                blurRadius = sp.getFloat("blurRadius", 32.0f),
                fresnelGlow = sp.getFloat("fresnelGlow", 2.6f),
                tintColorHex = sp.getLong("tintColorHex", 0x18FFFFFF),
                touchRippleEnabled = sp.getBoolean("touchRippleEnabled", true),
                replaceSystemBlur = sp.getBoolean("replaceSystemBlur", true),
                injectAvesHeaders = sp.getBoolean("injectAvesHeaders", true),
                injectAvesNav = sp.getBoolean("injectAvesNav", true),
                presetName = sp.getString("presetName", "Apple Liquid Glass") ?: "Apple Liquid Glass"
            )
        } catch (_: Throwable) {}

        return GlassConfig()
    }
}
