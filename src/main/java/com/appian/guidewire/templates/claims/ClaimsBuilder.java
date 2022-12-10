package com.appian.guidewire.templates.claims;

import java.util.List;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.guidewire.templates.UIBuilders.RestParamsBuilder;

public class ClaimsBuilder extends RestParamsBuilder {

  public ClaimsBuilder(String api) {
    super(api);
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

  public void  buildPost(SimpleIntegrationTemplate simpleIntegrationTemplate) {

    super.buildPost(simpleIntegrationTemplate);

  }


  public List<PropertyDescriptor>  buildPatch() {

    return super.buildPatch();


  }


  public List<PropertyDescriptor>  buildDelete() {

    return super.buildDelete();

  }
}
