package com.example.ui.screens.leads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel

@Composable
fun BulkImportDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var rawCsvText by remember {
        mutableStateOf(
            """
Name,Mobile,Email,Source,Location,BudgetMin,BudgetMax
Sanjay Agarwal,9886123456,sanjay.a@techcorp.com,Meta Ads,Hoskote Road,85,140
Meera Krishnan,9740123456,meera.k@startup.io,99acres,Whitefield,90,160
Vikas Nambiar,9900987654,vikas.n@fintech.com,Google Ads,Sarjapur East,50,95
Ramesh Gupta,9845123456,ramesh.g@enterprise.com,MagicBricks,Hoskote,120,200
            """.trimIndent()
        )
    }

    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var importedCount by remember { mutableStateOf(0) }
    var duplicateCount by remember { mutableStateOf(0) }
    var isSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bulk Lead Import",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "CSV / Excel format supported",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats / Preview
                val lines = rawCsvText.lines().filter { it.isNotBlank() }
                val rowCount = (lines.size - 1).coerceAtLeast(0)

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Rows", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$rowCount", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Valid Format", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$rowCount", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF059669))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Duplicate Rule", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Active", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "CSV Data (Name, Mobile, Email, Source, Location, MinBudget, MaxBudget):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = rawCsvText,
                    onValueChange = { rawCsvText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("bulk_csv_input"),
                    textStyle = MaterialTheme.typography.bodySmall
                )

                if (importStatusMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = if (isSuccess) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isSuccess) Color(0xFF059669) else Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = importStatusMessage!!,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSuccess) Color(0xFF065F46) else Color(0xFF991B1B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            viewModel.bulkImportCsv(rawCsvText) { imported, dups, err ->
                                if (err != null) {
                                    isSuccess = false
                                    importStatusMessage = "Error: $err"
                                } else {
                                    isSuccess = true
                                    importedCount = imported
                                    duplicateCount = dups
                                    importStatusMessage = "Success! $imported leads imported. $dups duplicate records skipped."
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("execute_bulk_import_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Leads")
                    }
                }
            }
        }
    }
}
