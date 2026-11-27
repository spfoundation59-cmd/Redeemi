package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.GoogleSignInButton
import com.example.data.model.UserProfile
import com.example.data.model.formatInr
import com.example.ui.components.RedeemiFooter
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    userEmail: String,
    userName: String,
    unlockedCodeCount: Int,
    isAdmin: Boolean,
    isGuest: Boolean,
    supportEmail: String,
    onGuestSignedIn: () -> Unit,
    onGuestSignInError: (String) -> Unit,
    onExitGuestMode: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isGuest) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(RoyalPurple, DeepBlue))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PersonOutline,
                                contentDescription = "Guest Shopper",
                                tint = BrightGold,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Guest Shopper", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text("Browsing REDEEMI Storefront", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = RoyalPurple.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, RoyalPurple)
                            ) {
                                Text(
                                    text = "GUEST MODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Sign in with Google", style = MaterialTheme.typography.titleLarge, color = BrightGold)
                        Text(
                            text = "Sign in to complete INR payments, save orders, and unlock instant digital redeem codes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        GoogleSignInButton(
                            onAuthSuccess = onGuestSignedIn,
                            onAuthError = onGuestSignInError
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onExitGuestMode,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("exit_guest_mode_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Exit guest mode", tint = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Return to Welcome Screen", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            item { RedeemiFooter(supportEmail = supportEmail) }
        }
        return
    }

    val displayName = userProfile?.displayName?.takeIf { it.isNotBlank() } ?: userName.ifBlank { "REDEEMI Member" }
    val email = userProfile?.email?.takeIf { it.isNotBlank() } ?: userEmail
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "R"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(RoyalPurple, DeepBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineMedium,
                            color = BrightGold
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = if (isAdmin) GoldContainer else RoyalPurple.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, if (isAdmin) LuxuryGold else RoyalPurple)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (isAdmin) BrightGold else SkyBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAdmin) "REDEEMI STORE OWNER" else "VERIFIED CUSTOMER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAdmin) BrightGold else SkyBlue
                                )
                            }
                        }
                    }
                }
            }
        }

        // Customer Dashboard KPIs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStatCard(
                    title = "Orders",
                    value = (userProfile?.totalOrders ?: 0L).toString(),
                    icon = Icons.Filled.ShoppingBag,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    title = "Codes",
                    value = unlockedCodeCount.toString(),
                    icon = Icons.Filled.VpnKey,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    title = "Spent",
                    value = formatInr(userProfile?.totalSpentInr ?: 0L),
                    icon = Icons.Filled.VerifiedUser,
                    modifier = Modifier.weight(1.3f)
                )
            }
        }

        if (isAdmin) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldContainer),
                    border = BorderStroke(1.dp, LuxuryGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "REDEEMI Store Management",
                            style = MaterialTheme.typography.titleLarge,
                            color = BrightGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create products, upload redeem codes, control INR pricing, manage users, and configure payment methods.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onOpenAdminDashboard,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LuxuryGold,
                                contentColor = ObsidianVoid
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_store_console_button")
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Open Store Console", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Store Console", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOutClick,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out", tint = CrimsonError)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of REDEEMI", color = CrimsonError, fontWeight = FontWeight.SemiBold)
            }
        }

        item { RedeemiFooter(supportEmail = supportEmail) }
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CardSlate,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = title, tint = LuxuryGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
