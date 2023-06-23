package com.appian.guidewire.templates.UI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.BooleanDisplayMode;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.guidewire.templates.apis.GuidewireIntegrationTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import std.ConstantKeys;
import std.HTTP;
import std.Util;

public class GuidewireUIBuilder extends UIBuilder {



  public GuidewireUIBuilder(
      GuidewireIntegrationTemplate simpleIntegrationTemplate,
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      PropertyPath propertyPath) throws JsonProcessingException {

    super(simpleIntegrationTemplate, integrationConfiguration, connectedSystemConfiguration, propertyPath);
    setApi(connectedSystemConfiguration.getValue(API_TYPE));
  }


  public SimpleConfiguration build() throws IOException {
    List<PropertyDescriptor<?>> properties = new ArrayList<>(); // build properties to pass into .setProperties()
    Map<String, String> values = new HashMap<>(); // build values to pass into .setValues()


    TextPropertyDescriptor.TextPropertyDescriptorBuilder subApiChoicesUI = simpleIntegrationTemplate.textProperty(SUB_API_TYPE)
        .label("Guidewire Module")
        .instructionText("Select the Guidewire module to access within " + Util.camelCaseToTitleCase(connectedSystemConfiguration.getValue(API_TYPE)))
        .isRequired(true)
        .refresh(RefreshPolicy.ALWAYS);

    // On initial load, get list of subApis modules and set a dropdown property
    if (integrationConfiguration.getProperty(SUB_API_TYPE) == null) {
      // Get list of available subApis and their information map
      String rootUrl = connectedSystemConfiguration.getValue(ROOT_URL);
      Map<String, Object> initialResponse = HTTP.testAuth(connectedSystemConfiguration, rootUrl + "/rest/apis");
      if (initialResponse == null || initialResponse.containsKey("error")) {
        integrationConfiguration.setErrors(Arrays.asList("Error in connected system. Please verify that your authentication credentials are correct"));
      }

      Map<String,Map<String,String>> subApiList = objectMapper.readValue(initialResponse.get("result").toString(), Map.class);
      for (Map.Entry<String,Map<String,String>> subApiProperties : subApiList.entrySet()) {
        Map<String,String> subAPIInfoMap = subApiProperties.getValue();

        // Filter out all unusable swagger files
        Pattern pattern = Pattern.compile("/system/|/event/|/apis|/composite");
        Matcher matcher = pattern.matcher(subAPIInfoMap.get("basePath"));
        if (matcher.find()) continue;

        // Build and save map of all the subApi module choices with value of a map of all the subApi info
        subAPIInfoMap.put(SUB_API_KEY, subAPIInfoMap.get("title").replace(" ", ""));
        subAPIInfoMap.put("docs", subAPIInfoMap.get("docs").replace("swagger", "openapi"));
        String subAPIInfoMapStr = objectMapper.writeValueAsString(subAPIInfoMap);
        subApiChoicesUI.choice(Choice.builder().name(subAPIInfoMap.get("title")).value(subAPIInfoMapStr).build());
      }
      properties.add(subApiChoicesUI.build());
      return setPropertiesAndValues(properties, values);
    }

    // If the subAPI module has not been selected, retrieve list of subApis and only render the subAPI dropdown
    properties.add(integrationConfiguration.getProperty(SUB_API_TYPE));
    String subAPIInfoMapStr = integrationConfiguration.getValue(SUB_API_TYPE);
    if (subAPIInfoMapStr == null)  {
      return setPropertiesAndValues(properties, values);
    }

    // User has selected subAPI module. Set subApi and get OpenAPI spec/info either from memory or from endpoint.
    // Load swagger from memory (user searching or selecting another subapi module) or get swagger file of the subApi from
    // guidewire and save it in a hidden property. Property is transient and will not save permanently after saving the
    // integration object to conserve memory. Reopening integration will trigger another api call to get the spec.

    // User selected subApi module on first run, refresh, or user selected another subapi module
    TextPropertyDescriptor openAPIInfo = simpleIntegrationTemplate.textProperty(OPENAPI_INFO)
        .transientChoices(true)
        .isHidden(true)
        .build();
    properties.add(openAPIInfo);

    Map<String,String> subAPIInfoMap =  objectMapper.readValue(subAPIInfoMapStr, Map.class);
    setSubApi(subAPIInfoMap.get(SUB_API_KEY));
    String swaggerInfoMapAsStr = integrationConfiguration.getValue(OPENAPI_INFO);
    Map<String, String> swaggerInfoMap = swaggerInfoMapAsStr != null  ?
        objectMapper.readValue(swaggerInfoMapAsStr, Map.class) :
        new HashMap<>();
    boolean userSelectedAnotherSubapiModule = swaggerInfoMap.size() > 0 && !swaggerInfoMap.containsKey(subApi);

    // Search bar UI and hidden property used for searching added to property list
    properties.add(simpleIntegrationTemplate.textProperty(SEARCH)
        .label("Sort Endpoints")
        .refresh(RefreshPolicy.ALWAYS)
        .instructionText("Sort the endpoints dropdown below with a relevant search query")
        .placeholder("Sort Query")
        .build());
    properties.add(simpleIntegrationTemplate.textProperty(ENDPOINTS_FOR_SEARCH).isHidden(true).build());

    TextPropertyDescriptor.TextPropertyDescriptorBuilder listOfEndpointsUI = simpleIntegrationTemplate
        .textProperty(CHOSEN_ENDPOINT)
        .isRequired(true)
        .refresh(RefreshPolicy.ALWAYS)
        .label("Select Operation");

    if (swaggerInfoMapAsStr == null || userSelectedAnotherSubapiModule) {
      if (userSelectedAnotherSubapiModule) {
        integrationConfiguration.setValue(CHOSEN_ENDPOINT, null);
        swaggerInfoMap.clear();
      }

      String swaggerUrl = subAPIInfoMap.get("docs");
      Map<String, Object> apiSwaggerResponse = HTTP.testAuth(connectedSystemConfiguration, swaggerUrl);
      if (apiSwaggerResponse == null || apiSwaggerResponse.containsKey("error")) {
        integrationConfiguration.setErrors((List)apiSwaggerResponse.get("error"));
        return setPropertiesAndValues(properties, values);
      }

      // Create map of subApi module name to map of subApi module info and save as hidden property
      String swaggerStr = apiSwaggerResponse.get("result").toString();
      setOpenAPI(swaggerStr);
      if (openAPI == null || paths == null) {
        return setPropertiesAndValues(properties, values);
      }
      String compressedSwaggerStr = Util.compress(swaggerStr);
      swaggerInfoMap.put(subApi, compressedSwaggerStr);
      swaggerInfoMapAsStr = objectMapper.writeValueAsString(swaggerInfoMap);
      values.put(OPENAPI_INFO, swaggerInfoMapAsStr);
      values.put(SEARCH, "");


      // If subApi module selected on initial run, refresh, or new subApi module selected, get list of endpoints
      List<String> listOfChoicesForSearch = new ArrayList<>();

      paths.fields().forEachRemaining(pathNode -> {
        String pathName = pathNode.getKey();
        JsonNode path = pathNode.getValue();
        if (PATHS_TO_REMOVE.contains(pathName)) return;

        Map<String, JsonNode> operations = new HashMap<>();
        operations.put(GET, path.get(GET));
        operations.put(POST, path.get(POST));
        operations.put(PATCH, path.get(PATCH));
        operations.put(DELETE, path.get(DELETE));
        operations.forEach((restOperation, operation) -> {
          if (operation == null || operation.size() == 0) return;
          if (operation.get(DEPRECATED) != null && operation.get(DEPRECATED).asBoolean()) return;

          // Builds up endpoint choices and choices list for search on initial run with all paths
          String summary = operation.get(SUMMARY).asText();
          String description = operation.get(DESCRIPTION).asText();
          String name = restOperation.toUpperCase() + " - " + summary;
          String value = String.join(":", api, restOperation, pathName, subApi, summary, description);
          listOfEndpointsUI.choice(Choice.builder().name(name).value(value).build());
          listOfChoicesForSearch.add(value);
        });
      });


      properties.add(listOfEndpointsUI.build());
      values.put(ENDPOINTS_FOR_SEARCH, objectMapper.writeValueAsString(listOfChoicesForSearch));
      return setPropertiesAndValues(properties, values);
    }

    if (propertyPath != null) {
      swaggerInfoMapAsStr = objectMapper.writeValueAsString(swaggerInfoMap);
      values.put(OPENAPI_INFO, swaggerInfoMapAsStr);
    }


    String searchQuery = integrationConfiguration.getValue(SEARCH);
    String listOfChoicesForSearchStr = integrationConfiguration.getValue(ENDPOINTS_FOR_SEARCH);
    values.put(ENDPOINTS_FOR_SEARCH, listOfChoicesForSearchStr);
    List<String> listOfChoicesForSearch = objectMapper.readValue(listOfChoicesForSearchStr, List.class);

    // If there isn't a search query, render default endpoints list
    if (searchQuery == null || searchQuery.equals("")) {
      // rebuild default choices from listOfChoicesForSearch (as the choices themselves change order and listOfChoicesForSearch
      // is stable)
      listOfChoicesForSearch.forEach(choice -> {
        String[] pathInfo = choice.split(":");
        String restOperation = pathInfo[1];
        String summary = pathInfo[4];
        listOfEndpointsUI.choice(
            new Choice.ChoiceBuilder().name(restOperation.toUpperCase() + " - " + summary).value(choice).build()
        );
      });
    } else {
      // If there is a search query, sort the dropdown with the query
      List<ExtractedResult> extractedResults = FuzzySearch.extractSorted(searchQuery, listOfChoicesForSearch);
      for (ExtractedResult choice : extractedResults) {
        String[] pathInfo = choice.getString().split(":");
        String restOperation = pathInfo[1];
        String summary = pathInfo[4];
        listOfEndpointsUI.choice(
            new Choice.ChoiceBuilder().name(restOperation.toUpperCase() + " - " + summary).value(choice.getString()).build()
        );
      }
    }

    // If the endpoints list has already been generated but no endpoint is selected, just build the endpoints dropdown
    String selectedEndpoint = integrationConfiguration.getValue(CHOSEN_ENDPOINT);
    if (selectedEndpoint == null) {
      properties.add(listOfEndpointsUI.build());
      return setPropertiesAndValues(properties, values);
    }

    // If an endpoint is selected, the instructionText will update to the REST call and path name
    String[] pathInfo = selectedEndpoint.split(":");
    String restOperation = pathInfo[1];
    String chosenPath = pathInfo[2];
    String description = pathInfo[5];
    String instructionText = restOperation.toUpperCase() + " " + chosenPath + " " + description;
    listOfEndpointsUI.instructionText(instructionText);
    properties.add(listOfEndpointsUI.build());
    Map<String, String> swaggerMap = objectMapper.readValue(swaggerInfoMapAsStr, Map.class);

    String compressedSwaggerStr = swaggerMap.get(subApi);
    String swaggerStr = Util.decompress(compressedSwaggerStr);
    if (openAPI == null) {
      setOpenAPI(swaggerStr);
      if (openAPI == null || paths == null) {
        return setPropertiesAndValues(properties, values);
      }
    }
    buildRestCall(restOperation, properties, chosenPath);
    return setPropertiesAndValues(properties, values);
  }

  public void buildRestCall(String restOperation, List<PropertyDescriptor<?>> properties, String pathName) {
    setPathName(pathName);
    setRestOperation(restOperation);
    setPathVarsUI();
    if (getPathVarsUI().size() > 0) {
      properties.addAll(getPathVarsUI());
    }

    switch (restOperation) {
      case (GET):
        buildGet(properties);
        break;
      case (POST):
        buildPost(properties);
        break;
      case (PATCH):
        buildPatch(properties);
        break;
      case (DELETE):
        buildDelete(properties);
        break;
    }
  }

  public void buildGet(List<PropertyDescriptor<?>> properties) {

    JsonNode get = parse(paths, Arrays.asList(pathName, GET));

    // If Document content is sent back, UI to save that document in Knowledge Base
    JsonNode documentInResponse = parse(get, Arrays.asList(RESPONSES, "200", CONTENT, APPLICATION_JSON, SCHEMA, PROPERTIES, DATA,
        PROPERTIES, ATTRIBUTES, PROPERTIES, CONTENTS, FORMAT));
    if (documentInResponse != null && documentInResponse.asText().equals("byte")) {
      properties.add(simpleIntegrationTemplate.folderProperty(FOLDER)
          .isExpressionable(true)
          .isRequired(true)
          .label("Response File Save Location")
          .instructionText("Choose the folder you would like to save the response file to.")
          .build());
      properties.add(simpleIntegrationTemplate.textProperty(SAVED_FILENAME)
          .isExpressionable(true)
          .isRequired(true)
          .label("Response File Name")
          .instructionText("Choose the name of the file received in the response. Do not include the file extension.")
          .build());
    }

    // Pagination
    properties.add(simpleIntegrationTemplate.textProperty(PAGESIZE)
        .label("Pagination")
        .instructionText("Return 'n' number of items in the response or pass in a link to the 'next' or 'prev' set of items as " +
            "shown here: https://docs.guidewire.com/cloud/cc/202302/cloudapibf/cloudAPI/topics/101-Fund/03-query-parameters" +
            "/c_the-pagination-query-parameters.html.")
        .description("Every resource type has a default pageSize. This value is used when the query does not specify a pageSize. " +
            "To paginate through results, pass the href within the 'next' parameter of 'links', found in the result of the " +
            "initial call to the resource.")
        .isExpressionable(true)
        .placeholder("25 or /common/v1/activities?pageSize=25")
        .build());

    // Filtering and Sorting
    JsonNode extensions = parse(get, Arrays.asList(RESPONSES, "200", CONTENT, APPLICATION_JSON, SCHEMA, PROPERTIES, DATA, ITEMS,
        PROPERTIES, ATTRIBUTES));

    if (extensions != null && (extensions.has("x-gw-filterable") || extensions.has("x-gw-sortable"))) {
      // Building up sorting and filtering options
      TextPropertyDescriptor.TextPropertyDescriptorBuilder sortedChoices = simpleIntegrationTemplate.textProperty(SORT)
          .label("Sort Response")
          .instructionText("Sort response by selecting a field in the dropdown. If the dropdown is empty," +
              " there are no sortable fields available")
          .isExpressionable(true)
          .refresh(RefreshPolicy.ALWAYS);
      TextPropertyDescriptor.TextPropertyDescriptorBuilder filteredChoices = simpleIntegrationTemplate.textProperty(FILTER_FIELD)
          .label("Filter Response")
          .instructionText("Filter response by selecting a field in the dropdown. If the dropdown is " +
              "empty, there are no filterable fields available")
          .isExpressionable(true)
          .refresh(RefreshPolicy.ALWAYS);

      // If there are filtering options, add filtering UI
      JsonNode filterProperties = extensions.get("x-gw-filterable");
      if (filterProperties != null && filterProperties.size() > 0) {
        filterProperties.forEach(property -> {
          filteredChoices.choice(
              Choice.builder().name(Util.camelCaseToTitleCase(property.asText())).value(property.asText()).build()
          );
        });
        TextPropertyDescriptor.TextPropertyDescriptorBuilder filteringOperatorsBuilder = simpleIntegrationTemplate
            .textProperty(FILTER_OPERATOR)
            .instructionText("Select an operator to filter the results")
            .refresh(RefreshPolicy.ALWAYS)
            .isExpressionable(true);
        FILTERING_OPTIONS.entrySet().forEach(option -> {
          filteringOperatorsBuilder.choice(Choice.builder().name(option.getKey()).value(option.getValue()).build());
        });
        // If any of the options are selected, the set will have more items than just null and the rest of the fields become
        // required
        Set<String> requiredSet = new HashSet<>(
            Arrays.asList(integrationConfiguration.getValue(FILTER_FIELD), integrationConfiguration.getValue(FILTER_OPERATOR),
                integrationConfiguration.getValue(FILTER_VALUE)));
        boolean isRequired = requiredSet.size() > 1;

        // Add filtering fields to the UI
        properties.add(filteredChoices.isRequired(isRequired).build());
        properties.add(filteringOperatorsBuilder.isRequired(isRequired).build());
        properties.add(simpleIntegrationTemplate.textProperty(FILTER_VALUE)
            .instructionText("Insert the query to filter the chosen field")
            .isRequired(isRequired)
            .refresh(RefreshPolicy.ALWAYS)
            .isExpressionable(true)
            .refresh(RefreshPolicy.ALWAYS)
            .placeholder("22")
            .build());
      }

      // If there are sorting options, add sorting UI
      JsonNode sortProperties = extensions.get("x-gw-sortable");
      if (sortProperties != null && sortProperties.size() > 0) {
        sortProperties.forEach(property -> {
          sortedChoices.choice(
              Choice.builder().name(Util.camelCaseToTitleCase(property.asText())).value(property.asText()).build()
          );
        });
        properties.add(sortedChoices.isRequired(integrationConfiguration.getValue(SORT_ORDER) != null).build());
        Choice[] sortOrder = {Choice.builder().name("Ascending").value("+").build(),
            Choice.builder().name("Descending").value("-").build()};
        properties.add(simpleIntegrationTemplate.textProperty(SORT_ORDER)
            .label("Sort Order of Response")
            .choices(sortOrder)
            .isExpressionable(true)
            .isRequired(integrationConfiguration.getValue(SORT) != null)
            .displayHint(DisplayHint.NORMAL)
            .instructionText("Select the sort order. Default sort order is ascending")
            .refresh(RefreshPolicy.ALWAYS)
            .build());
      }
    }

    // Include Total UI
    properties.add(simpleIntegrationTemplate.booleanProperty(INCLUDE_TOTAL)
        .label("Include Total")
        .isExpressionable(true)
        .displayMode(BooleanDisplayMode.RADIO_BUTTON)
        .instructionText("Used to request that results should include a count of the total number of results available, " +
            "which may be more than the total number of results currently being returned.")
        .description("This value can only be set when there is more than one element returned. If not specified, the default is" +
            " considered to be `false.` If the number of resources to total is sufficiently large, using the includeTotal " +
            "parameter can affect performance. Guidewire recommends you use this parameter only when there is a need for it, and " +
            "only when the number of resources to total is unlikely to affect performance.")
        .build());


    // Included resources TODO: currently not working because guidewire docs are wrong
/*    Optional<Object> hasIncludedResources = Optional.ofNullable(get.getResponses())
        .map(schema -> schema.get("200"))
        .map(ApiResponse::getContent)
        .map(contentMap -> contentMap.get("application/json"))
        .map(MediaType::getSchema)
        .map(Schema::getProperties)
        .map(properties -> properties.get("included"));

    if (hasIncludedResources.isPresent()) {
      Set<?> included = ((Schema<?>)hasIncludedResources.get()).getProperties().keySet();

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
    }*/

  }

  public void buildPostOrPatch(List<PropertyDescriptor<?>> properties, String restOperation) {

    JsonNode reqBody = parse(paths, Arrays.asList(pathName, restOperation, REQUEST_BODY));
    if(reqBody == null) {
      properties.add(ConstantKeys.getChecksumUI(CHECKSUM_IN_HEADER));
      properties.add(NO_REQ_BODY_UI);
      return;
    }

    JsonNode documentType = parse(reqBody, Arrays.asList(CONTENT,MULTIPART_FORM_DATA));
    if (documentType != null) {
      properties.add(simpleIntegrationTemplate.documentProperty(DOCUMENT)
          .label("Document")
          .isExpressionable(true)
          .instructionText("Insert a document to upload")
          .build());
    }

    properties.add(ConstantKeys.getChecksumUI(CHECKSUM_IN_REQ_BODY));

    JsonNode schema = (documentType == null) ?
        parse(reqBody, Arrays.asList(CONTENT, APPLICATION_JSON, SCHEMA, PROPERTIES, DATA, PROPERTIES, ATTRIBUTES)) :
        parse(documentType, Arrays.asList(SCHEMA, PROPERTIES, METADATA, PROPERTIES, DATA, PROPERTIES, ATTRIBUTES));


    if (schema == null) {
      properties.add(NO_REQ_BODY_UI);
      return;
    }

    JsonNode requiredNode = parse(schema, Arrays.asList(REQUIRED));
    ReqBodyUIBuilder(properties, schema.get(PROPERTIES), requiredNode, restOperation);
  }

  public void buildPost(List<PropertyDescriptor<?>> properties) {
    buildPostOrPatch(properties, POST);
  }

  public void buildPatch(List<PropertyDescriptor<?>> properties) {
    buildPostOrPatch(properties, PATCH);
  }

  public void buildDelete(List<PropertyDescriptor<?>> result) {
    result.add(ConstantKeys.getChecksumUI(CHECKSUM_IN_HEADER));
  }
}
