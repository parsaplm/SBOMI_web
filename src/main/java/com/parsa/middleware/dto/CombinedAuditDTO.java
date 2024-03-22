package com.parsa.middleware.dto;

import com.parsa.middleware.model.AuditLog;
import com.parsa.middleware.model.THistoryEntity;

import java.util.List;

public class CombinedAuditDTO {

    private List<AuditLog> auditLog;
    private List<THistoryEntity> tHistoryEntity;

    public List<AuditLog> getAuditLog() {
        return auditLog;
    }

    public void setAuditLog(List<AuditLog> auditLog) {
        this.auditLog = auditLog;
    }

    public List<THistoryEntity> gettHistoryEntity() {
        return tHistoryEntity;
    }

    public void settHistoryEntity(List<THistoryEntity> tHistoryEntity) {
        this.tHistoryEntity = tHistoryEntity;
    }
}
