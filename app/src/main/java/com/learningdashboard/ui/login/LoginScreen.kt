package com.learningdashboard.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learningdashboard.R
import com.learningdashboard.domain.validation.CredentialsValidator
import com.learningdashboard.domain.validation.EmailError
import com.learningdashboard.domain.validation.PasswordError
import com.learningdashboard.ui.common.toMessage

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoggedIn()
    }

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogin = viewModel::login,
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(48.dp))
            Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.email)) },
                isError = state.emailError != null,
                supportingText = state.emailError?.let { { Text(it.toMessage()) } },
                singleLine = true,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.password)) },
                isError = state.passwordError != null,
                supportingText = state.passwordError?.let { { Text(it.toMessage()) } },
                singleLine = true,
                enabled = !state.isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(
                                if (passwordVisible) R.string.hide_password else R.string.show_password,
                            ),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onLogin()
                }),
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let { error ->
                Text(
                    text = error.toMessage(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onLogin()
                },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.login))
                }
            }

            Text(
                stringResource(R.string.demo_credentials),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun EmailError.toMessage(): String = when (this) {
    EmailError.Blank -> stringResource(R.string.error_email_blank)
    EmailError.Invalid -> stringResource(R.string.error_email_invalid)
}

@Composable
private fun PasswordError.toMessage(): String = when (this) {
    PasswordError.Blank -> stringResource(R.string.error_password_blank)
    PasswordError.TooShort -> stringResource(R.string.error_password_short, CredentialsValidator.MIN_PASSWORD_LENGTH)
}
