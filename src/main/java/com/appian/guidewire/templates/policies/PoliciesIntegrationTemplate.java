package com.appian.guidewire.templates.policies;

import java.util.HashMap;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;
import com.appian.guidewire.templates.UIBuilders.ParseOpenAPI;

import std.ConstantKeys;

@TemplateId(name="PoliciesIntegrationTemplate")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.WRITE)
public class PoliciesIntegrationTemplate extends SimpleIntegrationTemplate implements ConstantKeys {

  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      PropertyPath propertyPath,
      ExecutionContext executionContext) {


    PropertyDescriptor[] params = ParseOpenAPI.buildRootDropdown(integrationConfiguration,this, POLICIES);
    return integrationConfiguration.setProperties(params);

  }


  @Override
  protected IntegrationResponse execute(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      ExecutionContext executionContext) {
    Map<String,Object> requestDiagnostic = new HashMap<>();
    String csValue = connectedSystemConfiguration.getValue("");
    requestDiagnostic.put("csValue", csValue);
    String integrationValue = integrationConfiguration.getValue("");
    requestDiagnostic.put("integrationValue", integrationValue);
    Map<String,Object> result = new HashMap<>();

    // Important for debugging to capture the amount of time it takes to interact
    // with the external system. Since this integration doesn't interact
    // with an external system, we'll just log the calculation time of concatenating the strings
    final long start = System.currentTimeMillis();
    result.put("hello", "world");
    result.put("concat", csValue + integrationValue);
    final long end = System.currentTimeMillis();

    final long executionTime = end - start;
    final IntegrationDesignerDiagnostic diagnostic = IntegrationDesignerDiagnostic.builder()
        .addExecutionTimeDiagnostic(executionTime)
        .addRequestDiagnostic(requestDiagnostic)
        .build();

    return IntegrationResponse
        .forSuccess(result)
        .withDiagnostic(diagnostic)
        .build();
  }
}
