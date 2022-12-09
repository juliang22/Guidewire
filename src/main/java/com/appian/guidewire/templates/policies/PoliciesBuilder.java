package com.appian.guidewire.templates.policies;

import java.util.List;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.guidewire.templates.UIBuilders.RestParamsBuilder;


public class PoliciesBuilder extends RestParamsBuilder  {

  public PoliciesBuilder(String api) {
    super(api);
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

  public void buildPost(SimpleIntegrationTemplate simpleIntegrationTemplate) {

    super.buildPost(simpleIntegrationTemplate);

  }


  public List<PropertyDescriptor>  buildPatch() {

    return super.buildPatch();


  }

  public List<PropertyDescriptor>  buildDelete() {

    return super.buildDelete();

  }
}
