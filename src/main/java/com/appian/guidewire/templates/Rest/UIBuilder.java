package com.appian.guidewire.templates.Rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.BooleanDisplayMode;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptorBuilder;
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
  protected List<PropertyDescriptor<?>> pathVarsUI = new ArrayList<>();
  protected OpenAPI openAPI = null;

  protected Paths paths;
  protected List<String> choicesForSearch = new ArrayList<>();
  protected List<Choice> defaultChoices = new ArrayList<>();


  protected SimpleIntegrationTemplate simpleIntegrationTemplate;
  protected SimpleConfiguration integrationConfiguration;

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

  public PropertyDescriptor<?>[] build() {

    // If no endpoint is selected, just build the api dropdown
    String selectedEndpoint = integrationConfiguration.getValue(CHOSEN_ENDPOINT);
    TextPropertyDescriptor searchBar = simpleIntegrationTemplate.textProperty(SEARCH)
        .label("Sort Endpoints Dropdown")
        .refresh(RefreshPolicy.ALWAYS)
        .instructionText("Sort the endpoints dropdown below with a relevant search query.")
        .placeholder("Example query for the Claims API: 'injury incidents.'")
        .build();
    List<PropertyDescriptor<?>> result = new ArrayList<>(Arrays.asList(searchBar, endpointChoiceBuilder()));
    if (selectedEndpoint == null) {
      return result.toArray(new PropertyDescriptor<?>[0]);
    }

    // If a user switched to another api after they selected an endpoint, set the endpoint and search to null
    // Else if a user selects api then a corresponding endpoint, update label and description accordingly
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
      result.add(simpleIntegrationTemplate.textProperty(REQ_BODY).label(KEY_OF_REQ_BODY).isHidden(true).build());

      // Building the result with path variables, request body, and other functionality needed to make the request
      buildRestCall(restOperation, result, pathName);
    }
    return result.toArray(new PropertyDescriptor<?>[0]);
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
              "properties respectively. Make sure to update these autogenerated properties before making the request.")
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

  public void buildRestCall(String restOperation, List<PropertyDescriptor<?>> result, String pathName) {
    setPathName(pathName);
    setPathVarsUI();
    if (getPathVarsUI().size() > 0) {
      result.addAll(getPathVarsUI());
    }

    switch (restOperation) {
      case (GET):
        buildGet(result);
        break;
      case (POST):
        buildPost(result);
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
    // result.add(simpleIntegrationTemplate.textProperty(PADDING).isReadOnly(true).label("").build());
    result.add(simpleIntegrationTemplate.integerProperty(PAGESIZE)
        .instructionText("Return 'n' number of items in the response. Default returns maximum number of resources allowed by " +
            "the endpoint.")
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
        if (hasSorting.get()) {
          result.add(sortedChoices
              .isRequired(integrationConfiguration.getValue(SORT_ORDER) != null).build());
          Choice[] sortOrder = {
              Choice.builder().name("Ascending").value("+").build(),
              Choice.builder().name("Descending").value("-").build()
          };
          result.add(simpleIntegrationTemplate.textProperty(SORT_ORDER)
              .label("Sort Order of Response")
              .choices(sortOrder)
              .isExpressionable(true)
              .isRequired(integrationConfiguration.getValue(SORT) != null)
              .displayHint(DisplayHint.NORMAL)
              .instructionText("Default sort order is ascending.")
              .refresh(RefreshPolicy.ALWAYS)
              .build()
          );
        }

        // If there are filtering options, add filtering UI
        if (hasFiltering.get()) {
          TextPropertyDescriptorBuilder filteringOperatorsBuilder = simpleIntegrationTemplate.textProperty(FILTER_OPERATOR)
              .instructionText("Select an operator to filter the results")
              .refresh(RefreshPolicy.ALWAYS)
              .isExpressionable(true);
          FILTERING_OPTIONS.entrySet().forEach(option -> {
            filteringOperatorsBuilder.choice(
                Choice.builder().name(option.getKey()).value(option.getValue()
            ).build());
          });

          // If any of the options are selected, the set will have more items than just null and the rest of the fields become
          // required
          Set<String> requiredSet = new HashSet<>(Arrays.asList(
              integrationConfiguration.getValue(FILTER_FIELD),
              integrationConfiguration.getValue(FILTER_OPERATOR),
              integrationConfiguration.getValue(FILTER_VALUE)
          ));
          boolean isRequired = requiredSet.size() > 1;

          // Add sorting fields to the UI
          result.add(filteredChoices.isRequired(isRequired).build());
          result.add(filteringOperatorsBuilder.isRequired(isRequired).build());
          result.add(simpleIntegrationTemplate.textProperty(FILTER_VALUE)
              .instructionText("Insert the query to filter the chosen field")
              .isRequired(isRequired)
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
            .displayHint(DisplayHint.NORMAL)
            .displayMode(BooleanDisplayMode.RADIO_BUTTON)
            .label(Util.camelCaseToTitleCase(includedNameStr))
            .isExpressionable(false)
            .description("Related Resource")
            .build());
      });

      result.add(simpleIntegrationTemplate.localTypeProperty(includedBuilder.build())
          .label("Included Resources")
          .displayHint(DisplayHint.NORMAL)
          .refresh(RefreshPolicy.ALWAYS)
          .instructionText("The resource you are requesting may have relationships to other resources. " +
              "Select the related resources below that you would like to be attached to the call. If " +
              "they exist, they will be returned alongside the root resource.")
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
