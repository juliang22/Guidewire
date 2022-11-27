package std;

import com.appian.connectedsystems.simplified.sdk.configuration.ConfigurableTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.guidewire.templates.RESTDropdowns;
import com.appian.guidewire.templates.claims.ClaimsBuilder;
import com.appian.guidewire.templates.jobs.JobsBuilder;
import com.appian.guidewire.templates.policies.PoliciesBuilder;

public class RootDropdownBuilder extends ConfigurableTemplate implements ConstantKeys {

  public static SimpleConfiguration buildRestDropdownType(
      SimpleConfiguration integrationConfiguration, String apiCallType) {

    // If no REST calls (get, post, ...) are selected or if a new API is selected, return just the rest
    // dropdown and reset its values
    if (integrationConfiguration.getValue(REST_CALL) == null ||
        (integrationConfiguration.getValue(API_CALL_TYPE) != null &&
            !integrationConfiguration.getValue(API_CALL_TYPE).toString().equals(apiCallType))) {

      return integrationConfiguration.setProperties(API_CALL_TYPE_HIDDEN, REST_DROPDOWN)
          .setValue(API_CALL_TYPE, apiCallType)
          .setValue(REST_CALL, null);
    }

    // Get the type of API being used (policies, claims, jobs) and create a new builder for it's dropdowns
    RESTDropdowns restDropdown = null;
    switch (apiCallType) {
      case POLICIES:
        restDropdown = new PoliciesBuilder(); break;
      case CLAIMS:
        restDropdown = new ClaimsBuilder(); break;
      case JOBS:
        restDropdown = new JobsBuilder(); break;
    }

    // Based on the above builder, create dropdowns for whatever rest method is selected
    TextPropertyDescriptor restCallType = null;
    switch ((String) integrationConfiguration.getValue(REST_CALL)) {
      case GET:
        restCallType = restDropdown.buildGetDropdown(); break;
      case POST:
        restCallType = restDropdown.buildPostDropdown(); break;
      case PATCH:
        restCallType = restDropdown.buildPatchDropdown(); break;
      case DELETE:
        restCallType = restDropdown.buildDeleteDropdown(); break;
    }
    return integrationConfiguration.setProperties(API_CALL_TYPE_HIDDEN, REST_DROPDOWN, restCallType)
        .setValue(API_CALL_TYPE, apiCallType);
  }
}