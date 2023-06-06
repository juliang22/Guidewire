package com.appian.guidewire.templates.UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.BooleanDisplayMode;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.guidewire.templates.GuidewireCSP;
import com.appian.guidewire.templates.apis.GuidewireIntegrationTemplate;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import std.ConstantKeys;
import std.Util;

public class GuidewireUIBuilder extends UIBuilder {
  public GuidewireUIBuilder(
      GuidewireIntegrationTemplate simpleIntegrationTemplate, SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration, String api) {
    super();
    /*    setOpenAPI(api);*/
    setIntegrationConfiguration(integrationConfiguration);
    setSimpleIntegrationTemplate(simpleIntegrationTemplate);
    setConnectedSystemConfiguration(connectedSystemConfiguration);
    setApi(api);
    setSubApiList(api);
  }

  // Sets the OpenAPI api and the paths. This is stored statically in the CSP so that it is loaded when the plugin is installed
  // Modify this method and the CSP with relevant API constants and path names
  public void setOpenAPI(String api, String subApi) {
    // TODO: Update with OOTB swagger files
    setSubApi(subApi);

    setOpenAPI(GuidewireCSP.getOpenAPI(api, subApi) );
    setPaths(openAPI.getPaths());
    setDefaultEndpoints(null);
  }

  public PropertyDescriptor<?>[] build() {

    ArrayList<Choice> choices = new ArrayList<>();
    GuidewireCSP.getApiSwaggerMap(api).forEach((subApiKey, openAPIInfo) -> {
      choices.add(Choice.builder().name(openAPIInfo.getName()).value(subApiKey).build());
    });

    TextPropertyDescriptor subApiUI = simpleIntegrationTemplate.textProperty(SUB_API_TYPE)
        .label("Guidewire Module")
        .choices(choices.toArray(new Choice[0]))
        .description("Select the GuideWire API to access. Create a separate connected system for each additional API.")
        .isRequired(true)
        .refresh(RefreshPolicy.ALWAYS)
        .build();
    List<PropertyDescriptor<?>> result = new ArrayList<>(Collections.singletonList(subApiUI));

    // If the subAPI has not been selected, only render the subAPI dropdown
    if (integrationConfiguration.getValue(SUB_API_TYPE) == null) {
      return result.toArray(new PropertyDescriptor<?>[0]);
    }

    String subApi = integrationConfiguration.getValue(SUB_API_TYPE).toString();
    setOpenAPI(api, subApi);
    TextPropertyDescriptor searchBar = simpleIntegrationTemplate.textProperty(SEARCH)
        .label("Sort Endpoints Dropdown")
        .refresh(RefreshPolicy.ALWAYS)
        .instructionText("Sort the endpoints dropdown below with a relevant search query")
        .placeholder("Sort Query")
        .build();

    // If no endpoint is selected, just build the api dropdown
    String selectedEndpoint = integrationConfiguration.getValue(CHOSEN_ENDPOINT);
    if (selectedEndpoint == null) {
      result.addAll(Arrays.asList(searchBar, endpointChoiceBuilder()));
      return result.toArray(new PropertyDescriptor<?>[0]);
    }

    String[] selectedEndpointStr = selectedEndpoint.split(":");
    String apiType = selectedEndpointStr[0];
    String restOperation = selectedEndpointStr[1];
    String pathName = selectedEndpointStr[2];
    String subApiType = selectedEndpointStr[3];
    // If a user switched to another api after they selected an endpoint, set the endpoint and search to null
    if (!apiType.equals(api) || !subApiType.equals(subApi)) {
      integrationConfiguration.setValue(CHOSEN_ENDPOINT, null).setValue(SEARCH, "");
      result.addAll(Arrays.asList(searchBar, endpointChoiceBuilder()));
      return result.toArray(new PropertyDescriptor<?>[0]);
    }
    // Else if a user selects api then a corresponding endpoint, update label and description accordingly
    result.addAll(Arrays.asList(searchBar, endpointChoiceBuilder()));
    String KEY_OF_REQ_BODY = Util.removeSpecialCharactersFromPathName(pathName);
    result.add(simpleIntegrationTemplate.textProperty(REQ_BODY).label(KEY_OF_REQ_BODY).isHidden(true).build());
    // The key of the request body is dynamic so when I need to get it in the execute function:
    // key = integrationConfiguration.getProperty(REQ_BODY).getLabel();
    // integrationConfiguration.getProperty(key);

    // Building the result with path variables, request body, and other functionality needed to make the request
    buildRestCall(restOperation, result, pathName);
    return result.toArray(new PropertyDescriptor<?>[0]);
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
    // result.add(simpleIntegrationTemplate.textProperty(PADDING).isReadOnly(true).label("").build());
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
    Optional<Schema> returnedFieldItems = Optional.ofNullable(get.getResponses().get("200").getContent().get("application/json"))
        .map(MediaType::getSchema)
        .map(Schema::getProperties)
        .map(map -> map.get("data"))
        .map(schema -> ((Schema)schema).getItems());

    if (returnedFieldItems.isPresent()) {
      AtomicBoolean hasSorting = new AtomicBoolean(false);
      AtomicBoolean hasFiltering = new AtomicBoolean(false);

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

      ((Schema<?>)returnedFieldItems.get().getProperties().get("attributes")).getExtensions();
      Map<String, Object> extensions = ((Schema<?>)returnedFieldItems.get().getProperties().get("attributes")).getExtensions();

      if (extensions.size() > 0) {
        List<String> filterProperties = (List)extensions.get("x-gw-filterable");
        if (filterProperties.size() > 0) hasFiltering.set(true);

        List<String> sortProperties = (List)extensions.get("x-gw-sortable");
        if (sortProperties.size() > 0) hasSorting.set(true);

        filterProperties.forEach(property -> {
          filteredChoices.choice(Choice.builder().name(Util.camelCaseToTitleCase(property)).value(property).build());
        });

        sortProperties.forEach(property -> {
          sortedChoices.choice(Choice.builder().name(Util.camelCaseToTitleCase(property)).value(property).build());
        });
      }


      // Parsing to find filtering and sorting options available on the call
/*      Map<?,?> returnedFields = ((Schema<?>)returnedFieldItems.get().getProperties().get("attributes")).getProperties();
      returnedFields.forEach((key, val) -> {

        Optional<Object> extensions = Optional.ofNullable(((Schema<?>)val).getExtensions())
            .map(schema -> schema.get("x-gw-extensions"));
        Optional<?> filterable = extensions.map(extensionMap -> ((LinkedHashMap<?,?>)extensionMap).get("filterable"));
        Optional<?> sortable = extensions.map(extensionMap -> ((LinkedHashMap<?,?>)extensionMap).get("sortable"));

        if (filterable.isPresent()) {
          filteredChoices.choice(Choice.builder().name(Util.camelCaseToTitleCase(key.toString())).value(key.toString()).build());
          hasFiltering.set(true);
        }

        if (sortable.isPresent()) {
          sortedChoices.choice(Choice.builder().name(Util.camelCaseToTitleCase(key.toString())).value(key.toString()).build());
          hasSorting.set(true);
        }

      });*/
      // If there are sorting options, add sorting UI
      if (hasSorting.get()) {
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

      // If there are filtering options, add filtering UI
      if (hasFiltering.get()) {
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
    }

    // Included resources
    result.add(simpleIntegrationTemplate.booleanProperty(INCLUDE_TOTAL)
            .label("Include Total")
            .isExpressionable(true)
            .displayMode(BooleanDisplayMode.RADIO_BUTTON)
            .instructionText("Used to request that results should include a count of the total number of results available, " +
                "which may be more than the total number of results currently being returned.")
            .description("If not specified, the default is considered to be `false.` This value can only be set when there is " +
                "more than one element returned. If the number of resources to total is sufficiently large, using the includeTotal " +
                "parameter can affect performance. Guidewire recommends you use this parameter only when there is a need for it, and " +
                "only when the number of resources to total is unlikely to affect performance.")
        .build());
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
    }

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
