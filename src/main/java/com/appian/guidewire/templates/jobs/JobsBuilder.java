package com.appian.guidewire.templates.jobs;

import java.util.List;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.guidewire.templates.UIBuilders.RestParamsBuilder;

public class JobsBuilder extends RestParamsBuilder {

  public JobsBuilder(String api, SimpleIntegrationTemplate simpleIntegrationTemplate, SimpleConfiguration integrationConfiguration)
  {
    super(api, simpleIntegrationTemplate, integrationConfiguration);
  }
  public void buildGet(List<PropertyDescriptor> result) {


    super.buildGet(result);

   /* return TextPropertyDescriptor.builder()
        .key(GET_JOBS_DROPDOWN)
        .label("Choose Integrations")
        .choices(Choice.builder().name("Get All Jobs").value(GET_JOBS).build(),
            Choice.builder().name("Get Job By ID").value(GET_JOB_BY_ID).build(),
            Choice.builder().name("Get Resource on Job").value(GET_RESOURCE_ON_JOB).build())
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
