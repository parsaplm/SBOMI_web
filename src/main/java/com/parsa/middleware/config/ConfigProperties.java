package com.parsa.middleware.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;



@Configuration
public class ConfigProperties {

    @Value("${TC.url}")
    private String url;

    @Value("${TC.uName}")
    private String uName;


    @Value("${TC.password}")
    private String password;



    @Value("${application.TcMaxRetries}")
    private String tcMaxRetries;

    @Value("${application.transaction-folder-path}")
    private String transactionFolder;


    @Value("${application.TcRetryDelay}")
    private String tcRetryDelay;

    @Value("${import.schedule.cron}")
    private String importCronExpression;

    @Value("${application.alwaysClassify}")
    private boolean alwaysClassify;
    @Value("${application.serverURL}")
    private String serverURL;

    @Value("${application.searchParallel}")
    private String searchParallel;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getuName() {
        return uName;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }

    public String getPassword() {
        return password;
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

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
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
}
