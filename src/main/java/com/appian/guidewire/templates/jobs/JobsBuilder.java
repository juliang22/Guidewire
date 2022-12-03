package com.appian.guidewire.templates.jobs;

import java.util.List;

import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypePropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.guidewire.templates.UIBuilders.RestParamsBuilder;

public class JobsBuilder extends RestParamsBuilder {

  public JobsBuilder(String pathName) {
    super(pathName);
  }
  public List<PropertyDescriptor> buildGet() {


    return super.buildGet();

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

  public List<PropertyDescriptor>  buildPost() {

    return super.buildPost();


  }


  public List<PropertyDescriptor>  buildPatch() {

    return super.buildPatch();


  }


  public List<PropertyDescriptor>  buildDelete() {

    return super.buildDelete();

  }
}
