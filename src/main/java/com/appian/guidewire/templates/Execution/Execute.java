package com.appian.guidewire.templates.Execution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tika.mime.MimeTypeException;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationError.IntegrationErrorBuilder;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyState;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.guidewire.templates.HTTP.HTTP;
import com.appian.guidewire.templates.HTTP.HttpResponse;
import com.appian.guidewire.templates.integrationTemplates.GuidewireIntegrationTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import std.ConstantKeys;
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
  protected String reqBodyKey;
  protected Long start;
  protected Map<String, Object> builtRequestBody = new HashMap<>();
  protected HTTP httpService;
  protected Map<String,String> apiInfoMap;
  ObjectMapper objectMapper = GuidewireIntegrationTemplate.objectMapper;

  public abstract IntegrationResponse buildExecution() throws IOException;
  public abstract void executeGet() throws IOException, MimeTypeException;
  public abstract void executePost() throws IOException, MimeTypeException;
  public abstract void executePatch() throws IOException, MimeTypeException;
  public abstract void executeDelete() throws IOException, MimeTypeException;

  public Execute(SimpleConfiguration integrationConfiguration, SimpleConfiguration connectedSystemConfiguration,
      ExecutionContext executionContext) {
    this.start = System.currentTimeMillis();
    this.connectedSystemConfiguration = connectedSystemConfiguration;
    this.integrationConfiguration = integrationConfiguration;
    this.executionContext = executionContext;
    this.httpService = new HTTP(this);
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

  // Getting pathName with user inputted path parameters
  public void buildPathNameWithPathVars() {
    List<String> pathVars = Util.getPathVarsStr(pathNameModified);
    if (pathVars.size() == 0) return;

    pathVars.forEach(key -> {
      String val = integrationConfiguration.getValue(key);
      pathNameModified = pathNameModified.replace("{"+key+"}", val);
    });
  }

  // Getting request diagnostics
  public Map<String,Object> getRequestDiagnostics() {
    Map<String,Object> requestDiagnostic = new HashMap<>();
    requestDiagnostic.put("Operation: ", pathNameUnmodified);
    requestDiagnostic.put("Operation with Path Params: ", pathNameModified);
    if (integrationConfiguration.getProperty(reqBodyKey) != null) {
      requestDiagnostic.put("Request Body", builtRequestBody);
      requestDiagnostic.put("Raw Request Body", builtRequestBody.toString());
    }
    return requestDiagnostic;
  }

  public IntegrationDesignerDiagnostic getDiagnosticsUI() {

    return IntegrationDesignerDiagnostic.builder()
        .addExecutionTimeDiagnostic(System.currentTimeMillis() - start)
        .addRequestDiagnostic(getRequestDiagnostics())
        .addResponseDiagnostic(httpService.getHttpResponse().getCombinedResponse())
        .build();
  }

  // buildRequestBodyJSON() helper function to recursively extract user inputted values from Appian property descriptors
  public Map<String,Object> parseReqBodyJSON(String key, PropertyState val) {
    Map<String, Object> propertyMap = new HashMap<>();

/*    if (val == null) return propertyMap;*/

    // Base case: if the value does not have nested values, insert the value into the map
    if (NOT_NESTED_SET.contains(val.getType().getTypeDisplayName())) {
      if (val.getValue() == null) { // Use this line if you want to tell the user that they passed in a null value
        /*setError("No value set for: "+ key, "Set value for " + key + " or remove it from the request body.", "");*/
      }
      // insert into request body map if there are no errors
      propertyMap.put(key, val.getValue());
    } else { // The value does have nested values
      // If the nested value is an array, recursively add to that array and put array in the map
      if (val.getValue() instanceof ArrayList) {
        List<Object> propertyArr = new ArrayList<>();
        ((ArrayList<?>)val.getValue()).forEach(property -> {
          Map<String,Object> nestedVal = parseReqBodyJSON(property.toString(), ((PropertyState)property));
          propertyArr.add(nestedVal.get(property.toString()));
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
