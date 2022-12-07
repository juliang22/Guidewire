package std;

import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;

public interface ConstantKeys {

  // CSP Auth
  String USERNAME = "username";
  String PASSWORD = "password";


  // API Types enum (clean up below duplication later)
  public enum APIS {
    Jobs,
    Claims,
    Policies
  }

  String PATIENCE = "patience";


  // API Types
  String API_CALL_TYPE = "apiCallType";
  String JOBS = "jobs";
  String CLAIMS = "claims";
  String POLICIES = "policies";

  //Rest call types
  String REST_CALL = "restCall";
  String GET = "GET";

  String POST = "POST";
  String PATCH = "PATCH";
  String DELETE = "DELETE";

  String REQ_BODY = "reqBody";

  String TEXT = "text";
  String OBJECT = "object";
  String ARRAY = "array";
  String SEARCH = "search";
  TextPropertyDescriptor SEARCHBAR = new TextPropertyDescriptor.TextPropertyDescriptorBuilder()
      .key(SEARCH)
      .label("Sort Endpoints")
      .refresh(RefreshPolicy.ALWAYS)
      .instructionText(
          "Sort the endpoints below with a relevant search query. " +
              "Example query for the Claims API: 'injury incidents'")
      .build();

  TextPropertyDescriptor REST_DROPDOWN = TextPropertyDescriptor.builder()
      .key(REST_CALL)
      .label("Select Endpoint")
      .choices(Choice.builder().name("Get").value(GET).build(),
          Choice.builder().name("Post").value(POST).build(),
          Choice.builder().name("Patch").value(PATCH).build(),
          Choice.builder().name("Delete").value(DELETE).build())
      .isExpressionable(true)
      .refresh(RefreshPolicy.ALWAYS)
      .build();

  TextPropertyDescriptor API_CALL_TYPE_HIDDEN = TextPropertyDescriptor.builder().isHidden(true).key(API_CALL_TYPE).build();

  // Get Policies
  String GET_POLICIES_DROPDOWN = "getPoliciesDropdown";
  String GET_POLICIES = "getPolicies";
  String GET_POLICY_BY_ID = "getPolicyByID";
  String GET_RESOURCE_ON_POLICY = "getResourceOnPolicy";

  // Get Claims
  String GET_CLAIMS_DROPDOWN = "getClaimsDropdown";
  String GET_CLAIMS = "getClaims";
  String GET_CLAIM_BY_ID = "getClaimByID";
  String GET_RESOURCE_ON_CLAIM = "getResourceOnClaim";

  // Get Jobs
  String GET_JOBS_DROPDOWN = "getJobsDropdown";
  String GET_JOBS = "getJobs";
  String GET_JOB_BY_ID = "getJobByID";
  String GET_RESOURCE_ON_JOB = "getResourceOnJob";

}
