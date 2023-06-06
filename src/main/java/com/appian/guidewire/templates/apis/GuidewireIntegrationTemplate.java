package com.appian.guidewire.templates.apis;

import java.io.IOException;

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

import std.ConstantKeys;

@TemplateId(name = "ClaimsIntegrationTemplate")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ_AND_WRITE)
public class GuidewireIntegrationTemplate extends SimpleIntegrationTemplate implements ConstantKeys {

  // Claims Center Swagger Parsing


  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      PropertyPath propertyPath,
      ExecutionContext executionContext) {






    String apiType = connectedSystemConfiguration.getValue(API_TYPE);
    GuidewireUIBuilder restBuilder = new GuidewireUIBuilder(this, integrationConfiguration, connectedSystemConfiguration,
        apiType);
    return integrationConfiguration.setProperties(restBuilder.build());
   }

  @Override
  protected IntegrationResponse execute(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      ExecutionContext executionContext) {

    GuidewireExecute execute = new GuidewireExecute(this, integrationConfiguration, connectedSystemConfiguration,
        executionContext);
    try {
      return execute.buildExecution();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
