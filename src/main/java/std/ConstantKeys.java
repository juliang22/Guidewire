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
  String CHOSEN_ENDPOINT = "apiCallType";
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
  String REQ_BODY_PROPERTIES = "reqBody";



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

}
