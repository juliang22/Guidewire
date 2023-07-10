package com.appian.guidewire.templates.connectedSystemTemplates;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.tika.mime.MimeTypeException;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.simplified.sdk.connectiontesting.SimpleTestableConnectedSystemTemplate;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.connectiontesting.TestConnectionResult;
import com.appian.guidewire.templates.HTTP.HTTP;
import com.appian.guidewire.templates.HTTP.HttpResponse;

import std.ConstantKeys;

@TemplateId(name="BasicAuthTemplate")
public class BasicAuth extends SimpleTestableConnectedSystemTemplate implements ConstantKeys {


  @Override
  protected SimpleConfiguration getConfiguration(SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {

    return connectedSystemConfiguration.setProperties(
        textProperty(API_TYPE).choices(Choice.builder().name("Claims Center").value(CLAIMS).build(),
                Choice.builder().name("Policy Center").value(POLICIES).build(),
                Choice.builder().name("Billing Center").value(BILLING).build())
            .label("Guidewire API")
            .instructionText("Select the Guidewire API to access. Once the API is set, it cannot be changed. Create a separate " +
                "connected system object for each additional API.")
            .isRequired(true)
            .build(),
        textProperty(ROOT_URL)
            .label("Base Url")
            .instructionText("Enter the base url of your Guidewire instance. For example, https://cc-dev-gwcpdev.<Tenant>" +
                ".zeta1-andromeda.guidewire.net")
            .description("The root url should be anything preceding '/rest/v1/<module>.' Some root urls will require the Claim" +
                " Center, Policy Center, or Billing Center to be appended with /cc, /pc, or /bc.")
            .isRequired(true)
            .build(),
        textProperty(USERNAME)
            .label("Username")
            .instructionText("Enter your Guidewire username.")
            .isRequired(true)
            .build(),
        textProperty(PASSWORD)
            .label("Password")
            .instructionText("Enter your Guidewire password.")
            .isRequired(true)
            .masked(true)
            .build(),
        textProperty(AUTH_TYPE)
            .isHidden(true)
            .build()
    ).setValue(AUTH_TYPE, BASIC_AUTH);
  }

  @Override
  protected TestConnectionResult testConnection(SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {
    if (connectedSystemConfiguration.getValue(API_TYPE) == null ||
        connectedSystemConfiguration.getValue(ROOT_URL) == null ||
        connectedSystemConfiguration.getValue(USERNAME) == null ||
        connectedSystemConfiguration.getValue(PASSWORD) == null) {
      return TestConnectionResult.error(Collections.singletonList("Make sure to set all connected system values."));
    }

    // Get list of available subApis and their information map
    String rootUrl = connectedSystemConfiguration.getValue(ROOT_URL);
    HTTP httpService = new HTTP(connectedSystemConfiguration);
    try {
      httpService.get(rootUrl + "/rest/apis");
    } catch (IOException | MimeTypeException e) {
      return TestConnectionResult.error(Arrays.asList(e.getCause().toString(), e.getMessage()));
    }

    if (httpService.getHttpError() != null) {
      HttpResponse httpError = httpService.getHttpError();
      return TestConnectionResult.error(
          Arrays.asList("Error " + httpError.getStatusCode(), httpError.getResponse().toString(), httpError.getStatusLine())
      );
    }
    return TestConnectionResult.success();
  }
}