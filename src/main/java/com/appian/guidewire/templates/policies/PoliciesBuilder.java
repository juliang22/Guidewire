package com.appian.guidewire.templates.policies;

import com.appian.connectedsystems.simplified.sdk.configuration.ConfigurableTemplate;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.guidewire.templates.RESTDropdowns;

import std.ConstantKeys;

public class PoliciesBuilder extends ConfigurableTemplate implements ConstantKeys, RESTDropdowns {

  public TextPropertyDescriptor buildGetDropdown() {
    return TextPropertyDescriptor.builder()
        .key(GET_POLICIES_DROPDOWN)
        .label("Choose Integrations")
        .choices(Choice.builder().name("Get All Policies").value(GET_POLICIES).build(),
            Choice.builder().name("Get Policy By ID").value(GET_POLICY_BY_ID).build(),
            Choice.builder().name("Get Resource on Policy").value(GET_RESOURCE_ON_POLICY).build())
        .isExpressionable(true)
        .refresh(RefreshPolicy.ALWAYS)
        .build();
  }

  public TextPropertyDescriptor buildPostDropdown() {
    return TextPropertyDescriptor.builder()
        .key(GET_CLAIMS_DROPDOWN)
        .label("Choose Integrations")
        .choices(Choice.builder().name("Not built yet").value(GET_CLAIMS).build())
        .isExpressionable(true)
        .refresh(RefreshPolicy.ALWAYS)
        .build();
  }


  public TextPropertyDescriptor buildPatchDropdown() {
    return TextPropertyDescriptor.builder()
        .key(GET_CLAIMS_DROPDOWN)
        .label("Choose Integrations")
        .choices(Choice.builder().name("Not built yet").value(GET_CLAIMS).build())
        .isExpressionable(true)
        .refresh(RefreshPolicy.ALWAYS)
        .build();
  }


  public TextPropertyDescriptor buildDeleteDropdown() {
    return TextPropertyDescriptor.builder()
        .key(GET_CLAIMS_DROPDOWN)
        .label("Choose Integrations")
        .choices(Choice.builder().name("Not built yet").value(GET_CLAIMS).build())
        .isExpressionable(true)
        .refresh(RefreshPolicy.ALWAYS)
        .build();
  }
}
