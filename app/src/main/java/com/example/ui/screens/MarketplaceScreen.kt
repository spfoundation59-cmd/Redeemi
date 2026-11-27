package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PlatformConfig
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.UiState
import com.example.data.model.discountPercent
import com.example.data.model.formatInr
import com.example.ui.components.InrPriceRow
import com.example.ui.components.RedeemiFooter
import com.example.ui.components.accentGradient
import com.example.ui.components.categoryIcon
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
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
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    productsState: UiState<List<Product>>,
    platformConfig: PlatformConfig,
    searchQuery: String,
    selectedCategory: ProductCategory,
    isAdmin: Boolean,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (ProductCategory) -> Unit,
    onAddToCart: (Product, Long) -> Unit,
    onBuyNow: (Product, Long) -> Unit,
    onOpenAdminSeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    if (selectedProduct != null) {
        BackHandler { selectedProduct = null }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ProductDetailBottomSheet(
            product = selectedProduct!!,
            maxQuantity = platformConfig.maxCartQuantityPerItem,
            sheetState = sheetState,
            onDismiss = { selectedProduct = null },
            onAddToCart = { qty ->
                onAddToCart(selectedProduct!!, qty)
                selectedProduct = null
            },
            onBuyNow = { qty ->
                onBuyNow(selectedProduct!!, qty)
                selectedProduct = null
            }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 680.dp) 2 else 1

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (platformConfig.maintenanceMode) {
                item {
                    Surface(
                        color = CrimsonContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CrimsonError),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = "Maintenance notice", tint = CrimsonError)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "REDEEMI Checkout is temporarily paused for scheduled maintenance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            if (platformConfig.announcementBanner.isNotBlank()) {
                item {
                    Surface(
                        color = GoldContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, LuxuryGold.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = "Announcement", tint = BrightGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = platformConfig.announcementBanner,
                                style = MaterialTheme.typography.labelMedium,
                                color = BrightGold
                            )
                        }
                    }
                }
            }

            // Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "REDEEMI Hero Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ObsidianVoid.copy(alpha = 0.92f),
                                        ObsidianVoid.copy(alpha = 0.55f),
                                        ObsidianVoid.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = RoyalPurple.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, PurpleGlow.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "BUY. REDEEM. ENJOY.",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrightGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Digital Redeem Marketplace",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Verified INR codes • Instant vault delivery",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search Google Play, Steam, Valorant, Netflix...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search products", tint = PurpleGlow) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSlate,
                        unfocusedContainerColor = CardSlate,
                        focusedBorderColor = RoyalPurple,
                        unfocusedBorderColor = SubtleBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("marketplace_search_input")
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ProductCategory.entries) { category ->
                        val selected = category == selectedCategory
                        FilterChip(
                            selected = selected,
                            onClick = { onCategorySelected(category) },
                            label = {
                                Text(
                                    text = category.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CardSlate,
                                labelColor = TextSecondary,
                                selectedContainerColor = RoyalPurple,
                                selectedLabelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = SubtleBorder,
                                selectedBorderColor = LuxuryGold
                            ),
                            modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
                        )
                    }
                }
            }

            when (productsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = LuxuryGold)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Surface(
                            color = CrimsonContainer,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CrimsonError),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = productsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val activeProducts = productsState.data.filter { it.active }
                    val featured = activeProducts.filter { it.featured }
                    val filtered = activeProducts.filter { product ->
                        val matchesCategory = selectedCategory == ProductCategory.ALL || product.category == selectedCategory.label
                        val q = searchQuery.trim()
                        val matchesSearch = q.isEmpty() ||
                            product.title.contains(q, ignoreCase = true) ||
                            product.brand.contains(q, ignoreCase = true) ||
                            product.category.contains(q, ignoreCase = true)
                        matchesCategory && matchesSearch
                    }

                    if (activeProducts.isEmpty()) {
                        item {
                            EmptyCatalogCard(isAdmin = isAdmin, onOpenAdminSeed = onOpenAdminSeed)
                        }
                    } else {
                        if (featured.isNotEmpty() && searchQuery.isBlank() && selectedCategory == ProductCategory.ALL) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Featured Drops",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(featured, key = { it.id }) { prod ->
                                            FeaturedProductCard(
                                                product = prod,
                                                onClick = { selectedProduct = prod }
                                            )
                                        }
                                    }
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
                                    text = if (selectedCategory == ProductCategory.ALL) "All Digital Products" else selectedCategory.label,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${filtered.size} items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        if (filtered.isEmpty()) {
                            item {
                                Surface(
                                    color = CardSlate,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, SubtleBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("No matching digital vouchers found", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Try clearing your search or selecting All categories.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }
                        } else {
                            val rows = filtered.chunked(columns)
                            items(rows) { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { prod ->
                                        MarketplaceProductCard(
                                            product = prod,
                                            onOpenDetails = { selectedProduct = prod },
                                            onQuickAdd = { onAddToCart(prod, 1L) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size < columns) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                RedeemiFooter(supportEmail = platformConfig.supportEmail)
            }
        }
    }
}

@Composable
private fun FeaturedProductCard(
    product: Product,
    onClick: () -> Unit
) {
    val gradient = accentGradient(product.accentStyle)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.45f)),
        modifier = Modifier
            .width(230.dp)
            .testTag("featured_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(product.category),
                        contentDescription = product.category,
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Surface(
                    color = GoldContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "FEATURED",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrightGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = product.brand,
                style = MaterialTheme.typography.labelSmall,
                color = PurpleGlow
            )
            Text(
                text = product.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            InrPriceRow(
                priceInr = product.priceInr,
                mrpInr = product.mrpInr,
                discountPct = discountPercent(product.mrpInr, product.priceInr)
            )
        }
    }
}

@Composable
private fun MarketplaceProductCard(
    product: Product,
    onOpenDetails: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inStock = product.stockCount > 0L
    val gradient = accentGradient(product.accentStyle)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails)
            .testTag("product_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(product.category),
                        contentDescription = product.category,
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${product.brand} • ${product.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PurpleGlow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = if (inStock) EmeraldContainer else CrimsonContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (inStock) "${product.stockCount} Ready" else "Sold Out",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (inStock) EmeraldSuccess else CrimsonError,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InrPriceRow(
                    priceInr = product.priceInr,
                    mrpInr = product.mrpInr,
                    discountPct = discountPercent(product.mrpInr, product.priceInr),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onQuickAdd,
                    enabled = inStock,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalPurple,
                        contentColor = TextPrimary,
                        disabledContainerColor = ElevatedSlate,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_to_cart_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddShoppingCart,
                        contentDescription = "Add ${product.title} to cart",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (inStock) "Add" else "Out")
                }
            }
        }
    }
}

@Composable
private fun EmptyCatalogCard(
    isAdmin: Boolean,
    onOpenAdminSeed: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_vault),
                contentDescription = "Empty REDEEMI Catalog",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "REDEEMI Storefront Ready",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isAdmin) {
                    "No products published yet. Open the Store Console to add or stock INR gift cards and codes."
                } else {
                    "Fresh digital vouchers are being stocked. Please check back shortly."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (isAdmin) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onOpenAdminSeed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuxuryGold,
                        contentColor = ObsidianVoid
                    ),
                    modifier = Modifier.testTag("empty_state_seed_catalog_button")
                ) {
                    Icon(Icons.Filled.Inventory2, contentDescription = "Seed catalog", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stock Catalog in Console", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailBottomSheet(
    product: Product,
    maxQuantity: Long,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onAddToCart: (Long) -> Unit,
    onBuyNow: (Long) -> Unit
) {
    val maxAllowed = minOf(product.stockCount, maxQuantity).coerceAtLeast(1L)
    var qty by remember { mutableLongStateOf(1L) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${product.brand} • ${product.category}",
                style = MaterialTheme.typography.labelSmall,
                color = PurpleGlow
            )
            Text(
                text = product.title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            InrPriceRow(
                priceInr = product.priceInr,
                mrpInr = product.mrpInr,
                discountPct = discountPercent(product.mrpInr, product.priceInr)
            )
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Surface(
                color = CardSlate,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("How to Redeem", style = MaterialTheme.typography.titleSmall, color = BrightGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(product.redemptionInstructions, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            if (product.stockCount > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quantity (${product.stockCount} available)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (qty > 1L) qty-- },
                            modifier = Modifier.testTag("detail_qty_minus")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", tint = TextPrimary)
                        }
                        Text(
                            text = qty.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = BrightGold,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        IconButton(
                            onClick = { if (qty < maxAllowed) qty++ },
                            modifier = Modifier.testTag("detail_qty_plus")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase quantity", tint = TextPrimary)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAddToCart(qty) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, RoyalPurple),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("detail_add_to_cart_button")
                    ) {
                        Text("Add to Cart", color = TextPrimary)
                    }
                    Button(
                        onClick = { onBuyNow(qty) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuxuryGold,
                            contentColor = ObsidianVoid
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("detail_buy_now_button")
                    ) {
                        Text("Buy ${formatInr(product.priceInr * qty)}", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Surface(
                    color = CrimsonContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Out of stock — check back soon for new REDEEMI vault inventory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonError,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
