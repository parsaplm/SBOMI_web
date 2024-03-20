package com.parsa.middleware.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;


import java.lang.reflect.Proxy;


import java.lang.reflect.Field;
import java.util.Map;


@Configuration
@RefreshScope
public class ConfigProperties {

    @Value("${url}")
    private String url;

    @Value("${uName}")
    private String uName;


    @Value("${password}")
    private String password;


    @Value("${tcMaxRetries}")
    private String tcMaxRetries;

    @Value("${transaction-folder-path}")
    private String transactionFolder;

    @Value("${log-folder-path}")
    private String logFolder;


    @Value("${tcRetryDelay}")
    private String tcRetryDelay;

    @Value("${updateCron}")
    private String importCronExpression;

    @Value("${alwaysClassify}")
    private boolean alwaysClassify;


    @Value("${searchParallel}")
    private String searchParallel;

    @Value("${parallelImport}")
    private String parallelImport;

    @Value("${maximumErrors}")
    private String maximumErrors;
    @Value("${deleteSchedule}")
    private String deleteSchedule;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getuName() {
        String decryptedUserName = decryptProperty(uName);
        return decryptedUserName;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }

    public String getPassword() {

        String decryptedPassword = decryptProperty(password);
        return decryptedPassword;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getImportCronExpression() {
        return importCronExpression;
    }

    public void setImportCronExpression(String importCronExpression) {
        this.importCronExpression = importCronExpression;
    }

    public String getTcMaxRetries() {
        return tcMaxRetries;
    }

    public void setTcMaxRetries(String tcMaxRetries) {
        this.tcMaxRetries = tcMaxRetries;
    }

    public String getTcRetryDelay() {
        return tcRetryDelay;
    }

    public void setTcRetryDelay(String tcRetryDelay) {
        this.tcRetryDelay = tcRetryDelay;
    }

    public boolean isAlwaysClassify() {
        return alwaysClassify;
    }

    public void setAlwaysClassify(boolean alwaysClassify) {
        this.alwaysClassify = alwaysClassify;
    }

    public String getTransactionFolder() {
        return transactionFolder;
    }

    public void setTransactionFolder(String baseFolderPath) {
        this.transactionFolder = baseFolderPath;
    }

    public String getSearchParallel() {
        return searchParallel;
    }

    public void setSearchParallel(String searchParallel) {
        this.searchParallel = searchParallel;
    }

    public String getParallelImport() {
        return parallelImport;
    }

    public void setParallelImport(String parallelImport) {
        this.parallelImport = parallelImport;
    }


    public String getMaximumErrors() {
        return maximumErrors;
    }

    public void setMaximumErrors(String maximumErrors) {
        this.maximumErrors = maximumErrors;
    }

    public String getDeleteSchedule() {
        return deleteSchedule;
    }

    public void setDeleteSchedule(String deleteSchedule) {
        this.deleteSchedule = deleteSchedule;
    }

    public String getLogFolder() {
        return logFolder;
    }

    public void setLogFolder(String logFolder) {
        this.logFolder = logFolder;
    }


    private String decryptProperty(String encryptedProperty) {
        // Check if property is encrypted
        if (StringUtils.isEmpty(encryptedProperty)) {
            return encryptedProperty;
        }
        // Decrypt property
        return  encryptedProperty.startsWith("ENC(") ? encryptedProperty.substring(4, encryptedProperty.length() - 1) : encryptedProperty;
    }



    // Reload method to update properties with new values
    // Reload method to update properties with new values
    // Reload method to update properties with new values
    public void reload(Map<String, Object> yamlData) {

    }


}
