package com.parsa.middleware.repository;

import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.AppInstance;
import com.parsa.middleware.model.QueueEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueueRepository extends JpaRepository<QueueEntity, Integer> {

    List<QueueEntity> findByCurrentStatusOrderByIsFavoriteDesc(ImportStatus status);

    Page<QueueEntity> findByCurrentStatusInOrderByIsFavoriteDesc(List<ImportStatus> statuses, Pageable pageable);

//    Page<QueueEntity> findByCurrentStatusInAndAppInstanceInstanceIdOrderByIsFavoriteDesc(@Param("statuses") List<ImportStatus> statuses, @Param("instanceId") String instanceId, Pageable pageable);

    Page<QueueEntity> findByCurrentStatusInAndAppInstanceInstanceIdOrCurrentStatusOrderByIsFavoriteDesc(@Param("statuses") List<ImportStatus> statuses,
                                                                                                        @Param("instanceId") String instanceId,
                                                                                                        @Param("status") ImportStatus status,
                                                                                                        Pageable pageable);

    Page<QueueEntity> findByFilenameAndCurrentStatusIn(String filename, List<ImportStatus> statuses, Pageable pageable);

    @Query(value = "SELECT q FROM QueueEntity q  WHERE q.filename LIKE %?1% AND q.currentStatus IN ?2")
    Page<QueueEntity> findMatchingFilenameIncludeStatusesAndAppInstanceInstanceId(String filename, List<ImportStatus> statuses, Pageable pageable);

    List<QueueEntity> findByCurrentStatus(ImportStatus status);


    QueueEntity findFirstByFilenameOrderByTaskIdDesc(String name);


    QueueEntity findFirstByFilenameAndCurrentStatusOrderByCreationDateDesc(String fileName, ImportStatus status);

//    @Query(value = "SELECT q FROM QueueEntity q  WHERE q.drawingNumber LIKE %?1% AND q.currentStatus IN ?2")
//    @Query("SELECT q FROM QueueEntity q JOIN FETCH q.appInstance WHERE q.drawingNumber LIKE %?1%  AND q.currentStatus IN ?2 AND (q.appInstance.instanceId = ?3 OR q.appInstance IS NULL)")
//    Page<QueueEntity> findMatchingDrawingNumberIncludeStatuses(String drawingNumber, List<ImportStatus> statuses, String instanceId, Pageable pageable);

//    @Query("SELECT q FROM QueueEntity q WHERE q.drawingNumber LIKE %:drawingNumber% AND q.currentStatus IN :statuses ")
    Page<QueueEntity> findByDrawingNumberContainingAndCurrentStatusInAndAppInstanceInstanceId(
            @Param("drawingNumber") String drawingNumber,
            @Param("statuses") List<ImportStatus> statuses,
            Pageable pageable,
            @Param("instanceId") String instanceId);

    @Query(value = "SELECT q FROM QueueEntity q  WHERE q.teamcenterRootObject LIKE %?1% AND q.currentStatus IN ?2")
    Page<QueueEntity> findMatchingTeamcenterRootObjectIncludeStatusesAndAppInstanceInstanceId(String teamcenterRootObject, List<ImportStatus> statuses, Pageable pageable);

    @Query(value = "SELECT q FROM QueueEntity q  WHERE q.logfileName LIKE %?1% AND q.currentStatus IN ?2")
    Page<QueueEntity> findMatchingLogfileNameIncludeStatusesAndAppInstanceInstanceId(String logFile, List<ImportStatus> statuses, Pageable pageable);



}
