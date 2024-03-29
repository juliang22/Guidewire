package com.appian.ps.guidewire.templates.UI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.tika.mime.MimeTypeException;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.BooleanDisplayMode;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.Expression;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.ps.guidewire.templates.HTTP.HttpResponse;
import com.appian.ps.guidewire.templates.integrationTemplates.GuidewireIntegrationTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import std.ConstantKeys;
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

  public Map<String, Object> getSubApiList() throws IOException, MimeTypeException {
    // Get list of available subApis and their information map
    httpService.get(connectedSystemConfiguration.getValue(ROOT_URL) + "/rest/apis");
    if (httpService.getHttpError() != null) {
      HttpResponse httpError = httpService.getHttpError();
      integrationConfiguration.setErrors(
          Arrays.asList("Error " + httpError.getStatusCode(), httpError.getResponse().toString(), httpError.getStatusLine())
      );
      setOpenAPI(null);
      return null;
    }
    return httpService.getHttpResponse().getResponse();
  }

  public String getSwaggerFile() throws IOException, MimeTypeException {
    String selectedSubApiModule = integrationConfiguration.getValue(SUB_API_TYPE);
    Map<String,String> subApiInfoMap = objectMapper.readValue(selectedSubApiModule, Map.class);
    setSubApi(subApiInfoMap.get(SUB_API_KEY));

    String swaggerUrl = subApiInfoMap.get("docs");
    httpService.get(swaggerUrl);
    if (httpService.getHttpError() != null) {
      HttpResponse httpError = httpService.getHttpError();
      List<String> error = httpError.getStatusCode() == 404 || httpError.getStatusCode() == 401 ?
          Arrays.asList("Error " + httpError.getStatusCode(), httpError.getResponse().toString(),
              "Authentication error: user does not have access to this module.") :
          Arrays.asList("Error " + httpError.getStatusCode(), httpError.getResponse().toString(), httpError.getStatusLine());
      integrationConfiguration.setErrors(error);
      return null;
    }
    return objectMapper.writeValueAsString(httpService.getHttpResponse().getResponse());
  }

  public void buildSubApiModuleUI() throws IOException, MimeTypeException {
    TextPropertyDescriptor.TextPropertyDescriptorBuilder subApiChoicesUI = simpleIntegrationTemplate.textProperty(SUB_API_TYPE)
        .label("Guidewire Module")
        .instructionText("Select the Guidewire module to access within " +
            Util.camelCaseToTitleCase(connectedSystemConfiguration.getValue(API_TYPE)) + ". If no resource is found, the user " +
            "does not have access to the module.")
        .isRequired(true)
        .refresh(RefreshPolicy.ALWAYS);


    Map<String, Object> subApiList = getSubApiList();
    if (subApiList == null) return;

    for (Map.Entry<String, Object> subApiProperties : subApiList.entrySet()) {
      Map<String,String> subAPIInfoMap = (Map<String,String>)subApiProperties.getValue();

      // Filter out all unusable swagger files
      Pattern pattern = Pattern.compile("/system/|/event/|/apis");
      Matcher matcher = pattern.matcher(subAPIInfoMap.get("basePath"));
      if (matcher.find()) continue;

      // Build and save map of all the subApi module choices with value of a map of all the subApi info
      subAPIInfoMap.put(SUB_API_KEY, subAPIInfoMap.get("title").replace(" ", ""));

      String baseUrl = connectedSystemConfiguration.getValue(ROOT_URL).toString();
      String module = subAPIInfoMap.get("basePath");
      String version = connectedSystemConfiguration.getValue(VERSION).toString();
      String openApiDocsUrl = baseUrl + "/rest" + module.substring(0, module.lastIndexOf("/") + 1) + version + "/openapi.json";
      subAPIInfoMap.put("docs", openApiDocsUrl);
      String subAPIInfoMapStr = objectMapper.writeValueAsString(subAPIInfoMap);
      subApiChoicesUI.choice(Choice.builder().name(subAPIInfoMap.get("title")).value(subAPIInfoMapStr).build());
    }
    properties.add(subApiChoicesUI.build());
  }

  public void buildEndpointsUI() throws IOException {

    // Add endpoint search UI
    properties.add(simpleIntegrationTemplate.textProperty(SEARCH)
        .label("Sort Endpoints")
        .refresh(RefreshPolicy.ALWAYS)
        .instructionText("Sort the endpoints dropdown below with a relevant search query.")
        .description("Use the type of REST call and the description of the call for best results. For example, 'GET claim'.")
        .placeholder("Sort Query")
        .build());
    properties.add(simpleIntegrationTemplate.textProperty(ENDPOINTS_FOR_SEARCH).isHidden(true).build());

    // If subApi module selected on initial run, refresh, or new subApi module selected, get list of endpoints
    TextPropertyDescriptor.TextPropertyDescriptorBuilder listOfEndpointsUI = simpleIntegrationTemplate
        .textProperty(CHOSEN_ENDPOINT)
        .isRequired(true)
        .transientChoices(true)
        .refresh(RefreshPolicy.ALWAYS)
        .description("On save and refresh, only the selected operation will remain populated. To populate the dropdown with " +
            "the full list of operations, input text into the 'Sort Endpoints' field.")
        .label("Select Operation");
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

    values.put(ENDPOINTS_FOR_SEARCH, objectMapper.writeValueAsString(listOfChoicesForSearch));
    values.put(SEARCH, "");
    values.put(CHOSEN_ENDPOINT, null);
    properties.add(listOfEndpointsUI.build());
  }

  public TextPropertyDescriptor.TextPropertyDescriptorBuilder buildSearchEndpointsUI() throws IOException {
    // Add endpoint search UI
    properties.add(simpleIntegrationTemplate.textProperty(SEARCH)
        .label("Sort Endpoints")
        .refresh(RefreshPolicy.ALWAYS)
        .instructionText("Sort the endpoints dropdown below with a relevant search query.")
        .description("Use the type of REST call and the description of the call for best results. For example, 'GET claim'.")
        .placeholder("Sort Query")
        .build());
    properties.add(simpleIntegrationTemplate.textProperty(ENDPOINTS_FOR_SEARCH).isHidden(true).build());

    String listOfChoicesForSearchStr = integrationConfiguration.getValue(ENDPOINTS_FOR_SEARCH);
    values.put(ENDPOINTS_FOR_SEARCH, listOfChoicesForSearchStr);
    String searchQuery = integrationConfiguration.getValue(SEARCH);
    List<String> listOfChoicesForSearch = objectMapper.readValue(listOfChoicesForSearchStr, List.class);

    // If there isn't a search query, render default endpoints list
    TextPropertyDescriptor.TextPropertyDescriptorBuilder listOfEndpointsUI = simpleIntegrationTemplate
        .textProperty(CHOSEN_ENDPOINT)
        .isRequired(true)
        .transientChoices(true)
        .refresh(RefreshPolicy.ALWAYS)
        .description("On save and refresh, only the selected operation will remain populated. To populate the dropdown with " +
            "the full list of operations, input text into the 'Sort Endpoints' field.")
        .label("Select Operation");
    if (searchQuery == null || searchQuery.equals("")) {
      // rebuild default choices from listOfChoicesForSearch (as the choices themselves change order and listOfChoicesForSearch
      // is stable). Can't use the previously built property because we need to set the instruction text if the endpoint is
      // actually selected (buildSelectedEndpoint())
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
      FuzzyScore fuzzyScore = new FuzzyScore(Locale.ENGLISH);
      Collections.sort(listOfChoicesForSearch, (o1, o2) -> {
        int score1 = fuzzyScore.fuzzyScore(o1, searchQuery);
        int score2 = fuzzyScore.fuzzyScore(o2, searchQuery);
        return Integer.compare(score2, score1); // Sort in descending order of match score
      });

      for (String choice : listOfChoicesForSearch) {
        String[] pathInfo = choice.split(":");
        String restOperation = pathInfo[1];
        String summary = pathInfo[4];
        listOfEndpointsUI.choice(
            new Choice.ChoiceBuilder().name(restOperation.toUpperCase() + " - " + summary).value(choice).build()
        );
      }
    }
    return listOfEndpointsUI;
  }

  public void buildSelectedEndpoint(TextPropertyDescriptor.TextPropertyDescriptorBuilder listOfEndpointsUI) throws JsonProcessingException {

    // If an endpoint is selected, the instructionText will update to the REST call and path name
    String selectedEndpoint = integrationConfiguration.getValue(CHOSEN_ENDPOINT);
    String[] pathInfo = selectedEndpoint.split(":");
    String restOperation = pathInfo[1];
    String chosenPath = pathInfo[2];
    String description = pathInfo[5];
    String instructionText = restOperation.toUpperCase() + " " + chosenPath + " " + description;
    listOfEndpointsUI.instructionText(instructionText);
    properties.add(listOfEndpointsUI.build());
    buildRestCall(restOperation, chosenPath);
  }


  public SimpleConfiguration build() throws IOException, MimeTypeException {

    buildSubApiModuleUI(); // Build choice of subApi modules

    // Property path could be null initial load, after save, refresh after at least one save has occurred, or when switching
    // from "reads" to "modifies" data
    if (propertyPath == null) {
      if (integrationConfiguration.getProperties().size() <= 1) { // When switching from "reads" to "modifies" template type
        values.put(SUB_API_TYPE, null);
        return setPropertiesAndValues();
      }
      return integrationConfiguration; // On initial load, save, or refresh after save, return just the integration configuration
    }

    // If subApi module is set for the first time or switched to new subApi module, or if integration is being switched from
    // reads to modifies data and propertyPath is null
    if (propertyPath.toString().equals(SUB_API_TYPE)) {
      if (integrationConfiguration.getValue(SUB_API_TYPE) == null) { // User deselected subApi module
        return setPropertiesAndValues();
      }

      String swaggerStr = getSwaggerFile();
      if (swaggerStr == null) return setPropertiesAndValues();

      setOpenAPI(swaggerStr);
      if (openAPI == null || paths == null) return setPropertiesAndValues();

      buildEndpointsUI();
      return setPropertiesAndValues();
    }

    // Build endpoints list either from endpointsForSearch from buildEndpointsUI(); either default order or search order
    TextPropertyDescriptor.TextPropertyDescriptorBuilder listOfEndpointsUI = buildSearchEndpointsUI();

    // If the endpoints list has already been generated but no endpoint is selected, just build the endpoints dropdown
    String selectedEndpoint = integrationConfiguration.getValue(CHOSEN_ENDPOINT);
    if (selectedEndpoint == null) {
      properties.add(listOfEndpointsUI.build());
    } else {
      String swaggerStr = getSwaggerFile();
      if (swaggerStr == null) return setPropertiesAndValues();
      setOpenAPI(swaggerStr);
      if (openAPI == null || paths == null) return setPropertiesAndValues();

      buildSelectedEndpoint(listOfEndpointsUI);
    }

    return setPropertiesAndValues();
  }

  public void buildRestCall(String restOperation, String pathName) throws JsonProcessingException {
    setPathName(pathName);
    setRestOperation(restOperation);
    setPathVarsUI();
    if (getPathVarsUI().size() > 0) {
      properties.addAll(getPathVarsUI());
    }

    switch (restOperation) {
      case (GET):
        buildGet();
        break;
      case (POST):
        buildPost();
        break;
      case (PATCH):
        buildPatch();
        break;
      case (DELETE):
        buildDelete();
        break;
    }
  }

  public void buildGet() {

    JsonNode get = parse(paths, Arrays.asList(pathName, GET));

    // If Document content is sent back, UI to save that document in Knowledge Base
    JsonNode documentInResponse = parse(get, Arrays.asList(RESPONSES, "200", CONTENT, APPLICATION_JSON, SCHEMA, PROPERTIES, DATA,
        PROPERTIES, ATTRIBUTES, PROPERTIES, CONTENTS, FORMAT));
    if (documentInResponse != null && documentInResponse.asText().equals("byte")) {
      properties.add(simpleIntegrationTemplate.folderProperty(FOLDER)
          .isExpressionable(true)
          .isRequired(true)
          .label("Response File Save Location")
          .instructionText("Choose the folder you would like to save the response file to. Integration Operation must be set to" +
              " (Writes Data).")
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

    // Building up sorting and filtering options
    if (extensions != null) {

      // If there are filtering options, add filtering UI
      JsonNode filterProperties = extensions.get("x-gw-filterable");
      if (filterProperties != null && filterProperties.size() > 0) {
        TextPropertyDescriptor.TextPropertyDescriptorBuilder filteredChoices = simpleIntegrationTemplate.textProperty(FILTER_FIELD)
            .label("Filter Response")
            .transientChoices(true)
            .instructionText("Filter response by selecting a field in the dropdown.")
            .isExpressionable(true)
            .description("If setting this value as a rule input, use the abbreviation of the value as described in the " +
                "following list: " + filterProperties + ". If rule inputs are set for filter properties, the values " +
                "are required and cannot be null.")
            .refresh(RefreshPolicy.ALWAYS);

        filterProperties.forEach(property -> filteredChoices.choice(
                Choice.builder().name(Util.camelCaseToTitleCase(property.asText())).value(property.asText()).build()
            )
        );
        TextPropertyDescriptor.TextPropertyDescriptorBuilder filteringOperatorsBuilder = simpleIntegrationTemplate
            .textProperty(FILTER_OPERATOR)
            .label("Filter Operation")
            .instructionText("Select an operator to filter the results.")
            .refresh(RefreshPolicy.ALWAYS)
            .description("If setting this value as a rule input, use the string version of the value as described in the " +
                "following list: " + FILTERING_OPTIONS + ".")
            .isExpressionable(true);
        FILTERING_OPTIONS.forEach((key, value) -> filteringOperatorsBuilder.choice(Choice.builder().name(key).value(value).build()));
        // If any of the options are selected, the set will have more items than just null and the rest of the fields become
        // required
        boolean isRequired =
            integrationConfiguration.getValue(FILTER_FIELD) != null || integrationConfiguration.getValue(FILTER_FIELD) instanceof Expression ||
                integrationConfiguration.getValue(FILTER_OPERATOR) != null || integrationConfiguration.getValue(FILTER_OPERATOR) instanceof Expression ||
                integrationConfiguration.getValue(FILTER_VALUE) != null || integrationConfiguration.getValue(FILTER_VALUE) instanceof Expression;

        // Add filtering fields to the UI
        properties.add(filteredChoices.isRequired(isRequired).build());
        properties.add(filteringOperatorsBuilder.isRequired(isRequired).build());
        properties.add(simpleIntegrationTemplate.textProperty(FILTER_VALUE)
            .label("Filter Value")
            .instructionText("Insert the query to filter the chosen field.")
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
        TextPropertyDescriptor.TextPropertyDescriptorBuilder sortedChoices = simpleIntegrationTemplate.textProperty(SORT)
            .label("Sort Response")
            .instructionText("Sort response by selecting a field in the dropdown. If the dropdown is empty," +
                " there are no sortable fields available.")
            .transientChoices(true)
            .isExpressionable(true)
            .description("If setting this value as a rule input, use the string version of the value as described in the " +
                "following list: " + sortProperties + ".")
            .refresh(RefreshPolicy.ALWAYS);

        sortProperties.forEach(property -> sortedChoices.choice(
                Choice.builder().name(Util.camelCaseToTitleCase(property.asText())).value(property.asText()).build()
            )
        );
        properties.add(sortedChoices.isRequired(integrationConfiguration.getValue(SORT_ORDER) != null).build());
        Choice[] sortOrder = {Choice.builder().name("Ascending").value("+").build(),
            Choice.builder().name("Descending").value("-").build()};
        properties.add(simpleIntegrationTemplate.textProperty(SORT_ORDER)
            .label("Sort Order of Response")
            .choices(sortOrder)
            .isExpressionable(true)
            .isRequired(integrationConfiguration.getValue(SORT) != null)
            .displayHint(DisplayHint.NORMAL)
            .instructionText("Select the sort order. Default sort order is ascending.")
            .description("If setting this value as a rule input, use the operator of the value as described in the " +
                "following list: {Ascending: '+' Descending: '-'}. " + ". If rule inputs are set for sorting properties, the " +
                "values are required and cannot be null.")
            .refresh(RefreshPolicy.ALWAYS)
            .build());
      }
    }

    // Include Total UI
    properties.add(simpleIntegrationTemplate.booleanProperty(INCLUDE_TOTAL)
        .label("Include Total")
        .isExpressionable(true)
        .displayMode(BooleanDisplayMode.RADIO_BUTTON)
        .instructionText("Includes a count of the total number of results available, which may be more than the total number of" +
            " results currently being returned. This value can only be set when there is more than one element returned.")
        .description("If not specified, the default is considered to be `false.` If the number of resources is sufficiently " +
            "large, using the includeTotal parameter can affect performance. Guidewire recommends using this parameter only " +
            "when there is a need for it, and only when the number of resources to total is unlikely to affect performance.")
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

  public void buildPostOrPatch(String restOperation) throws JsonProcessingException {

    // If composite request
    if (pathName.equals("/composite")) {
      properties.add(simpleIntegrationTemplate.textProperty(COMPOSITE)
          .label("Composite Request Body")
          .isExpressionable(true)
          .displayHint(DisplayHint.EXPRESSION)
          .instructionText("Composite requests execute multiple API sub-requests in a single database transaction." +
              " Wrap the entire SAIL expression in a!toJson().")
          .description("Learn more here: https://docs.guidewire.com/cloud/cc/202302/cloudapibf/cloudAPI/topics/102-Optim/02-composite-requests/c_constructing-composite-requests.html?hl=composite")
          .isRequired(true)
          .build());
      return;
    }

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

    // If the schema could be one of many types
    if (parse(schema, ONE_OF) != null) {
      TextPropertyDescriptor.TextPropertyDescriptorBuilder oneOfUI = simpleIntegrationTemplate
          .textProperty(ONE_OF)
          .transientChoices(true)
          .isRequired(true)
          .refresh(RefreshPolicy.ALWAYS);
      if (integrationConfiguration.getProperty(ONE_OF) == null || integrationConfiguration.getValue(ONE_OF) == null) {
        // If the oneOf property hasn't been set yet, or has been deselected
        JsonNode listOfRefs = parse(schema, ONE_OF);
        String label = getRefIfPresent(listOfRefs.get(0)) != null && getRefIfPresent(listOfRefs.get(0)).get("title") != null ?
            getRefIfPresent(listOfRefs.get(0)).get("title").asText() :
            "";
        oneOfUI.label("Select " + Util.camelCaseToTitleCase(label) + " Schema");
        for (JsonNode refNodeStr : listOfRefs) {
          String refName = Util.pascalCaseToTileCase(refNodeStr.get(REF).asText());
          JsonNode ref = getRefIfPresent(refNodeStr);
          oneOfUI.choice(Choice.builder().name(refName).value(ref.toString()).build());
        }

        properties.add(oneOfUI.build());
        return;
      } else { // Render the correct oneOf choice
        properties.add(integrationConfiguration.getProperty(ONE_OF));
        schema = objectMapper.readValue(integrationConfiguration.getValue(ONE_OF).toString(), JsonNode.class);
      }
    }

    JsonNode requiredNode = restOperation.equals(POST) && parse(schema, REQUIRED_FOR_CREATE) != null ?
        parse(schema, REQUIRED_FOR_CREATE) :
        parse(schema, REQUIRED);
    ReqBodyUIBuilder(schema.get(PROPERTIES), requiredNode, restOperation);
  }

  public void buildPost() throws JsonProcessingException {
    buildPostOrPatch(POST);
  }

  public void buildPatch() throws JsonProcessingException {
    buildPostOrPatch(PATCH);
  }

  public void buildDelete() {
    properties.add(ConstantKeys.getChecksumUI(CHECKSUM_IN_HEADER));
  }
}
