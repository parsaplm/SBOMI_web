package com.parsa.middleware.repository;

import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.QueueEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueueRepository extends JpaRepository<QueueEntity, Integer> {

    List<QueueEntity> findByCurrentStatus(ImportStatus status);

    Page<QueueEntity> findByCurrentStatus(ImportStatus status, Pageable pageable);

    Page<QueueEntity> findByCurrentStatusIn(List<ImportStatus> statuses, Pageable pageable);


    QueueEntity findByFilename(String filename);


    QueueEntity findFirstByFilenameOrderByTaskIdDesc(String name);
}
