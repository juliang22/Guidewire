package std;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.appian.connectedsystems.templateframework.sdk.configuration.Document;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.guidewire.templates.Execution.Execute;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HTTP implements ConstantKeys {
  protected Execute executionService;

  public HTTP(Execute executionService) {
    this.executionService = executionService;
  }

  public static OkHttpClient getHTTPClient(Execute executionService, String contentType) {
    SimpleConfiguration connectedSystemConfiguration = executionService.getConnectedSystemConfiguration();
    String username = connectedSystemConfiguration.getValue(USERNAME);
    String password = connectedSystemConfiguration.getValue(PASSWORD);
    String usernamePassword = username + ":" + password;
    String encodedCredentials = Base64.getEncoder().encodeToString(usernamePassword.getBytes());

    return new OkHttpClient.Builder().connectTimeout(2, TimeUnit.MINUTES)
        .connectTimeout(2, TimeUnit.MINUTES)
        .readTimeout(2, TimeUnit.MINUTES)
        .callTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .addInterceptor(chain -> {
          Request.Builder newRequest = chain.request()
              .newBuilder()
              .addHeader("Content-Type", contentType)
              .addHeader("Authorization", "Basic " + encodedCredentials);
          if (executionService.getIntegrationConfiguration().getProperty(CHECKSUM_IN_HEADER) != null &&
              executionService.getIntegrationConfiguration().getValue(CHECKSUM_IN_HEADER) != null) {
              newRequest.addHeader(CHECKSUM_IN_HEADER, executionService.getIntegrationConfiguration().getValue(CHECKSUM_IN_HEADER));
          }
          return chain.proceed(newRequest.build());
        })
        .build();
  }

  public static Map<String, Object> testAuth(SimpleConfiguration connectedSystemConfiguration, String url) throws IOException {

    String username = connectedSystemConfiguration.getValue(USERNAME).toString().trim();
    String password = connectedSystemConfiguration.getValue(PASSWORD).toString().trim();

    String usernamePassword = username + ":" + password;
    String encodedCredentials = Base64.getEncoder().encodeToString(usernamePassword.getBytes());

    Request request = new Request.Builder().url(url).build();
    OkHttpClient client = new OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(chain -> {
          Request.Builder newRequest = chain.request()
              .newBuilder()
              .addHeader("Content-Type", "application/json")
              .addHeader("Authorization", "Basic " + encodedCredentials);
          return chain.proceed(newRequest.build());
        })
        .build();

    try {
      Response response = client.newCall(request).execute();
      ResponseBody body = response.body();
      if (body == null) {
        Map<String, Object> errorResponse = new HashMap<>();
        List<String> error = Arrays.asList("Null value return. Check to make sure " +
            "your Base Url is in the correct format. For example, https://cc-gwcpdev.saappian.zeta1-andromeda.guidewire.net");
        errorResponse.put("error", error);
        return errorResponse;
      }

      // Set response properties
      int code = response.code();
      String message = response.message();
      String bodyStr = body.string();

      // Set error if error is returned in response
      if (code > 400 || !response.isSuccessful()) {
        Map<String, Object> errorResponse = new HashMap<>();
        List<String> errors = Arrays.asList("Error Code: " + code, message, bodyStr);
        errorResponse.put("error", errors);
        return errorResponse;
      }

      // Normal json response sent back
      if (bodyStr.length() > 0) {
        Map<String, Object> result = new HashMap<String, Object>(){{ put("result", bodyStr); }};
        return result;
      }
    } catch (IOException e) {
      // TODO: error handle
      throw new RuntimeException(e);
    }

    // TODO: base case
    return null;
  }

  public HttpResponse executeRequest(OkHttpClient client, Request request) throws IOException {

    try (Response response = client.newCall(request).execute()) {
      // Check if null value is returned
      ResponseBody body = response.body();
      if (body == null) {
        return new HttpResponse(204,
            "Null value returned", new HashMap<String, Object>(){{put("Error","Response is empty");}});
      }

      // Set response properties
      int code = response.code();
      String message = response.message();
      String bodyStr = body.string();
      HashMap<String,Object> responseEntity = new HashMap<>();

      // Normal json response sent back
      if (bodyStr.length() > 0) {
        responseEntity.putAll(executionService.getObjectMapper().readValue(bodyStr, new TypeReference<HashMap<String,Object>>() {}));
      }

      // Set error if error is returned in response
      if (code > 400 || !response.isSuccessful()) {
        executionService.setError("Error Code: " + code, message, bodyStr);
      }

      // If there is a document capture the document
     Optional<Object> isDocument = Optional.ofNullable(responseEntity.get("data"))
         .flatMap(data -> data instanceof LinkedHashMap ?
             Optional.ofNullable(((LinkedHashMap)data).get("attributes")) :
             Optional.empty())
         .flatMap(attributes -> attributes instanceof LinkedHashMap ?
             Optional.ofNullable(((LinkedHashMap)attributes).get("contents")) :
             Optional.empty());

      if (isDocument.isPresent()) {
        String documentContent = isDocument.get().toString();
        String mimeType =
            ((LinkedHashMap)((LinkedHashMap)responseEntity.get("data")).get("attributes")).get("responseMimeType").toString();
        String extension = MimeTypes.getDefaultMimeTypes().forName(mimeType).getExtension();


        // If there is an incoming file, save it in the desired location with the desired name
        // Set errors if no name or file location has been chosen
        PropertyDescriptor<?> hasSaveFolder = executionService.getIntegrationConfiguration().getProperty(FOLDER);
        PropertyDescriptor<?> hasSaveFileName = executionService.getIntegrationConfiguration().getProperty(SAVED_FILENAME);
        if (hasSaveFolder == null) {
          executionService.setError(FILE_SAVING_ERROR_TITLE, FOLDER_LOCATION_ERROR_MESSAGE, "");
          return new HttpResponse(code, message, responseEntity);
        } else if (hasSaveFileName == null) {
          executionService.setError(FILE_SAVING_ERROR_TITLE, FILE_NAME_ERROR_MESSAGE, "");
          return new HttpResponse(code, message, responseEntity);
        }

        // Extracting files from the response body and saving them to Appian
        Long folderID = executionService.getIntegrationConfiguration().getValue(FOLDER);
        String fileName = executionService.getIntegrationConfiguration().getValue(SAVED_FILENAME) + extension;
        List<Document> documents = new ArrayList<>();
        byte[] decodedBytes = Base64.getDecoder().decode(documentContent);
        InputStream inputStream = new ByteArrayInputStream(decodedBytes);

        // adding doc to map to be returned to Appian
        Document document = executionService
            .getExecutionContext()
            .getDocumentDownloadService()
            .downloadDocument(inputStream, folderID, fileName);
        documents.add(document);


        return new HttpResponse(code, message, responseEntity, documents);
      }

      // If no document, just return the response
      return new HttpResponse(code, message, responseEntity);
    } catch (MimeTypeException e) {
      throw new RuntimeException(e);
    }
  }

  public HttpResponse get(String url) throws IOException {
    Request request = new Request.Builder().url(url).build();
    OkHttpClient client = getHTTPClient(executionService, "application/json");
    return executeRequest(client, request);
  }

  public HttpResponse post(String url, RequestBody body)
      throws IOException {
    Request request = new Request.Builder().url(url).post(body).build();
    OkHttpClient client = getHTTPClient(executionService, "application/json");
    return executeRequest(client, request);
  }

  public HttpResponse patch(String url, RequestBody body)
      throws IOException {
    Request request = new Request.Builder().url(url).patch(body).build();
    OkHttpClient client = getHTTPClient(executionService, "application/json");
    return executeRequest(client, request);
  }

  public HttpResponse multipartPost(String url, Map<String,Object> requestBody, Map<String,File> files)
      throws IOException {

    MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

    // Adding text request body json to the multipart builder
    requestBody.forEach((key, val) -> {
      if (val instanceof String) {
        multipartBuilder.addFormDataPart(key, ((String)val));
      } else {
        String jsonString;
        try {
          jsonString = executionService.getObjectMapper().writeValueAsString(val);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
        multipartBuilder.addFormDataPart(key, jsonString);
      }
    });

    // Adding files to the multipart builder
    files.forEach((fileName, file) -> {
      RequestBody requestFile = RequestBody.create(file, MediaType.parse("multipart/form-data"));
      MultipartBody.Part filePart = MultipartBody.Part.createFormData(fileName, file.getName(), requestFile);
      multipartBuilder.addPart(filePart);
    });

    // Getting the client/request and executing the request
    OkHttpClient client = getHTTPClient(executionService, "multipart/form-data");
    Request request = new Request.Builder().url(url).post(multipartBuilder.build()).build();
    return executeRequest(client, request);
  }

  public HttpResponse delete(String url) throws IOException {
    Request request = new Request.Builder().url(url).delete().build();
    OkHttpClient client = getHTTPClient(executionService, "application/json");
    return executeRequest(client, request);
  }

}
