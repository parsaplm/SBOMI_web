package com.parsa.middleware.controller;

import com.parsa.middleware.config.ConfigProperties;
import com.parsa.middleware.repository.AppInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequestMapping("/appInstance")
public class AppInstanceController {

    @Autowired
    private AppInstanceRepository appInstanceRepository;

    @Autowired
    private ConfigProperties configProperties;


    @GetMapping("/all")
    public List<com.parsa.middleware.model.AppInstance> getAppInstance(){
        return appInstanceRepository.findAll();
    }




}
