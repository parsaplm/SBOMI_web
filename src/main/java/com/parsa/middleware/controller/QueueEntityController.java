package com.parsa.middleware.controller;

import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.dto.CombinedAuditDTO;
import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.AppInstance;
import com.parsa.middleware.model.QueueEntity;
import com.parsa.middleware.repository.AppInstanceRepository;
import com.parsa.middleware.repository.QueueRepository;
import com.parsa.middleware.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/app")
public class QueueEntityController {

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AppInstanceRepository appInstanceRepository;

    @GetMapping("/queue-entities")
    public Page<QueueEntity> getQueueEntitiesByStatus(
            @RequestParam("statuses") List<ImportStatus> statuses,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "taskId") String sortField,
            @RequestParam(value = "direction", defaultValue = "asc") String sortDirection,
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "searchCriteria", defaultValue = "") String searchCriteria,
            @RequestParam("appInstanceName") String appInstanceName) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortField));

        AppInstance appInstance = appInstanceRepository.findByInstanceName(appInstanceName).get(0);


        Page<QueueEntity> response = null;
        if (query != null && !query.isEmpty() && searchCriteria != null && !searchCriteria.isEmpty()) {
            switch (searchCriteria) {
                case TcConstants.SC_FILE_NAME:
                    response = queueRepository.findMatchingFilenameIncludeStatusesAndAppInstanceInstanceId(query, statuses, pageable);
                    break;
                case TcConstants.SC_DRAWING_NUMBER:
                    response = queueRepository.findByDrawingNumberContainingAndCurrentStatusInAndAppInstanceInstanceId(query, statuses, pageable, appInstance.getInstanceId());
                    break;
                case TcConstants.SC_TEAMCENTER_ROOT_OBJECT:
                    response = queueRepository.findMatchingTeamcenterRootObjectIncludeStatusesAndAppInstanceInstanceId(query, statuses, pageable);
                    break;
                case TcConstants.SC_LOG_FILE_NAME:
                    response = queueRepository.findMatchingLogfileNameIncludeStatusesAndAppInstanceInstanceId(query, statuses, pageable);
                    break;

            }

        } else {
//            response = queueRepository.findByCurrentStatusInOrderByIsFavoriteDesc(statuses, pageable);
//            response = queueRepository.findByCurrentStatusInAndAppInstanceInstanceIdOrderByIsFavoriteDesc(statuses, appInstance.getInstanceId(),  pageable);
            response = queueRepository.findByCurrentStatusInAndAppInstanceInstanceIdOrCurrentStatusOrderByIsFavoriteDesc(statuses, appInstance.getInstanceId(), ImportStatus.IN_SCOPE,  pageable);
        }


        // Call the repository method with the list of statuses
        return response;
    }

    @GetMapping("/auditLogs")
    public CombinedAuditDTO getAuditLogs(@RequestParam Long entityId) {
        return auditService.getAuditLogs(entityId);
    }
}
