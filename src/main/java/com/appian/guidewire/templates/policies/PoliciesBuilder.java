package com.appian.guidewire.templates.policies;

import java.util.List;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.guidewire.templates.UIBuilders.RestParamsBuilder;


public class PoliciesBuilder extends RestParamsBuilder  {

  public PoliciesBuilder(String api,SimpleIntegrationTemplate simpleIntegrationTemplate) {

    super(api, simpleIntegrationTemplate);
  }

  public void buildGet(List<PropertyDescriptor> result) {

    super.buildGet(result);

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

  public void buildPost(List<PropertyDescriptor> result) {

    super.buildPost(result);

  }


  public void buildPatch(List<PropertyDescriptor> result) {

    super.buildPatch(result);


  }

  public void buildDelete(List<PropertyDescriptor> result) {

    super.buildDelete(result);

  }
}
