package com.appian.guidewire.templates.UIBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor.TextPropertyDescriptorBuilder;
import com.appian.guidewire.templates.GuidewireCSP;
import com.appian.guidewire.templates.claims.ClaimsBuilder;
import com.appian.guidewire.templates.policies.PoliciesBuilder;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import std.ConstantKeys;

public class ParseOpenAPI implements ConstantKeys {

  public static SimpleConfiguration buildRootDropdown(
      SimpleConfiguration integrationConfiguration,
      String api,
      List<String> choicesForSearch
  ) {

    TextPropertyDescriptorBuilder chosenApi = null;
    switch (api) {
      case POLICIES:
        chosenApi = GuidewireCSP.policies;
        break;
      case CLAIMS:
        chosenApi = GuidewireCSP.claims;
        break;
/*      case JOBS:
        chosenApi = GuidewireCSP.jobs;
        break;*/
    }

    // If there is a search query, sort the dropdown with the query
    String searchQuery = integrationConfiguration.getValue(SEARCH);
    chosenApi = (searchQuery == null || searchQuery.equals("")) ?
        chosenApi :
        endpointChoiceBuilder(api, searchQuery, choicesForSearch);

    // If no endpoint is selected, just build the api dropdown
    // If a user switched to another api after they selected an endpoint, set the endpoint and search to null
    // If a user selects api then a corresponding endpoint, update label and description accordingly
    String selectedEndpoint = integrationConfiguration.getValue(API_CALL_TYPE);
    List<PropertyDescriptor> result = new ArrayList<>(Arrays.asList(SEARCHBAR, chosenApi.build()));
    if (selectedEndpoint == null) {
      return integrationConfiguration.setProperties(result.toArray(new PropertyDescriptor[1]));
    }
    String[] selectedEndpointStr = selectedEndpoint.split(":");
    String apiType = selectedEndpointStr[0];
    String restOperation = selectedEndpointStr[1];
    String pathName = selectedEndpointStr[2];
    String pathSummary = selectedEndpointStr[3];
    if (!apiType.equals(api)) {
      return integrationConfiguration.setProperties(result.toArray(new PropertyDescriptor[1]))
          .setValue(API_CALL_TYPE, null)
          .setValue(SEARCH, "");
    } else {

      List<PropertyDescriptor> restParams = getRestParams(apiType, restOperation, pathName);
      result.addAll(restParams);
      return integrationConfiguration.setProperties(result.toArray(new PropertyDescriptor[1]));
    }
  }

  public static List<PropertyDescriptor> getRestParams(String api, String restOperation, String pathName) {
    RestParamsBuilder restParams = null;
    switch (api) {
      case POLICIES:
        restParams = new PoliciesBuilder(pathName);
        break;
      case CLAIMS:
        restParams = new ClaimsBuilder(pathName);
        break;
/*       case JOBS:
          restParams = new JobsBuilder(pathName);
         break;*/
    }

    List<PropertyDescriptor> builtParams = null;
    switch (restOperation) {
      case GET:
        builtParams = restParams.buildGet();
        break;
      case POST:
        builtParams = restParams.buildPost();
        break;
      case PATCH:
        builtParams = restParams.buildPatch();
        break;
      case DELETE:
        builtParams = restParams.buildDelete();
        break;
    }
    return builtParams;
  }


  // Parse through OpenAPI yaml and return all endpoints as Choice for dropdown
  public static TextPropertyDescriptorBuilder endpointChoiceBuilder(
      String api,
      String searchQuery,
      List<String> choicesForSearch) {
    Paths paths = null;
    switch (api) {
      case POLICIES:
        paths = GuidewireCSP.policiesOpenApi.getPaths();
        break;
      case CLAIMS:
        paths = GuidewireCSP.claimsOpenApi.getPaths();
        break;
/*        case JOBS:
        paths = GuidewireCSP.jobsOpenApi.getPaths();
        choicesForSearch = GuidewireCSP.jobPathsForSearch;
          break;*/
    }


      ArrayList<Choice> choices = new ArrayList<>();
      // Build search choices when search query has been entered
      if (!searchQuery.equals("") && choicesForSearch != null && !choicesForSearch.isEmpty()) {
        List<ExtractedResult> extractedResults = FuzzySearch.extractSorted(searchQuery, choicesForSearch);
        extractedResults.stream().forEach(choice -> {

          choices.add(Choice.builder()
              .name(choice.getString().substring(choice.getString().indexOf(" - ") + 3))
              .value(choice.getString().replace(" - ", ":"))
              .build());
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
              String name = api + " - " + restType + " - " + pathName + " - " + restOperation.getSummary();
              String value = api + ":" + restType + ":" + pathName + ":" + restOperation.getSummary();

              // Builds up choices for search on initial run with all paths
              choicesForSearch.add(name);

              // Choice UI built
              choices.add(Choice.builder().name(name).value(value).build());
            }
          });
        });
      }
      return TextPropertyDescriptor.builder()
        .key(API_CALL_TYPE)
        .isRequired(true)
        .refresh(RefreshPolicy.ALWAYS)
        .label("Select Endpoint")
        .transientChoices(true)
        .choices(choices.stream().toArray(Choice[]::new));
    }

  public static List<Object> buildRequestBodyUI (OpenAPI openAPI,
      String pathName){

    ObjectSchema schema = (ObjectSchema)openAPI.getPaths()
        .get(pathName)
        .getPost()
        .getRequestBody()
        .getContent()
        .get("application/json")
        .getSchema()
        .getProperties()
        .get("data");


    List<Object> reqBodyArr = new ArrayList<>();
    schema.getProperties().get("attributes").getProperties().forEach((key, item) -> {
      List<Object> newParam = parseRequestBody(key, (Schema)item);
      if (newParam != null) reqBodyArr.add(newParam.get(0));
    });

  return reqBodyArr;
  }

  public static List<Object> parseRequestBody(Object key, Schema item){

    if (item.getType().equals("object")) {

      if (item.getProperties() == null) return null;


      List<Object> objBuilder = new ArrayList<>();

      item.getProperties().forEach((innerKey, innerItem) -> {
        TextPropertyDescriptor newParam = (TextPropertyDescriptor)parseRequestBody(innerKey, (Schema) innerItem).get(0);
        if (newParam != null && newParam instanceof TextPropertyDescriptor) {
          objBuilder.add(newParam);
        }
      });

      return Arrays.asList(
          LocalTypeDescriptor.builder().name(key.toString())
            .properties(objBuilder.toArray(new PropertyDescriptor[0]))
            .build()
      );

    } else if (item.getType().equals("array")) {

      System.out.println(key + " : " + item.getType());
      return null;
/*      List<Object> arr = new ArrayList<>();
      item.getItems().getProperties().forEach((innerKey, innerItem) -> {
        Object newParam = parseRequestBody(innerKey, (Schema) innerItem, innerParams);
        if (newParam != null) arr.add((PropertyDescriptor)newParam);
      });
      return ListTypePropertyDescriptor.builder()
 */

    } else {
      System.out.println(key + " : " + item.getType());
      return Arrays.asList(TextPropertyDescriptor.builder()
          .key(key.toString())
          .instructionText(item.getDescription())
          .isExpressionable(true)
          .displayHint(DisplayHint.EXPRESSION)
          .placeholder(item.getDescription())
          .build());

    }

  }

}
