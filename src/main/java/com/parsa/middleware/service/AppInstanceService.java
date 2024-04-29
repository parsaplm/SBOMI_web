package com.parsa.middleware.service;

import com.parsa.middleware.model.AppInstance;
import com.parsa.middleware.repository.AppInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppInstanceService {
    @Autowired
    AppInstanceRepository appInstanceRepository;


    public void updateStatus(boolean isActive, String instanceName) {
        appInstanceRepository.updateStatus(isActive, instanceName);
    }

    public void registerAppInstance(AppInstance appInstance) {
        List<AppInstance> appInstanceList = appInstanceRepository.findByInstanceName(appInstance.getInstanceName());
        if (appInstanceList.size() == 0) {
            appInstanceRepository.save(appInstance);
        } else {
            appInstanceRepository.updateStatus(true, appInstanceList.get(0).getInstanceName());
        }
    }
}
