package com.parsa.middleware.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TCErrorLog {

    private Long id;

    private String message;

    private LocalDateTime timestamp;

    private String methodName;


    private List<String> parameterNames;

    private List<String> parameterValues;

    private String className;



    public TCErrorLog() {
        this.timestamp = LocalDateTime.now();
    }

    public TCErrorLog(String message, String methodName, List<String> parameterNames, List<String> parameterValues, String className) {
        this.message = message;
        this.methodName = methodName;
        this.parameterNames = parameterNames;
        this.parameterValues = parameterValues;
        this.className = className;
        this.timestamp = LocalDateTime.now();
    }

}
