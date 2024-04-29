package com.parsa.middleware.config;

import com.parsa.middleware.model.AppInstance;
import com.parsa.middleware.service.AppInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Configuration
public class AppInstanceConfig {

    private final AppInstanceService appInstanceService;
    private final ConfigProperties configProperties;


    @Autowired
    public AppInstanceConfig(AppInstanceService appInstanceService, ConfigProperties configProperties) {
        this.appInstanceService = appInstanceService;
        this.configProperties = configProperties;
    }


    @PostConstruct
    public void postConstruct() {
        String instanceName = configProperties.getInstanceName();
        String instanceUrl = configProperties.getInstanceUrl();
        String instanceEnvironmentName = configProperties.getInstanceEnvironmentName();
        boolean isActive = true;
        AppInstance appInstance = new AppInstance(instanceName, instanceUrl, instanceEnvironmentName, isActive);
        appInstanceService.registerAppInstance(appInstance);
    }

    @PreDestroy
    public void preDestroy() {
        String instanceName = configProperties.getInstanceName();
        appInstanceService.updateStatus(false, instanceName);
    }

}
