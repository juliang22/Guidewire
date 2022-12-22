package com.appian.guidewire.templates.Rest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyState;
import com.google.gson.Gson;

import std.ConstantKeys;
import std.Util;

public class Execute implements ConstantKeys {

  String pathNameUnmodified;
  String pathNameModified;
  String api;
  String restOperation;
  SimpleConfiguration integrationConfiguration;
  Gson gson;

  public Execute(SimpleConfiguration integrationConfiguration) {
    this.integrationConfiguration = integrationConfiguration;
    String[] pathData = integrationConfiguration.getValue(CHOSEN_ENDPOINT).toString().split(":");
    this.api = pathData[0];
    this.restOperation = pathData[1];
    this.pathNameUnmodified = pathData[2];
    this.pathNameModified = pathData[2];
    String ReqBodyKey = integrationConfiguration.getProperty(REQ_BODY).getLabel();
    this.gson = new Gson();
  }

  public void buildPathNameWithPathVars() {
    List<String> pathVars = Util.getPathVarsStr(pathNameModified);
    if (pathVars.size() == 0) return;

    pathVars.forEach(key -> {
      String val = integrationConfiguration.getValue(key);
      pathNameModified = pathNameModified.replace("{"+key+"}", val);
    });
    pathNameModified = pathNameModified + "?";
  }


  public void build() {
    buildPathNameWithPathVars();

    switch (restOperation) {
      case (GET):
        executeGet();
        break;
/*      case (POST):
        executePost();
        break;
      case (PATCH):
        executePatch();
        break;
      case (DELETE):
        executeDelete();*/
    }
  }

  public void executeGet() {


    // Pagination
    // TODO: pagination with next parameter
    int pageSize = integrationConfiguration.getValue(PAGESIZE) != null ? integrationConfiguration.getValue(PAGESIZE) : 0;
    if (pageSize > 0) {
      pathNameModified = pathNameModified + "pageSize=" + pageSize + "&";
    }

    // Included Resources
    String includedResourcesKey = Util.removeSpecialCharactersFromPathName(pathNameUnmodified) + INCLUDED_RESOURCES;
    Map<String, PropertyState> includedMap = integrationConfiguration.getValue(includedResourcesKey);
    if (includedMap != null && includedMap.size() > 0) {
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
      pathNameModified += "filter=" + filterField + ":" + filterOperator + ":" + "filterValue&";
    }

    // Include Total
    pathNameModified = pathNameModified + "includeTotal=true";

    // If none of the above options were set or if options have been set and there are no more edits required to the pathName
    String lastChar = pathNameModified.substring(pathNameModified.length() - 1);
    if (lastChar.equals("&") || lastChar.equals("?")) {
      pathNameModified = Util.removeLastChar(pathNameModified);
    }

    System.out.println(pathNameModified);
  }



}
