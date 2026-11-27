package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentStyle
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSlate
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductEditorDialog(
    existingProduct: Product?,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var title by remember { mutableStateOf(existingProduct?.title ?: "") }
    var brand by remember { mutableStateOf(existingProduct?.brand ?: "") }
    var category by remember { mutableStateOf(existingProduct?.category ?: ProductCategory.GIFT_CARDS.label) }
    var description by remember { mutableStateOf(existingProduct?.description ?: "") }
    var instructions by remember { mutableStateOf(existingProduct?.redemptionInstructions ?: "") }
    var priceText by remember { mutableStateOf(existingProduct?.priceInr?.toString() ?: "") }
    var mrpText by remember { mutableStateOf(existingProduct?.mrpInr?.toString() ?: "") }
    var active by remember { mutableStateOf(existingProduct?.active ?: true) }
    var featured by remember { mutableStateOf(existingProduct?.featured ?: false) }
    var accentStyle by remember { mutableStateOf(existingProduct?.accentStyle ?: "purple") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        titleContentColor = TextPrimary,
        title = {
            Text(
                text = if (existingProduct == null) "Create Digital Product" else "Edit Product & INR Price",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Product Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_prod_title_input")
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand (e.g. Steam, Google Play)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_prod_brand_input")
                )
                Text("Category", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProductCategory.creatableCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat.label,
                            onClick = { category = cat.label },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CardSlate,
                                selectedContainerColor = RoyalPurple
                            )
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("admin_prod_price_input")
                    )
                    OutlinedTextField(
                        value = mrpText,
                        onValueChange = { mrpText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("MRP (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("admin_prod_mrp_input")
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Redemption Instructions") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Card Accent", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AccentStyle.entries.forEach { style ->
                        FilterChip(
                            selected = accentStyle == style.raw,
                            onClick = { accentStyle = style.raw },
                            label = { Text(style.raw.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active in Storefront", color = TextPrimary)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Featured Drop", color = BrightGold)
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toLongOrNull() ?: 0L
                    val mrp = mrpText.toLongOrNull() ?: price
                    when {
                        title.trim().length < 2 -> errorMsg = "Title must be at least 2 characters."
                        brand.trim().length < 2 -> errorMsg = "Brand must be at least 2 characters."
                        price < 1L -> errorMsg = "Price must be at least ₹1."
                        mrp < price -> errorMsg = "MRP must be >= selling price."
                        description.trim().length < 5 -> errorMsg = "Description must be at least 5 characters."
                        instructions.trim().length < 5 -> errorMsg = "Redemption instructions must be at least 5 characters."
                        else -> onSave(title, brand, category, description, instructions, price, mrp, active, featured, accentStyle)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianVoid),
                modifier = Modifier.testTag("admin_save_product_button")
            ) {
                Text("Save Product", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
