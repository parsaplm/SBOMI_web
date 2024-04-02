package com.parsa.middleware.repository;

import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.QueueEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;
import java.util.List;

@Repository
public interface QueueRepository extends JpaRepository<QueueEntity, Integer> {

    List<QueueEntity> findByCurrentStatusOrderByIsFavoriteDesc(ImportStatus status);

    Page<QueueEntity> findByCurrentStatusInOrderByIsFavoriteDesc(List<ImportStatus> statuses, Pageable pageable);


    Page<QueueEntity> findByFilename(String filename, Pageable pageable);

    List<QueueEntity>  findByCurrentStatus(ImportStatus status);



    QueueEntity findFirstByFilenameOrderByTaskIdDesc(String name);


    QueueEntity findFirstByFilenameAndCurrentStatusOrderByCreationDateDesc(String fileName, ImportStatus status);

    Page<QueueEntity> findByDrawingNumber(String drawingNumber, Pageable pageable);

    Page<QueueEntity> findByTeamcenterRootObject(String teamcenterRootObject, Pageable pageable);
    Page<QueueEntity> findByLogfileName(String logFile, Pageable pageable);
}
