package com.appian.guidewire.templates.UIBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor.TextPropertyDescriptorBuilder;
import com.appian.guidewire.templates.GuidewireCSP;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import std.ConstantKeys;


public class ParseOpenAPI implements ConstantKeys {

  public static PropertyDescriptor[] buildRootDropdown(
      SimpleConfiguration integrationConfiguration,
      SimpleIntegrationTemplate simpleIntegrationTemplate,
      String api,
      List<String> choicesForSearch
  ) {

    RestParamsBuilder params = new RestParamsBuilder(api, simpleIntegrationTemplate);

    // If there is a search query, sort the dropdown with the query
    String searchQuery = integrationConfiguration.getValue(SEARCH);
    TextPropertyDescriptorBuilder endpointChoices = (searchQuery == null || searchQuery.equals("")) ?
        params.getEndpointChoices() :
        params.setEndpointChoices(
            endpointChoiceBuilder(api, searchQuery, choicesForSearch)
        );

    // If no endpoint is selected, just build the api dropdown
    // If a user switched to another api after they selected an endpoint, set the endpoint and search to null
    // If a user selects api then a corresponding endpoint, update label and description accordingly
    String selectedEndpoint = integrationConfiguration.getValue(CHOSEN_ENDPOINT);
    List<PropertyDescriptor> result = new ArrayList<>(Arrays.asList(SEARCHBAR, endpointChoices.build()));
    if (selectedEndpoint == null) {
/*      return integrationConfiguration.setProperties(result.toArray(new PropertyDescriptor[0]));*/
      return result.toArray(new PropertyDescriptor[0]);

    }
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
      params.setPathName(pathName);
      String KEY_OF_REQ_BODY = pathName.replace("/", "").replace("{", "").replace("}", "");
      result.add(simpleIntegrationTemplate.textProperty(REQ_BODY).label(KEY_OF_REQ_BODY).isHidden(true).build());

      // Building the result with path variables, request body, and other functionality needed to make the
      // request
      params.buildRestCall(restOperation, result);
    }
    return result.toArray(new PropertyDescriptor[0]);
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
      case JOBS:
        paths = GuidewireCSP.jobsOpenApi.getPaths();
        break;
      case ACCOUNTS:
        paths = GuidewireCSP.accountsOpenApi.getPaths();
        break;
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
        .key(CHOSEN_ENDPOINT)
        .isRequired(true)
        .refresh(RefreshPolicy.ALWAYS)
        .label("Select Endpoint")
        .transientChoices(true)
        .choices(choices.stream().toArray(Choice[]::new));
    }



}
