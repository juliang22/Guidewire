package com.appian.guidewire.templates.UIBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.ConfigurableTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypePropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
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

public class ParseOpenAPI extends ConfigurableTemplate implements ConstantKeys {


  public static List<PropertyDescriptor> testbs(SimpleIntegrationTemplate t) {
    List<Map<String,Object>> reqBodyArr = ParseOpenAPI.buildRequestBodyUI(GuidewireCSP.claimsOpenApi,
        "/claims/{claimId}/service-requests/{serviceRequestId}/invoices");

    List<PropertyDescriptor> result = new ArrayList<>(Arrays.asList(SEARCHBAR));
    LocalTypeDescriptor.Builder reqBody = t.localType(REQ_BODY);
    reqBodyArr.forEach(field -> {
      if (field.containsKey(TEXT) && field.get(TEXT) instanceof TextPropertyDescriptor) {
        TextPropertyDescriptor textParam = (TextPropertyDescriptor)field.get(TEXT);
        reqBody.properties(textParam);
      } else if (field.containsKey(OBJECT) && field.get(OBJECT) instanceof LocalTypeDescriptor) {
        LocalTypeDescriptor objParam = (LocalTypeDescriptor)field.get(OBJECT);
        reqBody.properties(
            t.localTypeProperty(objParam).build()
        );
      } else if (field.containsKey(ARRAY) && field.get(ARRAY) instanceof LocalTypeDescriptor) {
        LocalTypeDescriptor arrParam = (LocalTypeDescriptor)field.get(ARRAY);
        reqBody.properties(
            t.listTypeProperty(arrParam.getName()).itemType(TypeReference.from(arrParam)).build(),
            t.localTypeProperty(arrParam).isHidden(true).build()
        );
      }
    });

    result.add(t.localTypeProperty(reqBody.build()).key("SINGLE_QNA").displayHint(DisplayHint.EXPRESSION).isExpressionable(true).label("QnA").build());
    return result;

  }





  public static PropertyDescriptor[] buildRootDropdown(
      SimpleConfiguration integrationConfiguration,
      SimpleIntegrationTemplate simpleIntegrationTemplate,
      String api,
      List<String> choicesForSearch
  ) {

    RestParamsBuilder params = new RestParamsBuilder(api);

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

/*      return integrationConfiguration.setProperties(result.toArray(new PropertyDescriptor[0]))
          .setValue(CHOSEN_ENDPOINT, null)
          .setValue(SEARCH, "");*/
      integrationConfiguration.setValue(CHOSEN_ENDPOINT, null).setValue(SEARCH, "");
      return result.toArray(new PropertyDescriptor[0]);

    } else {

      params.setPathName(pathName);

      if (params.getPathVarsUI().size() > 0) {
        result.addAll(params.getPathVarsUI());
      }

      if (restOperation.equals(POST)) {

/*        integrationConfiguration.setValue(REQ_BODY,
            simpleIntegrationTemplate.localTypeProperty(
                simpleIntegrationTemplate.localType(REQ_BODY_PROPERTIES).property(
                    simpleIntegrationTemplate.textProperty("text").isHidden(true).build()
                ).build()
            ).isExpressionable(true).key(REQ_BODY).build()
        );*/
        
        params.buildPost(simpleIntegrationTemplate);
        if (params.getReqBodyProperties() != null) {
          result.add(params.getReqBodyProperties());
        }
      }


      return result.toArray(new PropertyDescriptor[0]);

    }
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
        .key(CHOSEN_ENDPOINT)
        .isRequired(true)
        .refresh(RefreshPolicy.ALWAYS)
        .label("Select Endpoint")
        .transientChoices(true)
        .choices(choices.stream().toArray(Choice[]::new));
    }

  public static List<Map<String,Object>> buildRequestBodyUI(OpenAPI openAPI,
      String pathName){

    if (openAPI.getPaths().get(pathName).getPost().getRequestBody() == null) return null;

    ObjectSchema schema = (ObjectSchema)openAPI.getPaths()
        .get(pathName)
        .getPost()
        .getRequestBody()
        .getContent()
        .get("application/json")
        .getSchema()
        .getProperties()
        .get("data");


    List<Map<String,Object>> reqBodyArr = new ArrayList<>();
    schema.getProperties().get("attributes").getProperties().forEach((key, item) -> {
      Map<String, Object> newParam = parseRequestBody(key, (Schema)item);
      if (newParam != null) reqBodyArr.add(newParam);
    });

  return reqBodyArr;
  }

  public static Map<String, Object> parseRequestBody(Object key, Schema item){

    if (item.getType().equals("object")) {

      if (item.getProperties() == null) return null;


      List<Object> objBuilder = new ArrayList<>();

      item.getProperties().forEach((innerKey, innerItem) -> {
        TextPropertyDescriptor newParam = (TextPropertyDescriptor)parseRequestBody(innerKey,
            (Schema) innerItem).get(TEXT);
        if (newParam != null && newParam instanceof TextPropertyDescriptor) {
          objBuilder.add(newParam);
        }
      });

      Map<String, Object> objMap = new HashMap<>();
      objMap.put(OBJECT,
          LocalTypeDescriptor.builder().name(key.toString())
          .properties(objBuilder.toArray(new PropertyDescriptor[0]))
          .build()
      );
      return objMap;

    } else if (item.getType().equals("array")) {

      if (item.getItems() == null && item.getItems().getProperties() == null) return null;

      List<Object> arrBuilder = new ArrayList<>();
      item.getItems().getProperties().forEach((innerKey, innerItem) -> {
        TextPropertyDescriptor newParam = (TextPropertyDescriptor)parseRequestBody(innerKey,
            (Schema) innerItem).get(TEXT);
        if (newParam != null && newParam instanceof TextPropertyDescriptor) {
          arrBuilder.add(newParam);
        }
      });

      Map<String, Object> arrMap = new HashMap<>();
      arrMap.put(ARRAY,
          LocalTypeDescriptor.builder().name(key.toString())
              .properties(arrBuilder.toArray(new PropertyDescriptor[0]))
              .build()
      );
      return arrMap;

    } else {
      System.out.println(key + " : " + item.getType());

      Map<String, Object> textMap = new HashMap<>();
      textMap.put(TEXT,
          TextPropertyDescriptor.builder()
              .key(key.toString())
              .instructionText(item.getDescription())
              .isExpressionable(true)
              .displayHint(DisplayHint.EXPRESSION)
              .placeholder(item.getDescription())
              .build()
          );
      return textMap;
    }

  }

}
