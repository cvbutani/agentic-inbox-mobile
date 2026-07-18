package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.local.CredentialsStorage
import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CredentialsRepositoryImplTest {

    private class FakeCredentialsStorage(
        var stored: Credentials = Credentials(),
    ) : CredentialsStorage {
        /** When set, [read] suspends here until completed — lets tests hold hydration in flight. */
        var readGate: CompletableDeferred<Unit>? = null
        val writes = mutableListOf<Credentials>()
        var deleteCount = 0

        override suspend fun read(): Credentials {
            readGate?.await()
            return stored
        }

        override suspend fun write(credentials: Credentials) {
            writes += credentials
            stored = credentials
        }

        override suspend fun delete() {
            deleteCount++
            stored = Credentials()
        }
    }

    private val saved = Credentials(
        baseUrl = "https://worker.example.dev",
        clientId = "client-id",
        clientSecret = "client-secret",
    )

    private fun TestScope.buildRepository(storage: FakeCredentialsStorage) =
        CredentialsRepositoryImpl(
            storage = storage,
            // Stands in for the app-lifetime scope, sharing the test's virtual clock.
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )

    @Test
    fun `a freshly created repository hydrates itself from storage`() = runTest {
        // The regression this guards: a theme change recreates the Activity, the DI graph is
        // rebuilt, and the saveable nav backstack skips Splash — so nobody calls loadIntoState()
        // and every request fires with blank credentials. Construction alone must hydrate.
        val storage = FakeCredentialsStorage(stored = saved)

        val repository = buildRepository(storage)
        advanceUntilIdle()

        assertEquals(saved, repository.state.value)
    }

    @Test
    fun `hydration never clobbers credentials staged while it was still reading`() = runTest {
        val storage = FakeCredentialsStorage(stored = saved)
        storage.readGate = CompletableDeferred()
        val repository = buildRepository(storage)

        val stagedInMeantime = Credentials("https://new.example.dev", "new-id", "new-secret")
        repository.stage(stagedInMeantime)

        storage.readGate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(stagedInMeantime, repository.state.value, "background hydration must only fill an empty state")
    }

    @Test
    fun `save writes to storage and updates state`() = runTest {
        val storage = FakeCredentialsStorage()
        val repository = buildRepository(storage)
        advanceUntilIdle()

        repository.save(saved)

        assertEquals(listOf(saved), storage.writes)
        assertEquals(saved, repository.state.value)
    }

    @Test
    fun `stage updates state without touching storage`() = runTest {
        val storage = FakeCredentialsStorage()
        val repository = buildRepository(storage)
        advanceUntilIdle()

        repository.stage(saved)

        assertEquals(emptyList(), storage.writes)
        assertEquals(saved, repository.state.value)
    }

    @Test
    fun `clear wipes storage and state`() = runTest {
        val storage = FakeCredentialsStorage(stored = saved)
        val repository = buildRepository(storage)
        advanceUntilIdle()
        assertEquals(saved, repository.state.value)

        repository.clear()

        assertEquals(1, storage.deleteCount)
        assertEquals(Credentials(), repository.state.value)
    }

    @Test
    fun `loadIntoState explicitly replaces state from storage`() = runTest {
        val storage = FakeCredentialsStorage(stored = saved)
        val repository = buildRepository(storage)
        advanceUntilIdle()

        storage.stored = Credentials("https://other.example.dev", "other", "other")
        repository.loadIntoState()

        assertEquals(storage.stored, repository.state.value)
    }
}
