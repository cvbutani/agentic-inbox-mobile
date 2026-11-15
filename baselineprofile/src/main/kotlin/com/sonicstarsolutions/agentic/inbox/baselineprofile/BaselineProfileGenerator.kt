package com.sonicstarsolutions.agentic.inbox.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val TARGET_PACKAGE = "com.sonicstarsolutions.agentic.inbox"
private const val TIMEOUT_MS = 15_000L

/**
 * Generates the app's Baseline Profile and Startup Profile.
 *
 * The app is pointed at [StubWorker] rather than a real deployment, so the journeys can sign in
 * and reach the inbox without credentials. The app's `nonMinifiedRelease` variant, the one these
 * tests run against, allows cleartext to loopback for exactly this; release does not.
 *
 * Only the two `startup*` tests feed the Startup Profile (DEX layout). Everything here feeds the
 * Baseline Profile.
 */
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    private val worker = StubWorker()
    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun startWorker() {
        worker.start()
    }

    @After
    fun stopWorker() {
        // Don't leave the app signed in to a Worker that is about to stop answering.
        prepareApp(signedIn = false)
        worker.shutdown()
    }

    /** Cold start on a fresh install: Splash, then Onboarding. */
    @Test
    fun startupSignedOut() {
        prepareApp(signedIn = false)
        rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            device.awaitLanding()
        }
    }

    /** Cold start for a returning user: Splash, then the mailbox picker loading its list. */
    @Test
    fun startupSignedIn() {
        prepareApp(signedIn = true)
        rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            // Not asserted to be the picker: on devices where the rule has to reinstall the app
            // to reset its profile, the saved sign-in is gone and this lands on Onboarding.
            if (device.awaitLanding() == Landing.MailboxPicker) device.waitFor(By.text(MAILBOX_NAME))
        }
    }

    /** Sign in, read mail, search, open the composer, switch folder, sign out. */
    @Test
    fun journeys() {
        rule.collect(packageName = TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()
            with(device) {
                // Always sign in afresh: Onboarding is part of the journey, and a sign-in left
                // over from an earlier run points at a stub that is no longer listening.
                if (awaitLanding() == Landing.MailboxPicker) signOut()
                connectTo(worker.baseUrl)

                waitFor(By.text(MAILBOX_NAME)).click()
                waitFor(By.desc(MENU))
                scrollInbox()
                readFirstThread()
                search()
                openComposer()
                switchFolder()

                // Leaves the next iteration, and the device, at Onboarding.
                pressBack()
                signOut()
            }
        }
    }

    /** Puts the app in a known sign-in state, outside of any profile collection. */
    private fun prepareApp(signedIn: Boolean) = with(device) {
        executeShellCommand("monkey -p $TARGET_PACKAGE -c android.intent.category.LAUNCHER 1")
        if (awaitLanding() == Landing.MailboxPicker) signOut()
        if (signedIn) connectTo(worker.baseUrl)
        executeShellCommand("am force-stop $TARGET_PACKAGE")
    }
}

private const val MAILBOX_NAME = "Support"
private const val FIRST_SUBJECT = "Weekly review item 0"
private const val CONNECT = "Connect"
private const val MENU = "Menu"
private const val SIGN_OUT = "Sign out"
private const val BACK = "Back"

private enum class Landing { Onboarding, MailboxPicker }

/** Splash routes to one of two screens; waits for whichever it turns out to be. */
private fun UiDevice.awaitLanding(): Landing {
    val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
    while (SystemClock.uptimeMillis() < deadline) {
        if (hasObject(By.desc(SIGN_OUT))) return Landing.MailboxPicker
        if (hasObject(By.text(CONNECT))) return Landing.Onboarding
        SystemClock.sleep(100)
    }
    error("App reached neither Onboarding nor the mailbox picker within ${TIMEOUT_MS}ms")
}

private fun UiDevice.connectTo(workerUrl: String) {
    // Set through accessibility rather than typed: `input text` races the keyboard coming up and
    // drops leading characters, which turned the URL into "ttp://...".
    waitFor(By.clazz("android.widget.EditText"))
    val fields = findObjects(By.clazz("android.widget.EditText"))
    listOf(workerUrl, "profile-generator", "not-a-real-secret").forEachIndexed { index, value ->
        fields[index].text = value
    }
    waitFor(By.text(CONNECT)).click()
    waitFor(By.desc(SIGN_OUT))
}

private fun UiDevice.signOut() {
    waitFor(By.desc(SIGN_OUT)).click()
    // The dialog's title carries the same words as its confirm button, and Compose puts the click
    // handler on the button around the text rather than on the text itself. The button comes
    // after the title, so it is the last match; clicking its text lands inside the button.
    waitFor(By.text(SIGN_OUT))
    findObjects(By.text(SIGN_OUT)).last().click()
    waitFor(By.text(CONNECT))
}

private fun UiDevice.scrollInbox() {
    waitFor(By.text(FIRST_SUBJECT))
    val list = waitFor(By.scrollable(true))
    // Keep clear of the screen edges, where a swipe is a system back gesture.
    list.setGestureMargin(displayWidth / 5)
    repeat(4) { list.fling(Direction.DOWN) }
    repeat(5) { list.fling(Direction.UP) }
}

private fun UiDevice.readFirstThread() {
    waitFor(By.text(FIRST_SUBJECT)).click()
    // "Reply" sits under the last message, so it showing means the thread has loaded and laid out.
    waitFor(By.text("Reply"))
    findObject(By.scrollable(true))?.run {
        setGestureMargin(displayWidth / 5)
        fling(Direction.DOWN)
        fling(Direction.UP)
    }
    // A wide window shows the thread in a pane beside the list, with Close in place of Back.
    (findObject(By.desc(BACK)) ?: waitFor(By.desc("Close"))).click()
    waitFor(By.desc(MENU))
}

private fun UiDevice.search() {
    waitFor(By.desc("Search")).click()
    waitFor(By.clazz("android.widget.EditText")).text = "review"
    waitFor(By.text(FIRST_SUBJECT))
    waitFor(By.desc(BACK)).click()
    waitFor(By.desc(MENU))
}

private fun UiDevice.openComposer() {
    composeFab().click()
    waitFor(By.desc("Send"))
    // Nothing is typed, so there is no draft to save and nothing is left behind.
    waitFor(By.desc("Cancel")).click()
    waitFor(By.desc(MENU))
}

private fun UiDevice.switchFolder() {
    waitFor(By.desc(MENU)).click()
    // The drawer slides in; a tap during the animation lands on the scrim and is lost, so wait for
    // it to settle and tap again if the drawer is still up.
    for (attempt in 1..3) {
        waitForIdle()
        waitFor(By.text("Sent")).click()
        if (wait(Until.gone(By.text("Switch mailbox")), 3_000) == true) break
    }
    waitFor(By.text(FIRST_SUBJECT))
}

private fun UiDevice.waitFor(selector: BySelector): UiObject2 =
    wait(Until.findObject(selector), TIMEOUT_MS)
        ?: error("Timed out waiting for $selector. On screen: ${visibleLabels()}")

/** Every text and content description currently on screen, so a timeout says what was there. */
private fun UiDevice.visibleLabels(): String {
    val xml = java.io.ByteArrayOutputStream().also { dumpWindowHierarchy(it) }.toString()
    val labels = Regex("""(?:text|content-desc)="([^"]+)"""").findAll(xml).map { it.groupValues[1] }.joinToString(" | ")
    val clickable = Regex("""<node [^>]*class="([^"]+)"[^>]*clickable="true"[^>]*bounds="([^"]+)"""")
        .findAll(xml).joinToString(" ; ") { "${it.groupValues[1].substringAfterLast('.')}${it.groupValues[2]}" }
    return "$labels || CLICKABLE: $clickable"
}

/**
 * The inbox's Compose button. Compose merges the button's label into a node uiautomator reports
 * with no text, so it can't be found by "Compose"; it is the one wide clickable view in the
 * lower right, which row star buttons (narrow) and rows (full width) are not.
 */
private fun UiDevice.composeFab(): UiObject2 {
    waitFor(By.desc(MENU))
    return findObjects(By.clickable(true)).firstOrNull {
        val bounds = it.visibleBounds
        bounds.left > displayWidth / 2 && bounds.width() > displayWidth / 5
    } ?: error("Compose button not found. On screen: ${visibleLabels()}")
}
