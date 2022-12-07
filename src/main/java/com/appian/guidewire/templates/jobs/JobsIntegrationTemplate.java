package com.appian.guidewire.templates.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypePropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
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


/*
*//*    LocalTypeDescriptor metaDataType = localType("METADATA_TYPE")
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
        ).build();*//*

    LocalTypeDescriptor.Builder meta2 = localType("METADATA_TYPEZ");
    List<String> arr = Arrays.asList("hello", "world");
    arr.forEach(el -> {
      meta2.properties(
          TextPropertyDescriptor.builder()
              .key(el)
              .instructionText(el)
              .isExpressionable(true)
              .displayHint(DisplayHint.EXPRESSION)
              .placeholder(el)
              .build()
      );
    });
    LocalTypeDescriptor builtMeta = meta2.build();

    LocalTypeDescriptor layered = localType("stoppid")
        .properties(builtMeta).build();

    LocalTypeDescriptor qnaType = localType("QNA_TYPE")
        .properties(
            textProperty("QNA_ANSWER")
                .label("Answer")
                .description("The answer to the question.")
                .placeholder("You can change the default message if you use the QnAMakerDialog. See this for details: https://docs.botframework.com/en-us/azure-bot-service/templates/qnamaker/#navtitle")
                .instructionText("Max length 25000")
                .isExpressionable(true)
                .isRequired(true)
                .build(),
            textProperty("QNA_SOURCE")
                .label("Source")
                .description("Source from which Q-A was indexed. eg. https://docs.microsoft.com/en-us/azure/cognitive-services/QnAMaker/FAQs")
                .placeholder("QnA Maker FAQ")
                .instructionText("Max length 300")
                .isExpressionable(true)
                .build(),
            listTypeProperty("QNA_QUESTIONS")
                .label("Questions")
                .itemType(SystemType.STRING)
                .description("List of questions associated with the answer.")
                .instructionText("Answers can have more than one question associated with it. Max length 1000")
                .isExpressionable(true)
                .isRequired(true)
                .build(),
*//*            listTypeProperty("metadata")
                .label("Metadata")
                .itemType(TypeReference.from(builtMeta))
                .isExpressionable(true)
                .instructionText("Fill out the name - value pairs of the metadata. Up to 10 metadata objects are allowed.")
                .description("List of metadata associated with the answer.")
                .build(),*//*

            localTypeProperty(layered)
                .label("Metadataddd")
                .isExpressionable(true)
                .instructionText("Fill out the name - value pairs of the metadata. Up to 10 metadata objects are allowed.")
                .description("List of metadata associated with the answer.")
                .build()
        ).build();

    return integrationConfiguration.setProperties(
*//*        localTypeProperty(builtMeta).label("Meta").isHidden(true).isExpressionable(true).build(),*//*

        localTypeProperty(qnaType).key("SINGLE_QNA").displayHint(DisplayHint.EXPRESSION).isExpressionable(true).label("QnA").build()

    );*/




    List<Object> reqBodyArr = ParseOpenAPI.buildRequestBodyUI(GuidewireCSP.claimsOpenApi,
        "/claims/{claimId}/service-requests/{serviceRequestId}/invoices");

    LocalTypeDescriptor.Builder reqBody = localType(REQ_BODY);
    reqBodyArr.forEach(field -> {
      if (field instanceof TextPropertyDescriptor) {
        reqBody.properties((TextPropertyDescriptor)field);
      } else if (field instanceof LocalTypeDescriptor) {
        reqBody.properties(localTypeProperty((LocalTypeDescriptor)field).build());
      }
    });


    return integrationConfiguration.setProperties(
        localTypeProperty(reqBody.build()).key("SINGLE_QNA").displayHint(DisplayHint.EXPRESSION).isExpressionable(true).label("QnA").build()
    );


    // TODO: change to jobs once I have access to that schema


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
