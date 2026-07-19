package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val phoneNumber: String = "",
    val isAdmin: Boolean = false,
    val isGoogleSignIn: Boolean = false
)

object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    // Sandbox Mock Users
    private val sandboxUsers = mutableListOf(
        AuthUser(
            uid = "admin_mock_uid",
            email = "admin@estategold.com",
            displayName = "Estate Gold Admin",
            phoneNumber = "+1 (555) 019-9283",
            isAdmin = true
        ),
        AuthUser(
            uid = "customer_mock_uid",
            email = "demo@example.com",
            displayName = "Demo Customer",
            phoneNumber = "+1 (555) 012-3456",
            isAdmin = false
        )
    )

    fun checkStatus() {
        if (FirebaseSyncManager.isFirebaseAvailable) {
            try {
                val fbAuth = FirebaseAuth.getInstance()
                val fbUser = fbAuth.currentUser
                if (fbUser != null) {
                    val email = fbUser.email ?: ""
                    _currentUser.value = AuthUser(
                        uid = fbUser.uid,
                        email = email,
                        displayName = fbUser.displayName ?: "SaaS User",
                        phoneNumber = fbUser.phoneNumber ?: "",
                        isAdmin = checkIfAdmin(email)
                    )
                } else {
                    _currentUser.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking Firebase Auth status", e)
            }
        }
    }

    private fun checkIfAdmin(email: String): Boolean {
        return email.equals("admin@estategold.com", ignoreCase = true) || 
               email.contains("admin@", ignoreCase = true)
    }

    fun signInWithEmail(email: String, password: String, onResult: (Result<AuthUser>) -> Unit) {
        if (FirebaseSyncManager.isFirebaseAvailable) {
            try {
                val fbAuth = FirebaseAuth.getInstance()
                fbAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val fbUser = task.result?.user
                            if (fbUser != null) {
                                val userEmail = fbUser.email ?: ""
                                val user = AuthUser(
                                    uid = fbUser.uid,
                                    email = userEmail,
                                    displayName = fbUser.displayName ?: userEmail.substringBefore("@"),
                                    isAdmin = checkIfAdmin(userEmail)
                                )
                                _currentUser.value = user
                                onResult(Result.success(user))
                            } else {
                                onResult(Result.failure(Exception("User is null")))
                            }
                        } else {
                            onResult(Result.failure(task.exception ?: Exception("Authentication failed")))
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase Auth sign in failed, trying sandbox", e)
                handleSandboxSignIn(email, password, onResult)
            }
        } else {
            handleSandboxSignIn(email, password, onResult)
        }
    }

    fun signUpWithEmail(email: String, password: String, displayName: String, phoneNumber: String, isAdmin: Boolean, onResult: (Result<AuthUser>) -> Unit) {
        if (FirebaseSyncManager.isFirebaseAvailable) {
            try {
                val fbAuth = FirebaseAuth.getInstance()
                fbAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val fbUser = task.result?.user
                            if (fbUser != null) {
                                val user = AuthUser(
                                    uid = fbUser.uid,
                                    email = email,
                                    displayName = displayName,
                                    phoneNumber = phoneNumber,
                                    isAdmin = isAdmin || checkIfAdmin(email)
                                )
                                _currentUser.value = user
                                onResult(Result.success(user))
                            } else {
                                onResult(Result.failure(Exception("Failed to register user")))
                            }
                        } else {
                            onResult(Result.failure(task.exception ?: Exception("Registration failed")))
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase Auth sign up failed, trying sandbox", e)
                handleSandboxSignUp(email, displayName, phoneNumber, isAdmin, onResult)
            }
        } else {
            handleSandboxSignUp(email, displayName, phoneNumber, isAdmin, onResult)
        }
    }

    fun signInWithGoogleToken(idToken: String, onResult: (Result<AuthUser>) -> Unit) {
        if (FirebaseSyncManager.isFirebaseAvailable) {
            try {
                val fbAuth = FirebaseAuth.getInstance()
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                fbAuth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val fbUser = task.result?.user
                            if (fbUser != null) {
                                val userEmail = fbUser.email ?: ""
                                val user = AuthUser(
                                    uid = fbUser.uid,
                                    email = userEmail,
                                    displayName = fbUser.displayName ?: "Google User",
                                    phoneNumber = fbUser.phoneNumber ?: "",
                                    isAdmin = checkIfAdmin(userEmail),
                                    isGoogleSignIn = true
                                )
                                _currentUser.value = user
                                onResult(Result.success(user))
                            } else {
                                onResult(Result.failure(Exception("Google sign in failed")))
                            }
                        } else {
                            onResult(Result.failure(task.exception ?: Exception("Google Auth failed")))
                        }
                    }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        } else {
            onResult(Result.failure(Exception("Firebase not available for real Google Sign-In")))
        }
    }

    fun signInWithSandboxGoogle(email: String, name: String) {
        val user = AuthUser(
            uid = "google_sandbox_${email.hashCode()}",
            email = email,
            displayName = name,
            isAdmin = checkIfAdmin(email),
            isGoogleSignIn = true
        )
        _currentUser.value = user
    }

    fun signOut() {
        if (FirebaseSyncManager.isFirebaseAvailable) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                Log.e(TAG, "Firebase Sign Out failed", e)
            }
        }
        _currentUser.value = null
    }

    private fun handleSandboxSignIn(email: String, password: String, onResult: (Result<AuthUser>) -> Unit) {
        // If password is at least 6 chars (standard firebase requirement), allow simulated sign-in/sign-up
        if (password.length < 6) {
            onResult(Result.failure(Exception("Password must be at least 6 characters.")))
            return
        }

        val existing = sandboxUsers.find { it.email.equals(email, ignoreCase = true) }
        if (existing != null) {
            _currentUser.value = existing
            onResult(Result.success(existing))
        } else {
            // Auto-create standard sandbox account if not exists for a friendly experience
            val newUser = AuthUser(
                uid = "mock_${email.hashCode()}",
                email = email,
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                isAdmin = checkIfAdmin(email)
            )
            sandboxUsers.add(newUser)
            _currentUser.value = newUser
            onResult(Result.success(newUser))
        }
    }

    private fun handleSandboxSignUp(email: String, name: String, phone: String, isAdmin: Boolean, onResult: (Result<AuthUser>) -> Unit) {
        val newUser = AuthUser(
            uid = "mock_${email.hashCode()}",
            email = email,
            displayName = name,
            phoneNumber = phone,
            isAdmin = isAdmin || checkIfAdmin(email)
        )
        sandboxUsers.add(newUser)
        _currentUser.value = newUser
        onResult(Result.success(newUser))
    }
}
