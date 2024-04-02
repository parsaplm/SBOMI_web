package com.parsa.middleware.controller;

import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.dto.CombinedAuditDTO;
import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.QueueEntity;
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

    @GetMapping("/queue-entities")
    public Page<QueueEntity> getQueueEntitiesByStatus(
            @RequestParam("statuses") List<ImportStatus> statuses,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "taskId") String sortField,
            @RequestParam(value = "direction", defaultValue = "asc") String sortDirection,
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "searchCriteria", defaultValue = "") String searchCriteria) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortField));
        Page<QueueEntity> response = null;
        if (query != null && !query.isEmpty() && searchCriteria != null && !searchCriteria.isEmpty()) {
            switch (searchCriteria) {
                case TcConstants.SC_FILE_NAME:
                    response = queueRepository.findByFilename(query, pageable);
                    break;
                case TcConstants.SC_DRAWING_NUMBER:
                    response = queueRepository.findByDrawingNumber(query, pageable);
                    break;
                case TcConstants.SC_TEAMCENTER_ROOT_OBJECT:
                    response = queueRepository.findByTeamcenterRootObject(query, pageable);
                    break;
                case TcConstants.SC_LOG_FILE_NAME:
                    response = queueRepository.findByLogfileName(query, pageable);
                    break;

            }

        } else {
            response = queueRepository.findByCurrentStatusInOrderByIsFavoriteDesc(statuses, pageable);
        }


        // Call the repository method with the list of statuses
        return response;
    }

    @GetMapping("/auditLogs")
    public CombinedAuditDTO getAuditLogs(@RequestParam Long entityId) {
        return auditService.getAuditLogs(entityId);
    }
}
