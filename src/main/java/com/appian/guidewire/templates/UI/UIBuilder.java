package com.appian.guidewire.templates.UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptorBuilder;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TypeReference;
import com.appian.guidewire.templates.GuidewireCSP;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import std.ConstantKeys;
import std.Util;

public abstract class UIBuilder implements ConstantKeys {
  protected String api;
  protected String pathName;
  protected List<PropertyDescriptor<?>> pathVarsUI = new ArrayList<>();
  protected OpenAPI openAPI = null;

  protected Paths paths;
  protected List<String> choicesForSearch = new ArrayList<>();
  protected List<Choice> defaultChoices = new ArrayList<>();


  protected SimpleIntegrationTemplate simpleIntegrationTemplate;
  protected SimpleConfiguration integrationConfiguration;

  public abstract PropertyDescriptor<?>[] build();

  // Methods to implement when building out the API specific details of each request
  public abstract void buildRestCall(String restOperation, List<PropertyDescriptor<?>> result, String pathName);
  public abstract void buildGet(List<PropertyDescriptor<?>> result);
  public abstract void buildPost(List<PropertyDescriptor<?>> result);
  public abstract void buildPatch(List<PropertyDescriptor<?>> result);
  public abstract void buildDelete(List<PropertyDescriptor<?>> result);


  public UIBuilder(
      SimpleIntegrationTemplate simpleIntegrationTemplate,
      String api) {
    switch (api) {
      case POLICIES:
        this.openAPI = GuidewireCSP.policiesOpenApi;
        break;
      case CLAIMS:
        this.openAPI = GuidewireCSP.claimsOpenApi;
        break;
      case JOBS:
        this.openAPI = GuidewireCSP.jobsOpenApi;
        break;
      case ACCOUNTS:
        this.openAPI = GuidewireCSP.accountsOpenApi;
        break;
    }

    this.paths = openAPI.getPaths();
    this.api = api;
    this.simpleIntegrationTemplate = simpleIntegrationTemplate;
    setDefaultEndpoints();
  }


  public void setIntegrationConfiguration(SimpleConfiguration integrationConfiguration){
    this.integrationConfiguration = integrationConfiguration;
  }

  public void setPathName(String pathName) {
    this.pathName = pathName;
  }


  // Find all occurrences of variables inside path (ex. {claimId})
  protected void setPathVarsUI() {
    List<String> pathVars = Util.getPathVarsStr(pathName);
    pathVars.forEach(key -> {
      TextPropertyDescriptor ui = simpleIntegrationTemplate.textProperty(key)
          .instructionText("")
          .isRequired(true)
          .isExpressionable(true)
          .refresh(RefreshPolicy.ALWAYS)
          .placeholder("1")
          .label(Util.camelCaseToTitleCase(key))
          .build();
      pathVarsUI.add(ui);
    });
  }

  public List<PropertyDescriptor<?>> getPathVarsUI() {
    return pathVarsUI;
  }

  public void ReqBodyUIBuilder(List<PropertyDescriptor<?>> result, ObjectSchema schema) {
    LocalTypeDescriptor.Builder builder = simpleIntegrationTemplate.localType(REQ_BODY_PROPERTIES);
    schema.getProperties().get("attributes").getProperties().forEach((key, item) -> {
      Schema<?> itemSchema = (Schema<?>)item;
      String keyStr = key.toString();

      // Set for determining which properties to mark as required
      Set<?> requiredProperties =
          itemSchema.getRequired() != null ? new HashSet<>(itemSchema.getRequired()) : null;
      LocalTypeDescriptor property = parseRequestBody(keyStr, itemSchema, requiredProperties);
      if (property != null) {
        builder.properties(property.getProperties());
      }
    });

    result.add(simpleIntegrationTemplate.localTypeProperty(builder.build())
        .key(Util.removeSpecialCharactersFromPathName(pathName))
        .displayHint(DisplayHint.EXPRESSION)
        .isExpressionable(true)
        .label("Request Body")
        .description("Enter values to the properties below to send new or updated data to Guidewire. Not all properties are " +
            "required. Make sure to remove any unnecessary autogenerated properties.")
          .instructionText("Autogenerated properties are marked 'text', 'true', and '100' for string, boolean, and integer " +
              "properties, respectively. Make sure to update these autogenerated properties before making the request.")
        .refresh(RefreshPolicy.ALWAYS)
        .build());
  }

  public LocalTypeDescriptor parseRequestBody(String key, Schema<?> item, Set<?> requiredProperties) {

    LocalTypeDescriptor.Builder builder = simpleIntegrationTemplate.localType(key);
    if (item.getType().equals("object")) {

      if (item.getProperties() == null)
        return null;

      item.getProperties().forEach((innerKey, innerItem) -> {
        Schema<?> innerItemSchema = (Schema<?>)innerItem;
        Set<?> innerRequiredProperties = innerItemSchema.getRequired() != null ?
            new HashSet<>(innerItemSchema.getRequired()) :
            requiredProperties;
        LocalTypeDescriptor nested = parseRequestBody(innerKey, innerItemSchema, innerRequiredProperties);
        if (nested != null) {
          builder.properties(nested.getProperties());
        }
      });

      return simpleIntegrationTemplate.localType(key + "Builder")
          .properties(simpleIntegrationTemplate.localTypeProperty(
              builder.build()).refresh(RefreshPolicy.ALWAYS).build())
          .build();

    } else if (item.getType().equals("array")) {

      if (item.getItems() == null || item.getItems().getProperties() == null)
        return null;

      item.getItems().getProperties().forEach((innerKey, innerItem) -> {
        Schema<?> innerItemSchema = (Schema<?>)innerItem;
        Set<?> innerRequiredProperties = innerItemSchema.getRequired() != null ?
            new HashSet<>(innerItemSchema.getRequired()) :
            requiredProperties;
        LocalTypeDescriptor nested = parseRequestBody(innerKey, innerItemSchema, innerRequiredProperties);
        if (nested != null) {
          builder.properties(nested.getProperties());
        }
      });

      return simpleIntegrationTemplate.localType(key + "Builder").properties(
          // The listProperty needs a typeReference to a localProperty -> Below a localProperty is created
          // and hidden for use by the listProperty
          simpleIntegrationTemplate.localTypeProperty(builder.build())
              .key(key + "hidden")
              .isHidden(true)
              .refresh(RefreshPolicy.ALWAYS)
              .build(),
          simpleIntegrationTemplate.listTypeProperty(key)
              .refresh(RefreshPolicy.ALWAYS)
              .itemType(TypeReference.from(builder.build()))
              .build()).build();

    } else {

      // Base case: Create new property field depending on the type
      PropertyDescriptorBuilder<?> newProperty;
      switch(item.getType()) {
        case("boolean"):
          newProperty = simpleIntegrationTemplate.booleanProperty(key);
          break;
        case("integer"):
          newProperty = simpleIntegrationTemplate.integerProperty(key);
          break;
        default:
          newProperty = simpleIntegrationTemplate.textProperty(key);
          break;
      }

      String isRequired = requiredProperties != null && requiredProperties.contains(key) ?
          "(Required) " + item.getDescription() :
          item.getDescription();
      return simpleIntegrationTemplate.localType(key + "Container")
          .property(newProperty
              .isExpressionable(true)
              .displayHint(DisplayHint.EXPRESSION)
              .refresh(RefreshPolicy.ALWAYS)
              .placeholder(isRequired)
              .build())
          .build();
    }
  }

  // Runs on initialization to set the default paths for the dropdown as well as a list of strings of choices used for
  // sorting when a query is entered
  public void setDefaultEndpoints() {
    // Build search choices when no search query has been entered
    // Check if rest call exists on path and add each rest call of path to list of choices
    Map<String,Operation> operations = new HashMap<>();

    paths.forEach((pathName, path) -> {
      if (PATHS_TO_REMOVE.contains(pathName))
        return;

      operations.put(GET, path.getGet());
      operations.put(POST, path.getPost());
      operations.put(PATCH, path.getPatch());
      operations.put(DELETE, path.getDelete());

      operations.forEach((restType, restOperation) -> {
        if (restOperation != null) {
          String name = restType + " - " + restOperation.getSummary();
          String value = api + ":" + restType + ":" + pathName + ":" + restOperation.getSummary();

          // Builds up choices for search on initial run with all paths
          choicesForSearch.add(value);

          // Choice UI built
          defaultChoices.add(Choice.builder().name(name).value(value).build());
        }
      });
    });
  }

  // Sets the choices for endpoints with either default choices or sorted choices based off of the search query
  public TextPropertyDescriptor endpointChoiceBuilder() {

    // If there is a search query, sort the dropdown with the query
    String searchQuery = integrationConfiguration.getValue(SEARCH);
    ArrayList<Choice> choices = new ArrayList<>();
    if (searchQuery != null && !searchQuery.equals("") && !choicesForSearch.isEmpty()) {
      List<ExtractedResult> extractedResults = FuzzySearch.extractSorted(searchQuery, choicesForSearch);
      extractedResults.forEach(choice -> {
        String restType = choice.getString().split(":")[1];
        String restOperation = choice.getString().split(":")[3];
        choices.add(
            Choice.builder().name(restType + " - " + restOperation).value(choice.getString()).build());
      });
    }

    // If an endpoint is selected, the instructionText will update to the REST call and path name
    Object chosenEndpoint = integrationConfiguration.getValue(CHOSEN_ENDPOINT);
    String instructionText = chosenEndpoint != null ?
        chosenEndpoint.toString().split(":")[1] + "  " + chosenEndpoint.toString().split(":")[2] :
        "";
    return simpleIntegrationTemplate.textProperty(CHOSEN_ENDPOINT)
        .isRequired(true)
        .refresh(RefreshPolicy.ALWAYS)
        .label("Select Endpoint")
        .transientChoices(true)
        .instructionText(instructionText)
        .choices(choices.size() > 0 ? choices.toArray(new Choice[0]) : defaultChoices.toArray(new Choice[0]))
        .build();
  }

}
