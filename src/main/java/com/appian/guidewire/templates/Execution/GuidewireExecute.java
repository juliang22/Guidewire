package com.appian.guidewire.templates.Execution;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyState;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import std.Util;

public class GuidewireExecute extends Execute {

  public GuidewireExecute(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      ExecutionContext executionContext) {
    super(integrationConfiguration, connectedSystemConfiguration, executionContext);
  }

  @Override
  public IntegrationResponse buildExecution() throws IOException {
    try {
      switch (restOperation) {
        case GET:
          executeGet();
          break;
        case DELETE:
          executeDelete();
          break;
        case POST:
          executePost();
          break;
        case PATCH:
          executePatch();
          break;
      }
    } catch (IOException e) {
      return IntegrationResponse.forError(new IntegrationError.IntegrationErrorBuilder()
              .title(e.getCause().toString())
              .message(e.getMessage())
              .build())
          .build();
    }

    // If autogenerated 'text' property is submitted, null value submitted, or other errors created, return error
    if (getError() != null) {
      IntegrationError error = getError().build();
/*      return getDiagnosticsUI() != null ?
          IntegrationResponse.forError(error).withDiagnostic(getDiagnosticsUI()).build() :
          IntegrationResponse.forError(error).build();*/
      return IntegrationResponse
          .forSuccess(getHTTPResponse().getCombinedResponse())
          .toError(error)
          .withDiagnostic(getDiagnosticsUI())
          .build();
    }

    return IntegrationResponse
        .forSuccess(getHTTPResponse().getCombinedResponse())
        .withDiagnostic(getDiagnosticsUI())
        .build();
  }

  @Override
  public void executeGet() throws IOException {

    pathNameModified += "?";
    // Pagination
    // TODO: pagination with next parameter
    String pageSize = integrationConfiguration.getValue(PAGESIZE);
    if (pageSize != null) {
      if (Util.isInteger(pageSize) && Integer.parseInt(pageSize) > 0) { // Pagesize is just a number
        pathNameModified = pathNameModified + "pageSize=" + pageSize + "&";
      } else { // pagesize is a link to next/prev href
        // merge next/prev link into pathName
        String nextOrPrevPagination = Util.mergeStrings(pathNameModified, pageSize);
        setHTTPResponse(httpService.get(nextOrPrevPagination));
        return;
      }
    }

    // Included Resources exist and have been selected by user
    String includedResourcesKey = Util.removeSpecialCharactersFromPathName(pathNameUnmodified) + INCLUDED_RESOURCES;
    Map<String,PropertyState> includedMap = integrationConfiguration.getValue(includedResourcesKey);
    boolean includedPropertiesSelected = Optional.ofNullable(includedMap)
        .filter(m -> !m.isEmpty())
        .map(m -> m.values().stream().anyMatch(val -> Boolean.parseBoolean(val.getValue().toString())))
        .orElse(false);
    if (includedPropertiesSelected) {
      AtomicBoolean firstIncluded = new AtomicBoolean(true);
      includedMap.entrySet().forEach(entry -> {
        if (entry.getValue().getValue().equals(true)) {
          pathNameModified += firstIncluded.get() ? "include=" + entry.getKey() + "," : entry.getKey() + ",";
          firstIncluded.set(false);
        }
      });
      pathNameModified = Util.removeLastChar(pathNameModified) + "&";
    }

    // Sorting
    String sortField = integrationConfiguration.getValue(SORT);
    String sortOrder = integrationConfiguration.getValue(SORT_ORDER);
    if (sortField != null && sortOrder != null) {
      pathNameModified += sortOrder.equals("-") ?
          "sort=-" + sortField + "&" :
          "sort=" + sortField + "&";
    }

    // Filtering
    String filterField = Util.filterRules(integrationConfiguration.getValue(FILTER_FIELD));
    String filterOperator = Util.filterRules(integrationConfiguration.getValue(FILTER_OPERATOR));
    String filterValue = Util.filterRules(integrationConfiguration.getValue(FILTER_VALUE));
    if (filterField != null && filterOperator  != null && filterValue != null) {
      pathNameModified += "filter=" + filterField + ":" + filterOperator + ":" + filterValue + "&";
    }

    // Include Total
    pathNameModified = integrationConfiguration.getProperty(INCLUDE_TOTAL) != null && integrationConfiguration.getValue(INCLUDE_TOTAL).equals(true) ?
        pathNameModified + "includeTotal=true" :
        pathNameModified;

    // If none of the above options were set or if options have been set and there are no more edits required to the pathName
    String lastChar = pathNameModified.substring(pathNameModified.length() - 1);
    if (lastChar.equals("&") || lastChar.equals("?")) {
      pathNameModified = Util.removeLastChar(pathNameModified);
    }

    setHTTPResponse(httpService.get(pathNameModified));

    System.out.println(pathNameModified);
  }

  public RequestBody getCompletedRequestBody() throws IOException {
    // If not request body is needed for the post request
    if (integrationConfiguration.getProperty(NO_REQ_BODY) != null) {
      return RequestBody.create(new byte[0]);
    }

    HashMap<String, PropertyState> reqBodyProperties = integrationConfiguration.getValue(reqBodyKey);
    buildRequestBodyJSON(reqBodyProperties);
    Map<String, Object> attributesWrapper = new HashMap<>();
    attributesWrapper.put("attributes", builtRequestBody);

    //add checksum to request body if it exists
    if (integrationConfiguration.getProperty(CHECKSUM_IN_REQ_BODY) != null && integrationConfiguration.getValue(CHECKSUM_IN_REQ_BODY) != null) {
      attributesWrapper.put(CHECKSUM_IN_REQ_BODY, integrationConfiguration.getValue(CHECKSUM_IN_REQ_BODY).toString());
    }

    Map<String, Object> dataWrapper = Collections.singletonMap("data", attributesWrapper);
    String jsonString = new ObjectMapper().writeValueAsString(dataWrapper);
    System.out.println(jsonString);
    return RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
  }

  @Override
  public void executePost() throws IOException {
    setHTTPResponse(httpService.post(pathNameModified, getCompletedRequestBody()));
  }

  @Override
  public void executePatch() throws IOException {
    setHTTPResponse(httpService.patch(pathNameModified, getCompletedRequestBody()));
  }

  @Override
  public void executeDelete() throws IOException {
    setHTTPResponse(httpService.delete(pathNameModified));
  }
}
