package com.sonicstarsolutions.agentic.inbox.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {

    @Test
    fun `goTo pushes the destination onto the stack`() {
        val backStack = NavBackStack<NavKey>(Splash)
        val navigator = Navigator(backStack)

        navigator.goTo(Onboarding)

        assertEquals(listOf(Splash, Onboarding), backStack.toList())
    }

    @Test
    fun `goBack pops the top entry when more than one remains`() {
        val backStack = NavBackStack<NavKey>(Splash, Onboarding)
        val navigator = Navigator(backStack)

        navigator.goBack()

        assertEquals(listOf(Splash), backStack.toList())
    }

    @Test
    fun `goBack is a no-op when only one entry remains`() {
        val backStack = NavBackStack<NavKey>(Splash)
        val navigator = Navigator(backStack)

        navigator.goBack()

        assertEquals(listOf(Splash), backStack.toList())
    }

    @Test
    fun `replaceRoot leaves only the new destination on the stack`() {
        val backStack = NavBackStack<NavKey>(Splash, Onboarding, MailboxPicker)
        val navigator = Navigator(backStack)

        navigator.replaceRoot(Inbox(mailboxId = "m1", mailboxName = "Inbox"))

        assertEquals(listOf(Inbox(mailboxId = "m1", mailboxName = "Inbox")), backStack.toList())
    }

    @Test
    fun `replaceRoot on a single-entry stack still ends with one entry`() {
        val backStack = NavBackStack<NavKey>(Onboarding)
        val navigator = Navigator(backStack)

        navigator.replaceRoot(MailboxPicker)

        assertEquals(listOf(MailboxPicker), backStack.toList())
    }
}
