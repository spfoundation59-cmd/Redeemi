package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.RedeemiRepository
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.signOut
import com.example.data.model.CartItem
import com.example.data.model.UiState
import com.example.ui.components.RedeemiTopBar
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartAndCheckoutScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.VaultAndOrdersScreen
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSlate
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RedeemiTheme
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.RedeemiViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RedeemiTheme {
                RedeemiRootApp()
            }
        }
    }
}

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    STORE("Store", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    CART("Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    VAULT("Vault", Icons.Filled.VpnKey, Icons.Outlined.VpnKey),
    PROFILE("Account", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun RedeemiRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isGuestMode by rememberSaveable { mutableStateOf(false) }
    var pendingGuestCart by remember { mutableStateOf<List<CartItem>>(emptyList()) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                isGuestMode = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    isGuestMode = false
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    if (currentUser == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                isGuestMode = false
            },
            onContinueAsGuest = {
                isGuestMode = true
            }
        )
    } else {
        val user = currentUser
        val shellKey = user?.uid ?: "guest_session"
        key(shellKey) {
            RedeemiAppShell(
                currentUserId = user?.uid,
                userEmail = user?.email.orEmpty(),
                userName = user?.displayName.orEmpty(),
                initialGuestCart = if (user != null) pendingGuestCart else emptyList(),
                onGuestCartChanged = { items ->
                    if (user == null) {
                        pendingGuestCart = items
                    }
                },
                onGuestSignedIn = {
                    currentUser = Firebase.auth.currentUser
                    isGuestMode = false
                },
                onExitGuestMode = {
                    isGuestMode = false
                },
                onSignOutRequested = {
                    pendingGuestCart = emptyList()
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope,
                        onSignOutComplete = {
                            currentUser = null
                            isGuestMode = false
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun RedeemiAppShell(
    currentUserId: String?,
    userEmail: String,
    userName: String,
    initialGuestCart: List<CartItem>,
    onGuestCartChanged: (List<CartItem>) -> Unit,
    onGuestSignedIn: () -> Unit,
    onExitGuestMode: () -> Unit,
    onSignOutRequested: () -> Unit
) {
    val vmKey = currentUserId ?: "guest_session"
    val viewModel: RedeemiViewModel = viewModel(
        key = vmKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                RedeemiViewModel(RedeemiRepository(db), currentUserId, initialGuestCart)
            }
        }
    )

    val isGuest = currentUserId == null
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val platformConfig by viewModel.platformConfig.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val enrichedCart by viewModel.enrichedCart.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val checkoutState by viewModel.checkoutState.collectAsStateWithLifecycle()
    val userOrdersState by viewModel.userOrdersState.collectAsStateWithLifecycle()
    val userRedeemCodesState by viewModel.userRedeemCodesState.collectAsStateWithLifecycle()
    val allUsersState by viewModel.allUsersState.collectAsStateWithLifecycle()
    val allOrdersState by viewModel.allOrdersState.collectAsStateWithLifecycle()
    val allAdminCodesState by viewModel.allAdminCodesState.collectAsStateWithLifecycle()
    val isActionLoading by viewModel.isActionLoading.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    LaunchedEffect(cartItems, isGuest) {
        if (isGuest) {
            onGuestCartChanged(cartItems)
        }
    }

    var selectedTabIndex by rememberSaveable(vmKey) { mutableIntStateOf(0) }
    var isConsoleMode by rememberSaveable(vmKey) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    if (!isAdmin && isConsoleMode) {
        isConsoleMode = false
    }

    if (!isConsoleMode && selectedTabIndex != 0) {
        BackHandler { selectedTabIndex = 0 }
    }

    val cartTotalItems = enrichedCart.sumOf { it.cartItem.quantity }.toInt()
    val unlockedCount = (userRedeemCodesState as? UiState.Success)?.data?.size ?: 0
    val currentTab = MainTab.entries.getOrElse(selectedTabIndex) { MainTab.STORE }

    val pageSubtitle = if (isConsoleMode) {
        "REDEEMI Store Console • Inventory, Pricing & Rules"
    } else if (isGuest) {
        when (currentTab) {
            MainTab.STORE -> "REDEEMI | Guest Storefront • Buy. Redeem. Enjoy."
            MainTab.CART -> "REDEEMI Cart • Sign in with Google to Pay"
            MainTab.VAULT -> "REDEEMI Vault • Sign in to Access Codes"
            MainTab.PROFILE -> "REDEEMI Account • Guest Session"
        }
    } else {
        when (currentTab) {
            MainTab.STORE -> "REDEEMI | Digital Code Marketplace • Buy. Redeem. Enjoy."
            MainTab.CART -> "REDEEMI Checkout • Instant INR Delivery"
            MainTab.VAULT -> "REDEEMI Vault • Your Unlocked Codes & Orders"
            MainTab.PROFILE -> "REDEEMI Account • Customer Dashboard"
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            containerColor = ObsidianVoid,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = CardSlate,
                        contentColor = BrightGold
                    )
                }
            },
            topBar = {
                RedeemiTopBar(
                    pageSubtitle = pageSubtitle,
                    cartCount = cartTotalItems,
                    isAdmin = isAdmin,
                    isAdminMode = isConsoleMode,
                    onCartClick = {
                        isConsoleMode = false
                        selectedTabIndex = MainTab.CART.ordinal
                    },
                    onToggleAdminClick = {
                        if (isAdmin) isConsoleMode = !isConsoleMode
                    },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                )
            },
            bottomBar = {
                if (!useRail && !isConsoleMode) {
                    NavigationBar(
                        containerColor = MidnightSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        MainTab.entries.forEachIndexed { index, tab ->
                            val selected = selectedTabIndex == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTabIndex = index },
                                icon = {
                                    if (tab == MainTab.CART && cartTotalItems > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = LuxuryGold, contentColor = ObsidianVoid) {
                                                    Text(cartTotalItems.toString())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrightGold,
                                    selectedTextColor = BrightGold,
                                    indicatorColor = RoyalPurple.copy(alpha = 0.28f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !isConsoleMode) {
                    NavigationRail(
                        containerColor = MidnightSurface,
                        contentColor = TextPrimary
                    ) {
                        MainTab.entries.forEachIndexed { index, tab ->
                            val selected = selectedTabIndex == index
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTabIndex = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = BrightGold,
                                    selectedTextColor = BrightGold,
                                    indicatorColor = RoyalPurple.copy(alpha = 0.28f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (isConsoleMode && isAdmin) {
                        AdminDashboardScreen(
                            productsState = productsState,
                            codesState = allAdminCodesState,
                            ordersState = allOrdersState,
                            usersState = allUsersState,
                            platformConfig = platformConfig,
                            isActionLoading = isActionLoading,
                            currentUserEmail = userEmail,
                            onBackToStore = { isConsoleMode = false },
                            onSeedSampleCatalog = viewModel::seedSampleCatalog,
                            onSaveProduct = viewModel::saveProduct,
                            onDeleteProduct = viewModel::deleteProduct,
                            onUploadCodes = viewModel::uploadRedeemCodes,
                            onDeleteUnsoldCode = viewModel::deleteUnsoldCode,
                            onRefundOrder = viewModel::refundOrder,
                            onToggleUserBlocked = viewModel::toggleUserBlocked,
                            onToggleUserAdmin = viewModel::toggleUserAdmin,
                            onSavePlatformConfig = viewModel::updatePlatformConfig
                        )
                    } else {
                        when (currentTab) {
                            MainTab.STORE -> MarketplaceScreen(
                                productsState = productsState,
                                platformConfig = platformConfig,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                isAdmin = isAdmin,
                                onSearchChange = viewModel::setSearchQuery,
                                onCategorySelected = viewModel::setSelectedCategory,
                                onAddToCart = viewModel::addToCart,
                                onBuyNow = { prod, qty ->
                                    viewModel.addToCart(prod, qty)
                                    selectedTabIndex = MainTab.CART.ordinal
                                },
                                onOpenAdminSeed = {
                                    if (isAdmin) isConsoleMode = true
                                }
                            )
                            MainTab.CART -> CartAndCheckoutScreen(
                                cartLines = enrichedCart,
                                platformConfig = platformConfig,
                                checkoutState = checkoutState,
                                isGuest = isGuest,
                                onGuestSignedIn = onGuestSignedIn,
                                onGuestSignInError = { viewModel.showFeedback(it) },
                                onUpdateQty = viewModel::updateCartQuantity,
                                onRemoveItem = viewModel::removeFromCart,
                                onPayClick = viewModel::completeCheckout,
                                onResetCheckout = viewModel::resetCheckoutState,
                                onExploreMarketplace = { selectedTabIndex = MainTab.STORE.ordinal },
                                onOpenVault = { selectedTabIndex = MainTab.VAULT.ordinal }
                            )
                            MainTab.VAULT -> VaultAndOrdersScreen(
                                codesState = userRedeemCodesState,
                                ordersState = userOrdersState,
                                supportEmail = platformConfig.supportEmail,
                                isGuest = isGuest,
                                onGuestSignedIn = onGuestSignedIn,
                                onGuestSignInError = { viewModel.showFeedback(it) },
                                onLoadOrderItems = viewModel::loadOrderItems
                            )
                            MainTab.PROFILE -> ProfileScreen(
                                userProfile = userProfile,
                                userEmail = userEmail,
                                userName = userName,
                                unlockedCodeCount = unlockedCount,
                                isAdmin = isAdmin,
                                isGuest = isGuest,
                                supportEmail = platformConfig.supportEmail,
                                onGuestSignedIn = onGuestSignedIn,
                                onGuestSignInError = { viewModel.showFeedback(it) },
                                onExitGuestMode = onExitGuestMode,
                                onOpenAdminDashboard = {
                                    if (isAdmin) isConsoleMode = true
                                },
                                onSignOutClick = onSignOutRequested
                            )
                        }
                    }
                }
            }
        }
    }
}
