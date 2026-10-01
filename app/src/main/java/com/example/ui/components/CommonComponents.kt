package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun LeadPriorityBadge(priority: LeadPriority, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (priority) {
        LeadPriority.HOT -> PriorityHot.copy(alpha = 0.15f) to PriorityHot
        LeadPriority.WARM -> PriorityWarm.copy(alpha = 0.15f) to PriorityWarm
        LeadPriority.COLD -> PriorityCold.copy(alpha = 0.15f) to PriorityCold
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = priority.name,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LeadStatusBadge(status: LeadStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        LeadStatus.NEW_LEAD -> StatusNew.copy(alpha = 0.12f) to StatusNew
        LeadStatus.ASSIGNED -> StatusAssigned.copy(alpha = 0.12f) to StatusAssigned
        LeadStatus.CONTACTED -> StatusContacted.copy(alpha = 0.12f) to StatusContacted
        LeadStatus.QUALIFIED -> StatusQualified.copy(alpha = 0.12f) to StatusQualified
        LeadStatus.PROJECT_SHARED -> StatusProjectShared.copy(alpha = 0.12f) to StatusProjectShared
        LeadStatus.SITE_VISIT_PLANNED, LeadStatus.SITE_VISIT_DONE -> StatusSiteVisit.copy(alpha = 0.12f) to StatusSiteVisit
        LeadStatus.NEGOTIATION -> StatusNegotiation.copy(alpha = 0.12f) to StatusNegotiation
        LeadStatus.BOOKING -> StatusBooking.copy(alpha = 0.12f) to StatusBooking
        LeadStatus.CLOSED_LOST -> StatusClosedLost.copy(alpha = 0.12f) to StatusClosedLost
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun UserRoleBadge(role: UserRole, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (role) {
        UserRole.SUPER_ADMIN -> NexaGoldDark.copy(alpha = 0.15f) to NexaGoldDark
        UserRole.ADMIN -> NexaNavyMedium.copy(alpha = 0.15f) to NexaNavyMedium
        UserRole.SALES_MANAGER -> NexaTeal.copy(alpha = 0.15f) to NexaTeal
        UserRole.SALES_EXECUTIVE -> Color(0xFF2563EB).copy(alpha = 0.15f) to Color(0xFF2563EB)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = role.displayName,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tintColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.testTag("section_action_${title.lowercase().replace(" ", "_")}")
            ) {
                Text(
                    text = actionLabel,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun TimelineItem(activity: LeadActivity) {
    val dateStr = androidx.compose.runtime.remember(activity.timestamp) {
        java.text.SimpleDateFormat("dd MMM yyyy • hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(activity.timestamp))
    }

    val icon = when (activity.type) {
        ActivityType.CALL -> Icons.Default.Call
        ActivityType.REMARK_ADDED -> Icons.Default.Comment
        ActivityType.STATUS_CHANGED -> Icons.Default.Transform
        ActivityType.PROJECT_SHARED -> Icons.Default.Share
        ActivityType.FOLLOW_UP_CREATED -> Icons.Default.Event
        ActivityType.SITE_VISIT_PLANNED, ActivityType.SITE_VISIT_UPDATED -> Icons.Default.Tour
        ActivityType.BOOKING_CREATED -> Icons.Default.CheckCircle
        ActivityType.EMAIL_SENT -> Icons.Default.Email
        ActivityType.WHATSAPP_SENT -> Icons.Default.Send
        ActivityType.NOTE -> Icons.Default.Description
    }

    val iconTint = when (activity.type) {
        ActivityType.CALL -> StatusContacted
        ActivityType.BOOKING_CREATED -> StatusBooking
        ActivityType.SITE_VISIT_PLANNED, ActivityType.SITE_VISIT_UPDATED -> StatusSiteVisit
        ActivityType.PROJECT_SHARED, ActivityType.WHATSAPP_SENT -> NexaTeal
        ActivityType.STATUS_CHANGED -> StatusQualified
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
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
                    text = activity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "By ${activity.userName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

