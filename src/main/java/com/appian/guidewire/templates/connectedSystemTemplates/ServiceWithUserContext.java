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

@TemplateId(name="ServiceWithUserContext")
public class ServiceWithUserContext extends SimpleTestableConnectedSystemTemplate implements ConstantKeys {

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
            .instructionText("Enter the base url of your Guidewire instance. ")
            .description("For example, https://cc-dev-gwcpdev.<Tenant>.zeta1-andromeda.guidewire.net")
            .isRequired(true)
            .build(),
        textProperty(AUTH_SERVER_URL)
            .label("Authentication Server Url")
            .instructionText("Enter the Okta authentication url of your Guidewire instance to receive an authentication token " +
                "(Make sure to append with /<VERSION>/token).")
            .description("For example, https://guidewire-hub.okta.com/oauth2/<ID>/v1/token")
            .isRequired(true)
            .build(),
        textProperty(USER_CONTEXT_USERNAME)
            .label("User Context Username")
            .instructionText("Enter the proxy user's username.")
            .description("A proxy user is an internal user that is assigned to an external user or service when the API call is made." +
                "Learn more here: https://docs.guidewire.com/cloud/pc/202302/restapiframework/rest-framework/topics/S02_Authentication/06_services-userContext/c_overview-of-authentication-for-services-with-user-context.html")
            .isRequired(true)
            .build(),
        textProperty(SCOPES)
            .label("Scopes")
            .instructionText("Enter the scopes required to authenticate this service. All scopes must be space separated.")
            .description("For example, Policy Center scopes may look like 'tenant.<TENANT> project.gwcp planet_class.<PLANET_CLASS> " +
                "pc.service scp.pc.<TENANT>_serviceusercontext pc.allowusercontext'. Information about standalone service " +
                "scopes can be found here: https://docs.guidewire.com/cloud/pc/202205/cloudapica/cloudAPI/topics/71_Authentication/07_services-standalone/c_example-flow-for-standalone-services-pc.html")
            .isRequired(true)
            .build(),
        textProperty(CLIENT_ID)
            .label("Client ID")
            .instructionText("Enter your Guidewire Client ID.")
            .isRequired(true)
            .build(),
        textProperty(CLIENT_SECRET)
            .label("Client Secret")
            .instructionText("Enter your Guidewire Client Secret.")
            .isRequired(true)
            .masked(true)
            .build(),
        textProperty(AUTH_TYPE)
            .isHidden(true)
            .build()
    ).setValue(AUTH_TYPE, SERVICE_USER_CONTEXT);
  }

  @Override
  protected TestConnectionResult testConnection(SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {
    if (connectedSystemConfiguration.getValue(API_TYPE) == null ||
        connectedSystemConfiguration.getValue(ROOT_URL) == null ||
        connectedSystemConfiguration.getValue(AUTH_SERVER_URL) == null ||
        connectedSystemConfiguration.getValue(CLIENT_ID) == null ||
        connectedSystemConfiguration.getValue(USER_CONTEXT_USERNAME) == null ||
        connectedSystemConfiguration.getValue(CLIENT_SECRET) == null ||
        connectedSystemConfiguration.getValue(SCOPES) == null) {
      return TestConnectionResult.error(Collections.singletonList("Make sure to set all connected system values."));
    }

    // Get token
    HTTP httpService = new HTTP(connectedSystemConfiguration);
    try {
      httpService.retrieveToken();
    } catch (IOException | MimeTypeException e) {
      return TestConnectionResult.error(Arrays.asList(e.getCause().toString(), e.getMessage()));
    }

    if (httpService.getHttpError() != null) {
      HttpResponse httpError = httpService.getHttpError();
      return TestConnectionResult.error(
          Arrays.asList("Error " + httpError.getStatusCode(), httpError.getResponse().toString())
      );
    }
    return TestConnectionResult.success();
  }
}