package com.komikta.feature.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.komikta.BuildConfig
import com.komikta.core.common.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

object GoogleSignIn {
    suspend fun idToken(context: Context): Result<String> {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) return Result.Error("Login Google belum dikonfigurasi di aplikasi ini", "NOT_CONFIGURED")
        return try {
            val request = GetCredentialRequest.Builder().addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build()).build()
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Result.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.Error("Jenis kredensial tidak didukung")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Result.Error("Login dibatalkan", "CANCELLED")
        } catch (e: GetCredentialException) {
            Result.Error("Login Google gagal: ${e.message ?: "tidak diketahui"}", throwable = e)
        } catch (e: GoogleIdTokenParsingException) {
            Result.Error("Token Google tidak valid", throwable = e)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Login Google gagal", throwable = e)
        }
    }
}

@Composable
fun rememberGoogleSignIn(onToken: (String) -> Unit, onError: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenHandler = rememberUpdatedState(onToken)
    val errorHandler = rememberUpdatedState(onError)
    val action: () -> Unit = {
        scope.launch {
            when (val r = GoogleSignIn.idToken(context)) {
                is Result.Success -> tokenHandler.value(r.data)
                is Result.Error -> if (r.code != "CANCELLED") errorHandler.value(r.message)
                else -> Unit
            }
        }
    }
    return action
}
