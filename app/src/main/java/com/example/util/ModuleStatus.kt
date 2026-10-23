package com.example.util

object ModuleStatus {
    /**
     * Default implementation returns false.
     * When the LSPosed module is active and scoped to this app,
     * LSPosed hooks this method and replaces the result with true.
     * Contains ZERO Xposed imports so it never causes NoClassDefFoundError.
     */
    @JvmStatic
    fun isModuleActive(): Boolean = false
}
