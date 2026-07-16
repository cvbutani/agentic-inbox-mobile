package com.sonicstarsolutions.agentic.inbox

import androidx.compose.runtime.Composable
import com.sonicstarsolutions.agentic.inbox.di.commonModules
import com.sonicstarsolutions.agentic.inbox.navigation.AppNavHost
import com.sonicstarsolutions.agentic.inbox.theme.AppTheme
import org.koin.compose.KoinApplication
import org.koin.core.module.Module

@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
    appModule: List<Module> = emptyList(),
) {
    KoinApplication(
        application = {
            // Fail fast on duplicate definitions. Koin's default silently lets the last-loaded
            // module win — which once let a bare platform HttpClient shadow networkModule's
            // configured one (no defaultRequest URL -> every request went to localhost).
            allowOverride(false)
            modules(commonModules + appModule)
        }
    ) {
        AppTheme(onThemeChanged = onThemeChanged) {
            AppNavHost()
        }
    }
}
