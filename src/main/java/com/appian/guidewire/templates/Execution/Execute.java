package com.appian.guidewire.templates.Execution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationError.IntegrationErrorBuilder;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyState;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.guidewire.templates.GuidewireCSP;
import com.appian.guidewire.templates.apis.GuidewireIntegrationTemplate;
import com.google.gson.Gson;

import std.ConstantKeys;
import std.HTTP;
import std.HttpResponse;
import std.Util;

public abstract class Execute implements ConstantKeys {

  protected String pathNameUnmodified;
  protected String pathNameModified;
  protected String api;
  protected String subApi;
  protected String restOperation;
  protected SimpleConfiguration integrationConfiguration;
  protected SimpleConfiguration connectedSystemConfiguration;
  protected ExecutionContext executionContext;
  protected IntegrationErrorBuilder error = null;
  protected Gson gson;
  protected String reqBodyKey;
  protected Long start;
  protected Map<String, Object> builtRequestBody = new HashMap<>();
  protected HttpResponse HTTPResponse;
  protected Map<String,Object> requestDiagnostic;
  protected HTTP httpService;

  public abstract IntegrationResponse buildExecution() throws IOException;
  public abstract void executeGet() throws IOException ;
  public abstract void executePost() throws IOException ;
  public abstract void executePatch() throws IOException ;
  public abstract void executeDelete() throws IOException ;

  public Execute(
      GuidewireIntegrationTemplate simpleIntegrationTemplate,
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      ExecutionContext executionContext) {
    this.start = System.currentTimeMillis();
    this.connectedSystemConfiguration = connectedSystemConfiguration;
    this.integrationConfiguration = integrationConfiguration;
    this.executionContext = executionContext;
    this.httpService = new HTTP(this);
    String[] pathData = integrationConfiguration.getValue(CHOSEN_ENDPOINT).toString().split(":");
    this.api = pathData[0];
    this.subApi = pathData[3];
    this.restOperation = pathData[1];
    this.pathNameUnmodified = pathData[2];
    this.pathNameModified =
        connectedSystemConfiguration.getValue(ROOT_URL) + "/rest" + GuidewireCSP.getApiSwaggerMap(api).get(subApi).getKey() + pathNameUnmodified;
    this.gson = new Gson();
    this.reqBodyKey = integrationConfiguration.getProperty(REQ_BODY) != null ?
        integrationConfiguration.getProperty(REQ_BODY).getLabel() :
        null;
    buildPathNameWithPathVars();
  }

  // Getting Appian execution details
  public SimpleConfiguration getConnectedSystemConfiguration() {
    return connectedSystemConfiguration;
  }
  public SimpleConfiguration getIntegrationConfiguration() {
    return integrationConfiguration;
  }
  public ExecutionContext getExecutionContext() {
    return executionContext;
  }

  // Error setting/getting
  public IntegrationErrorBuilder getError() { return this.error; }
  public void setError(String title, String message, String detail) {
    error = new IntegrationErrorBuilder().title(title).message(message).detail(detail);
  }

  public String getPathNameUnmodified() {return pathNameUnmodified;}

  // Getting pathName with user inputted path parameters
  public void buildPathNameWithPathVars() {
    List<String> pathVars = Util.getPathVarsStr(pathNameModified);
    if (pathVars.size() == 0) return;

    pathVars.forEach(key -> {
      String val = integrationConfiguration.getValue(key);
      pathNameModified = pathNameModified.replace("{"+key+"}", val);
    });
  }

  // getting/setting diagnostics
  public Map<String,Object> getDiagnostics() {return this.requestDiagnostic;}

  public void setRequestDiagnostics() {
    Map<String,Object> requestDiagnostic = new HashMap<>();
    requestDiagnostic.put("Operation: ", pathNameUnmodified);
    requestDiagnostic.put("Operation with Path Params: ", pathNameModified);
    if (this.reqBodyKey != null) {
      requestDiagnostic.put("Request Body", this.builtRequestBody);
    }
    this.requestDiagnostic = requestDiagnostic;
  }

  public IntegrationDesignerDiagnostic getDiagnosticsUI() {
    setRequestDiagnostics();
    return IntegrationDesignerDiagnostic.builder()
        .addExecutionTimeDiagnostic(System.currentTimeMillis() - start)
        .addRequestDiagnostic(getDiagnostics())
        .addResponseDiagnostic(getHTTPResponse().getCombinedResponse())
        .build();
  }

  public void setHTTPResponse(HttpResponse HTTPResponse) { this.HTTPResponse = HTTPResponse; }
  public HttpResponse getHTTPResponse() { return this.HTTPResponse; }

  // buildRequestBodyJSON() helper function to recursively extract user inputted values from Appian property descriptors
  public Map<String,Object> parseReqBodyJSON(String key, PropertyState val) {

    Set<String> notNested = new HashSet<>(Arrays.asList("STRING", "INTEGER", "BOOLEAN", "DOUBLE", "PARAGRAPH"));
    Map<String, Object> propertyMap = new HashMap<>();

/*    if (val == null) return propertyMap;*/

    // Base case: if the value does not have nested values, insert the value into the map
    if (notNested.contains(val.getType().getTypeDisplayName())) {
      if (val.getValue() == null) { // Use this line if you want to tell the user that they passed in a null value
        /*setError("No value set for: "+ key, "Set value for " + key + " or remove it from the request body.", "");*/
      } else if (val.getValue().toString().equals("text")) { // Set error if autogenerated key is in Req Body
        setError(AUTOGENERATED_ERROR_TITLE, AUTOGENERATED_ERROR_MESSAGE, AUTOGENERATED_ERROR_DETAIL);
      }
      // insert into request body map if there are no errors
      propertyMap.put(key, val.getValue());
    } else { // The value does have nested values
      // If the nested value is an array, recursively add to that array and put array in the map
      if (val.getValue() instanceof ArrayList) {
        List<Map<String, Object>> propertyArr = new ArrayList<>();
        ((ArrayList<?>)val.getValue()).forEach(property -> {
          Map<String,Object> nestedVal = parseReqBodyJSON(property.toString(), ((PropertyState)property));
          propertyArr.add((Map<String,Object>)nestedVal.get(property.toString()));
        });
        propertyMap.put(key, propertyArr);
      } else {
        // If value is an object, recursively add nested elements to a map
        ((Map<String,PropertyState>)val.getValue()).forEach((innerKey, innerVal) -> {
          // If map already contains the key to nested maps of values, add key/val pair to that map
          Map<String,Object> newKeyVal = parseReqBodyJSON(innerKey, innerVal);
          if (propertyMap.containsKey(key)) {
            ((Map<String, Object>)propertyMap.get(key)).put(innerKey, newKeyVal.get(innerKey));
          } else {
            propertyMap.put(key, newKeyVal);
          }
        });
      }
    }
    return propertyMap;
  }

  // Builds request body json from Appian property descriptors
  public void buildRequestBodyJSON(HashMap<String, PropertyState> reqBodyProperties) {
    // Converting PropertyState request body from ui into Map<String, Object> where objects could be more nested JSON
    reqBodyProperties.entrySet().forEach(prop -> {
      String key = prop.getKey();
      PropertyState val = prop.getValue();

      // If flat level value has nested values, recursively insert those values, otherwise, insert the value
      Set<String> notNested = new HashSet<>(Arrays.asList("STRING", "INTEGER", "BOOLEAN", "DOCUMENT"));

      if (val == null || val.getValue() == null || val.getValue().equals("")) {
        // Use this line if you would like to set an error when a user passes in a null value
        /*        setError("No value set for: "+ key, "Set value for " + key + " or remove it from the request body.", "");*/
      } else if (val.getValue().toString().equals("text")) { // Set error if autogenerated key is in Req Body
        setError(AUTOGENERATED_ERROR_TITLE, AUTOGENERATED_ERROR_MESSAGE, AUTOGENERATED_ERROR_DETAIL);
      } else {
        // flatValue could be a string or more nested Json of type Map<String, Object>
        Object flatValue = notNested.contains(val.getType().getTypeDisplayName()) ?
            val.getValue() :
            parseReqBodyJSON(key, val).get(key);

        if (flatValue.equals("true")) {
          flatValue = true;
        } else if (flatValue.equals("false")) {
          flatValue = false;
        }

        // Build the request body json
        builtRequestBody.put(key, flatValue);
      }
    });
  }

}
