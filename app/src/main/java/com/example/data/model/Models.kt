package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.text.NumberFormat
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class UserRole(val raw: String) {
    CUSTOMER("customer"),
    ADMIN("admin");

    companion object {
        fun fromRaw(value: String?): UserRole =
            entries.firstOrNull { it.raw.equals(value, ignoreCase = true) } ?: CUSTOMER
    }
}

enum class ProductCategory(val label: String) {
    ALL("All"),
    GAMING("Gaming"),
    STREAMING("Streaming"),
    SHOPPING("Shopping"),
    GIFT_CARDS("Gift Cards"),
    APPS_CLOUD("Apps & Cloud");

    companion object {
        val creatableCategories = listOf(GAMING, STREAMING, SHOPPING, GIFT_CARDS, APPS_CLOUD)
        fun fromLabel(label: String?): ProductCategory =
            creatableCategories.firstOrNull { it.label == label } ?: GIFT_CARDS
    }
}

enum class PaymentMethodType(val code: String, val displayName: String, val subtitle: String) {
    UPI("UPI", "UPI Instant", "GPay, PhonePe, Paytm, BHIM"),
    CARD("CARD", "Credit / Debit Card", "Visa, Mastercard, RuPay"),
    NETBANKING("NETBANKING", "NetBanking", "All major Indian banks"),
    WALLET("WALLET", "REDEEMI Express Pay", "Instant verified wallet");

    companion object {
        fun fromCode(code: String?): PaymentMethodType =
            entries.firstOrNull { it.code == code } ?: UPI
    }
}

enum class AccentStyle(val raw: String) {
    PURPLE("purple"),
    BLUE("blue"),
    GOLD("gold"),
    EMERALD("emerald");

    companion object {
        fun fromRaw(raw: String?): AccentStyle =
            entries.firstOrNull { it.raw == raw } ?: PURPLE
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val role: String = "customer",
    val blocked: Boolean = false,
    val totalOrders: Long = 0L,
    val totalSpentInr: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val grantedBy: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlatformConfig(
    @DocumentId val id: String = "settings",
    val platformName: String = "REDEEMI",
    val tagline: String = "Buy. Redeem. Enjoy.",
    val currency: String = "INR",
    val platformFeePercent: Long = 2L,
    val gstPercent: Long = 0L,
    val maxCartQuantityPerItem: Long = 5L,
    val maintenanceMode: Boolean = false,
    val announcementBanner: String = "Instant Digital Delivery • 100% Verified INR Codes",
    val enabledPaymentMethods: List<String> = listOf("UPI", "CARD", "NETBANKING", "WALLET"),
    val supportEmail: String = "support@redeemi.in",
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val title: String = "",
    val brand: String = "",
    val category: String = "Gift Cards",
    val description: String = "",
    val redemptionInstructions: String = "",
    val priceInr: Long = 0L,
    val mrpInr: Long = 0L,
    val stockCount: Long = 0L,
    val active: Boolean = true,
    val featured: Boolean = false,
    val accentStyle: String = "purple",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CodeSlot(
    @DocumentId val id: String = "",
    val productId: String = "",
    val status: String = "available",
    val claimedBy: String? = null,
    val orderId: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RedeemCode(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val code: String = "",
    val status: String = "available",
    val assignedToUserId: String? = null,
    val orderId: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val productId: String = "",
    val quantity: Long = 1L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerEmail: String = "",
    val customerName: String = "",
    val status: String = "paid",
    val paymentMethod: String = "UPI",
    val paymentReference: String = "",
    val subtotalInr: Long = 0L,
    val platformFeeInr: Long = 0L,
    val totalInr: Long = 0L,
    val itemCount: Long = 1L,
    val productSummary: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrderItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val brand: String = "",
    val quantity: Long = 1L,
    val unitPriceInr: Long = 0L,
    val lineTotalInr: Long = 0L,
    val slotIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null
)

data class EnrichedCartLine(
    val cartItem: CartItem,
    val product: Product
) {
    val lineTotalInr: Long get() = product.priceInr * cartItem.quantity
}

data class CheckoutReceipt(
    val order: Order,
    val items: List<OrderItem>,
    val deliveredCodes: List<RedeemCode>
)

fun formatInr(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"))
    return "₹${formatter.format(amount)}"
}

fun discountPercent(mrp: Long, price: Long): Int {
    if (mrp <= price || mrp <= 0L) return 0
    return (((mrp - price) * 100.0) / mrp.toDouble()).toInt().coerceIn(0, 99)
}
