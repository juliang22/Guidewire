package com.appian.guidewire.templates.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.ConfigurableTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.BooleanDisplayMode;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.ListTypePropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypePropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptorBuilder;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.SystemType;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TypeReference;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;
import com.appian.guidewire.templates.GuidewireCSP;
import com.appian.guidewire.templates.UIBuilders.ParseOpenAPI;
import com.appian.guidewire.templates.UIBuilders.RestParamsBuilder;
import com.fasterxml.jackson.databind.annotation.JsonAppend;

import std.ConstantKeys;

@TemplateId(name = "JobsIntegrationTemplate")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.WRITE)
public class JobsIntegrationTemplate extends SimpleIntegrationTemplate implements ConstantKeys {

  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      PropertyPath propertyPath,
      ExecutionContext executionContext) {

    // Logic for Posts simplified
   /* LocalTypeDescriptor innerOne = localType("innerOne").properties(
        textProperty("textOne").label("textOne").build())
        .build();

    LocalTypeDescriptor innerTwo = localType("innerTwo").properties(
            textProperty("textTwo").label("textTwo").build())
        .build();

    LocalTypeDescriptor metaDataType = localType("METADATA_TYPE")
        .properties(
            textProperty("METADATA_NAME")
                .label("Metadata Name")
                .description("The name or key of the metadata")
                .placeholder("category")
                .isExpressionable(true)
                .build(),
            textProperty("METADATA_VALUE")
                .label("Metadata Value")
                .description("The value of the metadata")
                .placeholder("api")
                .isExpressionable(true)
                .build()
        ).build();
    LocalTypeDescriptor nested = localType("layered").properties(
        localTypeProperty(innerTwo).build()
*//*        listTypeProperty("lsity").itemType(TypeReference.from(metaDataType)).build()*//*
    ).build();


    List<PropertyDescriptor> innerOneProperties = new ArrayList<>(innerOne.getProperties());
*//*    innerOneProperties.addAll(innerTwo.getProperties());*//*
    innerOneProperties.addAll(nested.getProperties());
    LocalTypeDescriptor merged = localType("merged").properties(innerOneProperties).build();


    List<PropertyDescriptor> propertyDescriptors = Arrays.asList(
        localTypeProperty(merged).isExpressionable(true).displayHint(DisplayHint.EXPRESSION).build()
    );

    return integrationConfiguration.setProperties(propertyDescriptors.toArray(new PropertyDescriptor[0]));
*/


    LocalTypeDescriptor metaDataType = localType("weboolin")
        .properties(
            booleanProperty("bool")
                .label("Metadata Value")
                .description("The value of the metadata")
                .placeholder("api")
                .isExpressionable(true)
                .placeholder("placeholder")
                .instructionText("instrujction")
                .displayMode(BooleanDisplayMode.RADIO_BUTTON)
                .isRequired(false)
                .displayHint(DisplayHint.NORMAL)
                .refresh(RefreshPolicy.ALWAYS)
                .build()
        ).build();
    return integrationConfiguration.setProperties(localTypeProperty(metaDataType).key("boooool").build()).setValue("bool", false);


/*    return integrationConfiguration.setProperties(
        ParseOpenAPI.buildRootDropdown(integrationConfiguration,this, JOBS, GuidewireCSP.jobPathsForSearch)
    );*/

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

    return IntegrationResponse.forSuccess(result).withDiagnostic(diagnostic).build();
  }
}
