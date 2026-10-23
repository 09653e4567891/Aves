package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.GlassConfig
import com.example.provider.GlassSettingsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aves Liquid Glass", appName)
  }

  @Test
  fun `verify glass presets exist`() {
    assertTrue(GlassConfig.PRESETS.isNotEmpty())
    val firstPreset = GlassConfig.PRESETS.first()
    assertEquals("Apple Liquid Glass", firstPreset.presetName)
    assertTrue(firstPreset.refraction > 1.0f)
    assertTrue(firstPreset.chromaticAberration > 0f)
  }

  @Test
  fun `verify glass settings provider persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val config = GlassConfig(refraction = 1.88f, chromaticAberration = 0.77f)
    GlassSettingsProvider.saveConfig(context, config)

    val loaded = GlassSettingsProvider.getSavedConfig(context)
    assertEquals(1.88f, loaded.refraction, 0.01f)
    assertEquals(0.77f, loaded.chromaticAberration, 0.01f)
  }
}
