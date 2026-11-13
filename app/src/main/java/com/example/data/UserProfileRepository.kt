package com.example.data

import android.content.Context
import com.example.R
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snapshot = docRef.get().await()
            val profile = if (snapshot.exists()) {
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncUserProfileOnLogin(
        user: FirebaseUser,
        isPremium: Boolean = false
    ): Result<UserProfile> {
        val uid = requireUserId()
        require(uid == user.uid) { "Authenticated UID mismatch." }
        val docRef = db.collection("users").document(uid)

        val safeName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Alif User"
        val safeEmail = user.email?.takeIf { it.isNotBlank() } ?: "google-user@gmail.com"
        val safePhoto = user.photoUrl?.toString()?.takeIf { it.isNotBlank() && it.length <= 500 }

        return try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName.take(100),
                    "email" to safeEmail.take(200),
                    "isPremium" to isPremium,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safePhoto != null) {
                    createPayload["photoUrl"] = safePhoto
                }
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to safeName.take(100),
                    "email" to safeEmail.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safePhoto != null) {
                    updatePayload["photoUrl"] = safePhoto
                }
                docRef.update(updatePayload).await()
            }

            val refreshed = docRef.get().await().toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserProfile(
                id = uid,
                userId = uid,
                displayName = safeName,
                email = safeEmail,
                photoUrl = safePhoto,
                isPremium = isPremium
            )
            Result.success(refreshed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createOrRegisterProfile(
        displayName: String,
        email: String,
        photoUrl: String? = null,
        isPremium: Boolean = false
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "displayName" to displayName.trim().ifBlank { "Alif User" }.take(100),
            "email" to email.trim().ifBlank { "user@gmail.com" }.take(200),
            "isPremium" to isPremium,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!photoUrl.isNullOrBlank() && photoUrl.length <= 500) {
            payload["photoUrl"] = photoUrl
        }
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePremiumStatus(isPremium: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mapOf<String, Any>(
            "isPremium" to isPremium,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
