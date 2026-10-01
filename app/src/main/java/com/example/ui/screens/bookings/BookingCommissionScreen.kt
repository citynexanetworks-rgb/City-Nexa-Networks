package com.example.ui.screens.bookings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.CommissionStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.NexaGoldDark
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingCommissionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val bookings by viewModel.visibleBookings.collectAsState()
    val leads by viewModel.visibleLeads.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showCreateBookingDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateBookingDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Deal") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_booking_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bookings & Commissions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real estate transactions, payments & commission disbursements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Summary metrics
            val totalDealValue = bookings.sumOf { it.totalDealValueLakhs }
            val totalCommission = bookings.sumOf { it.commissionAmount }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Closed Deal Value", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.getDefault(), "%.2f", totalDealValue)} Lakhs", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Approved Commission", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.getDefault(), "%.2f", totalCommission)} Lakhs", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NexaGoldDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bookings List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookings, key = { it.id }) { booking ->
                    BookingCard(
                        booking = booking,
                        onUpdateCommissionStatus = { newComm ->
                            viewModel.updateBooking(booking.copy(commissionStatus = newComm))
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (showCreateBookingDialog) {
        var selectedLead by remember { mutableStateOf(leads.firstOrNull()) }
        var selectedProj by remember { mutableStateOf(projects.firstOrNull()) }
        var unitNumber by remember { mutableStateOf("T1-1002") }
        var config by remember { mutableStateOf("3 BHK (1450 sq.ft)") }
        var dealValue by remember { mutableStateOf("115.0") }
        var bookingAmount by remember { mutableStateOf("5.0") }
        var commPercent by remember { mutableStateOf("2.0") }

        AlertDialog(
            onDismissRequest = { showCreateBookingDialog = false },
            title = { Text("Log Real Estate Booking") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Lead: ${selectedLead?.name ?: "None"}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Select Project: ${selectedProj?.name ?: "None"}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                    OutlinedTextField(value = unitNumber, onValueChange = { unitNumber = it }, label = { Text("Unit Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = config, onValueChange = { config = it }, label = { Text("Configuration & Size") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = dealValue, onValueChange = { dealValue = it }, label = { Text("Deal Value (₹L)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = bookingAmount, onValueChange = { bookingAmount = it }, label = { Text("Booking Token (₹L)") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = commPercent, onValueChange = { commPercent = it }, label = { Text("Commission (%)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dVal = dealValue.toDoubleOrNull() ?: 100.0
                        val cPct = commPercent.toDoubleOrNull() ?: 2.0
                        val commAmt = (dVal * cPct) / 100.0
                        val user = currentUser ?: return@Button

                        viewModel.createBooking(
                            Booking(
                                id = "BK-${(100..999).random()}",
                                leadId = selectedLead?.id ?: "NX-1001",
                                customerName = selectedLead?.name ?: "Customer",
                                customerPhone = selectedLead?.mobile ?: "9880000000",
                                projectId = selectedProj?.id ?: "NX-PRJ-01",
                                projectName = selectedProj?.name ?: "Pinnacle Towers",
                                unitNumber = unitNumber,
                                configuration = config,
                                salespersonId = user.id,
                                salespersonName = user.name,
                                bookingDate = "2026-09-29",
                                totalDealValueLakhs = dVal,
                                bookingAmountPaidLakhs = bookingAmount.toDoubleOrNull() ?: 5.0,
                                commissionPercent = cPct,
                                commissionAmount = commAmt
                            )
                        )
                        showCreateBookingDialog = false
                    }
                ) {
                    Text("Record Deal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBookingDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun BookingCard(
    booking: Booking,
    onUpdateCommissionStatus: (CommissionStatus) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF059669).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = booking.bookingStatus.label,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "#${booking.id} • ${booking.bookingDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${booking.customerName} — Unit ${booking.unitNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${booking.projectName} (${booking.configuration})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Deal Value", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${booking.totalDealValueLakhs} Lakhs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column {
                    Text("Token Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${booking.bookingAmountPaidLakhs} Lakhs", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF059669))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Commission (${booking.commissionPercent}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(Locale.getDefault(), "%.2f", booking.commissionAmount)} Lakhs", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NexaGoldDark)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Commission: ${booking.commissionStatus.label}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (booking.commissionStatus == CommissionStatus.APPROVED || booking.commissionStatus == CommissionStatus.RECEIVED) Color(0xFF059669) else NexaGoldDark
                )

                if (booking.commissionStatus != CommissionStatus.RECEIVED) {
                    TextButton(
                        onClick = {
                            val next = when (booking.commissionStatus) {
                                CommissionStatus.PENDING -> CommissionStatus.INVOICE_RAISED
                                CommissionStatus.INVOICE_RAISED -> CommissionStatus.APPROVED
                                CommissionStatus.APPROVED -> CommissionStatus.RECEIVED
                                CommissionStatus.RECEIVED -> CommissionStatus.RECEIVED
                            }
                            onUpdateCommissionStatus(next)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Advance Commission State →", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
