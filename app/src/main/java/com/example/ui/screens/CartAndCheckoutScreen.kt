package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.firebase.GoogleSignInButton
import com.example.data.model.EnrichedCartLine
import com.example.data.model.PaymentMethodType
import com.example.data.model.PlatformConfig
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
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CheckoutUiState

@Composable
fun CartAndCheckoutScreen(
    cartLines: List<EnrichedCartLine>,
    platformConfig: PlatformConfig,
    checkoutState: CheckoutUiState,
    isGuest: Boolean,
    onGuestSignedIn: () -> Unit,
    onGuestSignInError: (String) -> Unit,
    onUpdateQty: (String, Long) -> Unit,
    onRemoveItem: (String) -> Unit,
    onPayClick: (String) -> Unit,
    onResetCheckout: () -> Unit,
    onExploreMarketplace: () -> Unit,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledMethods = remember(platformConfig.enabledPaymentMethods) {
        platformConfig.enabledPaymentMethods.map { PaymentMethodType.fromCode(it) }.ifEmpty {
            listOf(PaymentMethodType.UPI)
        }
    }
    var selectedMethod by remember(enabledMethods) {
        mutableStateOf(enabledMethods.first())
    }
    var paymentInputHint by remember { mutableStateOf("customer@upi") }
    var showEmailDialog by remember { mutableStateOf(false) }

    if (checkoutState is CheckoutUiState.Processing) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(ObsidianVoid, MidnightSurface))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                CircularProgressIndicator(
                    color = LuxuryGold,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "REDEEMI Secure Pay",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Verifying payment & unlocking your digital codes...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        return
    }

    if (checkoutState is CheckoutUiState.Success) {
        BackHandler { onResetCheckout() }
        val receipt = checkoutState.receipt

        if (showEmailDialog) {
            EmailReceiptDialog(
                order = receipt.order,
                items = receipt.items,
                codes = receipt.deliveredCodes,
                onDismiss = { showEmailDialog = false }
            )
        }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                    border = BorderStroke(1.dp, EmeraldSuccess),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Payment Verified",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "REDEEMI Delivery Complete!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paid ${formatInr(receipt.order.totalInr)} via ${receipt.order.paymentMethod}",
                            style = MaterialTheme.typography.titleSmall,
                            color = BrightGold
                        )
                        Text(
                            text = "Payment Ref: ${receipt.order.paymentReference}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Unlocked Redeem Codes (${receipt.deliveredCodes.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    OutlinedButton(
                        onClick = { showEmailDialog = true },
                        border = BorderStroke(1.dp, LuxuryGold),
                        modifier = Modifier.testTag("view_email_receipt_button")
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = "Email receipt", tint = BrightGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Receipt", color = BrightGold)
                    }
                }
            }

            items(receipt.deliveredCodes, key = { it.id }) { code ->
                RedeemCodeCard(redeemCode = code)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onResetCheckout()
                            onOpenVault()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, RoyalPurple),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("receipt_open_vault_button")
                    ) {
                        Icon(Icons.Filled.VpnKey, contentDescription = "Open Vault", tint = PurpleGlow, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("My Vault", color = TextPrimary)
                    }
                    Button(
                        onClick = {
                            onResetCheckout()
                            onExploreMarketplace()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianVoid),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("receipt_continue_shopping_button")
                    ) {
                        Text("Shop More", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { RedeemiFooter(supportEmail = platformConfig.supportEmail) }
        }
        return
    }

    if (cartLines.isEmpty()) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_vault),
                            contentDescription = "Empty REDEEMI Cart",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(22.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your REDEEMI Cart is Empty",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Browse verified gaming pins, gift cards, and streaming vouchers with instant INR delivery.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onExploreMarketplace,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LuxuryGold,
                                contentColor = ObsidianVoid
                            ),
                            modifier = Modifier.testTag("empty_cart_explore_button")
                        ) {
                            Icon(Icons.Filled.Storefront, contentDescription = "Marketplace", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Explore REDEEMI Store", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { RedeemiFooter(supportEmail = platformConfig.supportEmail) }
        }
        return
    }

    val subtotalInr = cartLines.sumOf { it.lineTotalInr }
    val feeRate = (platformConfig.platformFeePercent + platformConfig.gstPercent).coerceIn(0L, 48L)
    val feeInr = (subtotalInr * feeRate) / 100L
    val totalInr = subtotalInr + feeInr

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "REDEEMI Checkout (${cartLines.sumOf { it.cartItem.quantity }} items)",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }

        if (checkoutState is CheckoutUiState.Error) {
            item {
                Surface(
                    color = CrimsonContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CrimsonError),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = checkoutState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        items(cartLines, key = { it.cartItem.productId }) { line ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(line.product.brand, style = MaterialTheme.typography.labelSmall, color = PurpleGlow)
                        Text(line.product.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatInr(line.product.priceInr)} × ${line.cartItem.quantity} = ${formatInr(line.lineTotalInr)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                            color = BrightGold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onUpdateQty(line.product.id, line.cartItem.quantity - 1L) },
                            modifier = Modifier.testTag("cart_qty_minus_${line.product.id}")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", tint = TextPrimary)
                        }
                        Text(
                            text = line.cartItem.quantity.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = {
                                val next = line.cartItem.quantity + 1L
                                if (next <= minOf(line.product.stockCount, platformConfig.maxCartQuantityPerItem)) {
                                    onUpdateQty(line.product.id, next)
                                }
                            },
                            modifier = Modifier.testTag("cart_qty_plus_${line.product.id}")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase quantity", tint = TextPrimary)
                        }
                        IconButton(
                            onClick = { onRemoveItem(line.product.id) },
                            modifier = Modifier.testTag("cart_remove_${line.product.id}")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove item", tint = CrimsonError)
                        }
                    }
                }
            }
        }

        // Payment Gateway Selection
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = "Payment Gateway", tint = LuxuryGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REDEEMI Payment Gateway (INR)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }

                    enabledMethods.forEach { method ->
                        val isSelected = method == selectedMethod
                        Surface(
                            color = if (isSelected) RoyalPurple.copy(alpha = 0.2f) else MidnightSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isSelected) LuxuryGold else SubtleBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMethod = method }
                                .testTag("payment_method_${method.code.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedMethod = method },
                                    colors = RadioButtonDefaults.colors(selectedColor = LuxuryGold, unselectedColor = TextMuted)
                                )
                                Icon(
                                    imageVector = when (method) {
                                        PaymentMethodType.UPI -> Icons.Filled.QrCode2
                                        PaymentMethodType.CARD -> Icons.Filled.CreditCard
                                        PaymentMethodType.NETBANKING -> Icons.Filled.AccountBalance
                                        PaymentMethodType.WALLET -> Icons.Filled.AccountBalanceWallet
                                    },
                                    contentDescription = method.displayName,
                                    tint = if (isSelected) BrightGold else SkyBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(method.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(method.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                    }

                    if (selectedMethod == PaymentMethodType.UPI) {
                        OutlinedTextField(
                            value = paymentInputHint,
                            onValueChange = { paymentInputHint = it },
                            label = { Text("UPI ID (VPA)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = SubtleBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Order Price Summary
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Order Summary (Verified INR Prices)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Items Subtotal", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(formatInr(subtotalInr), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily), color = TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gateway & Platform Fee (${platformConfig.platformFeePercent + platformConfig.gstPercent}%)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(formatInr(feeInr), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily), color = TextPrimary)
                    }
                    HorizontalDivider(color = SubtleBorder)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Payable", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            formatInr(totalInr),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                            color = BrightGold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    if (isGuest) {
                        Text(
                            text = "Sign in with Google to complete INR payment and receive your instant digital codes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        GoogleSignInButton(
                            onAuthSuccess = onGuestSignedIn,
                            onAuthError = onGuestSignInError
                        )
                    } else {
                        Button(
                            onClick = { onPayClick(selectedMethod.code) },
                            enabled = !platformConfig.maintenanceMode,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LuxuryGold,
                                contentColor = ObsidianVoid
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("checkout_pay_button")
                        ) {
                            Text(
                                text = if (platformConfig.maintenanceMode) "Checkout Paused (Maintenance)" else "Pay ${formatInr(totalInr)} & Unlock Codes",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item { RedeemiFooter(supportEmail = platformConfig.supportEmail) }
    }
}
