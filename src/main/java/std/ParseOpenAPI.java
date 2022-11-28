package std;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor.TextPropertyDescriptorBuilder;
import com.appian.guidewire.templates.GuidewireCSP;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

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
    String selectedEndpoint = integrationConfiguration.getValue(API_CALL_TYPE);

    if (selectedEndpoint == null) {
      return integrationConfiguration.setProperties(
          SEARCHBAR,
          chosenApi.build()
      );
    }


    // If a user switched to another api after they selected an endpoint, set the endpoint and search to null
    // If a user selects api then a corresponding endpoint, update label and description accordingly
    String[] endpoint = selectedEndpoint.split(":");
    if (!endpoint[0].equals(api)) {
      return integrationConfiguration.setProperties(SEARCHBAR, chosenApi.build())
          .setValue(API_CALL_TYPE, null)
          .setValue(SEARCH, "");
    } else {
      return integrationConfiguration.setProperties(
          SEARCHBAR,
          chosenApi.description(endpoint[1] + " " + endpoint[2]).instructionText(endpoint[3]).build()
      );
    }

  }

  public static TextPropertyDescriptorBuilder initializePaths(String api, String searchQuery) {

    ArrayList<Choice> choices = null;
    if (searchQuery.equals("")) {
      choices = ParseOpenAPI.endpointChoiceBuilder(api);
    } else {
      switch (api) {
        case POLICIES:
          choices = getEndpointChoices(GuidewireCSP.policyPathsForSearch, searchQuery);
          break;
        case CLAIMS:
          choices = getEndpointChoices(GuidewireCSP.claimPathsForSearch, searchQuery);
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

  public static ArrayList<Choice> getEndpointChoices(
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
