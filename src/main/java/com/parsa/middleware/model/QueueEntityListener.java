package com.parsa.middleware.model;

import com.parsa.middleware.component.ApplicationContextProvider;
import com.parsa.middleware.repository.AuditLogRepository;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class QueueEntityListener {

    // Define the set of field names to skip
    private static final Set<String> fieldsToSkip = new HashSet<>();

    static {
        fieldsToSkip.add("oldState");
    }

    @PostPersist
    public void logCreation(QueueEntity entity) {
        AuditLog auditLog = createAuditLog(entity, "INSERT", null, null);
        saveAuditLog(auditLog);
    }

    @PreUpdate
    @Transactional
    public void logUpdates(QueueEntity entity) {
            compareAndLogChanges(entity, entity.getOldState());
    }

    @PreRemove
    public void logRemoval(QueueEntity entity) {
        AuditLog auditLog = createAuditLog(entity, "DELETE", null, null);
        saveAuditLog(auditLog);
    }

    private void compareAndLogChanges(QueueEntity newEntity, QueueEntity oldEntity) {
        for (Field field : QueueEntity.class.getDeclaredFields()) {
            if (fieldsToSkip.contains(field.getName())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object oldValue = null;
                if(oldEntity != null)
                oldValue = field.get(oldEntity);

                Object newValue = field.get(newEntity);

                if (isChanged(oldValue, newValue)) {
                    String oldValueString = oldValue != null ? oldValue.toString() : null;
                    String newValueString = newValue != null ? newValue.toString() : null;

                    AuditLog auditLog = createAuditLog(newEntity, field.getName(), oldValueString, newValueString);
                    saveAuditLog(auditLog);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace(); // Handle exception appropriately
            }
        }
    }

    private boolean isChanged(Object oldValue, Object newValue) {
        return oldValue == null ? newValue != null : !oldValue.equals(newValue);

    }

    private AuditLog createAuditLog(QueueEntity entity, String propertyName, String oldValue, String newValue) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityName(entity.getClass().getSimpleName());
        auditLog.setEntityId((long)entity.getTaskId());
        auditLog.setPropertyName(propertyName);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setChangeTime(OffsetDateTime.now());
        return auditLog;
    }

    private void saveAuditLog(AuditLog auditLog) {
        AuditLogRepository auditLogRepository = ApplicationContextProvider.getBean(AuditLogRepository.class);
        auditLogRepository.save(auditLog);
    }

}
