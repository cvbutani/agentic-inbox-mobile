package com.sonicstarsolutions.agentic.inbox.ui.inbox

import androidx.compose.material3.SwipeToDismissBoxValue
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeAction
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SwipeActionTest {

    private fun email(read: Boolean = false, starred: Boolean = false) = EmailSummary(
        id = "e1",
        subject = "Subject",
        sender = "a@example.dev",
        recipient = "b@example.dev",
        date = "2026-07-16T00:00:00Z",
        read = read,
        starred = starred,
        threadId = null,
        folderId = "inbox",
        snippet = null,
    )

    @Test
    fun `by default right archives and left deletes`() {
        val settings = SwipeSettings()

        assertEquals(SwipeAction.Archive, settings.actionFor(SwipeToDismissBoxValue.StartToEnd))
        assertEquals(SwipeAction.Delete, settings.actionFor(SwipeToDismissBoxValue.EndToStart))
    }

    @Test
    fun `no swipe has no action`() {
        assertNull(SwipeSettings().actionFor(SwipeToDismissBoxValue.Settled))
    }

    @Test
    fun `each direction follows its own configured action`() {
        val settings = SwipeSettings(swipeRight = SwipeAction.ToggleRead, swipeLeft = SwipeAction.ToggleStar)

        assertEquals(SwipeAction.ToggleRead, settings.actionFor(SwipeToDismissBoxValue.StartToEnd))
        assertEquals(SwipeAction.ToggleStar, settings.actionFor(SwipeToDismissBoxValue.EndToStart))
    }

    @Test
    fun `a direction set to None reports None rather than no swipe`() {
        val settings = SwipeSettings(swipeRight = SwipeAction.None)

        assertEquals(SwipeAction.None, settings.actionFor(SwipeToDismissBoxValue.StartToEnd))
    }

    @Test
    fun `fixed actions are labelled by what they do`() {
        assertEquals("Archive", SwipeAction.Archive.labelFor(email()))
        assertEquals("Delete", SwipeAction.Delete.labelFor(email()))
    }

    @Test
    fun `the read toggle names the change it will make`() {
        assertEquals("Mark read", SwipeAction.ToggleRead.labelFor(email(read = false)))
        assertEquals("Mark unread", SwipeAction.ToggleRead.labelFor(email(read = true)))
    }

    @Test
    fun `the star toggle names the change it will make`() {
        assertEquals("Star", SwipeAction.ToggleStar.labelFor(email(starred = false)))
        assertEquals("Unstar", SwipeAction.ToggleStar.labelFor(email(starred = true)))
    }
}
