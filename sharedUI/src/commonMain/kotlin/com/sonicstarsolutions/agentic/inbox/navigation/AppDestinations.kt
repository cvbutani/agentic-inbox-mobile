package com.sonicstarsolutions.agentic.inbox.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Type-safe navigation destinations for Navigation 3.
 *
 * Each destination is a [NavKey] carried directly on the back stack. Arguments travel as
 * constructor properties instead of encoded route strings, so they stay strongly typed.
 */
sealed interface AppDestination : NavKey

@Serializable
data object Splash : AppDestination

@Serializable
data object Onboarding : AppDestination

@Serializable
data object MailboxPicker : AppDestination

@Serializable
data class Inbox(
    val mailboxId: String,
    val mailboxName: String,
) : AppDestination

@Serializable
data class EmailThread(
    val mailboxId: String,
    val emailId: String,
    val threadId: String?,
) : AppDestination

/**
 * Back stack save/restore configuration.
 *
 * On Android alone the reflection-based [androidx.navigation3.runtime.rememberNavBackStack] overload
 * would work, but iOS (and any non-JVM target) has no reflection, so we register every destination
 * for polymorphic serialization of [NavKey] explicitly. This makes the back stack survivable across
 * process death and configuration changes on all targets.
 */
internal val appNavConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Splash::class, Splash.serializer())
            subclass(Onboarding::class, Onboarding.serializer())
            subclass(MailboxPicker::class, MailboxPicker.serializer())
            subclass(Inbox::class, Inbox.serializer())
            subclass(EmailThread::class, EmailThread.serializer())
        }
    }
}
