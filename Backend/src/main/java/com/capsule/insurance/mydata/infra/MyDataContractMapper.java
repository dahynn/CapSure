// #Demo Setting
package com.capsule.insurance.mydata.infra;

import com.capsule.insurance.mydata.domain.MyDataContract;
import com.capsule.insurance.mydata.domain.MyDataContractCoverage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MyDataContractMapper {

    List<MyDataContract> findContractsByUserId(@Param("userId") Long userId);

    List<MyDataContractCoverage> findCoveragesByContractId(@Param("myDataContractId") Long myDataContractId);
}
