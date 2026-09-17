package com.sonicstarsolutions.agentic.inbox.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

private const val TARGET_PACKAGE = "com.sonicstarsolutions.agentic.inbox"

class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()

        // A fresh install always lands on Onboarding (no credentials saved yet). That alone
        // is the journey worth profiling here: it covers cold-start class loading/verification
        // for Activity creation, Koin's DI graph wiring, and the Compose/Navigation 3 host --
        // the bulk of what a baseline profile buys. Scripting past Onboarding into the inbox
        // needs live Cloudflare Worker credentials, which this instrumented test intentionally
        // doesn't have or fabricate. Extend this once the app has a scriptable demo/offline
        // mode meant to ship (DemoDataModule.kt today is explicitly screenshot-only and not
        // committed).
        device.waitForIdle()
    }
}
