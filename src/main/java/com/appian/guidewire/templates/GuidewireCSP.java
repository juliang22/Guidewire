package com.appian.guidewire.templates;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.simplified.sdk.connectiontesting.SimpleTestableConnectedSystemTemplate;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.connectiontesting.TestConnectionResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import std.ConstantKeys;
import std.HTTP;
import std.OpenAPIInfo;

@TemplateId(name="GuidewireCSP")
public class GuidewireCSP extends SimpleTestableConnectedSystemTemplate implements ConstantKeys {


  // Claims Center Swagger Parsing
  private static Map<String,OpenAPIInfo> CLAIMS_SWAGGER_MAP = new HashMap<>();
  private static Map<String, OpenAPIInfo> POLICY_SWAGGER_MAP = new HashMap<>();
  private static Map<String, OpenAPIInfo> BILLING_SWAGGER_MAP = new HashMap<>();

  private static Map<String, Map<String, OpenAPIInfo>> instantiatedConnectedSystemsSwaggerMap = new HashMap<>();

  public static Map<String, OpenAPIInfo> getApiSwaggerMap(String apiType) {
    switch (apiType) {
      case CLAIMS:
        return CLAIMS_SWAGGER_MAP;
      case POLICIES:
        return POLICY_SWAGGER_MAP;
      default:
        return BILLING_SWAGGER_MAP;
    }
  }

  public static OpenAPI getOpenAPI(String api, String subApi) {
    return getApiSwaggerMap(api).get(subApi).getOpenAPI();
  }

  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration simpleConfiguration, ExecutionContext executionContext) {

    return simpleConfiguration.setProperties(
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
            .isReadOnly(simpleConfiguration.toConfiguration().getTypes().size() == 0 ? false : true)
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
            .build()
    );
  }

  @Override
  protected TestConnectionResult testConnection(SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {

    String rootUrl = connectedSystemConfiguration.getValue(ROOT_URL);
    try {
      Map<String, Object> initialResponse = HTTP.testAuth(connectedSystemConfiguration, rootUrl + "/rest/apis");
      if (initialResponse == null || initialResponse.containsKey("error")) {
        // TODO: error handle
      }

      String apiType = connectedSystemConfiguration.getValue(API_TYPE);
      Map<String, OpenAPIInfo> swaggerMap = getApiSwaggerMap(apiType);
      // clearing values from map if testConnection was previously already clicked. This is to make sure that if the user
      // switches usernames/passwords/base url, the map is up-to-date
      if (swaggerMap.size() > 0) swaggerMap.clear();

      Map<String,Object> subApiList = new ObjectMapper().readValue(initialResponse.get("result").toString(), Map.class);
      subApiList.forEach((api, properties) -> {
        Map<String, String> propertiesMap = ((Map<String, String>)properties);
        String apiSwaggerUrl = propertiesMap.get("docs").replace("swagger", "openapi");
        try {
          Map<String, Object> apiSwaggerResponse = HTTP.testAuth(connectedSystemConfiguration, apiSwaggerUrl);

          if (apiSwaggerResponse.containsKey("error")) return; // skip to next iteration if there's no available swagger docs

          String swaggerStr = apiSwaggerResponse.get("result").toString();
          ParseOptions parseOptions = new ParseOptions();
          parseOptions.setResolve(true); // implicit
          parseOptions.setResolveFully(true);
          parseOptions.setResolveCombinators(false);
          OpenAPI openAPI = new OpenAPIV3Parser().readContents(swaggerStr, null, null).getOpenAPI();

          OpenAPIInfo apiInfo = new OpenAPIInfo(
              propertiesMap.get("title"),
              propertiesMap.get("basePath"),
              apiSwaggerUrl,
              propertiesMap.get("description"),
              openAPI
          );

          String subApi = propertiesMap.get("title").replace(" ", "");
          swaggerMap.put(subApi, apiInfo);

        } catch (IOException e) {
          // TODO: error handle
          throw new RuntimeException(e);
        }
      });

    } catch (IOException e) {
      return TestConnectionResult.error(e.getMessage());
    }
    // TODO: base case
    return TestConnectionResult.success();
  }
}
