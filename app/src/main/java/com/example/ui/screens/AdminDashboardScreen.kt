package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.PaymentMethodType
import com.example.data.model.PlatformConfig
import com.example.data.model.Product
import com.example.data.model.RedeemCode
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.formatInr
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
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

@Composable
fun AdminDashboardScreen(
    productsState: UiState<List<Product>>,
    codesState: UiState<List<RedeemCode>>,
    ordersState: UiState<List<Order>>,
    usersState: UiState<List<UserProfile>>,
    platformConfig: PlatformConfig,
    isActionLoading: Boolean,
    currentUserEmail: String,
    onBackToStore: () -> Unit,
    onSeedSampleCatalog: () -> Unit,
    onSaveProduct: (Product?, String, String, String, String, String, Long, Long, Boolean, Boolean, String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onUploadCodes: (Product, String) -> Unit,
    onDeleteUnsoldCode: (RedeemCode) -> Unit,
    onRefundOrder: (String) -> Unit,
    onToggleUserBlocked: (UserProfile) -> Unit,
    onToggleUserAdmin: (UserProfile) -> Unit,
    onSavePlatformConfig: (PlatformConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackToStore() }

    var adminTab by remember { mutableIntStateOf(0) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val codes = (codesState as? UiState.Success)?.data ?: emptyList()
    val orders = (ordersState as? UiState.Success)?.data ?: emptyList()
    val users = (usersState as? UiState.Success)?.data ?: emptyList()

    if (showCreateDialog || editingProduct != null) {
        ProductEditorDialog(
            existingProduct = editingProduct,
            onDismiss = {
                showCreateDialog = false
                editingProduct = null
            },
            onSave = { title, brand, category, desc, instr, price, mrp, active, featured, accent ->
                onSaveProduct(editingProduct, title, brand, category, desc, instr, price, mrp, active, featured, accent)
                showCreateDialog = false
                editingProduct = null
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = GoldContainer,
            border = BorderStroke(0.5.dp, LuxuryGold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToStore, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to storefront", tint = BrightGold)
                    }
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Store Console", tint = BrightGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REDEEMI Store Console", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                }
                if (isActionLoading) {
                    CircularProgressIndicator(color = BrightGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }

        val tabs = listOf("Overview", "Products", "Code Vault", "Orders & Users", "Rules & Pay")
        ScrollableTabRow(
            selectedTabIndex = adminTab,
            containerColor = MidnightSurface,
            contentColor = BrightGold,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = adminTab == index,
                    onClick = { adminTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (adminTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (adminTab == index) BrightGold else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_$index")
                )
            }
        }

        when (adminTab) {
            0 -> AdminOverviewTab(
                products = products,
                codes = codes,
                orders = orders,
                users = users,
                onSeedSampleCatalog = onSeedSampleCatalog,
                onCreateProduct = { showCreateDialog = true }
            )
            1 -> AdminProductsTab(
                products = products,
                onCreateClick = { showCreateDialog = true },
                onEditClick = { editingProduct = it },
                onDeleteClick = onDeleteProduct
            )
            2 -> AdminCodeVaultTab(
                products = products,
                codes = codes,
                onUploadCodes = onUploadCodes,
                onDeleteUnsoldCode = onDeleteUnsoldCode
            )
            3 -> AdminOrdersAndUsersTab(
                orders = orders,
                users = users,
                currentUserEmail = currentUserEmail,
                onRefundOrder = onRefundOrder,
                onToggleBlocked = onToggleUserBlocked,
                onToggleAdmin = onToggleUserAdmin
            )
            4 -> AdminPlatformRulesTab(
                platformConfig = platformConfig,
                onSaveConfig = onSavePlatformConfig
            )
        }
    }
}

@Composable
private fun AdminOverviewTab(
    products: List<Product>,
    codes: List<RedeemCode>,
    orders: List<Order>,
    users: List<UserProfile>,
    onSeedSampleCatalog: () -> Unit,
    onCreateProduct: () -> Unit
) {
    val totalRevenue = orders.filter { it.status == "paid" }.sumOf { it.totalInr }
    val availableCodes = codes.count { it.status == "available" }
    val soldCodes = codes.count { it.status == "sold" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Platform Performance (INR)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard("Gross Revenue", formatInr(totalRevenue), BrightGold, Modifier.weight(1f))
                AdminKpiCard("Paid Orders", orders.count { it.status == "paid" }.toString(), EmeraldSuccess, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard("Available Codes", availableCodes.toString(), SkyBlue, Modifier.weight(1f))
                AdminKpiCard("Sold Codes", soldCodes.toString(), PurpleGlow, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard("Products", products.size.toString(), TextPrimary, Modifier.weight(1f))
                AdminKpiCard("Users", users.size.toString(), TextPrimary, Modifier.weight(1f))
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Quick Catalog Actions", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        "Populate REDEEMI with 8 official INR digital products and 32 sample redeem codes, or create a custom product.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onSeedSampleCatalog,
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianVoid),
                            modifier = Modifier.weight(1f).testTag("admin_seed_catalog_button")
                        ) {
                            Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Seed Catalog", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onCreateProduct,
                            border = BorderStroke(1.dp, RoyalPurple),
                            modifier = Modifier.weight(1f).testTag("admin_quick_add_product")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = PurpleGlow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Product", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(title: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        color = CardSlate,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                color = accent
            )
        }
    }
}

@Composable
private fun AdminProductsTab(
    products: List<Product>,
    onCreateClick: () -> Unit,
    onEditClick: (Product) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianVoid),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_create_product_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Product", fontWeight = FontWeight.Bold)
            }
        }
        items(products, key = { it.id }) { prod ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${prod.brand} • ${prod.category}", style = MaterialTheme.typography.labelSmall, color = PurpleGlow)
                        Text(prod.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            "${formatInr(prod.priceInr)} (MRP ${formatInr(prod.mrpInr)}) • Stock: ${prod.stockCount} • ${if (prod.active) "Active" else "Hidden"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrightGold
                        )
                    }
                    IconButton(onClick = { onEditClick(prod) }, modifier = Modifier.testTag("admin_edit_prod_${prod.id}")) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit ${prod.title}", tint = SkyBlue)
                    }
                    IconButton(onClick = { onDeleteClick(prod.id) }, modifier = Modifier.testTag("admin_delete_prod_${prod.id}")) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete ${prod.title}", tint = CrimsonError)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCodeVaultTab(
    products: List<Product>,
    codes: List<RedeemCode>,
    onUploadCodes: (Product, String) -> Unit,
    onDeleteUnsoldCode: (RedeemCode) -> Unit
) {
    var selectedProduct by remember(products) { mutableStateOf(products.firstOrNull()) }
    var productDropdownOpen by remember { mutableStateOf(false) }
    var codesText by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }

    val filteredCodes = codes.filter {
        statusFilter == "all" || it.status == statusFilter
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Upload Digital Redeem Codes", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    if (products.isEmpty()) {
                        Text("Create a product first before uploading codes.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    } else {
                        Box {
                            OutlinedButton(
                                onClick = { productDropdownOpen = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedProduct?.title ?: "Select Product", color = BrightGold)
                            }
                            DropdownMenu(expanded = productDropdownOpen, onDismissRequest = { productDropdownOpen = false }) {
                                products.forEach { prod ->
                                    DropdownMenuItem(
                                        text = { Text("${prod.title} (${formatInr(prod.priceInr)})") },
                                        onClick = {
                                            selectedProduct = prod
                                            productDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = codesText,
                            onValueChange = { codesText = it },
                            label = { Text("Paste codes (one per line)") },
                            placeholder = { Text("RDM-GPLAY-1111-2222\nRDM-GPLAY-3333-4444") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth().testTag("admin_upload_codes_input")
                        )
                        Button(
                            onClick = {
                                selectedProduct?.let { prod ->
                                    onUploadCodes(prod, codesText)
                                    codesText = ""
                                }
                            },
                            enabled = selectedProduct != null && codesText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianVoid),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_upload_codes_button")
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload to Secure Vault", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All (${codes.size})", "available" to "Unsold", "sold" to "Sold").forEach { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalPurple)
                    )
                }
            }
        }

        items(filteredCodes, key = { it.id }) { code ->
            Surface(
                color = CardSlate,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(code.productTitle, style = MaterialTheme.typography.labelSmall, color = PurpleGlow)
                        Text(
                            code.code,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                            color = BrightGold
                        )
                        Text(
                            if (code.status == "available") "UNSOLD • In Vault" else "SOLD • Order ${code.orderId ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (code.status == "available") EmeraldSuccess else TextMuted
                        )
                    }
                    if (code.status == "available") {
                        IconButton(onClick = { onDeleteUnsoldCode(code) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete unsold code", tint = CrimsonError)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOrdersAndUsersTab(
    orders: List<Order>,
    users: List<UserProfile>,
    currentUserEmail: String,
    onRefundOrder: (String) -> Unit,
    onToggleBlocked: (UserProfile) -> Unit,
    onToggleAdmin: (UserProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Customer Orders (${orders.size})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }
        items(orders, key = { it.id }) { order ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${order.customerName} (${order.customerEmail})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(order.productSummary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            "${formatInr(order.totalInr)} • ${order.paymentMethod} • ${order.status.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (order.status == "paid") EmeraldSuccess else CrimsonError
                        )
                    }
                    if (order.status == "paid") {
                        OutlinedButton(onClick = { onRefundOrder(order.id) }, border = BorderStroke(1.dp, CrimsonError)) {
                            Text("Refund", color = CrimsonError)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = SubtleBorder)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Registered Users (${users.size})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        items(users, key = { it.userId }) { user ->
            val isBootstrap = user.email.equals("spfoundation59@gmail.com", ignoreCase = true) ||
                user.email.equals(currentUserEmail, ignoreCase = true)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(
                            color = if (user.role == "admin") GoldContainer else RoyalPurple.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (user.role == "admin") "MANAGER" else "CUSTOMER",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user.role == "admin") BrightGold else SkyBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        "Orders: ${user.totalOrders} • Spent: ${formatInr(user.totalSpentInr)} • ${if (user.blocked) "SUSPENDED" else "Active"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (user.blocked) CrimsonError else TextMuted
                    )
                    if (!isBootstrap) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onToggleAdmin(user) },
                                border = BorderStroke(1.dp, LuxuryGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (user.role == "admin") "Revoke Manager" else "Make Manager", color = BrightGold)
                            }
                            OutlinedButton(
                                onClick = { onToggleBlocked(user) },
                                border = BorderStroke(1.dp, if (user.blocked) EmeraldSuccess else CrimsonError),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Block, contentDescription = null, tint = if (user.blocked) EmeraldSuccess else CrimsonError, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (user.blocked) "Unblock" else "Block", color = if (user.blocked) EmeraldSuccess else CrimsonError)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPlatformRulesTab(
    platformConfig: PlatformConfig,
    onSaveConfig: (PlatformConfig) -> Unit
) {
    var feeText by remember(platformConfig) { mutableStateOf(platformConfig.platformFeePercent.toString()) }
    var gstText by remember(platformConfig) { mutableStateOf(platformConfig.gstPercent.toString()) }
    var maxQtyText by remember(platformConfig) { mutableStateOf(platformConfig.maxCartQuantityPerItem.toString()) }
    var banner by remember(platformConfig) { mutableStateOf(platformConfig.announcementBanner) }
    var supportEmail by remember(platformConfig) { mutableStateOf(platformConfig.supportEmail) }
    var maintenance by remember(platformConfig) { mutableStateOf(platformConfig.maintenanceMode) }
    var selectedMethods by remember(platformConfig) { mutableStateOf(platformConfig.enabledPaymentMethods.toSet()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("REDEEMI Rules & Payment Gateway", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enabled Payment Gateways", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                    PaymentMethodType.entries.forEach { method ->
                        val checked = method.code in selectedMethods
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedMethods = if (isChecked) {
                                        selectedMethods + method.code
                                    } else if (selectedMethods.size > 1) {
                                        selectedMethods - method.code
                                    } else {
                                        selectedMethods
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = LuxuryGold, checkmarkColor = ObsidianVoid)
                            )
                            Column {
                                Text(method.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(method.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pricing & Storefront Rules", style = MaterialTheme.typography.titleMedium, color = PurpleGlow)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = feeText,
                            onValueChange = { feeText = it.filter(Char::isDigit) },
                            label = { Text("Platform Fee %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gstText,
                            onValueChange = { gstText = it.filter(Char::isDigit) },
                            label = { Text("GST %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = maxQtyText,
                        onValueChange = { maxQtyText = it.filter(Char::isDigit) },
                        label = { Text("Max Quantity Per Item (1–20)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = banner,
                        onValueChange = { banner = it },
                        label = { Text("Storefront Announcement Banner") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supportEmail,
                        onValueChange = { supportEmail = it },
                        label = { Text("Support Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Maintenance Mode", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Pauses customer checkout temporarily", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Switch(checked = maintenance, onCheckedChange = { maintenance = it })
                    }
                    Button(
                        onClick = {
                            onSaveConfig(
                                platformConfig.copy(
                                    platformFeePercent = (feeText.toLongOrNull() ?: 2L).coerceIn(0L, 20L),
                                    gstPercent = (gstText.toLongOrNull() ?: 0L).coerceIn(0L, 28L),
                                    maxCartQuantityPerItem = (maxQtyText.toLongOrNull() ?: 5L).coerceIn(1L, 20L),
                                    announcementBanner = banner,
                                    supportEmail = supportEmail,
                                    maintenanceMode = maintenance,
                                    enabledPaymentMethods = selectedMethods.toList()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianVoid),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_save_rules_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save REDEEMI Platform Rules", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
