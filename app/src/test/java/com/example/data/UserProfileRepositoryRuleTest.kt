package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class UserProfileRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueEmail(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().take(8)}@gmail.com"

    @Test
    fun createUpdateAndObserveOwnProfile_authenticatedUser_succeeds() = runBlocking {
        val aliceEmail = uniqueEmail("alice")
        val uid = signInTestUser(aliceEmail)
        val repository = UserProfileRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrRegisterProfile(
                displayName = "Alice Rahman",
                email = aliceEmail,
                photoUrl = null,
                isPremium = false
            )
        }
        assertTrue(createResult.isSuccess)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updatePremiumStatus(true)
        }
        assertTrue(updateResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile(uid).getOrThrow()
        }
        assertNotNull(fetched)
        assertEquals(uid, fetched?.userId)
        assertEquals("Alice Rahman", fetched?.displayName)
        assertEquals(true, fetched?.isPremium)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it != null }
        }
        assertEquals(aliceEmail, observed?.email)
    }

    @Test
    fun getUserProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = uniqueEmail("alice")
        val bobEmail = uniqueEmail("bob")

        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = UserProfileRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrRegisterProfile(
                displayName = "Alice",
                email = aliceEmail,
                photoUrl = null,
                isPremium = false
            ).getOrThrow()
        }

        signInTestUser(bobEmail)
        val bobRepo = UserProfileRepository(firestore, auth)
        val readResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getUserProfile(aliceUid)
        }
        assertTrue(readResult.isFailure)
        val firestoreException = readResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeUserProfile_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = UserProfileRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserProfile("some_user_uid").first()
            }
        } catch (t: Throwable) {
            caught = t
        }

        assertNotNull(caught)
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
