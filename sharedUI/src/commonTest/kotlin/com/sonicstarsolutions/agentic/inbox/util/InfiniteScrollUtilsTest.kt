package com.sonicstarsolutions.agentic.inbox.util

import kotlin.test.Test
import kotlin.test.assertEquals

class InfiniteScrollUtilsTest {

    @Test
    fun `a null last-visible index (nothing laid out yet) does not trigger a load`() {
        assertEquals(false, InfiniteScrollUtils.shouldLoadMore(lastVisibleItemIndex = null, totalItemsCount = 50))
    }

    @Test
    fun `an empty list with zero total items does not trigger a load`() {
        assertEquals(false, InfiniteScrollUtils.shouldLoadMore(lastVisibleItemIndex = null, totalItemsCount = 0))
    }

    @Test
    fun `scrolled well short of the end does not trigger a load`() {
        assertEquals(false, InfiniteScrollUtils.shouldLoadMore(lastVisibleItemIndex = 10, totalItemsCount = 50))
    }

    @Test
    fun `scrolled to just outside the threshold does not trigger a load`() {
        assertEquals(false, InfiniteScrollUtils.shouldLoadMore(lastVisibleItemIndex = 46, totalItemsCount = 50))
    }

    @Test
    fun `reaching exactly the threshold distance from the end triggers a load`() {
        assertEquals(true, InfiniteScrollUtils.shouldLoadMore(lastVisibleItemIndex = 47, totalItemsCount = 50))
    }

    @Test
    fun `scrolled past the threshold toward the end triggers a load`() {
        assertEquals(true, InfiniteScrollUtils.shouldLoadMore(lastVisibleItemIndex = 49, totalItemsCount = 50))
    }

    @Test
    fun `a custom threshold is honored instead of the default`() {
        assertEquals(true, InfiniteScrollUtils.shouldLoadMore(lastVisibleItemIndex = 8, totalItemsCount = 10, threshold = 5))
    }
}
