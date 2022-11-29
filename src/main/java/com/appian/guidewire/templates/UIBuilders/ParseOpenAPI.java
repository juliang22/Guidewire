package com.appian.guidewire.templates.UIBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor.TextPropertyDescriptorBuilder;
import com.appian.guidewire.templates.GuidewireCSP;
import com.appian.guidewire.templates.claims.ClaimsBuilder;
import com.appian.guidewire.templates.policies.PoliciesBuilder;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import std.ConstantKeys;
import std.Util;

public class ParseOpenAPI implements ConstantKeys {

  public static SimpleConfiguration buildRootDropdown(
      SimpleConfiguration integrationConfiguration, String api) {

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
        initializePaths(api, searchQuery);


    // If no endpoint is selected, just build the api dropdown
    // If a user switched to another api after they selected an endpoint, set the endpoint and search to null
    // If a user selects api then a corresponding endpoint, update label and description accordingly
    String selectedEndpoint = integrationConfiguration.getValue(API_CALL_TYPE);
    if (selectedEndpoint == null) {
      return integrationConfiguration.setProperties(
          SEARCHBAR,
          chosenApi.build()
      );
    }
    String[] selectedEndpointStr = selectedEndpoint.split(":");
    String apiType = selectedEndpointStr[0];
    String restOperation = selectedEndpointStr[1];
    String pathName = selectedEndpointStr[2];
    String pathSummary = selectedEndpointStr[3];
    if (!apiType.equals(api)) {
      return integrationConfiguration.setProperties(
          SEARCHBAR,
          chosenApi.build()
      ).setValue(API_CALL_TYPE, null).setValue(SEARCH, "");
    } else {

      List<PropertyDescriptor> restParams = getRestParams(apiType, restOperation, pathName);
      Collections.addAll(restParams, SEARCHBAR,
          chosenApi
              .description(restOperation + " " + pathName)
              .instructionText(pathSummary)
              .build());
      return integrationConfiguration.setProperties(
          restParams.toArray(new PropertyDescriptor[0])
/*          disgusting(restParams, 0),
          disgusting(restParams, 1),
          disgusting(restParams, 2)*/
      );

    }
  }

  public static PropertyDescriptor disgusting(List<PropertyDescriptor> p, int i) {
    return i >= p.size() ?
        TextPropertyDescriptor.builder().key("stoopid"+i).isHidden(true).build() :
        p.get(i);
  }


  public static List<PropertyDescriptor>  getRestParams(String api, String restOperation, String pathName) {
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

    List<PropertyDescriptor>  builtParams = null;
    switch (restOperation) {
      case GET:
        builtParams = restParams.buildGet();
        break;
/*      case POST:
        builtParams = restParams.buildPost();
        break;
      case PATCH:
        builtParams = restParams.buildPatch();
        break;
      case DELETE:
        builtParams = restParams.buildDelete();
        break;*/
    }
    return builtParams;
  }

  public static TextPropertyDescriptorBuilder initializePaths(String api, String searchQuery) {

    ArrayList<Choice> choices = null;
    if (searchQuery.equals("")) {
      choices = ParseOpenAPI.endpointChoiceBuilder(api);
    } else {
      switch (api) {
        case POLICIES:
          choices = endpointChoiceBuilderWithSearch(GuidewireCSP.policyPathsForSearch, searchQuery);
          break;
        case CLAIMS:
          choices = endpointChoiceBuilderWithSearch(GuidewireCSP.claimPathsForSearch, searchQuery);
          break;
/*        case JOBS:
          choices = getEndpointChoices(GuidewireCSP.jobPathsForSearch, searchQuery);
          break;*/
      }
    }

    return TextPropertyDescriptor.builder()
        .key(API_CALL_TYPE)
        .isRequired(true)
        .refresh(RefreshPolicy.ALWAYS)
        .label("Select Endpoint")
        .transientChoices(true)
        .choices(choices.stream().toArray(Choice[]::new));
  }

  public static ArrayList<Choice> endpointChoiceBuilderWithSearch(
      Collection<String> choicesForSearch,
      String search) {

    List<ExtractedResult> extractedResults = FuzzySearch.extractSorted(search, choicesForSearch);
    ArrayList<Choice> newChoices = new ArrayList<>();
    extractedResults.stream().forEach(choice -> {

        newChoices.add(
            Choice.builder()
                .name(choice.getString().substring(choice.getString().indexOf(" - ")+3))
                .value(choice.getString().replace(" - ", ":"))
                .build()
        );

    });
    return newChoices;
  }

  // Parse through OpenAPI yaml and return all endpoints as Choice for dropdown
  public static ArrayList<Choice> endpointChoiceBuilder(String api) {
    Paths paths = Util.getOpenApi("com/appian/guidewire/templates/" + api + ".yaml").getPaths();
    ArrayList<Choice> choices = new ArrayList<>();

    // Check if rest call exists on path and add each rest call of path to list of choices
    Map<String,Operation> operations = new HashMap<String,Operation>();
    paths.forEach((pathName, path) -> {

      operations.put(GET, path.getGet());
      operations.put(POST, path.getPost());
      operations.put(PATCH, path.getPatch());
      operations.put(DELETE, path.getDelete());

      operations.forEach((restType, restOperation) -> {
        if (restOperation != null) {
          String name = api + " - " + restType + " - " + pathName + " - " + restOperation.getSummary();
          String value = api + ":" + restType + ":" + pathName + ":" + restOperation.getSummary();
          switch (api) {
            case POLICIES:
              GuidewireCSP.policyPathsForSearch.add(name);
              break;
            case CLAIMS:
              GuidewireCSP.claimPathsForSearch.add(name);
              break;
/*            case JOBS:
              GuidewireCSP.jobPathsForSearch.add(api + " - " + restType + " - " + pathName + " - " + restOperation.getSummary());
              break;
              */
          }
          choices.add(Choice.builder()
              .name(name)
              .value(value)
              .build());
        }
      });
    });
    return choices;
  }

}
