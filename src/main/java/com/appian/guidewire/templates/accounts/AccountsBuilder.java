package com.appian.guidewire.templates.accounts;

import java.util.List;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.guidewire.templates.UIBuilders.RestParamsBuilder;

public class AccountsBuilder extends RestParamsBuilder {
  public AccountsBuilder(String api, SimpleIntegrationTemplate simpleIntegrationTemplate, SimpleConfiguration integrationConfiguration) {

    super(api, simpleIntegrationTemplate, integrationConfiguration);
  }
  public void buildGet(List<PropertyDescriptor> result) {

    super.buildGet(result);
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
