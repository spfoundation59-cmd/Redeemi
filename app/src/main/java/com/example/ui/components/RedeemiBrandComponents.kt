package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.RedeemCode
import com.example.data.model.formatInr
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldContainer
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
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RedeemiLogoMark(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 36.dp,
    showTagline: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(RoyalPurple, DeepBlue)
                    )
                )
                .border(1.dp, LuxuryGold.copy(alpha = 0.65f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(badgeSize * 0.62f)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.88f)
                    lineTo(w * 0.2f, h * 0.14f)
                    lineTo(w * 0.64f, h * 0.14f)
                    lineTo(w * 0.82f, h * 0.34f)
                    lineTo(w * 0.64f, h * 0.54f)
                    lineTo(w * 0.22f, h * 0.54f)
                    moveTo(w * 0.48f, h * 0.54f)
                    lineTo(w * 0.84f, h * 0.88f)
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(BrightGold, LuxuryGold, Color.White),
                        start = Offset.Zero,
                        end = Offset(w, h)
                    ),
                    style = Stroke(
                        width = w * 0.15f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "REDEEMI",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(LuxuryGold)
                )
            }
            if (showTagline) {
                Text(
                    text = "Buy. Redeem. Enjoy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrightGold
                )
            }
        }
    }
}

@Composable
fun RedeemiTopBar(
    pageSubtitle: String,
    cartCount: Int,
    isAdmin: Boolean,
    isAdminMode: Boolean,
    onCartClick: () -> Unit,
    onToggleAdminClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MidnightSurface.copy(alpha = 0.96f),
        border = BorderStroke(0.5.dp, SubtleBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                RedeemiLogoMark(badgeSize = 34.dp, showTagline = false)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = pageSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAdmin) {
                    Surface(
                        onClick = onToggleAdminClick,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAdminMode) LuxuryGold else GoldContainer,
                        border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("top_bar_console_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = "Toggle Store Console",
                                tint = if (isAdminMode) ObsidianVoid else BrightGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAdminMode) "Store" else "Console",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdminMode) ObsidianVoid else BrightGold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(
                    onClick = onCartClick,
                    modifier = Modifier.testTag("top_bar_cart_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (cartCount > 0) {
                                Badge(
                                    containerColor = LuxuryGold,
                                    contentColor = ObsidianVoid
                                ) {
                                    Text(cartCount.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = "Open REDEEMI Cart",
                            tint = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RedeemiLoadingScreen(
    message: String = "Securing your REDEEMI session...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "redeemi_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ObsidianVoid, MidnightSurface, ObsidianVoid)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(modifier = Modifier.alpha(alpha)) {
                RedeemiLogoMark(badgeSize = 56.dp, showTagline = true)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun RedeemiFooter(
    supportEmail: String = "support@redeemi.in",
    modifier: Modifier = Modifier
) {
    Surface(
        color = MidnightSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RedeemiLogoMark(badgeSize = 28.dp, showTagline = true)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "REDEEMI | Digital Code Marketplace",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Official INR Pricing • Instant Code Vault • Support: $supportEmail",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

fun categoryIcon(category: String): ImageVector = when (category) {
    "Gaming" -> Icons.Filled.SportsEsports
    "Streaming" -> Icons.Filled.Movie
    "Shopping" -> Icons.Filled.ShoppingBag
    "Apps & Cloud" -> Icons.Filled.Cloud
    else -> Icons.Filled.CardGiftcard
}

fun accentGradient(style: String): List<Color> = when (style) {
    "blue" -> listOf(DeepBlue, ElectricBlue.copy(alpha = 0.7f))
    "gold" -> listOf(Color(0xFF4A3408), LuxuryGold.copy(alpha = 0.8f))
    "emerald" -> listOf(EmeraldContainer, EmeraldSuccess.copy(alpha = 0.75f))
    else -> listOf(DeepPurple, RoyalPurple.copy(alpha = 0.8f))
}

@Composable
fun InrPriceRow(
    priceInr: Long,
    mrpInr: Long,
    discountPct: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatInr(priceInr),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = BrightGold
        )
        if (mrpInr > priceInr) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatInr(mrpInr),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    textDecoration = TextDecoration.LineThrough
                ),
                color = TextMuted
            )
        }
        if (discountPct > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = EmeraldContainer,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, EmeraldSuccess.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "$discountPct% OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldSuccess,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun RedeemCodeCard(
    redeemCode: RedeemCode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var revealed by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified REDEEMI Code",
                        tint = LuxuryGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = redeemCode.productTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = GoldContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "INSTANT",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrightGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = ObsidianVoid,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (revealed) redeemCode.code else "••••-••••-••••-••••",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (revealed) BrightGold else TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        IconButton(
                            onClick = { revealed = !revealed },
                            modifier = Modifier.testTag("toggle_reveal_code_button")
                        ) {
                            Icon(
                                imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (revealed) "Mask redeem code" else "Reveal redeem code",
                                tint = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                copyToClipboard(context, "REDEEMI Code", redeemCode.code)
                            },
                            modifier = Modifier.testTag("copy_redeem_code_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy redeem code",
                                tint = SkyBlue
                            )
                        }
                    }
                }
            }

            if (!redeemCode.orderId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Order Ref: ${redeemCode.orderId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun EmailReceiptDialog(
    order: Order,
    items: List<OrderItem>,
    codes: List<RedeemCode>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatted = remember(order.createdAt) {
        val date = order.createdAt?.toDate()
        if (date != null) SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
        else "Just now"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = "REDEEMI Email Receipt",
                    tint = LuxuryGold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REDEEMI Digital Receipt",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = CardSlate,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("From: REDEEMI Delivery <support@redeemi.in>", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("To: ${order.customerEmail}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Subject: Your REDEEMI Codes for Order #${order.id.takeLast(8).uppercase()}",
                            style = MaterialTheme.typography.titleSmall,
                            color = BrightGold
                        )
                        Text("Date: $dateFormatted", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                Text(
                    text = "Hi ${order.customerName}, thank you for choosing REDEEMI — Buy. Redeem. Enjoy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )

                HorizontalDivider(color = SubtleBorder)

                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.productTitle} × ${item.quantity}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text(formatInr(item.lineTotalInr), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily), color = BrightGold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Platform Fee & Tax", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text(formatInr(order.platformFeeInr), style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily), color = TextSecondary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Paid (${order.paymentMethod})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(formatInr(order.totalInr), style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily), color = EmeraldSuccess)
                }

                if (codes.isNotEmpty()) {
                    HorizontalDivider(color = SubtleBorder)
                    Text("Unlocked Digital Codes:", style = MaterialTheme.typography.titleSmall, color = PurpleGlow)
                    codes.forEach { code ->
                        Surface(
                            color = ObsidianVoid,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, LuxuryGold.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(code.productTitle, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        code.code,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                                        color = BrightGold
                                    )
                                }
                                IconButton(onClick = { copyToClipboard(context, "REDEEMI Code", code.code) }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code", tint = SkyBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple)
            ) {
                Text("Done")
            }
        }
    )
}

fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, "REDEEMI: Copied to clipboard", Toast.LENGTH_SHORT).show()
}
