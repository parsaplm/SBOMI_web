package com.parsa.middleware.repository;

import com.parsa.middleware.model.QueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueueRepository extends JpaRepository<QueueEntity, Long> {
}
