package std;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;

public interface ConstantKeys {

  // CSP Auth
  String USERNAME = "username";
  String PASSWORD = "password";
  String ROOT_URL = "rootUrl";
  String OPENAPI_INFO = "openAPIInfo";
  String OPENAPISTR = "openAPIStr";
  String ENDPOINTS_FOR_SEARCH = "endpointsForSearch";

  String CHOSEN_ENDPOINT = "chosenEndpoint";

  // API Types
  String API_TYPE = "apiType";
  String SUB_API_TYPE = "subApiType";
  String CLAIMS = "claimCenter";
  String POLICIES = "policyCenter";
  String BILLING = "billingCenter";
  String API_VERSION = "v1";

  // Sub API Types
  String SUB_API_KEY = "subApiKey";
  // ClaimCenter
  String CLAIMS_ADMIN = "claimsAdmin";
  String CLAIMS_ASYNC = "claimsAsync";
  String CLAIMS_CLAIM = "claimsClaim";
  String CLAIMS_COMMON = "claimsCommon";
  String CLAIMS_COMPOSITE = "claimsComposite";
  String CLAIMS_SYSTEM_TOOLS = "claimsSystemTools";

  // PolicyCenter
  String POLICY_ACCOUNT = "policyAccount";
  String POLICY_ADMIN = "policyAdmin";
  String POLICY_ASYNC = "policyAsync";
  String POLICY_COMMON = "policyCommon";
  String POLICY_COMPOSITE = "policyComposite";
  String POLICY_JOB = "policyJob";
  String POLICY_POLICIES = "policyPolicies";
  String POLICY_PRODUCT_DEFINITION = "policyProductDefinition";
  String POLICY_SYSTEM_TOOLS = "policySystemTools";

  // BillingCenter
  String BILLING_ADMIN = "billingAdmin";
  String BILLING_ASYNC = "billingAsync";
  String BILLING_BILLING = "billingBilling";
  String BILLING_COMMON = "billingCommon";
  String BILLING_COMPOSITE = "billingComposite";
  String BILLING_SYSTEM_TOOLS = "billingSystemTools";



  Map<String, List<String>> SUB_API_MAP = new HashMap<String, List<String>>() {{
    put(CLAIMS, new ArrayList<>(Arrays.asList(CLAIMS_ADMIN, CLAIMS_ASYNC, CLAIMS_CLAIM, CLAIMS_COMMON,
/*        CLAIMS_COMPOSITE, */
        CLAIMS_SYSTEM_TOOLS)));
    put(POLICIES, new ArrayList<>(Arrays.asList(POLICY_ACCOUNT, POLICY_ADMIN, POLICY_ASYNC, POLICY_COMMON,
/*        POLICY_COMPOSITE,*/
        POLICY_JOB, POLICY_POLICIES, POLICY_PRODUCT_DEFINITION, POLICY_SYSTEM_TOOLS)));
    put(BILLING, new ArrayList<>(Arrays.asList(BILLING_ADMIN, BILLING_ASYNC, BILLING_BILLING, BILLING_COMMON,
/*        BILLING_COMPOSITE, */
        BILLING_SYSTEM_TOOLS )));
  }};

  // API URLS
  Map<String, String> URL_MAP = new HashMap<String, String>() {{
    put(CLAIMS, "/cc/rest/");
    put(POLICIES, "/pc/rest/");
    put(BILLING, "/bc/rest/");
  }};

  Map<String, String> CLAIMS_URL_MAP = new HashMap<String, String>() {{
    put(CLAIMS_ADMIN, "admin/" + API_VERSION);
    put(CLAIMS_ASYNC, "async/" + API_VERSION);
    put(CLAIMS_CLAIM, "claim/" + API_VERSION);
    put(CLAIMS_COMMON, "activities/" + API_VERSION);
/*    put(CLAIMS_COMPOSITE, "composite/" + API_VERSION);*/
    put(CLAIMS_SYSTEM_TOOLS, "systemtools/" + API_VERSION);
  }};

  Map<String, String> POLICY_URL_MAP = new HashMap<String, String>() {{
    put(POLICY_ACCOUNT, "account/" + API_VERSION);
    put(POLICY_ADMIN, "admin/" + API_VERSION);
    put(POLICY_ASYNC, "async/" + API_VERSION);
    put(POLICY_COMMON, "common/" + API_VERSION);
/*    put(POLICY_COMPOSITE, "composite/" + API_VERSION);*/
    put(POLICY_JOB, "job/" + API_VERSION);
    put(POLICY_POLICIES, "policy/" + API_VERSION);
    put(POLICY_PRODUCT_DEFINITION, "productdefinition/" + API_VERSION);
    put(POLICY_SYSTEM_TOOLS, "systemtools" + API_VERSION);
  }};

  Map<String, String> BILLING_URL_MAP = new HashMap<String, String>() {{
    put(BILLING_ADMIN, "admin/" + API_VERSION);
    put(BILLING_ASYNC, "async/" + API_VERSION);
    put(BILLING_BILLING, "billing/" + API_VERSION);
    put(BILLING_COMMON, "common/" + API_VERSION);
/*    put(BILLING_COMPOSITE, "composite/" + API_VERSION);*/
    put(BILLING_SYSTEM_TOOLS, "systemtools/" + API_VERSION);
  }};

  // OpenAPI Parsing Constants
  String PARAMETERS = "parameters";
  String PATHS = "paths";
  String REQUEST_BODY = "requestBody";
  String CONTENT = "content";
  String CONTENTS = "contents";
  String APPLICATION_JSON = "application/json";
  String MULTIPART_FORM_DATA = "multipart/form-data";
  String SCHEMAS = "schemas";
  String SCHEMA = "schema";
  String PROPERTIES = "properties";
  String FORMAT = "format";
  String DATA = "data";
  String ITEMS = "items";
  String ATTRIBUTES = "attributes";
  String COMPONENTS = "components";
  String REQUIRED = "required";
  String NAME = "name";
  String DESCRIPTION = "description";
  String REF = "$ref";
  String RESPONSES = "responses";
  String TYPE = "type";
  String METADATA = "metadata";
  String DEPRECATED = "deprecated";
  String SUMMARY = "summary";




  //Rest call types
  String GET = "get";

  String POST = "post";
  String PATCH = "patch";
  String DELETE = "delete";

  // Default Errors
  String AUTOGENERATED_ERROR_TITLE = "Autogenerated property with value 'text' must be removed before sending request";
  String AUTOGENERATED_ERROR_MESSAGE = "Please remove all autogenerated properties from request body before executing request.";
  String AUTOGENERATED_ERROR_DETAIL = "Autogenerated properties are marked 'text', 'true', '100', and '3.14' for string, boolean, integer, " +
      "and double properties, respectively. Make sure to update or remove these autogenerated properties before making " +
      "the request.";

  String FILE_SAVING_ERROR_TITLE = "Could not save file received from api.";
  String FOLDER_LOCATION_ERROR_MESSAGE = "Set desired folder location to save the incoming file.";
  String FILE_NAME_ERROR_MESSAGE = "Set desired file name save the incoming file.";

  Set<String> PATHS_TO_REMOVE = new HashSet<>(Arrays.asList("/swagger.json", "/openapi.json", "/batch", "/graph-schema"));

  // folder/filename setting
  String FOLDER = "folder";
  String SAVED_FILENAME = "savedFileName";

  String NO_REQ_BODY = "noReqBody";

  TextPropertyDescriptor NO_REQ_BODY_UI = new TextPropertyDescriptor.TextPropertyDescriptorBuilder()
      .key(NO_REQ_BODY)
      .isReadOnly(true)
      .instructionText("No Request Body is required to execute this POST")
      .build();
  String INCLUDED_RESOURCES = "includedResources";
  String INCLUDE_TOTAL = "includeTotal";
  String REQ_BODY = "reqBody";
  String REQ_BODY_PROPERTIES = "reqBodyProperties";
  String DOCUMENT = "document";
  String PAGESIZE = "pagesize";
  String SORT = "sort";
  String SORT_ORDER = "sortOrder";
  String FILTER_FIELD = "filterField";
  String FILTER_OPERATOR = "filterOperator";
  String FILTER_VALUE = "filterComparator";
  Map<String, String> FILTERING_OPTIONS = new HashMap<String, String>() {{
    put("=", "eq");
    put("≠", "ne");
    put("<", "lt");
    put(">", "gt");
    put("≤", "le");
    put("≥", "ge");
    put("In", "in");
    put("Not In", "ni");
    put("Starts With", "sw");
    put("Contains", "cn");
  }};
  String SEARCH = "search";

  String CHECKSUM_IN_REQ_BODY = "checksum";
  String CHECKSUM_IN_HEADER = "GW-Checksum";

  public static TextPropertyDescriptor getChecksumUI(String checksumInReqBodyOrHeader) {
    return new TextPropertyDescriptor.TextPropertyDescriptorBuilder()
        .key(checksumInReqBodyOrHeader)
        .label("Checksum")
        .placeholder("7a0d9677f11e246bbe3c124889219c50")
        .instructionText("Use checksum to verify that a resource has not been changed since you last interacted with it. When " +
            "you submit a request with a checksum, ClaimCenter calculates the checksum and compares that value " +
            "to the submitted checksum value. Use a GET call for the resource being modified to get the checksum value. Refer to " +
            "the documentation for more information: https://docs.guidewire" +
            ".com/cloud/cc/202302/cloudapibf/cloudAPI/topics/102-Optim/05-checksums/c_checksums.html")
        .description(
            "If the values match, ClaimCenter determines the resource has not been changed since the caller application last acquired the data. The request is executed.\n" +
                "If the values do not match, ClaimCenter determines the resource has been changed since the caller application last acquired the data. The request is not executed, and ClaimCenter returns an error")
        .isExpressionable(true)
        .build();
  }

}
