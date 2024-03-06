package com.parsa.middleware.model;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "queue", schema = "dbo", catalog = "sbomiDB")
public class QueueEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "task_id")
    private int taskId;
    @Basic
    @Column(name = "is_favorite")
    private boolean isFavorite;
    @Basic
    @Column(name = "drawing_number")
    private String drawingNumber;
    @Basic
    @Column(name = "filename")
    private String filename;
    @Basic
    @Column(name = "number_of_container")
    private int numberOfContainer;
    @Basic
    @Column(name = "current_status")
    private String currentStatus;
    @Basic
    @Column(name = "teamcenter_root_object")
    private String teamcenterRootObject;
    @Basic
    @Column(name = "import_progress")
    private int importProgress;
    @Basic
    @Column(name = "import_time")
    private Integer importTime;
    @Basic
    @Column(name = "creation_date")
    private OffsetDateTime creationDate;
    @Basic
    @Column(name = "start_import_date")
    private OffsetDateTime startImportDate;
    @Basic
    @Column(name = "end_import_date")
    private OffsetDateTime endImportDate;
    @Basic
    @Column(name = "number_of_objects")
    private Integer numberOfObjects;
    @Basic
    @Column(name = "logfile_name")
    private String logfileName;
    @Basic
    @Column(name = "sbomi_host_name")
    private String sbomiHostName;

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getDrawingNumber() {
        return drawingNumber;
    }

    public void setDrawingNumber(String drawingNumber) {
        this.drawingNumber = drawingNumber;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getNumberOfContainer() {
        return numberOfContainer;
    }

    public void setNumberOfContainer(int numberOfContainer) {
        this.numberOfContainer = numberOfContainer;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getTeamcenterRootObject() {
        return teamcenterRootObject;
    }

    public void setTeamcenterRootObject(String teamcenterRootObject) {
        this.teamcenterRootObject = teamcenterRootObject;
    }

    public int getImportProgress() {
        return importProgress;
    }

    public void setImportProgress(int importProgress) {
        this.importProgress = importProgress;
    }

    public Integer getImportTime() {
        return importTime;
    }

    public void setImportTime(Integer importTime) {
        this.importTime = importTime;
    }

    public Object getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Object getStartImportDate() {
        return startImportDate;
    }

    public void setStartImportDate(OffsetDateTime startImportDate) {
        this.startImportDate = startImportDate;
    }

    public Object getEndImportDate() {
        return endImportDate;
    }

    public void setEndImportDate(OffsetDateTime endImportDate) {
        this.endImportDate = endImportDate;
    }

    public Integer getNumberOfObjects() {
        return numberOfObjects;
    }

    public void setNumberOfObjects(Integer numberOfObjects) {
        this.numberOfObjects = numberOfObjects;
    }

    public String getLogfileName() {
        return logfileName;
    }

    public void setLogfileName(String logfileName) {
        this.logfileName = logfileName;
    }

    public String getSbomiHostName() {
        return sbomiHostName;
    }

    public void setSbomiHostName(String sbomiHostName) {
        this.sbomiHostName = sbomiHostName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueEntity that = (QueueEntity) o;
        return taskId == that.taskId && isFavorite == that.isFavorite && numberOfContainer == that.numberOfContainer && importProgress == that.importProgress && Objects.equals(drawingNumber, that.drawingNumber) && Objects.equals(filename, that.filename) && Objects.equals(currentStatus, that.currentStatus) && Objects.equals(teamcenterRootObject, that.teamcenterRootObject) && Objects.equals(importTime, that.importTime) && Objects.equals(creationDate, that.creationDate) && Objects.equals(startImportDate, that.startImportDate) && Objects.equals(endImportDate, that.endImportDate) && Objects.equals(numberOfObjects, that.numberOfObjects) && Objects.equals(logfileName, that.logfileName) && Objects.equals(sbomiHostName, that.sbomiHostName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, isFavorite, drawingNumber, filename, numberOfContainer, currentStatus, teamcenterRootObject, importProgress, importTime, creationDate, startImportDate, endImportDate, numberOfObjects, logfileName, sbomiHostName);
    }

    @PrePersist
    protected void onCreate() {
        creationDate = OffsetDateTime.now();
    }
}
