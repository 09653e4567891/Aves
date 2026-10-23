package com.example.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import com.example.model.GlassConfig

class GlassSettingsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.aistudio.liquidglassaves.wpxktq.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/settings")
        private const val PREFS_NAME = "aves_liquid_glass_prefs"

        fun getSavedConfig(context: Context): GlassConfig {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return GlassConfig(
                isEnabled = sp.getBoolean("isEnabled", true),
                refraction = sp.getFloat("refraction", 1.35f),
                chromaticAberration = sp.getFloat("chromaticAberration", 0.45f),
                viscosity = sp.getFloat("viscosity", 0.35f),
                blurRadius = sp.getFloat("blurRadius", 25.0f),
                fresnelGlow = sp.getFloat("fresnelGlow", 2.8f),
                tintColorHex = sp.getLong("tintColorHex", 0x22A0D8EF),
                touchRippleEnabled = sp.getBoolean("touchRippleEnabled", true),
                replaceSystemBlur = sp.getBoolean("replaceSystemBlur", true),
                injectAvesHeaders = sp.getBoolean("injectAvesHeaders", true),
                injectAvesNav = sp.getBoolean("injectAvesNav", true),
                presetName = sp.getString("presetName", "Prismal Quartz") ?: "Prismal Quartz"
            )
        }

        fun saveConfig(context: Context, config: GlassConfig) {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit()
                .putBoolean("isEnabled", config.isEnabled)
                .putFloat("refraction", config.refraction)
                .putFloat("chromaticAberration", config.chromaticAberration)
                .putFloat("viscosity", config.viscosity)
                .putFloat("blurRadius", config.blurRadius)
                .putFloat("fresnelGlow", config.fresnelGlow)
                .putLong("tintColorHex", config.tintColorHex)
                .putBoolean("touchRippleEnabled", config.touchRippleEnabled)
                .putBoolean("replaceSystemBlur", config.replaceSystemBlur)
                .putBoolean("injectAvesHeaders", config.injectAvesHeaders)
                .putBoolean("injectAvesNav", config.injectAvesNav)
                .putString("presetName", config.presetName)
                .apply()

            try {
                context.contentResolver.notifyChange(CONTENT_URI, null)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(arrayOf(
            "isEnabled", "refraction", "chromaticAberration", "viscosity",
            "blurRadius", "fresnelGlow", "tintColorHex", "touchRippleEnabled",
            "replaceSystemBlur", "injectAvesHeaders", "injectAvesNav", "presetName"
        ))
        val ctx = context ?: return cursor
        val config = getSavedConfig(ctx)
        cursor.addRow(arrayOf<Any>(
            if (config.isEnabled) 1 else 0,
            config.refraction,
            config.chromaticAberration,
            config.viscosity,
            config.blurRadius,
            config.fresnelGlow,
            config.tintColorHex,
            if (config.touchRippleEnabled) 1 else 0,
            if (config.replaceSystemBlur) 1 else 0,
            if (config.injectAvesHeaders) 1 else 0,
            if (config.injectAvesNav) 1 else 0,
            config.presetName
        ))
        return cursor
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val bundle = Bundle()
        val context = context ?: return bundle
        if (method == "getConfig") {
            val cfg = getSavedConfig(context)
            bundle.putBoolean("isEnabled", cfg.isEnabled)
            bundle.putFloat("refraction", cfg.refraction)
            bundle.putFloat("chromaticAberration", cfg.chromaticAberration)
            bundle.putFloat("viscosity", cfg.viscosity)
            bundle.putFloat("blurRadius", cfg.blurRadius)
            bundle.putFloat("fresnelGlow", cfg.fresnelGlow)
            bundle.putLong("tintColorHex", cfg.tintColorHex)
            bundle.putBoolean("touchRippleEnabled", cfg.touchRippleEnabled)
            bundle.putBoolean("replaceSystemBlur", cfg.replaceSystemBlur)
            bundle.putBoolean("injectAvesHeaders", cfg.injectAvesHeaders)
            bundle.putBoolean("injectAvesNav", cfg.injectAvesNav)
            bundle.putString("presetName", cfg.presetName)
        }
        return bundle
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.avesliquidglass.settings"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
