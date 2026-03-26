package com.capsule.insurance.audit.infra;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OutboxMapper {

    void saveOutbox(@Param("eventId") String eventId, @Param("payload") String payload);

    void deleteOutbox(@Param("id") Long id);

    void updateStatus(@Param("id") Long id, @Param("status") String status);

    void saveDlq(@Param("eventId") String eventId, @Param("payload") String payload, @Param("errorReason") String errorReason);

    List<Map<String, Object>> findPendingOutboxRecords();
}
