package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.RedeemiRepository
import com.example.data.model.CartItem
import com.example.data.model.CheckoutReceipt
import com.example.data.model.EnrichedCartLine
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.PlatformConfig
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.RedeemCode
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CheckoutUiState {
    data object Idle : CheckoutUiState
    data object Processing : CheckoutUiState
    data class Success(val receipt: CheckoutReceipt) : CheckoutUiState
    data class Error(val message: String) : CheckoutUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class RedeemiViewModel(
    private val repository: RedeemiRepository,
    val currentUserId: String?,
    initialGuestCart: List<CartItem> = emptyList()
) : ViewModel() {

    val isGuest: Boolean get() = currentUserId == null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ProductCategory.ALL)
    val selectedCategory: StateFlow<ProductCategory> = _selectedCategory.asStateFlow()

    private val _checkoutState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val checkoutState: StateFlow<CheckoutUiState> = _checkoutState.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _guestCartItems = MutableStateFlow(initialGuestCart)

    init {
        if (currentUserId != null) {
            viewModelScope.launch {
                repository.ensureUserProfile()
                if (initialGuestCart.isNotEmpty()) {
                    val maxPerItem = platformConfig.value.maxCartQuantityPerItem
                    initialGuestCart.forEach { item ->
                        repository.addToCart(item.productId, item.quantity, maxPerItem)
                    }
                }
            }
        }
    }

    private fun <T> Flow<T>.toUiState(tag: String, fallbackMessage: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "$tag stream error", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackMessage))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val isAdmin: StateFlow<Boolean> = if (currentUserId == null) {
        MutableStateFlow(false).asStateFlow()
    } else {
        repository.observeIsAdmin(currentUserId)
            .catch { error ->
                if (error is CancellationException) throw error
                emit(false)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)
    }

    val userProfile: StateFlow<UserProfile?> = if (currentUserId == null) {
        MutableStateFlow<UserProfile?>(null).asStateFlow()
    } else {
        repository.observeUserProfile(currentUserId)
            .catch { error ->
                if (error is CancellationException) throw error
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)
    }

    val platformConfig: StateFlow<PlatformConfig> = repository.observePlatformConfig()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(PlatformConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlatformConfig())

    val productsState: StateFlow<UiState<List<Product>>> = repository.observeProducts()
        .toUiState("Products", "Failed to load REDEEMI catalog")

    val cartItems: StateFlow<List<CartItem>> = if (currentUserId == null) {
        _guestCartItems.asStateFlow()
    } else {
        repository.observeCart(currentUserId)
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
    }

    val enrichedCart: StateFlow<List<EnrichedCartLine>> = combine(
        cartItems,
        productsState
    ) { cart, prodState ->
        val products = (prodState as? UiState.Success)?.data ?: emptyList()
        val productMap = products.associateBy { it.id }
        cart.mapNotNull { item ->
            productMap[item.productId]?.let { prod ->
                EnrichedCartLine(item, prod)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val userOrdersState: StateFlow<UiState<List<Order>>> = if (currentUserId == null) {
        MutableStateFlow(UiState.Success(emptyList<Order>())).asStateFlow()
    } else {
        repository.observeUserOrders(currentUserId)
            .toUiState("UserOrders", "Failed to load your purchase history")
    }

    val userRedeemCodesState: StateFlow<UiState<List<RedeemCode>>> = if (currentUserId == null) {
        MutableStateFlow(UiState.Success(emptyList<RedeemCode>())).asStateFlow()
    } else {
        repository.observeUserRedeemCodes(currentUserId)
            .toUiState("UserCodes", "Failed to load your REDEEMI code vault")
    }

    // Store-manager-only streams (strictly gated so non-managers never attach listeners)
    val allUsersState: StateFlow<UiState<List<UserProfile>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(emptyList())
            else repository.observeAllUsers()
        }
        .toUiState("ConsoleUsers", "Failed to load users")

    val allOrdersState: StateFlow<UiState<List<Order>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(emptyList())
            else repository.observeAllOrdersForAdmin()
        }
        .toUiState("ConsoleOrders", "Failed to load orders")

    val allAdminCodesState: StateFlow<UiState<List<RedeemCode>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(emptyList())
            else repository.observeAllRedeemCodesForAdmin()
        }
        .toUiState("ConsoleCodes", "Failed to load code vault")

    // --- STOREFRONT & CART ACTIONS ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: ProductCategory) {
        _selectedCategory.value = category
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun resetCheckoutState() {
        _checkoutState.value = CheckoutUiState.Idle
    }

    fun addToCart(product: Product, quantity: Long = 1L) {
        viewModelScope.launch {
            if (!product.active || product.stockCount <= 0L) {
                _feedbackMessage.value = "${product.title} is currently out of stock."
                return@launch
            }
            val maxPerItem = platformConfig.value.maxCartQuantityPerItem
            if (isGuest) {
                val current = _guestCartItems.value.toMutableList()
                val idx = current.indexOfFirst { it.productId == product.id }
                val existingQty = if (idx >= 0) current[idx].quantity else 0L
                val nextQty = (existingQty + quantity).coerceIn(1L, minOf(product.stockCount, maxPerItem))
                if (idx >= 0) {
                    current[idx] = current[idx].copy(quantity = nextQty)
                } else {
                    current.add(CartItem(id = product.id, productId = product.id, quantity = nextQty))
                }
                _guestCartItems.value = current
                _feedbackMessage.value = "Added ${product.title} to REDEEMI cart"
                return@launch
            }

            repository.addToCart(product.id, quantity, maxPerItem)
                .onSuccess { _feedbackMessage.value = "Added ${product.title} to REDEEMI cart" }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Unable to add to cart" }
        }
    }

    fun updateCartQuantity(productId: String, newQuantity: Long) {
        viewModelScope.launch {
            val maxPerItem = platformConfig.value.maxCartQuantityPerItem
            if (isGuest) {
                if (newQuantity <= 0L) {
                    _guestCartItems.value = _guestCartItems.value.filterNot { it.productId == productId }
                } else {
                    val clamped = newQuantity.coerceIn(1L, maxPerItem)
                    _guestCartItems.value = _guestCartItems.value.map { item ->
                        if (item.productId == productId) item.copy(quantity = clamped) else item
                    }
                }
                return@launch
            }

            repository.updateCartQuantity(productId, newQuantity, maxPerItem)
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to update cart" }
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            if (isGuest) {
                _guestCartItems.value = _guestCartItems.value.filterNot { it.productId == productId }
                return@launch
            }

            repository.removeFromCart(productId)
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to remove item" }
        }
    }

    fun completeCheckout(paymentMethodCode: String) {
        if (isGuest) {
            _checkoutState.value = CheckoutUiState.Error("Please sign in with Google to pay and unlock your digital codes.")
            return
        }
        viewModelScope.launch {
            _checkoutState.value = CheckoutUiState.Processing
            val result = repository.placeOrderAndClaimCodes(cartItems.value, paymentMethodCode)
            result.onSuccess { receipt ->
                _checkoutState.value = CheckoutUiState.Success(receipt)
                _feedbackMessage.value = "Payment verified! Instant codes delivered to your REDEEMI Vault."
            }.onFailure { error ->
                _checkoutState.value = CheckoutUiState.Error(
                    error.localizedMessage ?: "Checkout failed. No codes were charged."
                )
            }
        }
    }

    suspend fun loadOrderItems(orderId: String): List<OrderItem> {
        if (isGuest) return emptyList()
        return repository.getOrderItems(orderId, isAdmin.value).getOrElse { emptyList() }
    }

    // --- STORE MANAGEMENT ACTIONS (Protected both in UI and Firestore Rules) ---

    fun saveProduct(
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
    ) {
        if (!isAdmin.value) {
            _feedbackMessage.value = "Store manager authorization required."
            return
        }
        viewModelScope.launch {
            _isActionLoading.value = true
            repository.saveProduct(
                existingProduct, title, brand, category, description,
                redemptionInstructions, priceInr, mrpInr, active, featured, accentStyle
            ).onSuccess {
                _feedbackMessage.value = if (existingProduct == null) "Product created in REDEEMI catalog" else "Product updated"
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to save product"
            }
            _isActionLoading.value = false
        }
    }

    fun deleteProduct(productId: String) {
        if (!isAdmin.value) return
        viewModelScope.launch {
            _isActionLoading.value = true
            repository.deleteProduct(productId)
                .onSuccess { _feedbackMessage.value = "Product removed from catalog" }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to delete product" }
            _isActionLoading.value = false
        }
    }

    fun uploadRedeemCodes(product: Product, rawCodesText: String) {
        if (!isAdmin.value) return
        val lines = rawCodesText.lines().map { it.trim() }.filter { it.isNotBlank() }
        viewModelScope.launch {
            _isActionLoading.value = true
            repository.uploadRedeemCodesBatch(product, lines)
                .onSuccess { count ->
                    _feedbackMessage.value = "Uploaded $count redeem code(s) to ${product.title}"
                }
                .onFailure {
                    _feedbackMessage.value = it.localizedMessage ?: "Failed to upload codes"
                }
            _isActionLoading.value = false
        }
    }

    fun deleteUnsoldCode(code: RedeemCode) {
        if (!isAdmin.value) return
        viewModelScope.launch {
            repository.deleteUnsoldCode(code)
                .onSuccess { _feedbackMessage.value = "Unsold code removed from vault" }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to delete code" }
        }
    }

    fun updatePlatformConfig(config: PlatformConfig) {
        if (!isAdmin.value) return
        viewModelScope.launch {
            _isActionLoading.value = true
            repository.updatePlatformConfig(config)
                .onSuccess { _feedbackMessage.value = "REDEEMI platform rules & payment settings saved" }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to update settings" }
            _isActionLoading.value = false
        }
    }

    fun toggleUserBlocked(user: UserProfile) {
        if (!isAdmin.value) return
        viewModelScope.launch {
            repository.setUserBlocked(user.userId, !user.blocked)
                .onSuccess {
                    _feedbackMessage.value = if (!user.blocked) "${user.displayName} suspended" else "${user.displayName} restored"
                }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to update user" }
        }
    }

    fun toggleUserAdmin(user: UserProfile) {
        if (!isAdmin.value) return
        val makeAdmin = user.role != "admin"
        viewModelScope.launch {
            repository.setUserAdminRole(user, makeAdmin)
                .onSuccess {
                    _feedbackMessage.value = if (makeAdmin) "${user.displayName} granted Store Manager access" else "Store Manager access revoked for ${user.displayName}"
                }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to update role" }
        }
    }

    fun refundOrder(orderId: String) {
        if (!isAdmin.value) return
        viewModelScope.launch {
            repository.refundOrder(orderId)
                .onSuccess { _feedbackMessage.value = "Order marked as refunded" }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to update order" }
        }
    }

    fun seedSampleCatalog() {
        if (!isAdmin.value) return
        viewModelScope.launch {
            _isActionLoading.value = true
            repository.seedSampleCatalogIfAdmin()
                .onSuccess { count ->
                    _feedbackMessage.value = if (count > 0) {
                        "Stocked $count REDEEMI products with 32 verified digital codes!"
                    } else {
                        "Official catalog products already exist."
                    }
                }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to seed catalog" }
            _isActionLoading.value = false
        }
    }

    private companion object {
        const val TAG = "RedeemiVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
