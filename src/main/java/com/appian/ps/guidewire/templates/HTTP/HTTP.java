package com.appian.ps.guidewire.templates.HTTP;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.configuration.Document;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.ps.guidewire.templates.Execution.Execute;
import com.appian.ps.guidewire.templates.integrationTemplates.GuidewireIntegrationTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import std.ConstantKeys;

public class HTTP implements ConstantKeys {

  protected SimpleConfiguration integrationConfiguration;
  protected SimpleConfiguration connectedSystemConfiguration;
  protected ExecutionContext executionContext;
  protected HttpResponse httpError;
  protected HttpResponse httpResponse;
  protected String token;
  public ObjectMapper objectMapper = GuidewireIntegrationTemplate.objectMapper;


  // Used in execute
  public HTTP(Execute executionService) {
    this.integrationConfiguration = executionService.getIntegrationConfiguration();
    this.connectedSystemConfiguration = executionService.getConnectedSystemConfiguration();
    this.executionContext = executionService.getExecutionContext();
  }

  // Used in testConnection and getConfiguration
  public HTTP(SimpleConfiguration connectedSystemConfiguration) {
    this.connectedSystemConfiguration = connectedSystemConfiguration;
  }

  public void setHTTPError(HttpResponse httpError) { this.httpError = httpError;}
  public HttpResponse getHttpError() { return this.httpError; }

  public HttpResponse getHttpResponse() { return httpResponse; }
  public void setHttpResponse(HttpResponse httpResponse) { this.httpResponse = httpResponse; }

  public String getToken() { return token; }

  public void setToken(String token) { this.token = token; }

  // User context header for when authentication type is service with user context:
  // https://docs.guidewire.com/cloud/pc/202302/restapiframework/rest-framework/topics/S02_Authentication/06_services-userContext/c_overview-of-authentication-for-services-with-user-context.html
  public String createUserContextHeader() throws JsonProcessingException {
    String username = connectedSystemConfiguration.getValue(USER_CONTEXT_USERNAME).toString().trim();
    if (username == null) {
      setHTTPError(
          new HttpResponse(400, "Set username for Service with User Context in connected system.", new HashMap<>(), "", new HashMap<>())
      );
    }

    Map<String, String> userContextMap = new HashMap<>();
    userContextMap.put("sub", username);
    String usernameType = "";
    switch (connectedSystemConfiguration.getValue(API_TYPE).toString()) {
      case (POLICIES):
        usernameType = "pc_username";
        break;
      case (CLAIMS):
        usernameType = "cc_username";
        break;
      case (BILLING):
        usernameType = "bc_username";
        break;
      default:
        setHTTPError(
            new HttpResponse(400, "Set API Type in connected system.", new HashMap<>(), "", new HashMap<>())
        );
        break;
    }
    userContextMap.put(usernameType, username);
    String userContextHeader = objectMapper.writeValueAsString(userContextMap);
    return Base64.getEncoder().encodeToString(userContextHeader.getBytes());
  }

  public void retrieveToken() throws IOException, MimeTypeException {

    if (connectedSystemConfiguration.getValue(AUTH_TYPE).toString().equals(BASIC_AUTH)) {
      String username = connectedSystemConfiguration.getValue(USERNAME).toString().trim();
      String password = connectedSystemConfiguration.getValue(PASSWORD).toString().trim();
      String usernamePassword = username + ":" + password;
      setToken("Basic " + Base64.getEncoder().encodeToString(usernamePassword.getBytes()));
      return;
    }

    String clientId = connectedSystemConfiguration.getValue(CLIENT_ID).toString().trim();
    String clientSecret = connectedSystemConfiguration.getValue(CLIENT_SECRET).toString().trim();
    String authServerUrl = connectedSystemConfiguration.getValue(AUTH_SERVER_URL).toString().trim();
    String scopes = connectedSystemConfiguration.getValue(SCOPES).toString().trim();
    FormBody formBody = new FormBody.Builder()
        .addEncoded("client_id", clientId)
        .addEncoded("response_type", "token")
        .addEncoded("scope", scopes)
        .addEncoded("client_secret", clientSecret)
        .addEncoded("grant_type", "client_credentials")
        .build();
    Request.Builder request = new Request.Builder()
        .url(authServerUrl)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Accept", "application/json")
        .post(formBody);

    if (connectedSystemConfiguration.getValue(AUTH_TYPE).toString().equals(SERVICE_USER_CONTEXT)) {
      String userContextHeader = createUserContextHeader();
      request.header("GW-User-Context", userContextHeader);
    }

    // Execute request to get token
    executeRequest(new OkHttpClient(), request.build());
    if (getHttpError() != null) return;

    // Set token
    String token = getHttpResponse().getResponse().get(TOKEN_TYPE).toString() + " " +
        getHttpResponse().getResponse().get(ACCESS_TOKEN).toString();
    setToken(token);
  }

  public Request.Builder addRequestHeaders(Request.Builder request, String contentType) throws IOException, MimeTypeException {
    if (getToken() == null) retrieveToken();

    request.addHeader("Content-Type", contentType);
    request.addHeader("Authorization", getToken());

    if (integrationConfiguration != null &&
        integrationConfiguration.getValue(CHECKSUM_IN_HEADER) != null) {
      request.addHeader(CHECKSUM_IN_HEADER, integrationConfiguration.getValue(CHECKSUM_IN_HEADER));
    }
    // User context for when authentication type is service with user context
    if (connectedSystemConfiguration.getValue(USER_CONTEXT_USERNAME) != null) {
      String userContextHeader = createUserContextHeader();
      request.addHeader("GW-User-Context", userContextHeader);
    }
    return request;
  }

  public OkHttpClient getHTTPClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build();
  }

  public void executeRequest(OkHttpClient client, Request request) throws IOException, MimeTypeException {

    Response response = client.newCall(request).execute();
    // Set response properties
    ResponseBody body = response.body();
    int statusCode = response.code();
    String statusLine = response.message();
    String bodyStr = body.string();
    String contentType = response.header("Content-Type");
    Map<String, Object> headers = new HashMap<>(response.headers().toMultimap());
    // Check if null value is returned
    if (body == null) {
      setHTTPError(
          new HttpResponse(204, "Null value returned", new HashMap<String, Object>(){{put("Error","Response is empty");}},
              contentType, headers)
      );
      return;
    }

    // Making composite errors at least slightly readable
    if (integrationConfiguration != null && integrationConfiguration.getValue(COMPOSITE) != null && statusLine.equals("")) {
      Map<String, Object> bodyStrAsObj = objectMapper.readValue(bodyStr, Map.class);
      if (bodyStrAsObj.get("commitError") != null && ((Map)bodyStrAsObj.get("commitError")).get("cause")!= null ) {
        statusLine = ((Map)bodyStrAsObj.get("commitError")).get("cause").toString();
      }
    }

    Map<String,Object> responseEntity = new HashMap<>();
    // Normal json response sent back
    if (contentType != null && contentType.contains("application/json")) {
      // TODO: test replacing with readTree()
      responseEntity.putAll(objectMapper.readValue(bodyStr, new TypeReference<HashMap<String,Object>>() {}));
    } else {
      responseEntity.put("response", bodyStr);
    }

    // Set error if error is returned in response
    if (statusCode > 400 || !response.isSuccessful()) {
      setHTTPError(new HttpResponse(statusCode, statusLine, responseEntity, contentType, headers));
      return;
    }

    // If there is a document capture the document
   Optional<Object> isDocument = Optional.ofNullable(responseEntity.get("data"))
       .flatMap(data -> data instanceof LinkedHashMap ?
           Optional.ofNullable(((LinkedHashMap)data).get("attributes")) :
           Optional.empty())
       .flatMap(attributes -> attributes instanceof LinkedHashMap ?
           Optional.ofNullable(((LinkedHashMap)attributes).get("contents")) :
           Optional.empty());

    if (integrationConfiguration != null && isDocument.isPresent()) {
      String documentContent = isDocument.get().toString();
      String mimeType =
          ((LinkedHashMap)((LinkedHashMap)responseEntity.get("data")).get("attributes")).get("responseMimeType").toString();
      String extension = MimeTypes.getDefaultMimeTypes().forName(mimeType).getExtension();


      // If there is an incoming file, save it in the desired location with the desired name
      // Set errors if no name or file location has been chosen
      PropertyDescriptor<?> hasSaveFolder = integrationConfiguration.getProperty(FOLDER);
      PropertyDescriptor<?> hasSaveFileName = integrationConfiguration.getProperty(SAVED_FILENAME);
      if (hasSaveFolder == null) {
        setHTTPError(new HttpResponse(statusCode, FILE_SAVING_ERROR_TITLE + FOLDER_LOCATION_ERROR_MESSAGE, responseEntity,
            contentType, headers));
        return;
      } else if (hasSaveFileName == null) {
        setHTTPError(new HttpResponse(statusCode, FILE_SAVING_ERROR_TITLE + FILE_NAME_ERROR_MESSAGE, responseEntity,
            contentType, headers));
        return;
      }

      // Extracting files from the response body and saving them to Appian
      Long folderID = integrationConfiguration.getValue(FOLDER);
      String fileName = integrationConfiguration.getValue(SAVED_FILENAME) + extension;
      List<Document> documents = new ArrayList<>();
      byte[] decodedBytes = Base64.getDecoder().decode(documentContent);
      InputStream inputStream = new ByteArrayInputStream(decodedBytes);

      // adding doc to map to be returned to Appian
      Document document = executionContext
          .getDocumentDownloadService()
          .downloadDocument(inputStream, folderID, fileName);
      documents.add(document);

      setHttpResponse(new HttpResponse(statusCode, statusLine, responseEntity, contentType, headers, documents));
      return;
    }

    // If no document, just return the response
    setHttpResponse(new HttpResponse(statusCode, statusLine, responseEntity, contentType, headers));
  }


  public void get(String url) throws IOException, MimeTypeException {
    Request.Builder request = addRequestHeaders(new Request.Builder().url(url).get(), "application/json");
    OkHttpClient client = getHTTPClient();
    executeRequest(client, request.build());
  }

  public void post(String url, RequestBody body) throws IOException, MimeTypeException {
    Request.Builder request = addRequestHeaders(new Request.Builder().url(url).post(body), "application/json");
    OkHttpClient client = getHTTPClient();
    executeRequest(client, request.build());
  }

  public void patch(String url, RequestBody body) throws IOException, MimeTypeException {
    Request.Builder request = addRequestHeaders(new Request.Builder().url(url).patch(body), "application/json");
    OkHttpClient client = getHTTPClient();
    executeRequest(client, request.build());
  }

  public void delete(String url) throws IOException, MimeTypeException {
    Request.Builder request = addRequestHeaders(new Request.Builder().url(url).delete(), "application/json");
    OkHttpClient client = getHTTPClient();
    executeRequest(client, request.build());
  }
}
