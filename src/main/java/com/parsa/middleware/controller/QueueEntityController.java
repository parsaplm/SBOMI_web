package com.parsa.middleware.controller;

import com.parsa.middleware.dto.CombinedAuditDTO;
import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.AuditLog;
import com.parsa.middleware.model.QueueEntity;
import com.parsa.middleware.repository.AuditLogRepository;
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
            @RequestParam(value = "direction", defaultValue = "asc") String sortDirection) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortField));

        // Call the repository method with the list of statuses
        return queueRepository.findByCurrentStatusInOrderByIsFavoriteDesc(statuses, pageable);
    }

    @GetMapping("/auditLogs")
    public CombinedAuditDTO getAuditLogs(@RequestParam Long entityId) {
        return auditService.getAuditLogs(entityId);
    }
}
