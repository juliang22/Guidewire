package com.appian.guidewire.templates.integrationTemplates;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.tika.mime.MimeTypeException;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;
import com.appian.guidewire.templates.Execution.GuidewireExecute;
import com.appian.guidewire.templates.UI.GuidewireUIBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import std.ConstantKeys;

@TemplateId(name = "ClaimsIntegrationTemplate")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ_AND_WRITE)
public class GuidewireIntegrationTemplate extends SimpleIntegrationTemplate implements ConstantKeys {

  public static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      PropertyPath propertyPath,
      ExecutionContext executionContext) {


    integrationConfiguration.setErrors(Collections.singletonList("")); // resetting errors if they exist
    try {
      return new GuidewireUIBuilder(this, integrationConfiguration, connectedSystemConfiguration, propertyPath).build();
    } catch (IOException | MimeTypeException e) {
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

    try {
      return new GuidewireExecute(integrationConfiguration, connectedSystemConfiguration, executionContext).buildExecution();
    } catch (IOException | MimeTypeException e) {
      return IntegrationResponse.forError(
          IntegrationError.builder().title(e.getCause().toString()).message(e.getMessage()).build())
          .build();
    }
  }
}
