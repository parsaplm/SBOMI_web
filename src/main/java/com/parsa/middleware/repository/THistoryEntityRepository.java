package com.parsa.middleware.repository;

import com.parsa.middleware.model.THistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface THistoryEntityRepository extends JpaRepository<THistoryEntity, Integer> {

    List<THistoryEntity> findByTaskId(int taskId);
}
