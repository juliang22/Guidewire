package com.appian.guidewire.templates.UIBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.appian.connectedsystems.simplified.sdk.configuration.ConfigurableTemplate;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor.TextPropertyDescriptorBuilder;

import std.ConstantKeys;

public class RestParamsBuilder extends ConfigurableTemplate implements ConstantKeys {
  protected final String pathName;

  public RestParamsBuilder(String pathName) {
      super();
      this.pathName = pathName;
    }
    public List<PropertyDescriptor> buildGet() {

      // Find all occurrences of variables inside path (ex. {claimId})
      Matcher m = Pattern.compile("[^{*}]+(?=})").matcher(pathName);

      // TODO: This logic needs to be in it's own function so I can access the start/end index of the
      //  variableName from the execute function to build the final path to call
      // Map of variableName to [startIndex, endIndex] of the variable's location in the path string
      HashMap<String, int[]> pathVars = new HashMap<>();
      while (m.find()) {
        pathVars.put(m.group(), new int[] {m.start(), m.end()});
      }

      List<PropertyDescriptor> pathVarUI = new ArrayList<>();
      pathVars.forEach((key, value) -> {
        TextPropertyDescriptor ui = TextPropertyDescriptor.builder()
            .key(key)
            .instructionText("")
            .isRequired(true)
            .isExpressionable(true)
            .label(key)
            .build();
        pathVarUI.add(ui);
      });
      return pathVarUI;
  }
  public TextPropertyDescriptor buildPost() {
    return new TextPropertyDescriptor.TextPropertyDescriptorBuilder().key("test").build();
  }
  public TextPropertyDescriptor buildPatch() {
    return new TextPropertyDescriptor.TextPropertyDescriptorBuilder().key("test").build();
  }
  public TextPropertyDescriptor buildDelete() {
    return new TextPropertyDescriptor.TextPropertyDescriptorBuilder().key("test").build();
  }
}
