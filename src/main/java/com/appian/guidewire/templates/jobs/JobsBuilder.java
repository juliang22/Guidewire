package com.appian.guidewire.templates.jobs;

import com.appian.connectedsystems.simplified.sdk.configuration.ConfigurableTemplate;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.guidewire.templates.RESTDropdowns;

import std.ConstantKeys;

public class JobsBuilder extends ConfigurableTemplate implements ConstantKeys, RESTDropdowns {

  public TextPropertyDescriptor buildGetDropdown() {
    return TextPropertyDescriptor.builder()
        .key(GET_JOBS_DROPDOWN)
        .label("Choose Integrations")
        .choices(Choice.builder().name("Get All Jobs").value(GET_JOBS).build(),
            Choice.builder().name("Get Job By ID").value(GET_JOB_BY_ID).build(),
            Choice.builder().name("Get Resource on Job").value(GET_RESOURCE_ON_JOB).build())
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
