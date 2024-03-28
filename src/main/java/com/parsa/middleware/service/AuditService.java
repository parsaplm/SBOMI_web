package com.parsa.middleware.service;

import com.parsa.middleware.dto.CombinedAuditDTO;
import com.parsa.middleware.model.AuditLog;
import com.parsa.middleware.model.THistoryEntity;
import com.parsa.middleware.repository.AuditLogRepository;
import com.parsa.middleware.repository.THistoryEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuditService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private THistoryEntityRepository tHistoryRepository;

    public CombinedAuditDTO getAuditLogs(Long entityId) {
        CombinedAuditDTO combinedAuditDTOs = new CombinedAuditDTO();

        combinedAuditDTOs.setAuditLog((auditLogRepository.findByEntityId(entityId)));
        if (combinedAuditDTOs.getAuditLog().isEmpty()) {
            combinedAuditDTOs.settHistoryEntity(tHistoryRepository.findByTaskId(entityId.intValue()));
        }
        return combinedAuditDTOs;
    }
}
