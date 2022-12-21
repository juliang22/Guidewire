package com.appian.guidewire.templates;

import com.appian.connectedsystems.simplified.sdk.SimpleConnectedSystemTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;

import io.swagger.v3.oas.models.OpenAPI;
import std.ConstantKeys;
import std.Util;

@TemplateId(name="GuidewireCSP")
public class GuidewireCSP extends SimpleConnectedSystemTemplate implements ConstantKeys {


  public static final ClassLoader classLoader = GuidewireCSP.class.getClassLoader();
  public static final OpenAPI claimsOpenApi = Util.getOpenApi("com/appian/guidewire/templates/claims.yaml", classLoader);
  public static final OpenAPI policiesOpenApi = Util.getOpenApi("com/appian/guidewire/templates/policies" +
      ".yaml", classLoader);
  public static final OpenAPI jobsOpenApi = Util.getOpenApi("com/appian/guidewire/templates/jobs.yaml", classLoader);
  public static final OpenAPI accountsOpenApi = Util.getOpenApi("com/appian/guidewire/templates/accounts" +
      ".yaml", classLoader);


  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration simpleConfiguration, ExecutionContext executionContext) {

    return simpleConfiguration.setProperties(
        textProperty(USERNAME)
            .label("Username")
            .description("Enter your GuideWire username")
            .build(),
        textProperty(PASSWORD)
            .label("Password")
            .description("Enter your GuideWire password")
            .masked(true)
            .build()
    );
  }
}
