package com.appian.guidewire.templates;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.simplified.sdk.connectiontesting.SimpleTestableConnectedSystemTemplate;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.ConfigurationDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.connectiontesting.TestConnectionResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import std.ConstantKeys;
import std.HTTP;

@TemplateId(name="GuidewireCSP")
public class GuidewireCSP extends SimpleTestableConnectedSystemTemplate implements ConstantKeys {

  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {

    List<TextPropertyDescriptor> defaultProperties = Arrays.asList(
        textProperty(API_TYPE).choices(Choice.builder().name("Claims Center").value(CLAIMS).build(),
                Choice.builder().name("Policy Center").value(POLICIES).build(),
                Choice.builder().name("Billing Center").value(BILLING).build())
            .label("Guidewire API")
            .instructionText("Select the Guidewire API to access. Once the API is set, it cannot be changed. Create a separate " +
                "connected system object for each additional API.")
            .isRequired(true)
            // If the connected system is being created for the first time, running simpleConfiguration.getProperty or .getValue errors
            // This checks if it is the first run or not and sets the API choice list if it is. This makes it so the api cannot
            // be changed after being set, they have to create a new connected system object for other apis.
            .isReadOnly(connectedSystemConfiguration.toConfiguration().getTypes().size() == 0 ? false : true)
            .build(),
        textProperty(ROOT_URL)
            .label("Base Url")
            .instructionText("Enter the base url of your Guidewire instance. For example, https://cc-gwcpdev.saappian.zeta1-andromeda.guidewire.net")
            .isRequired(true)
            .build(),
        textProperty(USERNAME)
            .label("Username")
            .instructionText("Enter your Guidewire username")
            .isRequired(true)
            .build(),
        textProperty(PASSWORD)
            .label("Password")
            .instructionText("Enter your Guidewire password")
            .isRequired(true)
            .masked(true)
            .build(),
        textProperty(OPENAPI_INFO)
            .isHidden(true)
            .build()
    );




/*    if (connectedSystemConfiguration.toConfiguration().getTypes().size() == 0) {
      return connectedSystemConfiguration.setProperties(defaultProperties.toArray(new PropertyDescriptor[0]));
    }*/


  /*  if (connectedSystemConfiguration.toConfiguration().getRootState() == null) {
      return connectedSystemConfiguration.setProperties(defaultProperties.toArray(new PropertyDescriptor[0]));
    }

    // If all config values set and user saves, this will run
    if (connectedSystemConfiguration.getProperty(API_TYPE) != null  && connectedSystemConfiguration.getValue(API_TYPE) != null &&
        connectedSystemConfiguration.getProperty(ROOT_URL) != null  && connectedSystemConfiguration.getValue(ROOT_URL) != null &&
        connectedSystemConfiguration.getProperty(USERNAME) != null  && connectedSystemConfiguration.getValue(USERNAME) != null &&
        connectedSystemConfiguration.getProperty(PASSWORD) != null  && connectedSystemConfiguration.getValue(PASSWORD) != null) {

      String rootUrl = connectedSystemConfiguration.getValue(ROOT_URL);
      try {
        Map<String, Object> initialResponse = HTTP.testAuth(connectedSystemConfiguration, rootUrl + "/rest/apis");
        if (initialResponse == null || initialResponse.containsKey("error")) {
          // TODO: error handle
        }

        Map<String, Map<String,String>> apiInfoMap = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String,Object> subApiList = objectMapper.readValue(initialResponse.get("result").toString(), Map.class);
        subApiList.forEach((api, properties) -> {
          Map<String, String> subAPIInfoMap = ((Map<String, String>)properties);
          String apiSwaggerUrl = subAPIInfoMap.get("docs").replace("swagger", "openapi");
          try {
            Map<String, Object> apiSwaggerResponse = HTTP.testAuth(connectedSystemConfiguration, apiSwaggerUrl);

            if (apiSwaggerResponse.containsKey("error")) return; // skip to next iteration if there's no available swagger docs

            String openAPIStr = apiSwaggerResponse.get("result").toString();
            subAPIInfoMap.put(OPENAPISTR, openAPIStr);

            String subApiKey = subAPIInfoMap.get("title").replace(" ", "");
            apiInfoMap.put(subApiKey, subAPIInfoMap);

          } catch (IOException e) {
            // TODO: error handle
            throw new RuntimeException(e);
          }
        });

        // TODO: encoding
        String openAPIInfoStr = objectMapper.writeValueAsString(apiInfoMap);

        return connectedSystemConfiguration
            .setProperties(defaultProperties.toArray(new PropertyDescriptor[0]))
            .setValue(OPENAPI_INFO, "testyTest");
      } catch (IOException e) {
        // TODO: Error handle
      }
    }*/

    return connectedSystemConfiguration.setProperties(defaultProperties.toArray(new PropertyDescriptor[0]));

  }

  @Override
  protected TestConnectionResult testConnection(SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {

    String rootUrl = connectedSystemConfiguration.getValue(ROOT_URL);
    try {
      Map<String, Object> initialResponse = HTTP.testAuth(connectedSystemConfiguration, rootUrl + "/rest/apis");
      if (initialResponse == null || initialResponse.containsKey("error")) {
        // TODO: error handle
      }

      Map<String, Map<String,String>> apiInfoMap = new HashMap<>();
      ObjectMapper objectMapper = new ObjectMapper();
      Map<String,Object> subApiList = objectMapper.readValue(initialResponse.get("result").toString(), Map.class);
      subApiList.forEach((api, properties) -> {
        Map<String, String> subAPIInfoMap = ((Map<String, String>)properties);
        String apiSwaggerUrl = subAPIInfoMap.get("docs").replace("swagger", "openapi");
        try {
          Map<String, Object> apiSwaggerResponse = HTTP.testAuth(connectedSystemConfiguration, apiSwaggerUrl);

          if (apiSwaggerResponse.containsKey("error")) return; // skip to next iteration if there's no available swagger docs

          String openAPIStr = apiSwaggerResponse.get("result").toString();
          subAPIInfoMap.put(OPENAPISTR, openAPIStr);

          String subApiKey = subAPIInfoMap.get("title").replace(" ", "");
          apiInfoMap.put(subApiKey, subAPIInfoMap);

        } catch (IOException e) {
          // TODO: error handle
          throw new RuntimeException(e);
        }
      });

      // TODO: encoding
      String openAPIInfoStr = objectMapper.writeValueAsString(apiInfoMap);
      connectedSystemConfiguration.setProperties(
          textProperty(OPENAPI_INFO)
              .isHidden(true)
              .build()
      ).setValue(OPENAPI_INFO, openAPIInfoStr);
    } catch (IOException e) {
      return TestConnectionResult.error(e.getMessage());
    }

    return TestConnectionResult.success();
  }
}
