// #Demo Setting
package com.capsule.insurance.dashboard.application;

import com.capsule.insurance.dashboard.dto.DashboardSummary;
import com.capsule.insurance.dashboard.dto.HomeDashboardResponse;
import com.capsule.insurance.subscription.infra.SubscriptionMapper;
import com.capsule.insurance.subscription.infra.projection.RecentSubscriptionHomeProjection;
import com.capsule.insurance.subscription.infra.projection.RenewalSoonInsuranceProjection;
import com.capsule.insurance.subscription.infra.projection.SnapshotCategoryCodeProjection;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            .withZone(ZoneId.systemDefault());
    private static final String DEFAULT_CAPSULE_NAME = "나만의 캡슐";

    private final SubscriptionMapper subscriptionMapper;

    @Autowired
    public DashboardService(SubscriptionMapper subscriptionMapper) {
        this.subscriptionMapper = subscriptionMapper;
    }

    public DashboardSummary getSummary() {
        // TODO: 실제 대시보드 집계용 조회 로직을 구현해야 합니다.
        return new DashboardSummary(2, 1, 0);
    }

    public HomeDashboardResponse getHomeDashboard(Long userId) {
        List<RecentSubscriptionHomeProjection> recentSubscriptions =
                subscriptionMapper.findRecentSubscriptionsForHome(userId);
        List<Long> capsuleSnapshotIds = recentSubscriptions.stream()
                .map(RecentSubscriptionHomeProjection::capsuleSnapshotId)
                .toList();

        Map<Long, List<String>> categoriesBySnapshotId = capsuleSnapshotIds.isEmpty()
                ? Map.of()
                : subscriptionMapper.findCategoryCodesBySnapshotIds(capsuleSnapshotIds).stream()
                        .collect(Collectors.groupingBy(
                                SnapshotCategoryCodeProjection::capsuleSnapshotId,
                                Collectors.mapping(
                                        projection -> toCategoryLabel(projection.coverageCategoryCode()),
                                        Collectors.toList())));

        List<HomeDashboardResponse.SubscribedCapsuleCard> subscribedCapsules = recentSubscriptions.stream()
                .map(subscription -> new HomeDashboardResponse.SubscribedCapsuleCard(
                        subscription.capsuleSnapshotId(),
                        subscription.subscriptionId(),
                        resolveCapsuleName(subscription.capsuleName()),
                        formatDate(subscription.createdAt()),
                        formatDate(subscription.nextBillingAt()),
                        subscription.expectedNextAmount(),
                        categoriesBySnapshotId.getOrDefault(subscription.capsuleSnapshotId(), List.of())))
                .toList();

        List<HomeDashboardResponse.ActiveInsuranceCard> activeInsurances =
                subscriptionMapper.findRenewalSoonInsurancesForHome(userId).stream()
                        .map(this::toActiveInsuranceCard)
                        .toList();

        return new HomeDashboardResponse(subscribedCapsules, activeInsurances);
    }

    private HomeDashboardResponse.ActiveInsuranceCard toActiveInsuranceCard(
            RenewalSoonInsuranceProjection projection
    ) {
        long daysUntilRenewal = calculateDaysUntilRenewal(projection.nextBillingAt());
        return new HomeDashboardResponse.ActiveInsuranceCard(
                projection.subscriptionId(),
                projection.productSourceId(),
                projection.productName(),
                projection.companyName(),
                toCategoryLabel(projection.coverageCategoryCode()),
                projection.monthlyPrice(),
                projection.billingAnchorDay(),
                formatDate(projection.nextBillingAt()),
                daysUntilRenewal
        );
    }

    private long calculateDaysUntilRenewal(Instant nextBillingAt) {
        if (nextBillingAt == null) {
            return Long.MAX_VALUE;
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate billingDate = LocalDate.ofInstant(nextBillingAt, ZoneId.systemDefault());
        long diff = ChronoUnit.DAYS.between(today, billingDate);
        return Math.max(diff, 0);
    }

    private String formatDate(Instant instant) {
        return instant == null ? null : DATE_FORMATTER.format(instant);
    }

    private String toCategoryLabel(String coverageCategoryCode) {
        if (coverageCategoryCode == null) {
            return "기타";
        }
        return switch (coverageCategoryCode) {
            case "DEATH" -> "사망";
            case "CANCER" -> "암";
            case "BRAIN_HEART" -> "뇌/심장";
            case "ACTUAL_LOSS" -> "실손";
            case "SURGERY" -> "수술";
            case "ACCIDENT" -> "상해";
            case "LIABILITY" -> "배상";
            default -> "기타";
        };
    }

    private String resolveCapsuleName(String capsuleName) {
        if (capsuleName == null || capsuleName.isBlank()) {
            return DEFAULT_CAPSULE_NAME;
        }
        return capsuleName.trim();
    }
}
