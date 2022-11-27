package std;

import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;

public interface ConstantKeys {

  // CSP Auth
  String USERNAME = "username";
  String PASSWORD = "password";

  // API Types
  String API_CALL_TYPE = "apiCallType";
  String JOBS = "job";
  String CLAIMS = "claims";
  String POLICIES = "policies";

  //Rest call types
  String REST_CALL = "restCall";
  String GET = "get";

  String POST = "post";
  String PATCH = "patch";
  String DELETE = "delete";

  TextPropertyDescriptor REST_DROPDOWN = TextPropertyDescriptor.builder()
      .key(REST_CALL)
      .label("Choose REST Call")
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
