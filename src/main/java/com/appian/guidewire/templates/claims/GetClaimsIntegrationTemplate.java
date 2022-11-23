package com.appian.guidewire.templates.claims;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;
import com.google.common.io.Resources;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import std.ConstantKeys;




@TemplateId(name="GetClaimsIntegrationTemplate")
// Set template type to READ since this integration does not have side effects
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ)
public class GetClaimsIntegrationTemplate extends SimpleIntegrationTemplate implements ConstantKeys {

  @Override
  protected SimpleConfiguration getConfiguration(
    SimpleConfiguration integrationConfiguration,
    SimpleConfiguration connectedSystemConfiguration,
    PropertyPath propertyPath,
    ExecutionContext executionContext) {


    try (InputStream input = GetClaimsIntegrationTemplate.class.getClassLoader()
        .getResourceAsStream("com/appian/guidewire/templates/Policies.yaml")) {
      String content = IOUtils.toString(input, "utf-8");
/*      System.out.println(content);*/
      OpenAPI openAPI = new OpenAPIV3Parser().readContents(content).getOpenAPI();
      System.out.println(openAPI.getPaths());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

/*    try {
      String content = Resources.toString(Resources.getResource(OPENAPI_SAMPLE),
          StandardCharsets.UTF_8);
      System.out.println(content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }*/
/*    OpenAPI openAPI = new OpenAPIV3Parser().readContents("openapis/petstore3.yaml").getOpenAPI();*/



/*    OpenAPI openAPI = new OpenAPIV3Parser().readContents(content).getOpenAPI();
    Paths paths = openAPI.getPaths();*/

    TextPropertyDescriptor REST_CALL_TYPE = TextPropertyDescriptor.builder().key(REST_CALL).label(
        "Choose Integrations")
        .choices(
            Choice.builder().name("Get").value(GET).build(),
            Choice.builder().name("Post").value(POST).build(),
            Choice.builder().name("Patch").value(PATCH).build(),
            Choice.builder().name("Delete").value(DELETE).build()
        ).isExpressionable(true)
        .refresh(RefreshPolicy.ALWAYS)
        .build();


/*    List<String> places = Arrays.asList("Buenos Aires", "Córdoba", "La Plata");
    ArrayList<PropertyDescriptor> descriptorArrayList=new ArrayList<>();
    IntStream.range(0, places.size()).mapToObj(i ->
        descriptorArrayList.add(textProperty(places.get(i)).label(places.get(i)).build()
        )
    );
    LocalTypeDescriptor localPlaces = localType("local").properties(
        descriptorArrayList.stream().toArray(PropertyDescriptor[]::new)
    ).build();*/

    String[] choices = {"get", "post", "patch", "post"};
    if (integrationConfiguration.getValue("search") == null) {
      return integrationConfiguration.setProperties(
          textProperty("search").label("search")
              .isRequired(true)
              .refresh(RefreshPolicy.ALWAYS)
              .description("paths.toString()")
              .build()
/*          REST_CALL_TYPE*/
          );

/*      return integrationConfiguration.setProperties(
          REST_CALL_TYPE,
          ListTypePropertyDescriptor.builder()
          .key("testinglistproperty")
          .label("list property")
          .isExpressionable(true)
          .itemType(SystemType.EXPRESSION)
          .refresh(RefreshPolicy.ALWAYS)
          .build(),

          localTypeProperty(localPlaces, "local").label("local Places").build()
      );*/
    } else {
      String choice = integrationConfiguration.getValue("search");
      List<String> choiceFromArr = Arrays.stream(choices)
          .filter(c -> c.equals(choice))
          .collect(Collectors.toList());
      TextPropertyDescriptor chosen = TextPropertyDescriptor.builder()
          .key(REST_CALL)
          .label("Chosen Integration")
          .choices(
              Choice.builder().name(choiceFromArr.get(0)).value(choiceFromArr.get(0)).build()
          )
          .refresh(RefreshPolicy.ALWAYS)
          .isRequired(true)
          .build();

      return integrationConfiguration.setProperties(
          textProperty("search").label("search")
              .isRequired(true)
              .refresh(RefreshPolicy.ALWAYS)
              .description("This will be concatenated with the connected system text property on execute")
              .build(),
          chosen
      );
    }


/*    switch ((String)integrationConfiguration.getValue(REST_CALL)) {*/
  /*  switch ((String)integrationConfiguration.getValue("search")) {
      case GET:
        return integrationConfiguration.setProperties(
            REST_CALL_TYPE,
            textProperty("testingget").label("get")
                .isRequired(true)
                .description("This will be concatenated with the connected system text property on execute")
                .build());

      case POST:
        return integrationConfiguration.setProperties(
            REST_CALL_TYPE,
            textProperty("testingpost").label("post")
                .isRequired(true)
                .description("This will be concatenated with the connected system text property on execute")
                .build());

      case PATCH:
        return integrationConfiguration.setProperties(
            REST_CALL_TYPE,
            textProperty("testingpatch").label("patch")
                .isRequired(true)
                .description("This will be concatenated with the connected system text property on execute")
                .build());

      case DELETE:
        return integrationConfiguration.setProperties(
            REST_CALL_TYPE,
            textProperty("testingdelete").label("delete")
                .isRequired(true)
                .description("This will be concatenated with the connected system text property on execute")
                .build());

      default:
        return integrationConfiguration.setProperties(
            REST_CALL_TYPE);
    }*/

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
