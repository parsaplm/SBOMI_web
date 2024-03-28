# SBOMI Backend Application README

## Introduction
This document provides instructions for configuring and running the SBOMI backend application. The application utilizes Java 11, Maven, SQL Server, Swagger UI for API documentation, and Jasypt for encryption.

## Configuration

### Application.yaml
1. Locate the `application.yaml` file in the `resources` directory.
2. Configure the following properties:
    - `datasource`: Provide the necessary details for your SQL Server datasource.
    - `teamcenter`:
        - `username`: Specify your Teamcenter username.
        - `password`: Encrypt your Teamcenter password using the Jasypt command provided.
3. Encrypt the sensitive information in `application.yaml` using the following command:
    ```bash
    mvn jasypt:encrypt -Djasypt.encryptor.password=secretKey -Djasypt.plugin.path=file:resources/application.yaml
    ```

### Encryption/Decryption
- To encrypt a value, use:
    ```bash
    mvn jasypt:encrypt-value -Djasypt.encryptor.password=secretKey -Djasypt.plugin.value=plainPassword -Djasypt.algorithm=PBEWithMD5AndDES
    ```
- To decrypt a value, use:
    ```bash
    mvn jasypt:decrypt-value -Djasypt.encryptor.password=secretKey -Djasypt.plugin.value=encryptedPassword
    ```

### Run the Application
1. Execute the following command:
    ```bash
    mvn spring-boot:run -Djasypt.encryptor.password=secretKey
    ```
2. Ensure the `SPRING_CONFIG_LOCATION` environment variable points to the `application.yaml` file:
    ```bash
    export SPRING_CONFIG_LOCATION=resources/application.yaml
    ```

## Accessing APIs
Once the application is running, access the API documentation using the following URL:
[http://localhost:8080/swagger-ui/#/](http://localhost:8080/swagger-ui/#/)


## Building the Application

### Build JAR File
To build the JAR file, execute the following command:
```bash
mvn clean package