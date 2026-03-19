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
}
