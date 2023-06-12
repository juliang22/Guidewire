package com.appian.guidewire.templates.apis;

import java.io.IOException;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
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

    /*this.textProperty("").transientChoices(true).choice(Choice.builder().name("").value("").build());*/
    // TODO: transient choices, save OAS to choice value

    GuidewireUIBuilder restBuilder = null;
    try {
      restBuilder = new GuidewireUIBuilder(this, integrationConfiguration, connectedSystemConfiguration);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    PropertyDescriptor<?>[] result = new PropertyDescriptor[0];
    try {
      result = restBuilder.build().toArray(new PropertyDescriptor<?>[0]);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return integrationConfiguration.setProperties(result);

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
