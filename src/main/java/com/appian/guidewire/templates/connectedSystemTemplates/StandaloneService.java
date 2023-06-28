package com.appian.guidewire.templates.connectedSystemTemplates;

    import java.util.Arrays;
    import java.util.List;
    import java.util.Map;
    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

    import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
    import com.appian.connectedsystems.simplified.sdk.connectiontesting.SimpleTestableConnectedSystemTemplate;
    import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
    import com.appian.connectedsystems.templateframework.sdk.TemplateId;
    import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
    import com.appian.connectedsystems.templateframework.sdk.connectiontesting.TestConnectionResult;
    import com.fasterxml.jackson.core.JsonProcessingException;
    import com.fasterxml.jackson.databind.ObjectMapper;

    import std.ConstantKeys;
    import std.HTTP;

@TemplateId(name="StandaloneService")
public class StandaloneService extends SimpleTestableConnectedSystemTemplate implements ConstantKeys {

  protected ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected SimpleConfiguration getConfiguration(SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {

    return connectedSystemConfiguration.setProperties(
        textProperty(API_TYPE).choices(Choice.builder().name("Claims Center").value(CLAIMS).build(),
                Choice.builder().name("Policy Center").value(POLICIES).build(),
                Choice.builder().name("Billing Center").value(BILLING).build())
            .label("Guidewire API")
            .instructionText("Select the Guidewire API to access. Once the API is set, it cannot be changed. Create a separate " +
                "connected system object for each additional API.")
            .isRequired(true)
            /*            .isReadOnly(connectedSystemConfiguration.toConfiguration().getTypes().size() == 0 ? false : true)*/
            .build(),
        textProperty(ROOT_URL)
            .label("Base Url")
            .instructionText("Enter the base url of your Guidewire instance. For example, https://cc-dev-gwcpdev.<Tenant>" +
                ".zeta1-andromeda.guidewire.net")
            .isRequired(true)
            /*            .isReadOnly(connectedSystemConfiguration.toConfiguration().getTypes().size() == 0 ? false : true)*/
            .build(),
        textProperty(CLIENT_ID)
            .label("Client ID")
            .instructionText("Enter your Guidewire Client ID")
            .isRequired(true)
            /*            .isReadOnly(connectedSystemConfiguration.toConfiguration().getTypes().size() == 0 ? false : true)*/
            .build(),
        textProperty(CLIENT_SECRET)
            .label("Password")
            .instructionText("Enter your Guidewire Client Secret")
            .isRequired(true)
            .masked(true)
            .build(),
        textProperty(SCOPES)
            .label("Scopes")
            .instructionText("Enter the scopes for standalone service, comma separated. For example, ...")
            .isRequired(true)
            .masked(true)
            .build()
    );
  }

  @Override
  protected TestConnectionResult testConnection(SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {
    if (connectedSystemConfiguration.getValue(ROOT_URL) == null ||
        connectedSystemConfiguration.getValue(USERNAME) == null ||
        connectedSystemConfiguration.getValue(PASSWORD) == null) {
      return TestConnectionResult.error(Arrays.asList("Make sure to set all connected system values."));
    }

    // Get list of available subApis and their information map
    String rootUrl = connectedSystemConfiguration.getValue(ROOT_URL);
    Map<String, Object> initialResponse = HTTP.testAuth(connectedSystemConfiguration, rootUrl + "/rest/apis");
    if (initialResponse == null || initialResponse.containsKey("error")) {
      return TestConnectionResult.error(
          Arrays.asList("Error in connected system. Please verify that your authentication credentials are correct.")
      );
    }
    try {

      Map<String, Map<String, String>> subApiInfoMap = objectMapper.readValue(initialResponse.get("result").toString(), Map.class);
      Pattern pattern = Pattern.compile("/common/");
      for (Map.Entry<String, Map<String, String>> entry : subApiInfoMap.entrySet()) {
        String key = entry.getKey();
        Map<String, String> subApiMap = entry.getValue();
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
          String swaggerUrl = subApiMap.get("docs");
          Map<String, Object> apiSwaggerResponse = HTTP.testAuth(connectedSystemConfiguration, swaggerUrl);
          if (apiSwaggerResponse == null || apiSwaggerResponse.containsKey("error")) {
            return TestConnectionResult.error((List)apiSwaggerResponse.get("error"));
          }
        }
      }

      return TestConnectionResult.success();
    } catch (JsonProcessingException e) {
      return TestConnectionResult.error(Arrays.asList(e.getMessage(), e.getCause().toString()));
    }
  }
}
