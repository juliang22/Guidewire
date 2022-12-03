package com.appian.guidewire.templates.UIBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.appian.connectedsystems.simplified.sdk.configuration.ConfigurableTemplate;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;

import std.ConstantKeys;

public class RestParamsBuilder extends ConfigurableTemplate implements ConstantKeys {
  protected final String pathName;
  private List<PropertyDescriptor> pathVarsUI = new ArrayList<>();

  public RestParamsBuilder(String pathName) {
    super();
    this.pathName = pathName;
    this.pathVarsUI = setPathVarsUI();
  }

  public static List<String> getPathVarsStr(String pathName) {
    Matcher m = Pattern.compile("[^{*}]+(?=})").matcher(pathName);

    // TODO: This logic needs to be in it's own function so I can access the start/end index of the
    //  variableName from the execute function to build the final path to call
    // Map of variableName to [startIndex, endIndex] of the variable's location in the path string
    List<String> pathVars = new ArrayList<>();
    while (m.find()) {pathVars.add(m.group());}
    return pathVars;
  }

  protected List<PropertyDescriptor> setPathVarsUI() {
    // Find all occurrences of variables inside path (ex. {claimId})

    List<String> pathVars = getPathVarsStr(pathName);
    pathVars.forEach(key -> {
      TextPropertyDescriptor ui = TextPropertyDescriptor.builder()
          .key(key)
          .instructionText("")
          .isRequired(true)
          .isExpressionable(true)
          .label(key)
          .build();
      pathVarsUI.add(ui);
    });
    return pathVarsUI;
  }

  protected List<PropertyDescriptor> getPathVarsUI() {
    return pathVarsUI;
  }

  public List<PropertyDescriptor> buildGet() {
    return pathVarsUI;

  }

  public List<PropertyDescriptor>  buildPost() {
    return pathVarsUI;
  }

  public List<PropertyDescriptor> buildPatch() {
    return pathVarsUI;
  }

  public List<PropertyDescriptor>  buildDelete() {
    return pathVarsUI;
  }
}
