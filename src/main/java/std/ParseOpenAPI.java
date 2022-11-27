package std;

import java.util.ArrayList;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor.TextPropertyDescriptorBuilder;
import com.appian.guidewire.templates.GuidewireCSP;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;


public class ParseOpenAPI implements ConstantKeys {

  public static SimpleConfiguration buildRootDropdown(
      SimpleConfiguration integrationConfiguration, String api) {

    // Get the endpoint the user has selected and compare
    String selectedEndpoint = integrationConfiguration.getValue(API_CALL_TYPE);
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

    // If not endpoint is selected, just build the api dropdown
    // If a user switched to another api after haven selected an endpoint, set the endpoint to null
    // If a user selects api then endpoint, update label and description accordingly
    if (selectedEndpoint == null) {
      return integrationConfiguration.setProperties(chosenApi.build());
    }
    String[] endpoint = selectedEndpoint.split(":");
    if (!endpoint[0].equals(api)) {
      return integrationConfiguration.setProperties(chosenApi.build()).setValue(API_CALL_TYPE, null);
    } else {
      return integrationConfiguration.setProperties(
          chosenApi.description(endpoint[1] + " " + endpoint[2]).instructionText(endpoint[3]).build());
    }

}

  public static TextPropertyDescriptorBuilder initializePaths(String api) {
    // Choice of API to pass in are Claims, Jobs, and Policies
    OpenAPI openAPI = Util.getOpenApi("com/appian/guidewire/templates/"+api+".yaml");
    ArrayList<Choice> choices = new ArrayList<>();
    openAPI.getPaths().entrySet().forEach(item -> {
      PathItem path = item.getValue();
      String key = item.getKey();

      Operation get, post, patch, delete;
      if ((get = path.getGet()) != null) {
        choices.add(
            Choice.builder().name("GET - " + key + " - "  + get.getSummary())
                .value(api+":GET:" + key + ":" + get.getSummary()).build()
        );
      }
      if ((post = path.getPost()) != null) {
        choices.add(
            Choice.builder().name("POST - " + key + " - "  + post.getSummary())
                .value(api + ":POST:" + key + ":" + post.getSummary()).build()
        );
      }
      if ((patch = path.getPatch()) != null) {
        choices.add(
            Choice.builder().name("PATCH - " + key + " - "  + patch.getSummary())
                .value(api + ":PATCH:" + key + ":" + patch.getSummary()).build()
        );
      }
      if ((delete = path.getDelete()) != null) {
        choices.add(
            Choice.builder().name("DELETE - " + key + " - " + delete.getSummary())
                .value(api + ":DELETE:" + key +":"+ delete.getSummary()).build()
        );
      }
    });
    return TextPropertyDescriptor.builder()
        .key(API_CALL_TYPE)
        .isRequired(true)
        .refresh(RefreshPolicy.ALWAYS)
        .label("Choose REST Call")
        .transientChoices(true)
        .choices(choices.stream().toArray(Choice[]::new));
  }
}
