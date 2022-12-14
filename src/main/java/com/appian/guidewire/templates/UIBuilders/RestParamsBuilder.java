package com.appian.guidewire.templates.UIBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.templateframework.sdk.configuration.BooleanDisplayMode;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor.TextPropertyDescriptorBuilder;
import com.appian.connectedsystems.templateframework.sdk.configuration.TypeReference;
import com.appian.guidewire.templates.GuidewireCSP;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import std.ConstantKeys;

public class RestParamsBuilder implements ConstantKeys {
  protected String pathName;
  protected TextPropertyDescriptorBuilder endpointChoices;
  protected List<PropertyDescriptor> pathVarsUI = new ArrayList<>();
  protected Paths openAPIPaths = null;
  protected OpenAPI openAPI = null;

  protected SimpleIntegrationTemplate simpleIntegrationTemplate;

  public RestParamsBuilder(String api, SimpleIntegrationTemplate simpleIntegrationTemplate) {
    super();

    this.simpleIntegrationTemplate = simpleIntegrationTemplate;
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
  }

  public void setPathName(String pathName) {
    this.pathName = pathName;
    setPathVarsUI();
  }

  public String getPathName(String pathName) {
    return this.pathName;
  }

  public TextPropertyDescriptorBuilder setEndpointChoices(TextPropertyDescriptorBuilder endpointChoices) {
    this.endpointChoices = endpointChoices;
    return endpointChoices;
  }

  public TextPropertyDescriptorBuilder getEndpointChoices() {
    return this.endpointChoices;
  }

  public static List<String> getPathVarsStr(String pathName) {
    Matcher m = Pattern.compile("[^{*}]+(?=})").matcher(pathName);
    List<String> pathVars = new ArrayList<>();

    while (m.find()) {
      pathVars.add(m.group());
    }
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

  public List<Map<String,Object>> buildRequestBodyUI() {

    if (openAPI.getPaths().get(pathName).getPost().getRequestBody() == null)
      return null;

    ObjectSchema schema = (ObjectSchema)openAPI.getPaths()
        .get(pathName)
        .getPost()
        .getRequestBody()
        .getContent()
        .get("application/json")
        .getSchema()
        .getProperties()
        .get("data");

    List<Map<String,Object>> reqBodyArr = new ArrayList<>();
    schema.getProperties().get("attributes").getProperties().forEach((key, item) -> {
      Map<String,Object> newParam = parseRequestBody(key, (Schema)item);
      if (newParam != null)
        reqBodyArr.add(newParam);
    });

    return reqBodyArr;
  }

  public Map<String,Object> parseRequestBody(Object key, Schema item) {

    if (item.getType().equals("object")) {

      if (item.getProperties() == null)
        return null;

      List<Object> objBuilder = new ArrayList<>();

      item.getProperties().forEach((innerKey, innerItem) -> {
        TextPropertyDescriptor newParam = (TextPropertyDescriptor)parseRequestBody(innerKey,
            (Schema)innerItem).get(TEXT);
        if (newParam != null && newParam instanceof TextPropertyDescriptor) {
          objBuilder.add(newParam);
        }
      });

      Map<String,Object> objMap = new HashMap<>();
      objMap.put(OBJECT, simpleIntegrationTemplate.localType(key.toString())
          .properties(objBuilder.toArray(new PropertyDescriptor[0]))
          .build());
      return objMap;

    } else if (item.getType().equals("array")) {

      if (item.getItems() == null && item.getItems().getProperties() == null)
        return null;

      List<Object> arrBuilder = new ArrayList<>();
      item.getItems().getProperties().forEach((innerKey, innerItem) -> {
        TextPropertyDescriptor newParam = (TextPropertyDescriptor)parseRequestBody(innerKey,
            (Schema)innerItem).get(TEXT);
        if (newParam != null && newParam instanceof TextPropertyDescriptor) {
          arrBuilder.add(newParam);
        }
      });

      Map<String,Object> arrMap = new HashMap<>();
      arrMap.put(ARRAY, this.simpleIntegrationTemplate.localType(key.toString())
          .properties(arrBuilder.toArray(new PropertyDescriptor[0]))
          .build()

      );
      return arrMap;

    } else {
      /*      System.out.println(key + " : " + item.getType());*/

      Map<String,Object> textMap = new HashMap<>();
      textMap.put(TEXT, this.simpleIntegrationTemplate.textProperty(key.toString())
          .instructionText(item.getDescription())
          .isExpressionable(true)
          .displayHint(DisplayHint.EXPRESSION)
          .refresh(RefreshPolicy.ALWAYS)
          .placeholder(item.getDescription())
          .isRequired(true)
          .build());
      return textMap;
    }

  }

  public void buildRestCall(String restOperation, List<PropertyDescriptor> result) {
    if (getPathVarsUI().size() > 0) {
      result.addAll(getPathVarsUI());
    }

    switch (restOperation) {
      case (POST):
        buildPost(result);
        break;
      case (GET):
        buildGet(result);
        break;
    }

  }

  public void buildGet(List<PropertyDescriptor> result) {

    Operation get = openAPIPaths.get(pathName).getGet();

    // Filtering and Sorting
    Map returnedFieldProperties = get.getResponses()
        .get("200")
        .getContent()
        .get("application/json")
        .getSchema()
        .getProperties();

    result.add(
        simpleIntegrationTemplate
            .integerProperty(PAGESIZE)
            .instructionText("Return 'n' number of items in the response")
            .label("Pagination")
            .placeholder("25")
            .build()
    );

    if (returnedFieldProperties != null) {
      Schema returnedFieldItems= ((Schema)returnedFieldProperties.get("data")).getItems();
      if (returnedFieldItems!= null) {

        TextPropertyDescriptorBuilder sorted = simpleIntegrationTemplate.textProperty(SORT)
            .label("Sort Response")
            .instructionText("Sort response by selecting a field in the dropdown. If the dropdown is empty," +
                " there are no sortable fields available.")
            .refresh(RefreshPolicy.ALWAYS);
        Map returnedFields = ((Schema)returnedFieldItems
            .getProperties()
            .get("attributes"))
            .getProperties();
        returnedFields.forEach((key, val) -> {
          Map extensions = ((Schema)val).getExtensions();
          if (extensions != null && extensions.get("x-gw-extensions") instanceof LinkedHashMap) {
            Object isFilterable = ((LinkedHashMap<?,?>)extensions.get("x-gw-extensions")).get("filterable");
            Object isSortable = ((LinkedHashMap<?,?>)extensions.get("x-gw-extensions")).get("sortable");
            if (isFilterable != null) {
              System.out.println(key + " is filterable");
            }
            if (isSortable != null) {
              sorted.choice(
                  Choice.builder().name(key.toString()).value(key.toString()).build()
              );
              System.out.println(key + " is sortable");
            } else {
              System.out.println("KEY "+ key);
            }
          }
        });
        result.add(sorted.build());
      }

    }

    // Included resources
    Schema hasIncludedResources = ((Schema)get.getResponses()
        .get("200")
        .getContent()
        .get("application/json")
        .getSchema()
        .getProperties()
        .get("included"));
    if (hasIncludedResources != null) {
      Set included = hasIncludedResources.getProperties().keySet();
      System.out.println(included);

      result.add(
          simpleIntegrationTemplate
              .textProperty("IncludedResourcesTitle")
              .isReadOnly(true)
              .refresh(RefreshPolicy.ALWAYS)
              .label("Included Resources")
              .instructionText("The resource you are requesting may have relationships to other resources. " +
                  "Select the related resources below that you would like to be attached to the call. If " +
                  "they exist, they will be returned alongside the root resource.")
              .build()
          );
      included.forEach(includedName -> {
            result.add(
                simpleIntegrationTemplate
                    .booleanProperty(includedName.toString())
                    .refresh(RefreshPolicy.ALWAYS)
                    .displayMode(BooleanDisplayMode.RADIO_BUTTON)
                    .label(includedName.toString())
                    .description("Related Resource")
                    .build()
            );
          }
      );
    }


  }

  public void buildPost(List<PropertyDescriptor> result) {

    List<Map<String,Object>> reqBodyArr = buildRequestBodyUI();

    if (reqBodyArr == null) return;

    LocalTypeDescriptor.Builder reqBody = simpleIntegrationTemplate.localType(REQ_BODY_PROPERTIES);
    reqBodyArr.forEach(field -> {
      if (field.containsKey(TEXT) && field.get(TEXT) instanceof TextPropertyDescriptor) {
        TextPropertyDescriptor textParam = (TextPropertyDescriptor)field.get(TEXT);
        reqBody.properties(textParam);
      } else if (field.containsKey(OBJECT) && field.get(OBJECT) instanceof LocalTypeDescriptor) {
        LocalTypeDescriptor objParam = (LocalTypeDescriptor)field.get(OBJECT);
        reqBody.properties(
            simpleIntegrationTemplate.localTypeProperty(objParam).refresh(RefreshPolicy.ALWAYS).build());
      } else if (field.containsKey(ARRAY) && field.get(ARRAY) instanceof LocalTypeDescriptor) {
        LocalTypeDescriptor arrParam = (LocalTypeDescriptor)field.get(ARRAY);
        reqBody.properties(simpleIntegrationTemplate.listTypeProperty(arrParam.getName())
            .refresh(RefreshPolicy.ALWAYS)
            .itemType(TypeReference.from(arrParam))
            .build(), simpleIntegrationTemplate.localTypeProperty(arrParam)
            .key(arrParam.getName() + "hidden")
            .isHidden(true)
            .refresh(RefreshPolicy.ALWAYS)
            .build());
      }
    });

    // Key can't have any special characters so to get reqBody key, get it from integrationTemplate.get
    // (REQ_BODY).getLabel()
    String key = pathName.replace("/", "").replace("{", "").replace("}", "");
    result.add(
        simpleIntegrationTemplate.localTypeProperty(reqBody.build())
            .key(key)
            .isHidden(false)
            .displayHint(DisplayHint.EXPRESSION)
            .isExpressionable(true)
            .label("Request Body")
            .refresh(RefreshPolicy.ALWAYS)
            .build()
    );
  }

  public void buildPatch(List<PropertyDescriptor> result) {

  }

  public void buildDelete(List<PropertyDescriptor> result) {

  }
}
