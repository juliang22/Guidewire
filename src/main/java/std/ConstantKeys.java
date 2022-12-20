package std;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
  String CHOSEN_ENDPOINT = "apiCallType";
  String JOBS = "jobs";
  String CLAIMS = "claims";
  String POLICIES = "policies";
  String ACCOUNTS = "accounts";

  //Rest call types
  String REST_CALL = "restCall";
  String GET = "GET";

  String POST = "POST";
  String PATCH = "PATCH";
  String DELETE = "DELETE";

  String NO_REQ_BODY = "noReqBody";
  String INCLUDED_RESOURCES = "includedResources";

  String REQ_BODY = "reqBody";
  String REQ_BODY_PROPERTIES = "reqBody";
  String PAGESIZE = "pagesize";
  String SORT = "sort";
  String FILTER_FIELD = "filterField";
  String FILTER_OPERATOR = "filterOperator";
  String FILTER_VALUE = "filterComparator";

  List<String> FILTERING_OPTIONS = new ArrayList<>(Arrays.asList("=", "≠", "<", ">", "≤", "≥", "In", "Not " +
      "In", "Starts With", "Contains"));





  String TEXT = "text";
  String OBJECT = "object";
  String ARRAY = "array";
  String SEARCH = "search";
  TextPropertyDescriptor SEARCHBAR = new TextPropertyDescriptor.TextPropertyDescriptorBuilder()
      .key(SEARCH)
      .label("Sort Endpoints Dropdown")
      .refresh(RefreshPolicy.ALWAYS)
      .instructionText(
          "Sort the endpoints dropdown below with a relevant search query.")
      .placeholder("Example query for the Claims API: 'injury incidents.'")
      .build();
}
