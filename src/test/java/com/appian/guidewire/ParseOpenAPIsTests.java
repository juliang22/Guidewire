package com.appian.guidewire;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.appian.guidewire.templates.GuidewireCSP;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import std.HTTP;
import std.Util;

public class ParseOpenAPIsTests {

/*  @Test
  public void swagger() {


    try (InputStream input = GuidewireCSP.classLoader.getResourceAsStream("com/appian/guidewire/templates/test.json")) {
      String content = IOUtils.toString(input, StandardCharsets.UTF_8);
      ParseOptions parseOptions = new ParseOptions();
      parseOptions.setResolve(true); // implicit
      parseOptions.setResolveFully(true);
                  parseOptions.setResolveCombinators(false);
*//*      SwaggerParseResult result = new OpenAPIParser().readContents(content, null, null);*//*
      OpenAPI result = new OpenAPIV3Parser().readContents(content, null, parseOptions).getOpenAPI();
      System.out.println(result);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }*/

  @Test
  public void bananas() throws JsonProcessingException {
    String sampleOpenAPI = "openapi: 3.0.0\n" + "info:\n" + "  title: Sample API\n" + "  version: 1.0.0\n" +
        "  description: This is a sample API definition using OpenAPI 3.0\n" + "servers:\n" +
        "  - url: https://api.example.com/v1\n" + "    description: Production server\n" +
        "  - url: https://api.staging.example.com/v1\n" + "    description: Staging server\n" + "paths:\n" + "  /users:\n" +
        "    get:\n" + "      summary: Get a list of users\n" + "      operationId: getUsers\n" + "      responses:\n" +
        "        '200':\n" + "          description: Successful response\n" + "          content:\n" +
        "            application/json:\n" + "              schema:\n" + "                type: array\n" +
        "                items:\n" + "                  type: object\n" + "                  properties:\n" +
        "                    id:\n" + "                      type: integer\n" + "                      description: User ID\n" +
        "                    name:\n" + "                      type: string\n" +
        "                      description: User name\n" + "    post:\n" + "      summary: Create a new user\n" +
        "      operationId: createUser\n" + "      requestBody:\n" + "        required: true\n" + "        content:\n" +
        "          application/json:\n" + "            schema:\n" + "              type: object\n" +
        "              properties:\n" + "                name:\n" + "                  type: string\n" +
        "                  description: User name\n" + "                email:\n" + "                  type: string\n" +
        "                  format: email\n" + "                  description: User email address\n" + "      responses:\n" +
        "        '201':\n" + "          description: User created successfully\n" + "        '400':\n" +
        "          description: Invalid request\n" + "  /users/{id}:\n" + "    get:\n" + "      summary: Get a user by ID\n" +
        "      operationId: getUserById\n" + "      parameters:\n" + "        - name: id\n" + "          in: path\n" +
        "          description: User ID\n" + "          required: true\n" + "          schema:\n" +
        "            type: integer\n" + "      responses:\n" + "        '200':\n" +
        "          description: Successful response\n" + "          content:\n" + "            application/json:\n" +
        "              schema:\n" + "                type: object\n" + "                properties:\n" +
        "                  id:\n" + "                    type: integer\n" + "                    description: User ID\n" +
        "                  name:\n" + "                    type: string\n" + "                    description: User name\n" +
        "    put:\n" + "      summary: Update a user by ID\n" + "      operationId: updateUserById\n" + "      parameters:\n" +
        "        - name: id\n" + "          in: path\n" + "          description: User ID\n" + "          required: true\n" +
        "          schema:\n" + "            type: integer\n" + "      requestBody:\n" + "        required: true\n" +
        "        content:\n" + "          application/json:\n" + "            schema:\n" + "              type: object\n" +
        "              properties:\n" + "                name:\n" + "                  type: string\n" +
        "                  description: User name\n" + "                email:\n" + "                  type: string\n" +
        "                  format: email\n" + "                  description: User email address\n" + "      responses:\n" +
        "        '200':\n" + "          description: User updated successfully\n" + "        '400':\n" +
        "          description: Invalid request\n";
    ObjectMapper objectMapper = new ObjectMapper();
    long startTime, endTime;
    long executionTimeFunction1, executionTimeFunction2;

    // 1. turn openAPI str into OpenAPI object, turn that object to string, turn that string into OpenAPI object
    OpenAPI openAPI = Util.getOpenAPI(sampleOpenAPI);
    String openAPIInfoStr = objectMapper.writeValueAsString(openAPI);
    startTime = System.currentTimeMillis();

    objectMapper.readValue(openAPIInfoStr, OpenAPI.class);
    endTime = System.currentTimeMillis();
    executionTimeFunction1 = endTime - startTime;
    System.out.println("1. " + executionTimeFunction1);

    // 2.
    startTime = System.currentTimeMillis();
    OpenAPI openAPI2 = Util.getOpenAPI(sampleOpenAPI);
    endTime = System.currentTimeMillis();
    executionTimeFunction2 = endTime - startTime;
    System.out.println("2. " + executionTimeFunction2);






  }





/*@Test
  public void getGetOptions() {

  OpenAPI claimsOpenApi = Util.getOpenApi("com/appian/guidewire/templates/claims/claims_claim.yaml", GuidewireCSP.classLoader);
  *//*String pathName = "/claim-infos";*//*
  String pathName = "/claim-infos/{claimInfoId}";
  Operation get = claimsOpenApi.getPaths().get(pathName).getGet();

  Map returnedFieldProperties = get.getResponses()
      .get("200")
      .getContent()
      .get("application/json")
      .getSchema()
      .getProperties();

  if (returnedFieldProperties != null) {
    Schema returnedFieldItems= ((Schema)returnedFieldProperties.get("data")).getItems();
    if (returnedFieldItems!= null) {
      Map returnedFields = ((Schema)returnedFieldItems
          .getProperties()
          .get("attributes"))
          .getProperties();

      returnedFields.forEach((key, val) -> {
        Map extensions = ((Schema)val).getExtensions();
        if (extensions != null && extensions.get("x-gw-extensions") instanceof LinkedHashMap) {
          Object isFilterable = ((LinkedHashMap<?,?>)extensions.get("x-gw-extensions")).get("filterable");
          Object isSortable = ((LinkedHashMap<?,?>)extensions.get("x-gw-extensions")).get("sortable");
          if (isFilterable != null) {
            System.out.println(key + " is filterable");
          }
          if (isSortable != null) {
            System.out.println(key + " is sortable");
          } else {
            System.out.println("KEY "+ key);
          }
        }
      });
    }

  }


  Schema hasIncludedResources = ((Schema)get.getResponses()
      .get("200")
      .getContent()
      .get("application/json")
      .getSchema()
      .getProperties()
      .get("included"));
  if (hasIncludedResources != null) {
    Set included = hasIncludedResources.getProperties().keySet();
    System.out.println(included);
  }
  // TODO: directions for pageOffset with the next keyword in links (for data source queries for sync)
  // TODO: set includeTotal to true
  // TODO: Maximum pageSize doesn't seem to exist anywhere




 *//* get.getParameters().forEach((queryParam) -> {
*//**//*        System.out.println("HERE"+queryParams.getIn() +queryParams.getName());*//**//*

        if (queryParam.getName().equals("include")) {          System.out.println("INLCUDES"+((Schema)get
              .getResponses()
              .get("200")
              .getContent()
              .get("application/json")
              .getSchema()
              .getProperties()
              .get("included")));

          Map included = ((Schema)get
              .getResponses()
              .get("200")
              .getContent()
              .get("application/json")
              .getSchema()
              .getProperties()
              .get("included")).getProperties();
          System.out.println("Param "+ queryParam.getName() + " "+ included.keySet());
        } else if (queryParam.getName().equals("filter")) {
          // TODO: only when x-gw-extensions: filterable: true
          System.out.println("Param "+ queryParam.getName());
        } else if (queryParam.getName().equals("pageSize")) {
          System.out.println("Param "+ queryParam.getName());
        } else if (queryParam.getName().equals("sort")) {
          // TODO: only when x-gw-extensions: sortable: true
          System.out.println("Param "+ queryParam.getName());
        } else if (queryParam.getName().equals("fields")) {
          // TODO: only when x-gw-extensions: sortable: true

          System.out.println("Param "+ queryParam.getName());
        }
      }
  );*//*

}*/






/*
  @Test
  public void pets() {
    OpenAPI openAPI = Util.getOpenApi("com/appian/guidewire/templates/petstore3.yaml");
    Schema schema =
        openAPI.getPaths().get("/user/createWithList").getPost().getRequestBody().getContent().get("application/json").getSchema();
    System.out.println(schema.getItems());
  }

  @Test
  public void test_ParseMany() throws Exception {
      OpenAPI openAPI = Util.getOpenApi("com/appian/guidewire/templates/claims_claim.yaml");
        openAPI.getPaths().entrySet().forEach(s -> {
            PathItem path = s.getValue();

            if(s.getKey().equals("/batch")) return;
            if(s.getKey().equals("/policies/{policyId}/contingencies/{contingencyId}/documents")) return;
            if(s.getKey().equals("/policies/{policyId}/documents")) return;

            if(s.getKey().equals("/claims/{claimId}/documents/{policyId}/contingencies/{contingencyId}/documents")) return;
            if(s.getKey().equals("/claims/{claimId}/documents")) return;
            if(s.getKey().equals("/claims/{claimId}/documents/{documentId}")) return;
            if(s.getKey().equals("/claims/{claimId}/documents/{documentId}/documents")) return;


            if (path.getPost() != null && path.getPost().getRequestBody() != null) {



              String ref = openAPI.getPaths()
                  .get("/claims/{claimId}/activities")
                  .getPost()
                  .getRequestBody()
                  .getContent()
                  .get("application/json")
                  .getSchema()
                  .get$ref();

              ref = ref.substring(ref.lastIndexOf("/")+1);



              Schema data = (Schema)openAPI.getComponents().getSchemas().get(ref).getProperties().get("data");

              String newRef = data.get$ref().substring(data.get$ref().lastIndexOf("/")+1);
              System.out.println(newRef);
              Schema attributes = (Schema) openAPI.getComponents().getSchemas().get(newRef).getProperties().get(
                  "attributes");
              String lastOne = attributes.get$ref();
              String lastOneStr = lastOne.substring(lastOne.lastIndexOf("/")+1);
              System.out.println(lastOneStr);
              System.out.println(openAPI.getComponents().getSchemas().get(lastOneStr).getProperties());





                Object schema = path.getPost()
                    .getRequestBody()
                    .getContent()
                    .get("application/json")
                    .getSchema()
                    .getProperties()
                    .get("data");
                ((ObjectSchema)schema).getProperties().get("attributes").getProperties().forEach((key, item) -> {
                    Schema itemSchema = ((Schema)item);

                    if (itemSchema.getType().equals("object")) {
                        System.out.println(key + " : " + itemSchema.getType());
                        itemSchema.getProperties().forEach((innerKey, innerItem) -> {
                            Schema innerItemSchema = ((Schema)innerItem);
                            System.out.println("        "+innerKey + " : " + innerItemSchema.getType());

                        });
                    } else {
                        System.out.println(key + " : " + itemSchema.getType());
                    }
                });
            }

            if (path.getPatch() != null) {
                System.out.println("PATCH");
                Object schema = path.getPatch()
                    .getRequestBody()
                    .getContent()
                    .get("application/json")
                    .getSchema()
                    .getProperties()
                    .get("data");
                ((ObjectSchema)schema).getProperties().get("attributes").getProperties().forEach((key, item) -> {
                    Schema itemSchema = ((Schema)item);

                    if (itemSchema.getType().equals("object")) {
                        System.out.println(key + " : " + itemSchema.getType());
                        itemSchema.getProperties().forEach((innerKey, innerItem) -> {
                            Schema innerItemSchema = ((Schema)innerItem);
                            System.out.println("        "+innerKey + " : " + innerItemSchema.getType());

                        });
                    } else {
                        System.out.println(key + " : " + itemSchema.getType());
                    }
                });
            }
        });
  }

  public void isObj(Object key, Schema itemSchema) {
    System.out.println(key + " : " + itemSchema.getType());

    if (itemSchema.getProperties() == null) {
      return;
    }

    itemSchema.getProperties().forEach((innerKey, innerItem) -> {
      Schema innerItemSchema = ((Schema)innerItem);
      System.out.println("        " + innerKey + " : " + innerItemSchema.getType());
    });
  }


    *//*        System.out.println(ParseOpenAPI.initializePaths(ConstantKeys.CLAIMS));*//*

    // Checks if parser works on all post/patch paths
    // - Works for policy_policies.yaml (which doesn't have patch paths for some reason, might have to try
    // getting the openapi 3.0 schema from that endpoint. Excludes document paths which have a
    // different parsing structure.
    // - Almost working for Claims, excluding documents and
    // /claims/{claimId}/service-requests/{serviceRequestId}/invoices has a different structure to work
    // out, Will come back to it once I actually need to build requestBodies
    *//*  OpenAPI openAPI = Util.getOpenApi("com/appian/guidewire/templates/policy_policies.yaml");
        openAPI.getPaths().entrySet().forEach(s -> {
            PathItem path = s.getValue();

            if(s.getKey().equals("/batch")) return;
            if(s.getKey().equals("/policies/{policyId}/contingencies/{contingencyId}/documents")) return;
            if(s.getKey().equals("/policies/{policyId}/documents")) return;

            if(s.getKey().equals("/claims/{claimId}/documents/{policyId}/contingencies/{contingencyId}/documents")) return;
            if(s.getKey().equals("/claims/{claimId}/documents")) return;
            if(s.getKey().equals("/claims/{claimId}/documents/{documentId}")) return;
            if(s.getKey().equals("/claims/{claimId}/documents/{documentId}/documents")) return;

            System.out.println(s.getKey());
            if (path.getPost() != null && path.getPost().getRequestBody() != null) {
                Object schema = path.getPost()
                    .getRequestBody()
                    .getContent()
                    .get("application/json")
                    .getSchema()
                    .getProperties()
                    .get("data");
                ((ObjectSchema)schema).getProperties().get("attributes").getProperties().forEach((key, item) -> {
                    Schema itemSchema = ((Schema)item);

                    if (itemSchema.getType().equals("object")) {
                        System.out.println(key + " : " + itemSchema.getType());
                        itemSchema.getProperties().forEach((innerKey, innerItem) -> {
                            Schema innerItemSchema = ((Schema)innerItem);
                            System.out.println("        "+innerKey + " : " + innerItemSchema.getType());

                        });
                    } else {
                        System.out.println(key + " : " + itemSchema.getType());
                    }
                });
            }

            if (path.getPatch() != null) {
                System.out.println("PATCH");
                Object schema = path.getPatch()
                    .getRequestBody()
                    .getContent()
                    .get("application/json")
                    .getSchema()
                    .getProperties()
                    .get("data");
                ((ObjectSchema)schema).getProperties().get("attributes").getProperties().forEach((key, item) -> {
                    Schema itemSchema = ((Schema)item);

                    if (itemSchema.getType().equals("object")) {
                        System.out.println(key + " : " + itemSchema.getType());
                        itemSchema.getProperties().forEach((innerKey, innerItem) -> {
                            Schema innerItemSchema = ((Schema)innerItem);
                            System.out.println("        "+innerKey + " : " + innerItemSchema.getType());

                        });
                    } else {
                        System.out.println(key + " : " + itemSchema.getType());
                    }
                });
            }
        });*//*







    *//*        assertNotNull(openAPI);*//*

  }*/
}
