package com.parsa.middleware.repository;

import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.QueueEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueueRepository extends JpaRepository<QueueEntity, Integer> {

    List<QueueEntity> findByCurrentStatusOrderByIsFavoriteDesc(ImportStatus status);

    Page<QueueEntity> findByCurrentStatusInOrderByIsFavoriteDesc(List<ImportStatus> statuses, Pageable pageable);


    Page<QueueEntity> findByFilenameAndCurrentStatusIn(String filename, List<ImportStatus> statuses, Pageable pageable);

    @Query(value = "SELECT q FROM QueueEntity q  WHERE q.filename LIKE %?1% AND q.currentStatus IN ?2")
    Page<QueueEntity> findMatchingFilenameIncludeStatuses(String filename, List<ImportStatus> statuses, Pageable pageable);

    List<QueueEntity> findByCurrentStatus(ImportStatus status);


    QueueEntity findFirstByFilenameOrderByTaskIdDesc(String name);


    QueueEntity findFirstByFilenameAndCurrentStatusOrderByCreationDateDesc(String fileName, ImportStatus status);

    @Query(value = "SELECT q FROM QueueEntity q  WHERE q.drawingNumber LIKE %?1% AND q.currentStatus IN ?2")
    Page<QueueEntity> findMatchingDrawingNumberIncludeStatuses(String drawingNumber, List<ImportStatus> statuses, Pageable pageable);

    @Query(value = "SELECT q FROM QueueEntity q  WHERE q.teamcenterRootObject LIKE %?1% AND q.currentStatus IN ?2")
    Page<QueueEntity> findMatchingTeamcenterRootObjectIncludeStatuses(String teamcenterRootObject, List<ImportStatus> statuses, Pageable pageable);

    @Query(value = "SELECT q FROM QueueEntity q  WHERE q.logfileName LIKE %?1% AND q.currentStatus IN ?2")
    Page<QueueEntity> findMatchingLogfileNameIncludeStatuses(String logFile, List<ImportStatus> statuses, Pageable pageable);
}
