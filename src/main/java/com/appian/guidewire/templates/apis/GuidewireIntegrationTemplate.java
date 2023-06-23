package com.appian.guidewire.templates.apis;

import java.io.IOException;
import java.util.Arrays;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;
import com.appian.guidewire.templates.Execution.GuidewireExecute;
import com.appian.guidewire.templates.UI.GuidewireUIBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;

import std.ConstantKeys;

@TemplateId(name = "ClaimsIntegrationTemplate")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ_AND_WRITE)
public class GuidewireIntegrationTemplate extends SimpleIntegrationTemplate implements ConstantKeys {

  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      PropertyPath propertyPath,
      ExecutionContext executionContext) {


    integrationConfiguration.setErrors(Arrays.asList("")); // resetting errors if they exist
    try {
      return new GuidewireUIBuilder(this, integrationConfiguration, connectedSystemConfiguration, propertyPath).build();
    } catch (IOException e) {
      return integrationConfiguration.setErrors(
          Arrays.asList("Error in connected system. Please verify that your authentication credentials are correct", e.getMessage())
      );
    }
  }

  @Override
  protected IntegrationResponse execute(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      ExecutionContext executionContext) {

    GuidewireExecute execute = null;
    try {
      execute = new GuidewireExecute(this, integrationConfiguration, connectedSystemConfiguration,
          executionContext);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    try {
      return execute.buildExecution();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
