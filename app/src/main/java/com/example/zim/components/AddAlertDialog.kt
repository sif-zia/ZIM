package com.example.zim.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.zim.helperclasses.AlertType
import com.example.zim.helperclasses.toAlertType

@Composable
fun AddAlertDialog(
    addAlertType: AlertType?,
    description: String,
    onAddAlertTypeChange: (AlertType) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val horizontalPadding = 20.dp
    val verticalPadding = 12.dp
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        val options = AlertType.entries.map { it.toName() }

        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f),
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),

            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                verticalArrangement = Arrangement.spacedBy(verticalPadding)
            ) {
                Text(
                    "Add Alert",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                val selectedText = addAlertType?.toName() ?: ""
                DropDownInput(
                    label = "Alert Type",
                    options = options,
                    selectedText = selectedText
                ) { selected ->
                    onAddAlertTypeChange(selected.toAlertType())
                }


                if (addAlertType == AlertType.CUSTOM) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(33))
                            .background(MaterialTheme.colorScheme.secondary),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = description,
                            onValueChange = onDescriptionChange,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.secondary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.secondary,
                                focusedTextColor = MaterialTheme.colorScheme.primary,
                                unfocusedTextColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(0.66f),
                                focusedPlaceholderColor = MaterialTheme.colorScheme.primary,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.primary.copy(
                                    0.66f
                                )
                            ),
                            label = { Text("Description") },
                            singleLine = true
                        )
                    }

                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(50)
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Cancel")
                    }
                    val isDisabled = (addAlertType == null) || (addAlertType == AlertType.CUSTOM && description.isEmpty())
                    Button(onClick = onConfirm, enabled = !isDisabled,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.secondary.copy(0.5f)
                        )) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}