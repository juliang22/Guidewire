package com.appian.guidewire.templates.UI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import std.ConstantKeys;
import std.HTTP;
import std.Util;

public class GuidewireUIBuilder extends UIBuilder {

  protected ObjectMapper objectMapper = new ObjectMapper();

  public GuidewireUIBuilder(
      GuidewireIntegrationTemplate simpleIntegrationTemplate,
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      PropertyPath propertyPath) throws JsonProcessingException {

    super(simpleIntegrationTemplate, integrationConfiguration, connectedSystemConfiguration, propertyPath);
    setApi(connectedSystemConfiguration.getValue(API_TYPE));
  }

  public SimpleConfiguration setPropertiesAndValues(List<PropertyDescriptor<?>> properties, Map<String, String> values) {
    integrationConfiguration.setProperties(properties.toArray(new PropertyDescriptor<?>[0]));
    if (values.size() > 0) {
      values.forEach((key, val) -> {
        integrationConfiguration.setValue(key, val);
      });
    }
    return integrationConfiguration;
  }

  public SimpleConfiguration build() throws IOException {
    List<PropertyDescriptor<?>> properties = new ArrayList<>(); // build properties to pass into .setProperties()
    Map<String, String> values = new HashMap<>(); // build values to pass into .setValues()


    TextPropertyDescriptor.TextPropertyDescriptorBuilder subApiChoicesUI = simpleIntegrationTemplate.textProperty(SUB_API_TYPE)
        .label("Guidewire Module")
        .description("Select the GuideWire API to access. Create a separate connected system for each additional API.")
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
        .instructionText("Sort the endpoints dropdown below with a relevant search query.")
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
      String compressedSwaggerStr = Util.compress(swaggerStr);
      swaggerInfoMap.put(subApi, compressedSwaggerStr);
      swaggerInfoMapAsStr = objectMapper.writeValueAsString(swaggerInfoMap);
      values.put(OPENAPI_INFO, swaggerInfoMapAsStr);
      values.put(SEARCH, "");
/*      values.put(ENDPOINTS_FOR_SEARCH, null);*/



      // If subApi module selected on initial run, refresh, or new subApi module selected, get list of endpoints
      long startTime = System.nanoTime();
      setOpenAPI(Util.getOpenAPI(swaggerStr));
      System.out.println("Getting openAPI: " + (System.nanoTime() - startTime)/1000000 + " milliseconds");

      List<String> listOfChoicesForSearch = new ArrayList<>();
      paths.forEach((pathName, path) -> {
        if (PATHS_TO_REMOVE.contains(pathName))
          return;

        Map<String,Operation> operations = new HashMap<>();
        operations.put(GET, path.getGet());
        operations.put(POST, path.getPost());
        operations.put(PATCH, path.getPatch());
        operations.put(DELETE, path.getDelete());

        operations.forEach((restOperation, openAPIOperation) -> {
          if (openAPIOperation != null) {
            // filter out deprecated endpoints
            if (openAPIOperation.getDeprecated() != null && openAPIOperation.getDeprecated()) return;

            String name = restOperation + " - " + openAPIOperation.getSummary();
            String value = api + ":" + restOperation + ":" + pathName + ":" + subApi + ":" + openAPIOperation.getSummary() +
                ":" + openAPIOperation.getDescription();

            // Builds up endpoint choices and choices list for search on initial run with all paths
            listOfEndpointsUI.choice(Choice.builder().name(name).value(value).build());
            listOfChoicesForSearch.add(value);
          }
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
        listOfEndpointsUI.choice(new Choice.ChoiceBuilder().name(restOperation + " - " + summary).value(choice).build());
      });
    } else {
      // If there is a search query, sort the dropdown with the query
      List<ExtractedResult> extractedResults = FuzzySearch.extractSorted(searchQuery, listOfChoicesForSearch);
      for (ExtractedResult choice : extractedResults) {
        String[] pathInfo = choice.getString().split(":");
        String restOperation = pathInfo[1];
        String summary = pathInfo[4];
        listOfEndpointsUI.choice(new Choice.ChoiceBuilder().name(restOperation + " - " + summary).value(choice.getString()).build());
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
    String instructionText = restOperation + " " + chosenPath + " " + description;
    listOfEndpointsUI.instructionText(instructionText);
    properties.add(listOfEndpointsUI.build());
    Map<String, String> swaggerMap = objectMapper.readValue(swaggerInfoMapAsStr, Map.class);

    String compressedSwaggerStr = swaggerMap.get(subApi);
    String swaggerStr = Util.decompress(compressedSwaggerStr);
    if (openAPI == null) setOpenAPI(Util.getOpenAPI(swaggerStr));
    buildRestCall(restOperation, properties, chosenPath);
    return setPropertiesAndValues(properties, values);
  }

  public void buildRestCall(String restOperation, List<PropertyDescriptor<?>> result, String pathName) {
    setPathName(pathName);
    setRestOperation(restOperation);
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
    result.add(simpleIntegrationTemplate.textProperty(PAGESIZE)
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
    Optional<Map<String,Object>> extensions = Optional.ofNullable(get.getResponses())
        .map(responses -> responses.get("200"))
        .map(ApiResponse::getContent)
        .map(content -> content.get("application/json"))
        .map(MediaType::getSchema)
        .map(Schema::getProperties)
        .map(map -> map.get("data"))
        .map(schema -> ((Schema)schema).getItems())
        .map(Schema::getProperties)
        .map(properties -> (Schema<?>)properties.get("attributes"))
        .map(Schema::getExtensions);

    // Parsing to find filtering and sorting options available on the call
    if (extensions.isPresent() && extensions.get().size() > 0) {

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
        List<String> filterProperties = (List)extensions.get().get("x-gw-filterable");
        if (filterProperties != null && filterProperties.size() > 0) {
          filterProperties.forEach(property -> {
            filteredChoices.choice(Choice.builder().name(Util.camelCaseToTitleCase(property)).value(property).build());
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

        // If there are sorting options, add sorting UI
        List<String> sortProperties = (List)extensions.get().get("x-gw-sortable");
        if (sortProperties != null && sortProperties.size() > 0) {
          sortProperties.forEach(property -> {
            sortedChoices.choice(Choice.builder().name(Util.camelCaseToTitleCase(property)).value(property).build());
          });
          result.add(sortedChoices.isRequired(integrationConfiguration.getValue(SORT_ORDER) != null).build());
          Choice[] sortOrder = {Choice.builder().name("Ascending").value("+").build(),
              Choice.builder().name("Descending").value("-").build()};
          result.add(simpleIntegrationTemplate.textProperty(SORT_ORDER)
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
    result.add(simpleIntegrationTemplate.booleanProperty(INCLUDE_TOTAL)
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
/*
    Optional<Object> hasIncludedResources = Optional.ofNullable(get.getResponses())
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

    Optional<Object> documentInResponse = Optional.ofNullable(get.getResponses())
        .map(responses -> responses.get("200"))
        .map(ApiResponse::getContent)
        .map(content -> content.get("application/json"))
        .map(MediaType::getSchema)
        .map(Schema::getProperties)
        .map(properties -> properties.get("data"))
        .flatMap(data -> data instanceof ObjectSchema ?
            Optional.of(((ObjectSchema)data).getProperties()) :
            Optional.empty())
        .map(properties -> properties.get("attributes"))
        .map(Schema::getProperties)
        .map(properties -> properties.get("contents"));

    if (documentInResponse.isPresent() && documentInResponse.get() instanceof ByteArraySchema) {
      result.add(simpleIntegrationTemplate.folderProperty(FOLDER)
          .isExpressionable(true)
          .isRequired(true)
          .label("Response File Save Location")
          .instructionText("Choose the folder you would like to save the response file to.")
          .build());
      result.add(simpleIntegrationTemplate.textProperty(SAVED_FILENAME)
          .isExpressionable(true)
          .isRequired(true)
          .label("Response File Name")
          .instructionText("Choose the name of the file received in the response. Do not include the file extension.")
          .build());
    }

  }

  public void buildPost(List<PropertyDescriptor<?>> result) {

    if (paths.get(pathName).getPost().getRequestBody() == null) {
      result.add(ConstantKeys.getChecksumUI(CHECKSUM_IN_HEADER));
      result.add(NO_REQ_BODY_UI);
      return;
    }

    // Composite Request -
/*    if (pathName.equals("/composite")) {
      Optional<Schema> schema = Optional.ofNullable(paths.get(pathName))
          .map(PathItem::getPost)
          .map(Operation::getRequestBody)
          .map(RequestBody::getContent)
          .map(content -> content.get("application/json"))
          .map(MediaType::getSchema);

      Set<String> required = schema.get().getRequired() != null ? new HashSet<>(schema.get().getRequired()) : null;
      ReqBodyUIBuilder(result, schema.get().getProperties(), required, new HashMap<>(), POST);
      return;
    }*/

    MediaType documentType = openAPI.getPaths().get(pathName).getPost().getRequestBody().getContent().get("multipart/form-data");
    if (documentType != null) {
      result.add(simpleIntegrationTemplate.documentProperty(DOCUMENT)
          .label("Document")
          .isRequired(true)
          .isExpressionable(true)
          .refresh(RefreshPolicy.ALWAYS)
          .instructionText("Insert a document to upload")
          .build());
    }

    result.add(ConstantKeys.getChecksumUI(CHECKSUM_IN_REQ_BODY));

    Optional<Schema> schema = (documentType == null) ?
        Optional.ofNullable(paths.get(pathName))
            .map(PathItem::getPost)
            .map(Operation::getRequestBody)
            .map(RequestBody::getContent)
            .map(content -> content.get("application/json"))
            .map(MediaType::getSchema)
            .map(Schema::getProperties)
            .map(properties -> properties.get("data"))
            .map(data -> ((ObjectSchema)data).getProperties())
            .map(dataMap -> dataMap.get("attributes")) :
        Optional.ofNullable(paths.get(pathName))
            .map(PathItem::getPost)
            .map(Operation::getResponses)
            .map(apiResponses -> apiResponses.get("201"))
            .map(ApiResponse::getContent)
            .map(content -> content.get("application/json"))
            .map(MediaType::getSchema)
            .map(Schema::getProperties)
            .map(properties -> properties.get("data"))
            .map(data -> ((ObjectSchema)data).getProperties())
            .map(properties -> properties.get("attributes"));

    if (!schema.isPresent()) {
      result.add(NO_REQ_BODY_UI);
      return;
    }

    Set<String> required = schema.get().getRequired() != null ? new HashSet<>(schema.get().getRequired()) : null;
    ReqBodyUIBuilder(result, schema.get().getProperties(), required, new HashMap<>(), POST);
  }

  public void buildPatch(List<PropertyDescriptor<?>> result) {

    if (paths.get(pathName).getPatch().getRequestBody() == null) {
      result.add(NO_REQ_BODY_UI);
      return;
    }

    MediaType documentType = openAPI.getPaths().get(pathName).getPatch().getRequestBody().getContent().get("multipart/form-data");
    if (documentType != null) {
      result.add(simpleIntegrationTemplate.documentProperty(DOCUMENT)
          .isRequired(true)
          .isExpressionable(true)
          .label("Document")
          .refresh(RefreshPolicy.ALWAYS)
          .instructionText("Insert a document to upload")
          .build());
    }

    result.add(ConstantKeys.getChecksumUI(CHECKSUM_IN_REQ_BODY));

    Optional<Schema> schema = (documentType == null) ?
        Optional.ofNullable(paths.get(pathName))
            .map(PathItem::getPatch)
            .map(Operation::getRequestBody)
            .map(RequestBody::getContent)
            .map(content -> content.get("application/json"))
            .map(MediaType::getSchema)
            .map(Schema::getProperties)
            .map(properties -> properties.get("data"))
            .map(data -> ((ObjectSchema)data).getProperties())
            .map(dataMap -> dataMap.get("attributes")) :
        Optional.ofNullable(paths.get(pathName))
            .map(PathItem::getPatch)
            .map(Operation::getResponses)
            .map(apiResponses -> apiResponses.get("200"))
            .map(ApiResponse::getContent)
            .map(content -> content.get("application/json"))
            .map(MediaType::getSchema)
            .map(Schema::getProperties)
            .map(properties -> properties.get("data"))
            .map(data -> ((ObjectSchema)data).getProperties())
            .map(properties -> properties.get("attributes"));

    if (!schema.isPresent()) {
      result.add(NO_REQ_BODY_UI);
      return;
    }

    Set<String> required = schema.get().getRequired() != null ? new HashSet<>(schema.get().getRequired()) : null;
    ReqBodyUIBuilder(result, schema.get().getProperties(), required, new HashMap<>(), PATCH);
  }

  public void buildDelete(List<PropertyDescriptor<?>> result) {
    result.add(ConstantKeys.getChecksumUI(CHECKSUM_IN_HEADER));
  }
}
