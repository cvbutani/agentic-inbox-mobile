package com.sonicstarsolutions.agentic.inbox.ui.inbox

import androidx.compose.material3.SwipeToDismissBoxValue
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeAction
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeSettings

/**
 * The action the user configured for a drag in [direction]. Null while the row is at rest, so
 * "no swipe" and "swipe configured to do nothing" ([SwipeAction.None]) stay distinguishable.
 */
fun SwipeSettings.actionFor(direction: SwipeToDismissBoxValue): SwipeAction? = when (direction) {
    SwipeToDismissBoxValue.StartToEnd -> swipeRight
    SwipeToDismissBoxValue.EndToStart -> swipeLeft
    SwipeToDismissBoxValue.Settled -> null
}
