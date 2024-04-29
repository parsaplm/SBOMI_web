package com.parsa.middleware.model;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name="app_instance")
public class AppInstance {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "instance_id", columnDefinition = "nvarchar(64)")
    private String instanceId;
    @Column(name = "instance_name", columnDefinition = "varchar(20)")
    private String instanceName;
    @Column(name = "instance_url", columnDefinition = "varchar(64)")
    private String instanceUrl;
    @Column(name = "instance_environment_name", columnDefinition = "varchar(20)")
    private String instanceEnvironmentName;
    @Column(name = "is_active")
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "create_timestamp")
    private Timestamp createTimestamp;
    @UpdateTimestamp
    @Column(name = "update_timestamp")
    private Timestamp updateTimestamp;

    public AppInstance() {

    }

    public AppInstance(String instanceName, String instanceUrl, String instanceEnvironmentName, boolean isActive) {
        this.instanceName = instanceName;
        this.instanceUrl = instanceUrl;
        this.instanceEnvironmentName = instanceEnvironmentName;
        this.isActive = isActive;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public String getInstanceEnvironmentName() {
        return instanceEnvironmentName;
    }

    public void setInstanceEnvironmentName(String instanceEnvironmentName) {
        this.instanceEnvironmentName = instanceEnvironmentName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Timestamp getCreateTimestamp() {
        return createTimestamp;
    }

    public void setCreateTimestamp(Timestamp createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    public Timestamp getUpdateTimestamp() {
        return updateTimestamp;
    }

    public void setUpdateTimestamp(Timestamp updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }
}
