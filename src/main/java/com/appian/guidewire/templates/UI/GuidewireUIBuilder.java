package com.appian.guidewire.templates.UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.templateframework.sdk.configuration.BooleanDisplayMode;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import std.Util;

public class GuidewireUIBuilder extends UIBuilder{
  public GuidewireUIBuilder(
      SimpleIntegrationTemplate simpleIntegrationTemplate, String api) {
    super(simpleIntegrationTemplate, api);
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
        TextPropertyDescriptor.TextPropertyDescriptorBuilder sortedChoices = simpleIntegrationTemplate.textProperty(SORT)
            .label("Sort Response")
            .instructionText("Sort response by selecting a field in the dropdown. If the dropdown is empty," +
                " there are no sortable fields available.")
            .isExpressionable(true)
            .refresh(RefreshPolicy.ALWAYS);
        TextPropertyDescriptor.TextPropertyDescriptorBuilder filteredChoices = simpleIntegrationTemplate.textProperty(FILTER_FIELD)
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
          TextPropertyDescriptor.TextPropertyDescriptorBuilder filteringOperatorsBuilder = simpleIntegrationTemplate.textProperty(FILTER_OPERATOR)
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
    ObjectSchema schema = (documentType == null) ?
        (ObjectSchema)paths.get(pathName)
            .getPost()
            .getRequestBody()
            .getContent()
            .get("application/json")
            .getSchema()
            .getProperties()
            .get("data") :
        ((ObjectSchema)openAPI.getPaths()
            .get(pathName)
            .getPost()
            .getResponses()
            .get("201")
            .getContent()
            .get("application/json")
            .getSchema()
            .getProperties()
            .get("data"));

    ReqBodyUIBuilder(result, schema);

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
    ObjectSchema schema = (documentType == null) ?
        (ObjectSchema)paths.get(pathName)
            .getPatch()
            .getRequestBody()
            .getContent()
            .get("application/json")
            .getSchema()
            .getProperties()
            .get("data") :
        ((ObjectSchema)openAPI.getPaths()
            .get(pathName)
            .getPatch()
            .getResponses()
            .get("200")
            .getContent()
            .get("application/json")
            .getSchema()
            .getProperties()
            .get("data"));

    ReqBodyUIBuilder(result, schema);
  }

  public void buildDelete(List<PropertyDescriptor<?>> result) {
  }
}
