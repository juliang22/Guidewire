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
    // Composite API is not possible due to SDK limitations. SDK currently doesn't support the user inputting unknown parameters
    // into a localTypeProperty (parameters need to be explicitly defined in java, not in expression editor)
/*    put(CLAIMS_COMPOSITE, Util.getOpenApi("com/appian/guidewire/templates/claims/claims_composite.yaml", classLoader));*/
    put(CLAIMS_SYSTEM_TOOLS, Util.getOpenApi("com/appian/guidewire/templates/claims/claims_systemtools.yaml", classLoader));
  }};

  private static Map<String, OpenAPI> POLICY_SWAGGER_MAP = new HashMap<String, OpenAPI>() {{
    put(POLICY_ACCOUNT, Util.getOpenApi("com/appian/guidewire/templates/policy/policy_account.yaml", classLoader));
    put(POLICY_ADMIN, Util.getOpenApi("com/appian/guidewire/templates/policy/policy_admin.yaml", classLoader));
    put(POLICY_ASYNC, Util.getOpenApi("com/appian/guidewire/templates/policy/policy_async.yaml", classLoader));
    put(POLICY_COMMON, Util.getOpenApi("com/appian/guidewire/templates/policy/policy_common.yaml", classLoader));
/*    put(POLICY_COMPOSITE, Util.getOpenApi("com/appian/guidewire/templates/policy/policy_composite.yaml", classLoader));*/
    put(POLICY_JOB, Util.getOpenApi("com/appian/guidewire/templates/policy/policy_job.yaml", classLoader));
    put(POLICY_POLICIES, Util.getOpenApi("com/appian/guidewire/templates/policy/policy_policies.yaml", classLoader));
    put(POLICY_POLICIES, Util.getOpenApi("com/appian/guidewire/templates/policy/policy_productdefinition.yaml", classLoader));
    put(POLICY_POLICIES, Util.getOpenApi("com/appian/guidewire/templates/policy/policy_systemtools.yaml", classLoader));
  }};

  private static Map<String, OpenAPI> BILLING_SWAGGER_MAP = new HashMap<String, OpenAPI>() {{
    put(BILLING_ADMIN, Util.getOpenApi("com/appian/guidewire/templates/billing/billing_admin.yaml", classLoader));
    put(BILLING_ASYNC, Util.getOpenApi("com/appian/guidewire/templates/billing/billing_async.yaml", classLoader));
    put(BILLING_BILLING, Util.getOpenApi("com/appian/guidewire/templates/billing/billing_billing.yaml", classLoader));
    put(BILLING_COMMON, Util.getOpenApi("com/appian/guidewire/templates/billing/billing_common.yaml", classLoader));
/*    put(BILLING_COMPOSITE, Util.getOpenApi("com/appian/guidewire/templates/billing/billing_composite.yaml", classLoader));*/
    put(BILLING_SYSTEM_TOOLS, Util.getOpenApi("com/appian/guidewire/templates/billing/billing_systemtools.yaml", classLoader));
  }};


  private static Map<String, Map<String, OpenAPI>> API_SWAGGER_MAP = new HashMap<String, Map<String, OpenAPI>>() {{
    put(CLAIMS, CLAIMS_SWAGGER_MAP);
    put(POLICIES, POLICY_SWAGGER_MAP);
    put(BILLING, BILLING_SWAGGER_MAP);
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
