package com.sonicstarsolutions.agentic.inbox.util

/**
 * Decides whether a scrolling list is close enough to its end to fetch the next page — e.g. in
 * a 50-item list, the last visible row at index 47 (47 >= 50 - 3) should trigger a fetch.
 * Shared by the inbox list (InboxScreen) and the search results list (SearchScreen) so both
 * screens use the same "how close is close enough" rule for infinite scroll.
 */
object InfiniteScrollUtils {

    fun shouldLoadMore(
        lastVisibleItemIndex: Int?,
        totalItemsCount: Int,
        threshold: Int = 3,
    ): Boolean = lastVisibleItemIndex != null && lastVisibleItemIndex >= totalItemsCount - threshold
}
