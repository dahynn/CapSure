package com.capsule.insurance.auth.infra;

import com.capsule.insurance.auth.domain.UserAccount;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountMapper {

    UserAccount findByUserId(@Param("userId") Long userId);

    UserAccount findByEmail(@Param("email") String email);

    boolean existsByEmail(@Param("email") String email);

    void insert(UserAccount userAccount);

    void withdraw(@Param("userId") Long userId);

    void updateProfile(UserAccount userAccount);

    void deleteOnboardingCategoriesByUserId(@Param("userId") Long userId);

    void insertOnboardingCategory(
            @Param("userId") Long userId,
            @Param("coverageCategoryCode") String coverageCategoryCode);

    List<String> findOnboardingCategoriesByUserId(@Param("userId") Long userId);
}
