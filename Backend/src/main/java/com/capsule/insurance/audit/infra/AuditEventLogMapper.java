// #Demo Setting
package com.capsule.insurance.audit.infra;

import com.capsule.insurance.audit.domain.AuditEventLog;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditEventLogMapper {

    List<AuditEventLog> findByActorUserId(@Param("actorUserId") Long actorUserId);

    List<AuditEventLog> findByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    /**
     * OutboxWorker에서 중복 적재를 방지하기 위해 사용하는 Idempotency Key(request_id 등) 검증 메서드
     */
    int countByOutboxTrackingKey(@Param("requestId") String requestId, @Param("eventType") String eventType, @Param("targetId") Long targetId);

    void saveAuditLog(AuditEventLog eventLog);
}
