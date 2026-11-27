package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.CheckoutReceipt
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.PlatformConfig
import com.example.data.model.Product
import com.example.data.model.RedeemCode
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RedeemiRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing REDEEMI.")

    private fun isBootstrapAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified && user.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)
    }

    private fun newCleanId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(16)}"

    // --- USER PROFILE & RBAC ---

    suspend fun ensureUserProfile(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val user = auth.currentUser!!
            val userDocRef = db.collection("users").document(uid)
            val existingSnap = userDocRef.get().await()
            val adminSnap = db.collection("admins").document(uid).get().await()
            val shouldBeAdmin = isBootstrapAdmin() || adminSnap.exists()

            if (!existingSnap.exists()) {
                val safeEmail = (user.email ?: "$uid@redeemi.in").take(200)
                val safeName = (user.displayName?.takeIf { it.isNotBlank() }
                    ?: safeEmail.substringBefore("@").ifBlank { "REDEEMI Member" }).take(100)
                val payload = mapOf(
                    "userId" to uid,
                    "email" to safeEmail,
                    "displayName" to safeName,
                    "photoUrl" to user.photoUrl?.toString()?.take(500),
                    "role" to if (shouldBeAdmin) "admin" else "customer",
                    "blocked" to false,
                    "totalOrders" to 0L,
                    "totalSpentInr" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                userDocRef.set(payload).await()
            } else {
                val safeEmail = (user.email ?: existingSnap.getString("email") ?: "$uid@redeemi.in").take(200)
                val safeName = (user.displayName?.takeIf { it.isNotBlank() }
                    ?: existingSnap.getString("displayName")
                    ?: "REDEEMI Member").take(100)
                val updates = mutableMapOf<String, Any>(
                    "email" to safeEmail,
                    "displayName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                user.photoUrl?.toString()?.take(500)?.let { updates["photoUrl"] = it }
                userDocRef.update(updates).await()
            }

            if (isBootstrapAdmin()) {
                ensureDefaultPlatformConfigIfAdmin()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeIsAdmin(userId: String): Flow<Boolean> = flow {
        if (isBootstrapAdmin()) {
            emit(true)
            return@flow
        }
        val path = "admins/$userId"
        emitAll(
            db.collection("admins").document(userId)
                .snapshots()
                .map { it.exists() }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                    }
                    emit(false)
                }
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, "users/$userId")
                }
                throw error
            }

    fun observeAllUsers(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun setUserBlocked(targetUserId: String, blocked: Boolean): Result<Unit> {
        val path = "users/$targetUserId"
        return try {
            requireUserId()
            require(targetUserId.isNotBlank()) { "Target user ID is required" }
            db.collection("users").document(targetUserId)
                .update(
                    mapOf(
                        "blocked" to blocked,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setUserAdminRole(targetUser: UserProfile, makeAdmin: Boolean): Result<Unit> {
        val path = "admins/${targetUser.userId}"
        return try {
            val adminUid = requireUserId()
            require(targetUser.userId.isNotBlank()) { "User ID required" }
            val batch = db.batch()
            val userRef = db.collection("users").document(targetUser.userId)
            val adminRef = db.collection("admins").document(targetUser.userId)
            batch.update(
                userRef,
                mapOf(
                    "role" to if (makeAdmin) "admin" else "customer",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (makeAdmin) {
                batch.set(
                    adminRef,
                    mapOf(
                        "userId" to targetUser.userId,
                        "email" to targetUser.email.ifBlank { "member@redeemi.in" },
                        "grantedBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.delete(adminRef)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- PLATFORM CONFIG ---

    fun observePlatformConfig(): Flow<PlatformConfig> =
        db.collection("platformConfig").document("settings")
            .snapshots()
            .map { snap ->
                snap.toObject(PlatformConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: PlatformConfig()
            }

    private suspend fun ensureDefaultPlatformConfigIfAdmin() {
        val ref = db.collection("platformConfig").document("settings")
        val snap = ref.get().await()
        if (!snap.exists()) {
            ref.set(
                mapOf(
                    "platformName" to "REDEEMI",
                    "tagline" to "Buy. Redeem. Enjoy.",
                    "currency" to "INR",
                    "platformFeePercent" to 2L,
                    "gstPercent" to 0L,
                    "maxCartQuantityPerItem" to 5L,
                    "maintenanceMode" to false,
                    "announcementBanner" to "Instant Digital Delivery • 100% Verified INR Codes",
                    "enabledPaymentMethods" to listOf("UPI", "CARD", "NETBANKING", "WALLET"),
                    "supportEmail" to "support@redeemi.in",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun updatePlatformConfig(config: PlatformConfig): Result<Unit> {
        val path = "platformConfig/settings"
        return try {
            requireUserId()
            require(config.enabledPaymentMethods.isNotEmpty()) { "Select at least one payment method." }
            require(config.platformFeePercent in 0L..20L) { "Platform fee must be 0%–20%." }
            require(config.gstPercent in 0L..28L) { "GST must be 0%–28%." }
            require(config.maxCartQuantityPerItem in 1L..20L) { "Max quantity must be 1–20." }
            db.collection("platformConfig").document("settings")
                .set(
                    mapOf(
                        "platformName" to config.platformName.trim().ifBlank { "REDEEMI" }.take(40),
                        "tagline" to config.tagline.trim().ifBlank { "Buy. Redeem. Enjoy." }.take(100),
                        "currency" to "INR",
                        "platformFeePercent" to config.platformFeePercent,
                        "gstPercent" to config.gstPercent,
                        "maxCartQuantityPerItem" to config.maxCartQuantityPerItem,
                        "maintenanceMode" to config.maintenanceMode,
                        "announcementBanner" to config.announcementBanner.trim().take(200),
                        "enabledPaymentMethods" to config.enabledPaymentMethods.distinct(),
                        "supportEmail" to config.supportEmail.trim().ifBlank { "support@redeemi.in" }.take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- PRODUCTS ---

    fun observeProducts(): Flow<List<Product>> =
        db.collection("products")
            .snapshots()
            .map { snap ->
                snap.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareByDescending<Product> { it.featured }.thenByDescending { it.createdAt ?: Timestamp(0, 0) })
            }

    suspend fun saveProduct(
        existingProduct: Product?,
        title: String,
        brand: String,
        category: String,
        description: String,
        redemptionInstructions: String,
        priceInr: Long,
        mrpInr: Long,
        active: Boolean,
        featured: Boolean,
        accentStyle: String
    ): Result<Unit> {
        val docId = existingProduct?.id?.takeIf { it.isNotBlank() } ?: newCleanId("prod")
        val path = "products/$docId"
        return try {
            requireUserId()
            require(title.trim().length in 2..80) { "Title must be 2–80 characters." }
            require(brand.trim().length in 2..40) { "Brand must be 2–40 characters." }
            require(description.trim().length in 5..600) { "Description must be 5–600 characters." }
            require(redemptionInstructions.trim().length in 5..600) { "Instructions must be 5–600 characters." }
            require(priceInr in 1L..1000000L) { "Price must be at least ₹1." }
            require(mrpInr >= priceInr && mrpInr <= 1000000L) { "MRP must be >= selling price." }

            val docRef = db.collection("products").document(docId)
            if (existingProduct == null) {
                docRef.set(
                    mapOf(
                        "title" to title.trim(),
                        "brand" to brand.trim(),
                        "category" to category,
                        "description" to description.trim(),
                        "redemptionInstructions" to redemptionInstructions.trim(),
                        "priceInr" to priceInr,
                        "mrpInr" to mrpInr,
                        "stockCount" to 0L,
                        "active" to active,
                        "featured" to featured,
                        "accentStyle" to accentStyle,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to title.trim(),
                        "brand" to brand.trim(),
                        "category" to category,
                        "description" to description.trim(),
                        "redemptionInstructions" to redemptionInstructions.trim(),
                        "priceInr" to priceInr,
                        "mrpInr" to mrpInr,
                        "active" to active,
                        "featured" to featured,
                        "accentStyle" to accentStyle,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val path = "products/$productId"
        return try {
            requireUserId()
            require(productId.isNotBlank()) { "Product ID required" }
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- REDEEM CODES & INVENTORY SLOTS ---

    fun observeAllRedeemCodesForAdmin(): Flow<List<RedeemCode>> =
        db.collection("redeemCodes")
            .snapshots()
            .map { snap ->
                snap.toObjects(RedeemCode::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeUserRedeemCodes(userId: String): Flow<List<RedeemCode>> =
        db.collection("redeemCodes")
            .whereEqualTo("assignedToUserId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(RedeemCode::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun uploadRedeemCodesBatch(product: Product, rawCodes: List<String>): Result<Int> {
        val path = "redeemCodes"
        return try {
            requireUserId()
            val cleanCodes = rawCodes.map { it.trim() }.filter { it.length in 4..120 }.distinct()
            require(cleanCodes.isNotEmpty()) { "Enter at least one valid code (4–120 chars)." }

            cleanCodes.chunked(150).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { codeValue ->
                    val slotId = newCleanId("slot")
                    val slotRef = db.collection("codeSlots").document(slotId)
                    val codeRef = db.collection("redeemCodes").document(slotId)
                    batch.set(
                        slotRef,
                        mapOf(
                            "productId" to product.id,
                            "status" to "available",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.set(
                        codeRef,
                        mapOf(
                            "productId" to product.id,
                            "productTitle" to product.title,
                            "code" to codeValue,
                            "status" to "available",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.update(
                    db.collection("products").document(product.id),
                    mapOf(
                        "stockCount" to FieldValue.increment(chunk.size.toLong()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
            }
            Result.success(cleanCodes.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteUnsoldCode(code: RedeemCode): Result<Unit> {
        val path = "redeemCodes/${code.id}"
        return try {
            requireUserId()
            require(code.status == "available") { "Cannot delete a sold redeem code." }
            val prodSnap = db.collection("products").document(code.productId).get().await()
            val batch = db.batch()
            batch.delete(db.collection("redeemCodes").document(code.id))
            batch.delete(db.collection("codeSlots").document(code.id))
            if (prodSnap.exists()) {
                val currentStock = prodSnap.getLong("stockCount") ?: 0L
                if (currentStock > 0L) {
                    batch.update(
                        db.collection("products").document(code.productId),
                        mapOf(
                            "stockCount" to (currentStock - 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- SHOPPING CART ---

    fun observeCart(userId: String): Flow<List<CartItem>> =
        db.collection("users").document(userId).collection("cart")
            .snapshots()
            .map { snap ->
                snap.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun addToCart(productId: String, quantityDelta: Long, maxPerItem: Long): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/cart/$productId"
        return try {
            require(productId.isNotBlank()) { "Invalid product ID" }
            require(quantityDelta in 1L..20L) { "Invalid quantity" }
            val cartRef = db.collection("users").document(uid).collection("cart").document(productId)
            val snap = cartRef.get().await()
            if (!snap.exists()) {
                val capped = quantityDelta.coerceAtMost(maxPerItem.coerceIn(1L, 20L))
                cartRef.set(
                    mapOf(
                        "productId" to productId,
                        "quantity" to capped,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val current = snap.getLong("quantity") ?: 1L
                val next = (current + quantityDelta).coerceAtMost(maxPerItem.coerceIn(1L, 20L))
                cartRef.update(
                    mapOf(
                        "quantity" to next,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(productId: String, newQuantity: Long, maxPerItem: Long): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/cart/$productId"
        return try {
            val cartRef = db.collection("users").document(uid).collection("cart").document(productId)
            if (newQuantity <= 0L) {
                cartRef.delete().await()
            } else {
                val capped = newQuantity.coerceIn(1L, maxPerItem.coerceIn(1L, 20L))
                cartRef.update(
                    mapOf(
                        "quantity" to capped,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(productId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/cart/$productId"
        return try {
            db.collection("users").document(uid).collection("cart").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- ORDERS & ATOMIC CHECKOUT ---

    fun observeUserOrders(userId: String): Flow<List<Order>> =
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeAllOrdersForAdmin(): Flow<List<Order>> =
        db.collection("orders")
            .snapshots()
            .map { snap ->
                snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun getOrderItems(orderId: String, isAdmin: Boolean): Result<List<OrderItem>> {
        val path = "orders/$orderId/items"
        return try {
            val uid = requireUserId()
            val query = if (isAdmin) {
                db.collection("orders").document(orderId).collection("items")
            } else {
                db.collection("orders").document(orderId).collection("items")
                    .whereEqualTo("userId", uid)
            }
            val items = query.get().await()
                .toObjects(OrderItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun refundOrder(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId)
                .update(
                    mapOf(
                        "status" to "refunded",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun placeOrderAndClaimCodes(
        cartItems: List<CartItem>,
        paymentMethod: String
    ): Result<CheckoutReceipt> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Please sign in first."))
        val orderId = newCleanId("ord")
        val path = "orders/$orderId"
        return try {
            require(cartItems.isNotEmpty()) { "Your cart is empty." }

            // 1. Read authoritative platform config & user profile
            val configSnap = db.collection("platformConfig").document("settings").get().await()
            val config = configSnap.toObject(PlatformConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: PlatformConfig()
            require(!config.maintenanceMode) { "REDEEMI is undergoing maintenance. Please try again shortly." }
            require(paymentMethod in config.enabledPaymentMethods) {
                "Payment method $paymentMethod is currently unavailable."
            }

            val userSnap = db.collection("users").document(uid).get().await()
            val userProfile = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            require(userProfile?.blocked != true) { "Your account is currently suspended. Contact REDEEMI support." }

            // 2. Fetch authoritative product prices & reserve available non-secret codeSlots
            data class PreparedLine(
                val product: Product,
                val quantity: Long,
                val slotIds: List<String>
            )

            val preparedLines = mutableListOf<PreparedLine>()
            for (line in cartItems) {
                val qty = line.quantity.coerceIn(1L, config.maxCartQuantityPerItem)
                val prodSnap = db.collection("products").document(line.productId).get().await()
                val product = prodSnap.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("A product in your cart is no longer available.")
                require(product.active) { "${product.title} is currently unavailable." }
                require(product.stockCount >= qty) { "Insufficient stock for ${product.title}." }

                val slotSnaps = db.collection("codeSlots")
                    .whereEqualTo("productId", product.id)
                    .whereEqualTo("status", "available")
                    .limit(qty)
                    .get()
                    .await()
                val slotIds = slotSnaps.documents.map { it.id }
                require(slotIds.size.toLong() == qty) {
                    "Not enough digital codes ready in vault for ${product.title}."
                }
                preparedLines.add(PreparedLine(product, qty, slotIds))
            }

            // 3. Calculate authoritative INR totals from admin prices & platform config
            val subtotalInr = preparedLines.sumOf { it.product.priceInr * it.quantity }
            val feePercent = (config.platformFeePercent + config.gstPercent).coerceIn(0L, 48L)
            val platformFeeInr = (subtotalInr * feePercent) / 100L
            val totalInr = subtotalInr + platformFeeInr
            val totalItemsCount = preparedLines.sumOf { it.quantity }
            val paymentRef = "RDM-${paymentMethod.take(3)}-${UUID.randomUUID().toString().take(8).uppercase()}"
            val summaryText = preparedLines.joinToString(", ") { "${it.product.title} × ${it.quantity}" }.take(200)
            val customerEmail = (auth.currentUser?.email ?: userProfile?.email ?: "member@redeemi.in").take(200)
            val customerName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: userProfile?.displayName ?: "REDEEMI Customer").take(100)

            // 4. Atomic Firestore Batch Write
            val batch = db.batch()
            val orderRef = db.collection("orders").document(orderId)
            batch.set(
                orderRef,
                mapOf(
                    "userId" to uid,
                    "customerEmail" to customerEmail,
                    "customerName" to customerName,
                    "status" to "paid",
                    "paymentMethod" to paymentMethod,
                    "paymentReference" to paymentRef,
                    "subtotalInr" to subtotalInr,
                    "platformFeeInr" to platformFeeInr,
                    "totalInr" to totalInr,
                    "itemCount" to totalItemsCount,
                    "productSummary" to summaryText,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val builtOrderItems = mutableListOf<OrderItem>()
            for (line in preparedLines) {
                val lineTotal = line.product.priceInr * line.quantity
                val itemRef = orderRef.collection("items").document(line.product.id)
                batch.set(
                    itemRef,
                    mapOf(
                        "userId" to uid,
                        "orderId" to orderId,
                        "productId" to line.product.id,
                        "productTitle" to line.product.title,
                        "brand" to line.product.brand,
                        "quantity" to line.quantity,
                        "unitPriceInr" to line.product.priceInr,
                        "lineTotalInr" to lineTotal,
                        "slotIds" to line.slotIds,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                builtOrderItems.add(
                    OrderItem(
                        id = line.product.id,
                        userId = uid,
                        orderId = orderId,
                        productId = line.product.id,
                        productTitle = line.product.title,
                        brand = line.product.brand,
                        quantity = line.quantity,
                        unitPriceInr = line.product.priceInr,
                        lineTotalInr = lineTotal,
                        slotIds = line.slotIds,
                        createdAt = Timestamp.now()
                    )
                )

                for (slotId in line.slotIds) {
                    batch.update(
                        db.collection("codeSlots").document(slotId),
                        mapOf(
                            "status" to "sold",
                            "claimedBy" to uid,
                            "orderId" to orderId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.update(
                        db.collection("redeemCodes").document(slotId),
                        mapOf(
                            "status" to "sold",
                            "assignedToUserId" to uid,
                            "orderId" to orderId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                batch.update(
                    db.collection("products").document(line.product.id),
                    mapOf(
                        "stockCount" to (line.product.stockCount - line.quantity),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                batch.delete(
                    db.collection("users").document(uid).collection("cart").document(line.product.id)
                )
            }

            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "totalOrders" to FieldValue.increment(1L),
                    "totalSpentInr" to FieldValue.increment(totalInr),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()

            // 5. Immediately fetch newly assigned secret redeem codes for instant delivery
            val deliveredCodes = db.collection("redeemCodes")
                .whereEqualTo("assignedToUserId", uid)
                .whereEqualTo("orderId", orderId)
                .get()
                .await()
                .toObjects(RedeemCode::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            val createdOrder = Order(
                id = orderId,
                userId = uid,
                customerEmail = customerEmail,
                customerName = customerName,
                status = "paid",
                paymentMethod = paymentMethod,
                paymentReference = paymentRef,
                subtotalInr = subtotalInr,
                platformFeeInr = platformFeeInr,
                totalInr = totalInr,
                itemCount = totalItemsCount,
                productSummary = summaryText,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            Result.success(CheckoutReceipt(createdOrder, builtOrderItems, deliveredCodes))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN SAMPLE CATALOG SEEDER ---

    suspend fun seedSampleCatalogIfAdmin(): Result<Int> {
        val path = "products"
        return try {
            requireUserId()
            ensureDefaultPlatformConfigIfAdmin()

            data class SeedItem(
                val id: String,
                val title: String,
                val brand: String,
                val category: String,
                val description: String,
                val instructions: String,
                val priceInr: Long,
                val mrpInr: Long,
                val featured: Boolean,
                val accent: String,
                val prefix: String
            )

            val items = listOf(
                SeedItem(
                    "prod_gplay_500", "Google Play Gift Code ₹500", "Google Play", "Gift Cards",
                    "Official India INR Google Play recharge voucher for apps, games, books, and subscriptions.",
                    "Open Google Play Store -> Tap Profile -> Payments & subscriptions -> Redeem code.",
                    485L, 500L, true, "purple", "GPLAY"
                ),
                SeedItem(
                    "prod_steam_1000", "Steam Wallet Code ₹1,000", "Steam", "Gaming",
                    "Instant INR Steam Wallet top-up for PC games, DLCs, and Steam Community Market.",
                    "Open Steam -> Click your account name -> Account Details -> Add funds -> Redeem Steam Wallet Code.",
                    969L, 1000L, true, "blue", "STEAM"
                ),
                SeedItem(
                    "prod_psn_1500", "PlayStation Store ₹1,500", "PlayStation", "Gaming",
                    "Redeemable PlayStation Network India wallet voucher for PS5 & PS4 games and PS Plus.",
                    "Open PlayStation Store -> Profile -> Redeem Code -> Enter the 12-character code.",
                    1449L, 1500L, true, "blue", "PSN"
                ),
                SeedItem(
                    "prod_amazon_1000", "Amazon Pay E-Gift Card ₹1,000", "Amazon", "Shopping",
                    "Instant Amazon Pay Balance gift code valid across shopping, bills, and Prime.",
                    "Open Amazon App -> Amazon Pay -> Add Gift Card -> Enter Voucher Code.",
                    975L, 1000L, true, "gold", "AMZN"
                ),
                SeedItem(
                    "prod_netflix_649", "Netflix Premium Pass ₹649", "Netflix", "Streaming",
                    "Digital Netflix India gift code valid toward 4K UHD Premium streaming plans.",
                    "Visit netflix.com/redeem -> Sign in -> Enter your REDEEMI digital voucher code.",
                    629L, 649L, false, "purple", "NFLX"
                ),
                SeedItem(
                    "prod_spotify_389", "Spotify Premium 3 Months", "Spotify", "Streaming",
                    "Ad-free music, offline downloads, and lossless playback voucher for Spotify India.",
                    "Visit spotify.com/redeem -> Log in -> Paste your REDEEMI activation PIN.",
                    359L, 389L, false, "emerald", "SPTF"
                ),
                SeedItem(
                    "prod_valorant_850", "Valorant 1000 VP India", "Riot Games", "Gaming",
                    "Official Riot PIN for 1000 Valorant Points on India/APAC servers with instant delivery.",
                    "Launch Valorant -> Click VP icon -> Prepaid Cards & Codes -> Paste Riot PIN.",
                    815L, 850L, true, "gold", "VALO"
                ),
                SeedItem(
                    "prod_xbox_549", "Xbox Game Pass Ultimate 1M", "Xbox", "Apps & Cloud",
                    "Play hundreds of console, PC, and cloud games with EA Play included.",
                    "Open redeem.microsoft.com -> Sign in -> Enter 25-character Xbox code.",
                    499L, 549L, false, "emerald", "XBOX"
                )
            )

            val batch = db.batch()
            var createdProducts = 0
            for (item in items) {
                val prodRef = db.collection("products").document(item.id)
                val existing = prodRef.get().await()
                if (!existing.exists()) {
                    batch.set(
                        prodRef,
                        mapOf(
                            "title" to item.title,
                            "brand" to item.brand,
                            "category" to item.category,
                            "description" to item.description,
                            "redemptionInstructions" to item.instructions,
                            "priceInr" to item.priceInr,
                            "mrpInr" to item.mrpInr,
                            "stockCount" to 4L,
                            "active" to true,
                            "featured" to item.featured,
                            "accentStyle" to item.accent,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    repeat(4) { idx ->
                        val slotId = newCleanId("slot")
                        val token = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
                        val codeString = "RDM-${item.prefix}-${token.take(4)}-${token.drop(4)}"
                        batch.set(
                            db.collection("codeSlots").document(slotId),
                            mapOf(
                                "productId" to item.id,
                                "status" to "available",
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        batch.set(
                            db.collection("redeemCodes").document(slotId),
                            mapOf(
                                "productId" to item.id,
                                "productTitle" to item.title,
                                "code" to codeString,
                                "status" to "available",
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    createdProducts++
                }
            }
            if (createdProducts > 0) {
                batch.commit().await()
            }
            Result.success(createdProducts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "spfoundation59@gmail.com"
    }
}
