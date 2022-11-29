package com.appian.guidewire.templates.policies;

import java.util.ArrayList;
import java.util.List;

import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.guidewire.templates.UIBuilders.RestParamsBuilder;


public class PoliciesBuilder extends RestParamsBuilder  {

  public PoliciesBuilder(String pathName) {
    super(pathName);
  }

  public List<PropertyDescriptor> buildGet() {

    return super.buildGet();

/*    return TextPropertyDescriptor.builder()
        .key(GET_POLICIES_DROPDOWN)
        .label("Choose Integrations")
        .choices(Choice.builder().name("Get All Policies").value(GET_POLICIES).build(),
            Choice.builder().name("Get Policy By ID").value(GET_POLICY_BY_ID).build(),
            Choice.builder().name("Get Resource on Policy").value(GET_RESOURCE_ON_POLICY).build())
        .isExpressionable(true)
        .refresh(RefreshPolicy.ALWAYS)
        .build();*/
  }

  public TextPropertyDescriptor buildPost() {

    super.buildPost();

    return TextPropertyDescriptor.builder()
        .key(GET_CLAIMS_DROPDOWN)
        .label("Choose Integrations")
        .choices(Choice.builder().name("Not built yet").value(GET_CLAIMS).build())
        .isExpressionable(true)
        .refresh(RefreshPolicy.ALWAYS)
        .build();
  }


  public TextPropertyDescriptor buildPatch() {

    super.buildPatch();

    return TextPropertyDescriptor.builder()
        .key(GET_CLAIMS_DROPDOWN)
        .label("Choose Integrations")
        .choices(Choice.builder().name("Not built yet").value(GET_CLAIMS).build())
        .isExpressionable(true)
        .refresh(RefreshPolicy.ALWAYS)
        .build();
  }

  public TextPropertyDescriptor buildDelete() {

    super.buildDelete();

    return TextPropertyDescriptor.builder()
        .key(GET_CLAIMS_DROPDOWN)
        .label("Choose Integrations")
        .choices(Choice.builder().name("Not built yet").value(GET_CLAIMS).build())
        .isExpressionable(true)
        .refresh(RefreshPolicy.ALWAYS)
        .build();
  }
}
