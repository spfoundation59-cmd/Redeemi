package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.firebase.GoogleSignInButton
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.RedeemCode
import com.example.data.model.UiState
import com.example.data.model.formatInr
import com.example.ui.components.EmailReceiptDialog
import com.example.ui.components.RedeemCodeCard
import com.example.ui.components.RedeemiFooter
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun VaultAndOrdersScreen(
    codesState: UiState<List<RedeemCode>>,
    ordersState: UiState<List<Order>>,
    supportEmail: String,
    isGuest: Boolean,
    onGuestSignedIn: () -> Unit,
    onGuestSignInError: (String) -> Unit,
    onLoadOrderItems: suspend (String) -> List<OrderItem>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var receiptOrder by remember { mutableStateOf<Order?>(null) }
    var receiptItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }

    val allUserCodes = (codesState as? UiState.Success)?.data ?: emptyList()

    if (receiptOrder != null) {
        val orderCodes = allUserCodes.filter { it.orderId == receiptOrder!!.id }
        EmailReceiptDialog(
            order = receiptOrder!!,
            items = receiptItems,
            codes = orderCodes,
            onDismiss = { receiptOrder = null }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isGuest) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_vault),
                            contentDescription = "REDEEMI Protected Code Vault",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                        Text(
                            text = "Sign in to Open Your Vault",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Your purchased redeem codes and verified INR receipts are tied securely to your Google account.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        GoogleSignInButton(
                            onAuthSuccess = onGuestSignedIn,
                            onAuthError = onGuestSignInError
                        )
                    }
                }
            }
            item { RedeemiFooter(supportEmail = supportEmail) }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "REDEEMI Vault & Orders",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("My Code Vault (${allUserCodes.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CardSlate,
                                selectedContainerColor = RoyalPurple,
                                labelColor = TextSecondary,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("vault_tab_codes")
                        )
                        FilterChip(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Purchase History") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CardSlate,
                                selectedContainerColor = RoyalPurple,
                                labelColor = TextSecondary,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("vault_tab_orders")
                        )
                    }
                }
            }
        }

        if (!isGuest) {

        if (selectedTab == 0) {
            when (codesState) {
                is UiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LuxuryGold)
                    }
                }
                is UiState.Error -> item {
                    Surface(color = CrimsonContainer, shape = RoundedCornerShape(14.dp)) {
                        Text(codesState.message, color = TextPrimary, modifier = Modifier.padding(16.dp))
                    }
                }
                is UiState.Success -> {
                    if (codesState.data.isEmpty()) {
                        item {
                            EmptyVaultCard(
                                title = "Your Code Vault is Empty",
                                subtitle = "Codes you purchase on REDEEMI are unlocked here instantaneously and stored securely."
                            )
                        }
                    } else {
                        items(codesState.data, key = { it.id }) { code ->
                            RedeemCodeCard(redeemCode = code)
                        }
                    }
                }
            }
        } else {
            when (ordersState) {
                is UiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LuxuryGold)
                    }
                }
                is UiState.Error -> item {
                    Surface(color = CrimsonContainer, shape = RoundedCornerShape(14.dp)) {
                        Text(ordersState.message, color = TextPrimary, modifier = Modifier.padding(16.dp))
                    }
                }
                is UiState.Success -> {
                    if (ordersState.data.isEmpty()) {
                        item {
                            EmptyVaultCard(
                                title = "No Purchases Yet",
                                subtitle = "Your verified INR orders and REDEEMI digital receipts will appear here."
                            )
                        }
                    } else {
                        items(ordersState.data, key = { it.id }) { order ->
                            OrderHistoryCard(
                                order = order,
                                onViewReceipt = {
                                    scope.launch {
                                        receiptItems = onLoadOrderItems(order.id)
                                        receiptOrder = order
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
            item { RedeemiFooter(supportEmail = supportEmail) }
        }
    }
}

@Composable
private fun OrderHistoryCard(
    order: Order,
    onViewReceipt: () -> Unit
) {
    val dateStr = remember(order.createdAt) {
        val d = order.createdAt?.toDate()
        if (d != null) SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(d) else "Just now"
    }
    val isPaid = order.status == "paid"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.id.takeLast(8).uppercase()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = PurpleGlow
                )
                Surface(
                    color = if (isPaid) EmeraldContainer else CrimsonContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPaid) EmeraldSuccess else CrimsonError,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(order.productSummary, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text("$dateStr • ${order.paymentMethod} (${order.paymentReference})", style = MaterialTheme.typography.bodySmall, color = TextMuted)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatInr(order.totalInr),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                    color = BrightGold
                )
                OutlinedButton(
                    onClick = onViewReceipt,
                    border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("order_receipt_${order.id}")
                ) {
                    Icon(Icons.Filled.Email, contentDescription = "Open digital receipt", tint = BrightGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Receipt & Codes", color = BrightGold)
                }
            }
        }
    }
}

@Composable
private fun EmptyVaultCard(title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_vault),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
