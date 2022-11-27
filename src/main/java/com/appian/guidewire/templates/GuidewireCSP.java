package com.appian.guidewire.templates;

import com.appian.connectedsystems.simplified.sdk.SimpleConnectedSystemTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor.TextPropertyDescriptorBuilder;

import std.ConstantKeys;
import std.ParseOpenAPI;

@TemplateId(name="GuidewireCSP")
public class GuidewireCSP extends SimpleConnectedSystemTemplate implements ConstantKeys {

  public static TextPropertyDescriptorBuilder claims = ParseOpenAPI.initializePaths(CLAIMS);
  public static TextPropertyDescriptorBuilder policies = ParseOpenAPI.initializePaths(POLICIES);
/*  public static TextPropertyDescriptorBuilder jobs = ParseOpenAPI.initializePaths(JOBS);*/


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
/*        textProperty(PATIENCE)
            .label("The initial load of this plugin will take ~1 minute.")
            .isReadOnly(true)
            .build()*/
    );
  }
}
