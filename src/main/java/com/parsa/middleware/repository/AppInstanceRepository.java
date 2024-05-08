package com.parsa.middleware.repository;

import com.parsa.middleware.model.AppInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AppInstanceRepository extends JpaRepository<AppInstance, String> {

    List<AppInstance> findByInstanceName(String instanceName);
    @Transactional
    @Modifying
    @Query("update AppInstance app set app.isActive = :isActive where app.instanceName = :instanceName")
    int updateStatus(@Param("isActive")boolean isActive, @Param("instanceName")String instanceName);
}
