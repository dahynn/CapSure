// #Demo Setting
package com.capsule.insurance.compliance.infra;

import com.capsule.insurance.compliance.domain.DigitalSeal;
import com.capsule.insurance.compliance.domain.UserLegalEvent;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ComplianceMapper {

    DigitalSeal findRepresentativeSealByUserId(@Param("userId") Long userId);

    List<UserLegalEvent> findLegalEventsByUserId(@Param("userId") Long userId);
}
