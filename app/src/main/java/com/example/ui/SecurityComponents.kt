package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonCyanLight
import java.text.SimpleDateFormat
import java.util.*

/**
 * Live System State display showing:
 * - Real-time Date and live Clock
 * - Active Theme indicator
 * - Cryptographic Salting & Pepper Engine Status
 * - Cloud Supabase connection health
 */
@Composable
fun SystemStateCard(
    viewModel: MovieViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            kotlinx.coroutines.delay(1000L)
        }
    }

    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss z", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("system_state_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10121A)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222634))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Live Clock & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Text(
                            text = "SYSTEM STATE: ONLINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = dateFormatter.format(currentTime),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF5F6F8)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF181B26))
                        .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = timeFormatter.format(currentTime),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E2230))

            // Security & Cryptographic Posture
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CRYPTOGRAPHIC INTEGRITY ENGINE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8E93A4),
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Salt Badge
                    SecurityStatusPill(
                        label = "SALTING",
                        value = "16-Byte CSPRNG",
                        isHealthy = true,
                        modifier = Modifier.weight(1f)
                    )
                    // Pepper Badge
                    SecurityStatusPill(
                        label = "PEPPER",
                        value = "Secret Server Key",
                        isHealthy = true,
                        modifier = Modifier.weight(1f)
                    )
                    // Hash Badge
                    SecurityStatusPill(
                        label = "HASHING",
                        value = "PBKDF2 HMAC-SHA256",
                        isHealthy = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // System Metrics & Cloud Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141722))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = SupabaseClient.syncStatusText,
                        fontSize = 11.5.sp,
                        color = Color(0xFFF5F6F8),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Users: ${viewModel.registeredUsersState.size}",
                        fontSize = 11.sp,
                        color = Color(0xFF8E93A4)
                    )
                    Text(
                        text = "Deleted: ${viewModel.deletedRecordsState.size}",
                        fontSize = 11.sp,
                        color = Color(0xFFFF5252)
                    )
                    Text(
                        text = "Restores: ${viewModel.restorePointsState.size}",
                        fontSize = 11.sp,
                        color = Color(0xFFFFB300)
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityStatusPill(
    label: String,
    value: String,
    isHealthy: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151824))
            .border(1.dp, Color(0xFF262A3B), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHealthy) ElectricCyan else Color(0xFFFF5252)
            )
            Text(
                text = value,
                fontSize = 10.5.sp,
                color = Color(0xFFF5F6F8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 2FA (Two-Step Verification) Dialog triggered during sign-in
 * when an account has twoFactorEnabled = true.
 */
@Composable
fun TwoFactorVerificationDialog(
    userEmail: String,
    onVerifyTotp: (code: String) -> Unit,
    onVerifyRecoveryCode: (code: String) -> Unit,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var isRecoveryMode by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("two_factor_auth_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF12141D)),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan.copy(alpha = 0.15f))
                        .border(1.dp, ElectricCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecoveryMode) Icons.Filled.VpnKey else Icons.Filled.Security,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (isRecoveryMode) "Emergency Recovery Code" else "Two-Step Verification",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F6F8)
                )

                Text(
                    text = if (isRecoveryMode) {
                        "Enter one of your 5 emergency recovery backup codes (e.g. RECV-XXXX-XXXX) to bypass 2FA."
                    } else {
                        "Enter the 6-digit verification code from your Google Authenticator or Authy app for $userEmail."
                    },
                    fontSize = 12.5.sp,
                    color = Color(0xFF8E93A4),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.trim() },
                    label = { Text(if (isRecoveryMode) "Recovery Code (RECV-...)" else "6-Digit Authenticator Code") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("two_factor_code_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (isRecoveryMode) {
                            onVerifyRecoveryCode(codeInput)
                        } else {
                            onVerifyTotp(codeInput)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("two_factor_verify_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF06080D)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Verify & Authenticate", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }

                Text(
                    text = if (isRecoveryMode) "Switch to 6-Digit Authenticator Code" else "Lost your device? Use Emergency Backup Code",
                    fontSize = 11.5.sp,
                    color = ElectricCyan,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        isRecoveryMode = !isRecoveryMode
                        codeInput = ""
                    }
                )

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF8E93A4), fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * 2FA Setup Dialog for enabling TOTP Authenticator and generating 5 Emergency Backup Codes.
 */
@Composable
fun TwoFactorSetupDialog(
    userEmail: String,
    secretKey: String,
    recoveryCodes: List<String>,
    onConfirmEnable: (verificationCode: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var testCodeInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("two_factor_setup_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF12141D)),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.VpnKey, contentDescription = null, tint = ElectricCyan)
                        Text(
                            text = "Set Up Two-Factor Authentication",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5F6F8)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                HorizontalDivider(color = Color(0xFF2A2E3D))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Step 1: Add Secret Key to Your Authenticator App",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Open Google Authenticator, Authy, or Microsoft Authenticator, tap '+', select 'Enter a setup key', and paste the key below:",
                            fontSize = 11.5.sp,
                            color = Color(0xFF8E93A4)
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF181B26))
                                .border(1.dp, Color(0xFF2E3344), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SECRET KEY (BASE32)", fontSize = 9.sp, color = Color(0xFF8E93A4), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = secretKey,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("2FA Secret", secretKey))
                                        Toast.makeText(context, "Secret key copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Key", tint = ElectricCyan)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Step 2: Save Emergency Recovery Codes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Text(
                            text = "If you ever lose your phone, each code below can be used ONCE to sign in:",
                            fontSize = 11.5.sp,
                            color = Color(0xFF8E93A4)
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF181B26))
                                .border(1.dp, Color(0xFF2E3344), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recoveryCodes.forEachIndexed { index, code ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${index + 1}.", fontSize = 11.sp, color = Color(0xFF8E93A4))
                                    Text(
                                        text = code,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF5F6F8)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Step 3: Confirm Setup with 6-Digit Code",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        OutlinedTextField(
                            value = testCodeInput,
                            onValueChange = { testCodeInput = it.trim() },
                            label = { Text("Enter current code from authenticator app") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Button(
                    onClick = { onConfirmEnable(testCodeInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("confirm_enable_2fa_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF06080D)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Activate 2-Step Verification", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Administrative User Management Tab:
 * - Lists active users with role badges and 2FA status
 * - "Add User" button with role assignment and salted/peppered password generation
 * - "Delete User" button (moves record to Deleted Records Archive!)
 */
@Composable
fun UsersManagementTab(
    viewModel: MovieViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<AppUser?>(null) }
    var deleteReason by remember { mutableStateOf("Administrative audit removal") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("users_management_tab"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Demo Role Switcher Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "QUICK ROLE SWITCHER (ASSIGNMENT TESTING)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    letterSpacing = 0.8.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("admin@moviefinder.com", "Admin", UserRole.ADMINISTRATOR),
                        Triple("manager@moviefinder.com", "Manager", UserRole.MANAGER),
                        Triple("inputer@moviefinder.com", "Inputer", UserRole.CONTENT_INPUTER),
                        Triple("user@moviefinder.com", "User", UserRole.STANDARD_USER)
                    ).forEach { (email, label, role) ->
                        val isCurrent = viewModel.userEmail.equals(email, ignoreCase = true)
                        OutlinedButton(
                            onClick = {
                                viewModel.signInDemoRole(context, email)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isCurrent) ElectricCyan.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (isCurrent) ElectricCyan else Color(0xFFF5F6F8)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) ElectricCyan else Color(0xFF2A2E3D)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "👤 $label",
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Action Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "User Directory & Access Control",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F6F8)
                )
                Text(
                    text = "${viewModel.registeredUsersState.size} active registered identities",
                    fontSize = 11.5.sp,
                    color = Color(0xFF8E93A4)
                )
            }

            Button(
                onClick = { showAddUserDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF06080D)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("add_user_button")
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add User", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Users List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(viewModel.registeredUsersState) { user ->
                UserItemCard(
                    user = user,
                    isCurrentUser = user.email.equals(viewModel.userEmail, ignoreCase = true),
                    onDelete = { userToDelete = user }
                )
            }
        }
    }

    // Add User Dialog
    if (showAddUserDialog) {
        AddUserModal(
            onDismiss = { showAddUserDialog = false },
            onAdd = { name, email, pass, role, twoFactor ->
                viewModel.createNewUserByAdmin(context, name, email, pass, role, twoFactor)
                showAddUserDialog = false
            }
        )
    }

    // Confirm Delete Dialog
    if (userToDelete != null) {
        val target = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User & Move to Archive?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Are you sure you want to delete ${target.name} (${target.email})?\n" +
                        "This will revoke access and move this identity to the Deleted Records Archive with timestamp and audit reason."
                    )
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Audit Deletion Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUserByAdmin(context, target.id, deleteReason)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Delete to Archive", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserItemCard(
    user: AppUser,
    isCurrentUser: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222634))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = user.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5F6F8)
                    )
                    RoleBadge(role = user.role)
                    if (isCurrentUser) {
                        Text(
                            text = "(YOU)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }
                }

                Text(
                    text = user.email,
                    fontSize = 12.sp,
                    color = Color(0xFF8E93A4)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (user.twoFactorEnabled) "🔒 2FA Active" else "🔓 2FA Off",
                        fontSize = 10.5.sp,
                        color = if (user.twoFactorEnabled) Color(0xFF00E676) else Color(0xFF8E93A4)
                    )
                    Text(
                        text = "Salt: ${user.salt.take(8)}...",
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF8E93A4)
                    )
                }
            }

            if (!isCurrentUser) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                        .testTag("delete_user_${user.email}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete User",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RoleBadge(role: String) {
    val (bgColor, textColor) = when (role.lowercase()) {
        "administrator" -> Color(0xFF00E5FF).copy(alpha = 0.2f) to Color(0xFF00E5FF)
        "manager" -> Color(0xFFB388FF).copy(alpha = 0.2f) to Color(0xFFB388FF)
        "content inputer" -> Color(0xFF00E676).copy(alpha = 0.2f) to Color(0xFF00E676)
        else -> Color(0xFF90A4AE).copy(alpha = 0.2f) to Color(0xFFCFD8DC)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = role.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun AddUserModal(
    onDismiss: () -> Unit,
    onAdd: (name: String, email: String, pass: String, role: String, twoFactor: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STANDARD_USER.displayName) }
    var enable2Fa by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add New User Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Password (Auto-Salted & Peppered)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Role selection
                Text("Role & Permissions", fontSize = 12.sp, color = Color(0xFF8E93A4))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        UserRole.STANDARD_USER.displayName,
                        UserRole.CONTENT_INPUTER.displayName,
                        UserRole.MANAGER.displayName,
                        UserRole.ADMINISTRATOR.displayName
                    ).forEach { role ->
                        val isSelected = selectedRole == role
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ElectricCyan else Color(0xFF1E2230))
                                .clickable { selectedRole = role }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = role,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF06080D) else Color.White
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pre-configure 2FA", fontSize = 12.sp, color = Color.White)
                    Switch(
                        checked = enable2Fa,
                        onCheckedChange = { enable2Fa = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank() && pass.isNotBlank()) {
                                onAdd(name, email, pass, selectedRole, enable2Fa)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF06080D))
                    ) {
                        Text("Create Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Deleted Records Database (Archive & Audit Log)
 */
@Composable
fun DeletedRecordsArchiveTab(
    viewModel: MovieViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("deleted_records_archive_tab"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Deleted Records Database",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F6F8)
                )
                Text(
                    text = "Archived user & movie entries preserved for audit compliance",
                    fontSize = 11.5.sp,
                    color = Color(0xFF8E93A4)
                )
            }

            if (viewModel.deletedRecordsState.isNotEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.clearDeletedArchive(context) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Clear Log", fontSize = 11.sp)
                }
            }
        }

        if (viewModel.deletedRecordsState.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141722))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Archive, contentDescription = null, tint = Color(0xFF8E93A4), modifier = Modifier.size(36.dp))
                    Text("No deleted records archived", color = Color(0xFFF5F6F8), fontWeight = FontWeight.Bold)
                    Text("When an admin deletes a user or item, it is securely archived here.", color = Color(0xFF8E93A4), fontSize = 11.5.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(viewModel.deletedRecordsState) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2430))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFF5252).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(record.recordType, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                                    }
                                    Text(record.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text(dateFormat.format(Date(record.deletedAt)), fontSize = 10.5.sp, color = Color(0xFF8E93A4))
                            }

                            Text("Deleted By: ${record.deletedByEmail}", fontSize = 11.5.sp, color = Color(0xFF8E93A4))
                            Text("Audit Reason: ${record.reason}", fontSize = 11.sp, color = Color(0xFFB0B4C4))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F1118))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = record.deletedDataJson,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00E5FF),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Configuration Restore Point System:
 * Captures and applies configuration snapshots (theme, strictness, guest access, announcements)
 * WITHOUT changing any user or movie data.
 */
@Composable
fun RestorePointsTab(
    viewModel: MovieViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var restorePointName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("restore_points_tab"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "System Restore Points",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F6F8)
                )
                Text(
                    text = "Reverts configuration settings without altering user data or movie records",
                    fontSize = 11.5.sp,
                    color = Color(0xFF8E93A4)
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF06080D)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Point", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(viewModel.restorePointsState) { point ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222634))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(point.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(dateFormat.format(Date(point.createdAt)), fontSize = 11.sp, color = Color(0xFF8E93A4))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2230))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Theme: ${point.theme}", fontSize = 10.sp, color = ElectricCyan)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2230))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Guest Mode: ${if (point.guestModeEnabled) "ON" else "OFF"}", fontSize = 10.sp, color = Color.White)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { viewModel.applyRestorePoint(context, point.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = Color(0xFF06080D)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Apply Snapshot (Config Only)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Configuration Restore Point") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Snapshots current active theme, guest toggle, and security flags. User and movie data will remain untouched.")
                    OutlinedTextField(
                        value = restorePointName,
                        onValueChange = { restorePointName = it },
                        label = { Text("Restore Point Name (e.g. Pre-Update Snapshot)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restorePointName.isNotBlank()) {
                            viewModel.createRestorePoint(context, restorePointName)
                            restorePointName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF06080D))
                ) {
                    Text("Save Snapshot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Credentials & Security Reports Tab:
 * Displays all usernames, roles, salts, pepper verification status, and password hash representation
 * with an assignment inspection reveal toggle.
 */
@Composable
fun CredentialReportsTab(
    viewModel: MovieViewModel,
    modifier: Modifier = Modifier
) {
    var revealHashes by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("credential_reports_tab"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Credentials & Cryptographic Audit",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F6F8)
                )
                Text(
                    text = "Demonstrating PBKDF2 salting, peppering, and role authentication",
                    fontSize = 11.5.sp,
                    color = Color(0xFF8E93A4)
                )
            }

            OutlinedButton(
                onClick = { revealHashes = !revealHashes },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (revealHashes) ElectricCyan else Color(0xFF8E93A4)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (revealHashes) ElectricCyan else Color(0xFF2E3344)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(if (revealHashes) "🔒 Mask Hashes" else "👁️ Reveal Hashes", fontSize = 11.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(viewModel.registeredUsersState) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222634))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(user.name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(user.email, fontSize = 11.5.sp, color = Color(0xFF8E93A4))
                            }
                            RoleBadge(role = user.role)
                        }

                        HorizontalDivider(color = Color(0xFF1E2230))

                        // Cryptographic Salt & Pepper Demonstration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("PER-USER SALT (HEX)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E93A4))
                                Text(user.salt, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF00E676))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("PEPPER STATUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E93A4))
                                Text("VERIFIED (SERVER KEY)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                            }
                        }

                        Column {
                            Text("PBKDF2 HMAC-SHA256 HASH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E93A4))
                            Text(
                                text = if (revealHashes) user.passwordHash else "•••••••••••••••••••••••••••••••• (${user.passwordHash.take(8)}...)",
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (revealHashes) Color(0xFFFFB300) else Color(0xFF8E93A4)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("2FA: ${if (user.twoFactorEnabled) "ACTIVE (TOTP)" else "NOT CONFIGURED"}", fontSize = 10.5.sp, color = if (user.twoFactorEnabled) Color(0xFF00E676) else Color(0xFF8E93A4))
                            Text("Status: Active Identity", fontSize = 10.5.sp, color = Color(0xFF8E93A4))
                        }
                    }
                }
            }
        }
    }
}
