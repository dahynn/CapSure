-- Retention cleanup scans events by occurrence time. The job is disabled until an environment
-- explicitly enables it with a reviewed retention period.
CREATE INDEX ix_audit_event_log_occurred_at
    ON public.audit_event_log (occurred_at);
