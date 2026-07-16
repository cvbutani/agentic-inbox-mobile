package com.sonicstarsolutions.agentic.inbox.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Wraps back-stack mutations so every call site goes through the same, correct primitives
 * instead of poking [NavBackStack] directly.
 *
 * NavDisplay reads the back stack reactively on every mutation and requires it to be non-empty at
 * all times — any recomposition triggered while the list is momentarily empty crashes its scene
 * computation. Every method here is written so the list's size never drops to zero at any point,
 * not even transiently between two calls.
 */
class Navigator(val backStack: NavBackStack<NavKey>) {

    fun goTo(destination: AppDestination) {
        backStack.add(destination)
    }

    /** No-op at the root — popping the last entry would leave NavDisplay with an empty stack. */
    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    /**
     * Replaces the entire stack with a single [destination]. Trims down to one entry and then
     * overwrites it in place, rather than clear() + add(), so the list is never briefly empty.
     */
    fun replaceRoot(destination: AppDestination) {
        backStack.add(destination)
    }
}
