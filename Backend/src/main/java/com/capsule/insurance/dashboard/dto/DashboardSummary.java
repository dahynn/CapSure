// #Demo Setting
package com.capsule.insurance.dashboard.dto;

public record DashboardSummary(
        long activeSubscriptions,
        long expiringSoon,
        long unreadAudits
) {
}
