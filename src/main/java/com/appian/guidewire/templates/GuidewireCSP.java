package com.appian.guidewire.templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.SimpleConnectedSystemTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;

import io.swagger.v3.oas.models.OpenAPI;
import std.ConstantKeys;
import std.Util;

@TemplateId(name="GuidewireCSP")
public class GuidewireCSP extends SimpleConnectedSystemTemplate implements ConstantKeys {


  public static final ClassLoader classLoader = GuidewireCSP.class.getClassLoader();

  // Claims Center Swagger Parsing
  private static Map<String, OpenAPI> CLAIMS_SWAGGER_MAP = new HashMap<String, OpenAPI>() {{
    put(CLAIMS_ADMIN, Util.getOpenApi("com/appian/guidewire/templates/claims/claims_admin.yaml", classLoader));
    put(CLAIMS_ASYNC, Util.getOpenApi("com/appian/guidewire/templates/claims/claims_async.yaml", classLoader));
    put(CLAIMS_CLAIM, Util.getOpenApi("com/appian/guidewire/templates/claims/claims_claim.yaml", classLoader));
    put(CLAIMS_COMMON, Util.getOpenApi("com/appian/guidewire/templates/claims/claims_common.yaml", classLoader));
    put(CLAIMS_COMPOSITE, Util.getOpenApi("com/appian/guidewire/templates/claims/claims_composite.yaml", classLoader));
    put(CLAIMS_SYSTEM_TOOLS, Util.getOpenApi("com/appian/guidewire/templates/claims/claims_systemtools.yaml", classLoader));
  }};

  private static Map<String, OpenAPI> POLICY_SWAGGER_MAP = new HashMap<String, OpenAPI>() {{
    put(POLICY_POLICIES, Util.getOpenApi("com/appian/guidewire/templates/policy/policy_policies.yaml", classLoader));
    put(POLICY_JOB, Util.getOpenApi("com/appian/guidewire/templates/policy/policy_job.yaml", classLoader));
  }};

  private static Map<String, Map<String, OpenAPI>> API_SWAGGER_MAP = new HashMap<String, Map<String, OpenAPI>>() {{
    put(CLAIMS, CLAIMS_SWAGGER_MAP);
    put(POLICIES, POLICY_SWAGGER_MAP);
  }};

  public static OpenAPI getOpenAPI(String api, String subApi) {
    return API_SWAGGER_MAP.get(api).get(subApi);
  }

  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration simpleConfiguration, ExecutionContext executionContext) {

    return simpleConfiguration.setProperties(
        textProperty(API_TYPE)
            .choices(Choice.builder().name("Claims Center").value(CLAIMS).build(),
                Choice.builder().name("Policy Center").value(POLICIES).build(),
                 Choice.builder().name("Billing Center").value(BILLING).build())
            .label("Guidewire API")
            .description("Select the GuideWire API to access. Create a separate connected system for each additional API.")
            .isRequired(true)
            .build(),
        folderProperty(SWAGGER_FOLDER_LOCATION)
            .label("Location to Save API Specification")
            .description("This plugin uses a Swagger file (a standard specification for documenting APIs) to generate the " +
                "operations available in this integration. Select an Appian folder to store this file.")
            .isExpressionable(false)
            .isRequired(true)
            .build(),
        textProperty(ROOT_URL)
            .label("Base Url")
            .description("Enter the base url of your GuideWire instance")
            .isRequired(true)
            .build(),
        textProperty(USERNAME)
            .label("Username")
            .description("Enter your GuideWire username")
            .isRequired(true)
            .build(),
        textProperty(PASSWORD)
            .label("Password")
            .description("Enter your GuideWire password")
            .isRequired(true)
            .masked(true)
            .build()
    );
  }
}
