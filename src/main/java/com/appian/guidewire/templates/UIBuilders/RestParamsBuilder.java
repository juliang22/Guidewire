package com.appian.guidewire.templates.UIBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.ConfigurableTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.ListTypePropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypePropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptorBuilder;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor.TextPropertyDescriptorBuilder;
import com.appian.connectedsystems.templateframework.sdk.configuration.TypeReference;
import com.appian.guidewire.templates.GuidewireCSP;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import std.ConstantKeys;

public class RestParamsBuilder extends ConfigurableTemplate implements ConstantKeys {



  protected String pathName;
  protected TextPropertyDescriptorBuilder endpointChoices;
  protected List<PropertyDescriptor> pathVarsUI = new ArrayList<>();
  protected PropertyDescriptor reqBodyProperties = null;
  protected Paths openAPIPaths = null;
  protected OpenAPI openAPI = null;


  public RestParamsBuilder(String api) {
    super();

    switch (api) {
      case POLICIES:
        this.openAPI = GuidewireCSP.policiesOpenApi;
        this.openAPIPaths = GuidewireCSP.policiesOpenApi.getPaths();
        this.endpointChoices = GuidewireCSP.policies;
        break;
      case CLAIMS:
        this.openAPI = GuidewireCSP.claimsOpenApi;
        this.openAPIPaths = GuidewireCSP.claimsOpenApi.getPaths();
        this.endpointChoices = GuidewireCSP.claims;
        break;
/*        case JOBS:
        this.openAPIPaths = GuidewireCSP.jobsOpenApi.getPaths();
        this.endpointChoices = GuidewireCSP.jobs;
          break;*/
    }

/*    this.pathVarsUI = setPathVarsUI();*/
  }


  public void setPathName(String pathName) {
    this.pathName = pathName;
    setPathVarsUI();
  };
  public String getPathName(String pathName) { return this.pathName; }


  public TextPropertyDescriptorBuilder setEndpointChoices(TextPropertyDescriptorBuilder endpointChoices)  {
    this.endpointChoices = endpointChoices;
    return endpointChoices;
  }
  public TextPropertyDescriptorBuilder getEndpointChoices() {return this.endpointChoices; };


  public void setReqBodyProperties(PropertyDescriptor reqBodyProperties) {
    this.reqBodyProperties = reqBodyProperties;
  }

  public PropertyDescriptor getReqBodyProperties() {
    return reqBodyProperties;
  }

  public static List<String> getPathVarsStr(String pathName) {
    Matcher m = Pattern.compile("[^{*}]+(?=})").matcher(pathName);

    // TODO: This logic needs to be in it's own function so I can access the start/end index of the
    //  variableName from the execute function to build the final path to call
    // Map of variableName to [startIndex, endIndex] of the variable's location in the path string
    List<String> pathVars = new ArrayList<>();
    while (m.find()) {pathVars.add(m.group());}
    return pathVars;
  }

  protected void setPathVarsUI() {
    // Find all occurrences of variables inside path (ex. {claimId})

    List<String> pathVars = getPathVarsStr(pathName);
    pathVars.forEach(key -> {
      TextPropertyDescriptor ui = TextPropertyDescriptor.builder()
          .key(key)
          .instructionText("")
          .isRequired(true)
          .isExpressionable(true)
          .label(key)
          .build();
      pathVarsUI.add(ui);
    });
  }

  public List<PropertyDescriptor> getPathVarsUI() { return pathVarsUI; }






  public List<PropertyDescriptor> buildGet() {
    return pathVarsUI;

  }

  public void buildPost(SimpleIntegrationTemplate simpleIntegrationTemplate) {

    List<Map<String,Object>> reqBodyArr = ParseOpenAPI.buildRequestBodyUI(this.openAPI, this.pathName);

    if (reqBodyArr == null) {
      setReqBodyProperties(null);
      return;
    }

    LocalTypeDescriptor.Builder reqBody = simpleIntegrationTemplate.localType(REQ_BODY_PROPERTIES);
    reqBodyArr.forEach(field -> {
      if (field.containsKey(TEXT) && field.get(TEXT) instanceof TextPropertyDescriptor) {
        TextPropertyDescriptor textParam = (TextPropertyDescriptor)field.get(TEXT);
        reqBody.properties(textParam);
      } else if (field.containsKey(OBJECT) && field.get(OBJECT) instanceof LocalTypeDescriptor) {
        LocalTypeDescriptor objParam = (LocalTypeDescriptor)field.get(OBJECT);
        reqBody.properties(simpleIntegrationTemplate.localTypeProperty(objParam).build());
      } else if (field.containsKey(ARRAY) && field.get(ARRAY) instanceof LocalTypeDescriptor) {
        LocalTypeDescriptor arrParam = (LocalTypeDescriptor)field.get(ARRAY);
        reqBody.properties(
            simpleIntegrationTemplate.listTypeProperty(arrParam.getName()).itemType(TypeReference.from(arrParam)).build(),
            simpleIntegrationTemplate.localTypeProperty(arrParam).isHidden(true).build()
        );
      }
    });
    setReqBodyProperties(
        simpleIntegrationTemplate.localTypeProperty(reqBody.build()).
            key(REQ_BODY).
            isHidden(false).
            displayHint(DisplayHint.EXPRESSION).
            isExpressionable(true)
            .label("QnA")
            .build()
    );
  }

  public List<PropertyDescriptor> buildPatch() {
    return pathVarsUI;
  }

  public List<PropertyDescriptor>  buildDelete() {
    return pathVarsUI;
  }
}
