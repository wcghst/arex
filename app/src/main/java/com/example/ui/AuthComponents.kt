package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AuthUser
import com.example.ui.theme.*
import com.example.viewmodel.ServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: ServiceViewModel,
    onAuthSuccess: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isAdminUser by remember { mutableStateOf(false) }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showGoogleChooser by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = CharcoalBg,
        unfocusedContainerColor = CharcoalBg,
        focusedBorderColor = GoldPrimary,
        focusedLabelColor = GoldPrimary,
        unfocusedBorderColor = Color(0x1AFFFFFF),
        unfocusedLabelColor = OnCharcoal.copy(alpha = 0.5f),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUp) "Create Account" else "Sign In",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (onDismiss != null) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = OnCharcoal)
                    }
                }
            }

            // Connection Status Alert Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (viewModel.isFirebaseAvailable) Color(0x152ECC71) else Color(0x15F1C40F),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (viewModel.isFirebaseAvailable) Color(0x402ECC71) else Color(0x40F1C40F))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (viewModel.isFirebaseAvailable) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                        contentDescription = "Status",
                        tint = if (viewModel.isFirebaseAvailable) Color(0xFF2ECC71) else GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = if (viewModel.isFirebaseAvailable) "Connected to Firebase Cloud" else "Local Sandbox Simulator Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (viewModel.isFirebaseAvailable) "Real-time sync to Firestore database is active." else "No setup required! Enter any credentials or use quick-logs below.",
                            fontSize = 11.sp,
                            color = OnCharcoal.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Toggle Tab
            TabRow(
                selectedTabIndex = if (isSignUp) 1 else 0,
                containerColor = CharcoalBg,
                contentColor = GoldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (isSignUp) 1 else 0]),
                        color = GoldPrimary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = !isSignUp,
                    onClick = { 
                        isSignUp = false
                        errorMessage = null
                        infoMessage = null
                    },
                    text = { Text("Sign In", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = isSignUp,
                    onClick = { 
                        isSignUp = true
                        errorMessage = null
                        infoMessage = null
                    },
                    text = { Text("Register", fontWeight = FontWeight.Bold) }
                )
            }

            // Error Display
            if (errorMessage != null) {
                Surface(
                    color = Color(0x22E74C3C),
                    border = BorderStroke(1.dp, Color(0xFFE74C3C)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFE74C3C))
                        Text(errorMessage!!, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Info Display
            if (infoMessage != null) {
                Surface(
                    color = Color(0x222ECC71),
                    border = BorderStroke(1.dp, Color(0xFF2ECC71)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF2ECC71))
                        Text(infoMessage!!, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Registration Fields
            if (isSignUp) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OnCharcoal) },
                    colors = customTextFieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = OnCharcoal) },
                    colors = customTextFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Common Credentials Fields
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = OnCharcoal) },
                colors = customTextFieldColors,
                modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (min 6 characters)") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OnCharcoal) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = OnCharcoal
                        )
                    }
                },
                colors = customTextFieldColors,
                modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                singleLine = true
            )

            if (isSignUp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAdminUser = !isAdminUser }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isAdminUser,
                        onCheckedChange = { isAdminUser = it },
                        colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                    )
                    Column {
                        Text("Register as Staff / Administrator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Grants full access to the Admin Lead Sequence Dashboard", color = OnCharcoal.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter both email and password."
                        return@Button
                    }
                    if (isSignUp && name.isBlank()) {
                        errorMessage = "Please enter your full name."
                        return@Button
                    }
                    
                    keyboardController?.hide()
                    isSubmitting = true
                    errorMessage = null
                    infoMessage = null

                    if (isSignUp) {
                        viewModel.signUpWithEmail(email, password, name, phone, isAdminUser) { result ->
                            isSubmitting = false
                            if (result.isSuccess) {
                                infoMessage = "Account registered successfully! Logging you in..."
                                onAuthSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Registration failed."
                            }
                        }
                    } else {
                        viewModel.signInWithEmail(email, password) { result ->
                            isSubmitting = false
                            if (result.isSuccess) {
                                infoMessage = "Successfully logged in!"
                                onAuthSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Login failed."
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = CharcoalBg),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CharcoalBg)
                } else {
                    Text(if (isSignUp) "Register SaaS Account" else "Sign In", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // OR divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x14FFFFFF))
                Text("OR", color = OnCharcoal.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x14FFFFFF))
            }

            // Google Sign In Button
            OutlinedButton(
                onClick = {
                    showGoogleChooser = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("google_signin_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Styled pure-compose Google "G" representation
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                    Text("Continue with Google", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Quick Demo Test Logins
            HorizontalDivider(color = Color(0x0AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "⚡ QUICK DEMO ONE-TAP LOGINS:",
                color = GoldPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        email = "admin@estategold.com"
                        password = "admin123"
                        isSignUp = false
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AD4AF37), contentColor = GoldPrimary),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Admin Login", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        email = "demo@example.com"
                        password = "customer123"
                        isSignUp = false
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x0FFFFFFF), contentColor = OnCharcoal),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Customer Login", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Google Sign-In Sandbox Account Chooser Dialog
    if (showGoogleChooser) {
        Dialog(onDismissRequest = { showGoogleChooser = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurfaceLight),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Sign in with Google",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Choose an account to continue to Estate Gold:",
                        color = OnCharcoal.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )

                    val accounts = listOf(
                        "customer.john@gmail.com" to "John Doe",
                        "customer.sarah@gmail.com" to "Sarah Jenkins",
                        "admin.estate@gmail.com" to "Estate Gold Chief Admin"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { (gEmail, gName) ->
                            Surface(
                                onClick = {
                                    viewModel.signInWithGoogle(gEmail, gName)
                                    showGoogleChooser = false
                                    onAuthSuccess()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = CharcoalBg,
                                border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(GoldPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = gName.take(1),
                                            color = CharcoalBg,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(gName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(gEmail, color = OnCharcoal.copy(alpha = 0.5f), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showGoogleChooser = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = GoldPrimary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
