package com.capsule.insurance.dashboard.infra;

import com.capsule.insurance.dashboard.dto.DashboardSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDashboardRepository {
    private final JdbcTemplate jdbc;
    public JdbcDashboardRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public DashboardSummary summary(Long userId) {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT count(*) FROM subscription WHERE user_id = ? AND subscription_status = 'ACTIVE') active,
                    (SELECT count(*) FROM subscription WHERE user_id = ? AND subscription_status = 'ACTIVE'
                        AND next_billing_at >= now() AND next_billing_at < now() + interval '7 days') expiring,
                    (SELECT count(*) FROM audit_event_log WHERE actor_user_id = ? AND audit_event_id >
                        COALESCE((SELECT last_audit_event_id FROM dashboard_audit_read_cursor WHERE user_id = ?), 0)) unread
                """, (rs, row) -> new DashboardSummary(rs.getLong("active"), rs.getLong("expiring"), rs.getLong("unread")),
                userId, userId, userId, userId);
    }

    public void markAuditsRead(Long userId) {
        jdbc.update("""
                INSERT INTO dashboard_audit_read_cursor(user_id, last_audit_event_id)
                SELECT ?, COALESCE(MAX(audit_event_id), 0) FROM audit_event_log WHERE actor_user_id = ?
                ON CONFLICT (user_id) DO UPDATE SET last_audit_event_id =
                    GREATEST(dashboard_audit_read_cursor.last_audit_event_id, EXCLUDED.last_audit_event_id)
                """, userId, userId);
    }
}
