package com.panelcut3d.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.ProjectDesign
import com.panelcut3d.app.data.QuoteDetails
import com.panelcut3d.app.ui.components.NumberStepper
import com.panelcut3d.app.ui.components.PcBottomSheet
import com.panelcut3d.app.ui.components.PcButton
import com.panelcut3d.app.ui.components.SheetTitle

@Composable
fun QuoteSheet(
    projectId: Long,
    projectName: String,
    design: ProjectDesign,
    currency: String,
    total: Double,
    onDismiss: () -> Unit,
    onSave: (QuoteDetails) -> Unit,
) {
    var customer by remember { mutableStateOf(design.quote.customer) }
    var number by remember {
        mutableStateOf(design.quote.number.ifBlank { "Q-${projectId.toString().padStart(4, '0')}" })
    }
    var validDays by remember { mutableStateOf(design.quote.validDays) }
    var discount by remember { mutableStateOf(design.quote.discountPercent) }
    var deposit by remember { mutableStateOf(design.quote.depositPercent) }
    var notes by remember { mutableStateOf(design.quote.notes) }
    val afterDiscount = total - total * discount / 100.0

    fun saveQuote() {
        onSave(
            QuoteDetails(
                number = number.trim(),
                customer = customer.trim(),
                validDays = validDays,
                discountPercent = discount,
                depositPercent = deposit,
                notes = notes.trim(),
            ),
        )
    }

    PcBottomSheet(
        onDismiss = onDismiss,
        footer = {
            PcButton(
                text = "Save quote",
                onClick = ::saveQuote,
                modifier = Modifier.fillMaxWidth(),
                enabled = customer.isNotBlank(),
            )
        },
    ) {
        SheetTitle(
            title = "Quote builder",
            subtitle = "Prepare a customer-ready quotation for $projectName.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedTextField(
                value = customer,
                onValueChange = { customer = it },
                label = { Text("Customer") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("Quote no.") },
                singleLine = true,
                modifier = Modifier.width(130.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            NumberStepper(
                label = "Valid",
                value = validDays.toFloat(),
                suffix = "days",
                onChange = { validDays = it.toInt().coerceIn(1, 120) },
                modifier = Modifier.weight(1f),
                min = 1f,
                max = 120f,
                step = 1f,
            )
            NumberStepper(
                label = "Discount",
                value = discount,
                suffix = "%",
                onChange = { discount = it.coerceIn(0f, 100f) },
                modifier = Modifier.weight(1f),
                min = 0f,
                max = 100f,
                step = 1f,
            )
            NumberStepper(
                label = "Deposit",
                value = deposit,
                suffix = "%",
                onChange = { deposit = it.coerceIn(0f, 100f) },
                modifier = Modifier.weight(1f),
                min = 0f,
                max = 100f,
                step = 5f,
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Terms / notes") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Quote total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Fmt.money(afterDiscount, currency), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            Column {
                Text("Deposit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Fmt.money(afterDiscount * deposit / 100.0, currency), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
