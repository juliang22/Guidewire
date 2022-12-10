package com.appian.guidewire.templates;

import java.util.ArrayList;
import java.util.List;

import com.appian.connectedsystems.simplified.sdk.SimpleConnectedSystemTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor.TextPropertyDescriptorBuilder;
import com.appian.guidewire.templates.UIBuilders.ParseOpenAPI;

import io.swagger.v3.oas.models.OpenAPI;
import std.ConstantKeys;
import std.Util;

@TemplateId(name="GuidewireCSP")
public class GuidewireCSP extends SimpleConnectedSystemTemplate implements ConstantKeys {


  public static final OpenAPI claimsOpenApi = Util.getOpenApi("com/appian/guidewire/templates/claims.yaml");
  public static List<String> claimPathsForSearch = new ArrayList<>();
  public static TextPropertyDescriptorBuilder claims = ParseOpenAPI.endpointChoiceBuilder(CLAIMS, "", claimPathsForSearch);


  public static final OpenAPI policiesOpenApi = Util.getOpenApi("com/appian/guidewire/templates/policies" +
      ".yaml");
  public static List<String> policyPathsForSearch = new ArrayList<>();
  public static TextPropertyDescriptorBuilder policies = ParseOpenAPI.endpointChoiceBuilder(POLICIES, "", policyPathsForSearch);

/*
  public static OpenAPI claimsOpenApi = Util.getOpenApi("com/appian/guidewire/templates/jobs.yaml");
public static TextPropertyDescriptorBuilder jobs = ParseOpenAPI.initializePaths(JOBS);*/
/*public static Collection<String> jobPathsForSearch = new ArrayList<>();;*/


  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration simpleConfiguration, ExecutionContext executionContext) {

    return simpleConfiguration.setProperties(
/*        ParseOpenAPI.initializePaths("Jobs"),*/
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
