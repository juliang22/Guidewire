package com.appian.guidewire.templates.Rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
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
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import std.ConstantKeys;
import std.Util;

public class UIBuilder implements ConstantKeys {
  protected String api;
  protected String pathName;
  protected TextPropertyDescriptor endpointChoices;
  protected List<PropertyDescriptor<?>> pathVarsUI = new ArrayList<>();
  protected OpenAPI openAPI = null;

  protected Paths paths;
  protected List<String> choicesForSearch = new ArrayList<>();

  protected SimpleIntegrationTemplate simpleIntegrationTemplate;
  protected SimpleConfiguration integrationConfiguration;

  public UIBuilder(
      SimpleConfiguration integrationConfiguration,
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
    if (paths == null) {
      integrationConfiguration.setErrors(
          new ArrayList<>(Collections.singletonList("Problem loading OpenAPI Specification")));
    }

    this.api = api;
    this.simpleIntegrationTemplate = simpleIntegrationTemplate;
    this.integrationConfiguration = integrationConfiguration;
    this.endpointChoices = endpointChoiceBuilder();
  }

  public PropertyDescriptor<?>[] build() {

    // If no endpoint is selected, just build the api dropdown
    String selectedEndpoint = integrationConfiguration.getValue(CHOSEN_ENDPOINT);
    List<PropertyDescriptor<?>> result = new ArrayList<>(Arrays.asList(SEARCHBAR, endpointChoiceBuilder()));
    if (selectedEndpoint == null) {
      return result.toArray(new PropertyDescriptor<?>[0]);
    }

    // If a user switched to another api after they selected an endpoint, set the endpoint and search to null
    // If a user selects api then a corresponding endpoint, update label and description accordingly
    String[] selectedEndpointStr = selectedEndpoint.split(":");
    String apiType = selectedEndpointStr[0];
    String restOperation = selectedEndpointStr[1];
    String pathName = selectedEndpointStr[2];
    String pathSummary = selectedEndpointStr[3];
    if (!apiType.equals(api)) {
      integrationConfiguration.setValue(CHOSEN_ENDPOINT, null).setValue(SEARCH, "");
    } else {
      // The key of the request body is dynamic so when I need to get it in the execute function:
      // key = integrationConfiguration.getProperty(REQ_BODY).getLabel();
      // integrationConfiguration.getProperty(key)
      // TODO: put below in buildRestCall()
      String KEY_OF_REQ_BODY = Util.removeSpecialCharactersFromPathName(pathName);
      result.add(
          simpleIntegrationTemplate.textProperty(REQ_BODY).label(KEY_OF_REQ_BODY).isHidden(true).build());

      // Building the result with path variables, request body, and other functionality needed to make the
      // request
      buildRestCall(restOperation, result, pathName);
    }
    return result.toArray(new PropertyDescriptor[0]);
  }

  public void setPathName(String pathName) {
    this.pathName = pathName;
  }

  public static List<String> getPathVarsStr(String pathName) {
    Matcher m = Pattern.compile("[^{*}]+(?=})").matcher(pathName);
    List<String> pathVars = new ArrayList<>();

    while (m.find()) {
      pathVars.add(m.group());
    }
    return pathVars;
  }

  // Find all occurrences of variables inside path (ex. {claimId})
  protected void setPathVarsUI() {
    List<String> pathVars = getPathVarsStr(pathName);
    pathVars.forEach(key -> {
      TextPropertyDescriptor ui = simpleIntegrationTemplate.textProperty(key)
          .instructionText("")
          .isRequired(true)
          .isExpressionable(true)
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

    String key = pathName.replace("/", "").replace("{", "").replace("}", "");
    result.add(simpleIntegrationTemplate.localTypeProperty(builder.build())
        .key(key)
        .displayHint(DisplayHint.EXPRESSION)
        .isExpressionable(true)
        .label("Request Body")
        .description("description")
        .instructionText("instruction text")
        .refresh(RefreshPolicy.ALWAYS)
        .build());
  }

  public LocalTypeDescriptor parseRequestBody(String key, Schema<?> item, Set<?> requiredProperties) {

    if (item.getType().equals("object")) {

      if (item.getProperties() == null)
        return null;

      LocalTypeDescriptor.Builder builder = simpleIntegrationTemplate.localType(key);
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
          .properties(simpleIntegrationTemplate.localTypeProperty(builder.build()).build())
          .build();

    } else if (item.getType().equals("array")) {

      if (item.getItems() == null || item.getItems().getProperties() == null)
        return null;

      LocalTypeDescriptor.Builder builder = simpleIntegrationTemplate.localType(key);
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
              .build(), simpleIntegrationTemplate.listTypeProperty(key)
              .itemType(TypeReference.from(builder.build()))
              .build()).build();

    } else {
      /*      System.out.println(key + " : " + item.getType());*/

      return simpleIntegrationTemplate.localType(key + "Container")
          .property(simpleIntegrationTemplate.textProperty(key)
              .isExpressionable(true)
              .displayHint(DisplayHint.EXPRESSION)
              .refresh(RefreshPolicy.ALWAYS)
              .placeholder(requiredProperties != null && requiredProperties.contains(key) ?
                  "(Required) " + item.getDescription() :
                  item.getDescription())
              .build())
          .build();
    }
  }

  // Parse through OpenAPI yaml and return all endpoints as Choice for dropdown
  public TextPropertyDescriptor endpointChoiceBuilder() {

    // If there is a search query, sort the dropdown with the query
    String searchQuery = integrationConfiguration.getValue(SEARCH);
    ArrayList<Choice> choices = new ArrayList<>();

    // Build search choices when search query has been entered
    if (searchQuery != null && !searchQuery.equals("") && !choicesForSearch.isEmpty()) {
      List<ExtractedResult> extractedResults = FuzzySearch.extractSorted(searchQuery, choicesForSearch);
      extractedResults.forEach(choice -> {

        String restType = choice.getString().split(":")[1];
        String restOperation = choice.getString().split(":")[3];
        choices.add(
            Choice.builder().name(restType + " - " + restOperation).value(choice.getString()).build());
      });
    } else { // Build search choices when no search query has been entered
      // Check if rest call exists on path and add each rest call of path to list of choices
      Map<String,Operation> operations = new HashMap<>();

      paths.forEach((pathName, path) -> {
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
            choices.add(Choice.builder().name(name).value(value).build());
          }
        });
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
        .choices(choices.toArray(new Choice[0]))
        .build();
  }

  public void buildRestCall(String restOperation, List<PropertyDescriptor<?>> result, String pathName) {
    setPathName(pathName);
    setPathVarsUI();
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
      case (PATCH):
        buildPatch(result);
        break;
      case (DELETE):
        buildDelete(result);
    }

  }

  public void buildGet(List<PropertyDescriptor<?>> result) {

    Operation get = paths.get(pathName).getGet();

    // Pagination
    result.add(simpleIntegrationTemplate.textProperty(PADDING).isReadOnly(true).label("").build());
    result.add(simpleIntegrationTemplate.integerProperty(PAGESIZE)
        .instructionText("Return 'n' number of items in the response")
        .label("Pagination")
        .isExpressionable(true)
        .placeholder("25")
        .build());

    // Filtering and Sorting
    Map<?,?> returnedFieldProperties = get.getResponses()
        .get("200")
        .getContent()
        .get("application/json")
        .getSchema()
        .getProperties();
    AtomicBoolean hasSorting = new AtomicBoolean(false);
    AtomicBoolean hasFiltering = new AtomicBoolean(false);
    if (returnedFieldProperties != null) {
      Schema<?> returnedFieldItems = ((Schema<?>)returnedFieldProperties.get("data")).getItems();
      if (returnedFieldItems != null) {

        // Building up sorting and filtering options
        TextPropertyDescriptorBuilder sortedChoices = simpleIntegrationTemplate.textProperty(SORT)
            .label("Sort Response")
            .instructionText("Sort response by selecting a field in the dropdown. If the dropdown is empty," +
                " there are no sortable fields available.")
            .isExpressionable(true)
            .refresh(RefreshPolicy.ALWAYS);
        TextPropertyDescriptorBuilder filteredChoices = simpleIntegrationTemplate.textProperty(FILTER_FIELD)
            .label("Filter Response")
            .instructionText("Filter response by selecting a field in the dropdown. If the dropdown is " +
                "empty, there are no filterable fields available.")
            .isExpressionable(true)
            .refresh(RefreshPolicy.ALWAYS);

        // Parsing to find filtering and sorting options available on the call
        Map<?,?> returnedFields = returnedFieldItems.getProperties().get("attributes").getProperties();
        returnedFields.forEach((key, val) -> {
          Map<?,?> extensions = ((Schema<?>)val).getExtensions();
          if (extensions != null && extensions.get("x-gw-extensions") instanceof LinkedHashMap) {

            Object isFilterable = ((LinkedHashMap<?,?>)extensions.get("x-gw-extensions")).get("filterable");
            if (isFilterable != null) {
              /*              System.out.println(key + " is filterable");*/
              filteredChoices.choice(Choice.builder().name(key.toString()).value(key.toString()).build());
              hasFiltering.set(true);
            }

            Object isSortable = ((LinkedHashMap<?,?>)extensions.get("x-gw-extensions")).get("sortable");
            if (isSortable != null) {
              sortedChoices.choice(Choice.builder().name(key.toString()).value(key.toString()).build());
              hasSorting.set(true);
            }
          }
        });
        // If there are sorting options, add sorting UI
        if (hasSorting.get())
          result.add(sortedChoices.build());

        // If there are filtering options, add filtering UI
        if (hasFiltering.get()) {
          TextPropertyDescriptorBuilder filteringOperatorsBuilder = simpleIntegrationTemplate.textProperty(
                  FILTER_OPERATOR)
              .instructionText("Select an operator to filter the results")
              .refresh(RefreshPolicy.ALWAYS)
              .isExpressionable(true);
          FILTERING_OPTIONS.forEach(option -> filteringOperatorsBuilder.choice(
              Choice.builder().name(option).value(option).build()));
          result.add(filteredChoices.build());
          result.add(filteringOperatorsBuilder.build());
          result.add(simpleIntegrationTemplate.textProperty(FILTER_VALUE)
              .instructionText("Insert the query to filter the chosen field")
              .refresh(RefreshPolicy.ALWAYS)
              .isExpressionable(true)
              .refresh(RefreshPolicy.ALWAYS)
              .placeholder("22")
              .build());
        }
      }

    }

    // Included resources
    Schema<?> hasIncludedResources = ((Schema<?>)get.getResponses()
        .get("200")
        .getContent()
        .get("application/json")
        .getSchema()
        .getProperties()
        .get("included"));
    if (hasIncludedResources != null) {
      Set<?> included = hasIncludedResources.getProperties().keySet();

      LocalTypeDescriptor.Builder includedBuilder = simpleIntegrationTemplate.localType(
          Util.removeSpecialCharactersFromPathName(pathName) + INCLUDED_RESOURCES);
      included.forEach(includedName -> {
        String includedNameStr = includedName.toString();
        includedBuilder.property(simpleIntegrationTemplate.booleanProperty(includedNameStr)
            .refresh(RefreshPolicy.ALWAYS)
            .displayMode(BooleanDisplayMode.RADIO_BUTTON)
            .label(Util.camelCaseToTitleCase(includedNameStr))
            .description("Related Resource")
            .refresh(RefreshPolicy.ALWAYS)
            .build());
      });

      result.add(simpleIntegrationTemplate.localTypeProperty(includedBuilder.build())
          .label("Included Resources")
          .displayHint(DisplayHint.NORMAL)
          .refresh(RefreshPolicy.ALWAYS)
          .refresh(RefreshPolicy.ALWAYS)
          .instructionText("The resource you are requesting may have relationships to other resources. " +
              "Select the related resources below that you would like to be attached to the call. If " +
              "they exist, they will be returned alongside the root resource.")
          .isExpressionable(true)
          .build());
    }

  }

  public void buildPost(List<PropertyDescriptor<?>> result) {

    if (paths.get(pathName).getPost().getRequestBody() == null) {
      result.add(NO_REQ_BODY_UI);
      return;
    }

    ObjectSchema schema = (ObjectSchema)paths.get(pathName)
        .getPost()
        .getRequestBody()
        .getContent()
        .get("application/json")
        .getSchema()
        .getProperties()
        .get("data");

    ReqBodyUIBuilder(result, schema);

  }

  public void buildPatch(List<PropertyDescriptor<?>> result) {

    if (paths.get(pathName).getPatch().getRequestBody() == null) {
      result.add(NO_REQ_BODY_UI);
      return;
    }

    ObjectSchema schema = (ObjectSchema)paths.get(pathName)
        .getPatch()
        .getRequestBody()
        .getContent()
        .get("application/json")
        .getSchema()
        .getProperties()
        .get("data");

    ReqBodyUIBuilder(result, schema);
  }

  public void buildDelete(List<PropertyDescriptor<?>> result) {
  }

}