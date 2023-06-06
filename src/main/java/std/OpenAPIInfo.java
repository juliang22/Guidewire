package std;

import io.swagger.v3.oas.models.OpenAPI;

public class OpenAPIInfo {

  protected String name;
  protected String key;
  protected String url;

  protected String description;

  protected OpenAPI openAPI;

  public OpenAPIInfo(String name, String key, String url, String description, OpenAPI openAPI) {
    this.name = name;
    this.key = key;
    this.url = url;
    this.description = description;
    this.openAPI = openAPI;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public OpenAPI getOpenAPI() {
    return openAPI;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

}
