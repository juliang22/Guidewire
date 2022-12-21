package com.appian.guidewire.templates.UIBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor.TextPropertyDescriptorBuilder;

import std.ConstantKeys;
import std.Util;

public class ParseOpenAPI implements ConstantKeys {

  public static PropertyDescriptor[] buildRootDropdown(
      SimpleConfiguration integrationConfiguration,
      SimpleIntegrationTemplate simpleIntegrationTemplate,
      String api
  ) {

    RestParamsBuilder params = new RestParamsBuilder(api, simpleIntegrationTemplate, integrationConfiguration);

    // If there is a search query, sort the dropdown with the query
    String searchQuery = integrationConfiguration.getValue(SEARCH);
    TextPropertyDescriptorBuilder endpointChoices = (searchQuery == null || searchQuery.equals("")) ?
        params.getEndpointChoices() :
        params.setEndpointChoices(params.endpointChoiceBuilder(api, searchQuery, integrationConfiguration));

    // If no endpoint is selected, just build the api dropdown
    // If a user switched to another api after they selected an endpoint, set the endpoint and search to null
    // If a user selects api then a corresponding endpoint, update label and description accordingly
    String selectedEndpoint = integrationConfiguration.getValue(CHOSEN_ENDPOINT);
    List<PropertyDescriptor> result = new ArrayList<>(Arrays.asList(SEARCHBAR, endpointChoices.build()));
    if (selectedEndpoint == null) {
      return result.stream().toArray(PropertyDescriptor[]::new);
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
      String KEY_OF_REQ_BODY = Util.removeSpecialCharactersFromPathName(pathName);
      result.add(simpleIntegrationTemplate.textProperty(REQ_BODY).label(KEY_OF_REQ_BODY).isHidden(true).build());

      // Building the result with path variables, request body, and other functionality needed to make the
      // request
      params.buildRestCall(restOperation, result);
    }
    return result.stream().toArray(PropertyDescriptor[]::new);
  }





}
