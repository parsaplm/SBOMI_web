package com.parsa.middleware.model;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "t_history", schema = "dbo", catalog = "sbomiDB")
public class THistoryEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "tstamp")
    private OffsetDateTime tstamp;
    @Basic
    @Column(name = "schemaname")
    private String schemaname;
    @Basic
    @Column(name = "operation")
    private String operation;
    @Basic
    @Column(name = "task_id")
    private Integer taskId;
    @Basic
    @Column(name = "new_val")
    private String newVal;
    @Basic
    @Column(name = "old_val")
    private String oldVal;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Object getTstamp() {
        return tstamp;
    }

    public void setTstamp(OffsetDateTime tstamp) {
        this.tstamp = tstamp;
    }

    public String getSchemaname() {
        return schemaname;
    }

    public void setSchemaname(String schemaname) {
        this.schemaname = schemaname;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getNewVal() {
        return newVal;
    }

    public void setNewVal(String newVal) {
        this.newVal = newVal;
    }

    public String getOldVal() {
        return oldVal;
    }

    public void setOldVal(String oldVal) {
        this.oldVal = oldVal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        THistoryEntity that = (THistoryEntity) o;
        return id == that.id && Objects.equals(tstamp, that.tstamp) && Objects.equals(schemaname, that.schemaname) && Objects.equals(operation, that.operation) && Objects.equals(taskId, that.taskId) && Objects.equals(newVal, that.newVal) && Objects.equals(oldVal, that.oldVal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tstamp, schemaname, operation, taskId, newVal, oldVal);
    }
}
