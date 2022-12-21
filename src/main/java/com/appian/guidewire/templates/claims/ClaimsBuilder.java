package com.appian.guidewire.templates.claims;

import java.util.List;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.guidewire.templates.UIBuilders.RestParamsBuilder;

public class ClaimsBuilder extends RestParamsBuilder {

  public ClaimsBuilder(String api, SimpleIntegrationTemplate simpleIntegrationTemplate, SimpleConfiguration integrationConfiguration) {

    super(api, simpleIntegrationTemplate, integrationConfiguration);
  }
  public void buildGet(List<PropertyDescriptor> result) {

    super.buildGet(result);

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

  public void  buildPost(List<PropertyDescriptor> result) {

    super.buildPost(result);

  }


  public void buildPatch(List<PropertyDescriptor> result) {

    super.buildPatch(result);


  }


  public void buildDelete(List<PropertyDescriptor> result) {

    super.buildDelete(result);

  }
}
