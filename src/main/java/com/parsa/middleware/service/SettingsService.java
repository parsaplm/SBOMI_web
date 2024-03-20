package com.parsa.middleware.service;

import com.parsa.middleware.config.ConfigProperties;
import com.parsa.middleware.config.StatusColorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


@Service
public class SettingsService {

    private final ConfigProperties appConfig;
    private final String configLocation;

    @Autowired
    private Environment environment;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private StatusColorConfig statusColorProperties;


    @Autowired
    public SettingsService(ConfigProperties appConfig, @Value("${SPRING_CONFIG_LOCATION}") String configLocation) {
        this.appConfig = appConfig;
        this.configLocation = configLocation;
    }

    public String updateSettings(ConfigProperties updatedConfigProperties) {
        try {
            // Load the existing YAML configuration
            FileReader fileReader = new FileReader(configLocation);
            Yaml yaml = new Yaml();
            Map<String, Object> yamlData = yaml.load(fileReader);
            fileReader.close();

            // Convert ConfigProperties to a map with non-null values
            Map<String, Object> updatedPropertiesMap = convertToMap(updatedConfigProperties);
            updatedPropertiesMap.entrySet().removeIf(entry -> isSensitiveProperty(entry.getKey()));
//            appConfig.reload(updatedPropertiesMap);

            // Update properties in the YAML data
            updateYamlData(yamlData, updatedPropertiesMap);

            // Write the updated YAML content back to the file
            FileWriter fileWriter = new FileWriter(configLocation);
            DumperOptions options = new DumperOptions();
            options.setPrettyFlow(true); // Preserve original formatting
            Yaml yamlWithFormatting = new Yaml(options);
            yamlWithFormatting.dump(yamlData, fileWriter);
            fileWriter.close();
            return "Config updated successfully.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to update config: " + e.getMessage();
        }
    }





    private Map<String, Object> convertToMap(ConfigProperties updatedConfigProperties) {
        Map<String, Object> propertiesMap = new HashMap<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(updatedConfigProperties.getClass());
            for (PropertyDescriptor property : beanInfo.getPropertyDescriptors()) {
                if (!"class".equals(property.getName())) {
                    Object value = property.getReadMethod().invoke(updatedConfigProperties);
                    if (value != null) {
                        String key = property.getName(); // Use property name directly
                        propertiesMap.put(key, value);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
            e.printStackTrace();
        }
        return propertiesMap;
    }

    private void updateYamlData(Map<String, Object> yamlData, Map<String, Object> updatedPropertiesMap) {
        for (Map.Entry<String, Object> entry : updatedPropertiesMap.entrySet()) {
            String propertyPath = entry.getKey();
            Object propertyValue = entry.getValue();
            setPropertyValue(yamlData, propertyPath, propertyValue);
//            updateConfigProperties(entry.getKey(), entry.getValue());

        }
    }

    private void setPropertyValue(Map<String, Object> yamlData, String propertyPath, Object propertyValue) {
        String[] pathSegments = propertyPath.split("\\."); // Split property path by '.'
        Map<String, Object> currentMap = yamlData;

        // Traverse the path to the correct nested location
        for (int i = 0; i < pathSegments.length - 1; i++) {
            String pathSegment = pathSegments[i];
            Object nestedMap = currentMap.get(pathSegment);

            // Create nested maps if they don't exist
            if (nestedMap == null || !(nestedMap instanceof Map)) {
                nestedMap = new LinkedHashMap<>(); // Use LinkedHashMap to maintain insertion order
                currentMap.put(pathSegment, nestedMap);
            }
            currentMap = (Map<String, Object>) nestedMap;
        }

        // Set the property value at the final nested location
        currentMap.put(pathSegments[pathSegments.length - 1], propertyValue);
    }

    public Map<String, String> getAllConfigurations() {
        Map<String, String> configurations = new HashMap<>();
        // Get all fields of the ConfigProperties class
        Field[] fields = ConfigProperties.class.getDeclaredFields();
        for (Field field : fields) {
            // Check if the field has the @Value annotation
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                // Get the key from the @Value annotation
                String key = valueAnnotation.value();
                // Remove the ${} from the key
                key = key.replaceAll("\\$\\{(.+?)\\}", "$1");
                // Get the value from the Environment
                if (!isSensitiveProperty(key)) {

                    String value = environment.getProperty(key);
                    // Put the key-value pair into the configurations map
                    configurations.put(key, value);
                }

            }
        }
        return configurations;
    }

    private boolean isSensitiveProperty(String key) {
        // Add logic to identify sensitive properties (e.g., password)
        return key.contains("password");
    }

    public void triggerReload() {
        // Get the base URL of the application dynamically
        String port = environment.getProperty("server.port");

        if (port == null) {
            port = "8080";
        }
        String baseUrl = "http://localhost:" + port;
        // Construct the URL for the /actuator/refresh endpoint
        String refreshUrl = baseUrl + "/actuator/refresh";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create an empty JSON object as the request body
        HttpEntity<String> request = new HttpEntity<>("{}", headers);

        // Trigger refresh by making an HTTP POST request to /actuator/refresh
        restTemplate.postForObject(refreshUrl, request, String.class);
    }


    public String updateStatusColors(Map<String, String> newStatusColors) throws IOException {
        try {
            // Update status colors in the StatusColorConfig bean
            statusColorProperties.setColors(newStatusColors);

            // Write the updated status colors to the application.yaml file
            updateYamlFile(newStatusColors);

            return "Status colors updated successfully!";
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to update status colors";
        }
    }
    private void updateYamlFile(Map<String, String> newStatusColors) throws IOException {
        // Write the updated status colors to the application.yaml file
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configLocation))) {
            writer.write("status-colors:\n");
            for (Map.Entry<String, String> entry : newStatusColors.entrySet()) {
                writer.write(String.format("  %s: \"%s\"\n", entry.getKey(), entry.getValue()));
            }
        }
    }
}
