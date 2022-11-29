package com.appian.guidewire.templates.claims;

import java.util.ArrayList;
import java.util.List;

import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.guidewire.templates.UIBuilders.RestParamsBuilder;

public class ClaimsBuilder extends RestParamsBuilder {

  public ClaimsBuilder(String pathName) {
    super(pathName);
  }
  public List<PropertyDescriptor> buildGet() {

    return super.buildGet();

/*    return TextPropertyDescriptor.builder()
        .key(GET_CLAIMS_DROPDOWN)
        .label("Choose Integrations")
        .choices(Choice.builder().name("Get All Claims").value(GET_CLAIMS).build(),
            Choice.builder().name("Get Claim By ID").value(GET_CLAIM_BY_ID).build(),
            Choice.builder().name("Get Resource on Claim").value(GET_RESOURCE_ON_CLAIM).build())
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
