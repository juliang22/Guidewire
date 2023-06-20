package com.appian.guidewire;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import std.ConstantKeys;
import std.Util;

public class ParseOpenAPIsTests implements ConstantKeys {

  public String formatParsingPath(JsonNode root, String[] path) {

    StringBuilder formattedPath = new StringBuilder("/");
    for (String loc : path) {
      String formattedLoc = loc.replace("/", "~1"); // Formatting "/" to work with .at()
      formattedPath.append(formattedLoc);
      formattedPath.append("/");
    }
    return formattedPath.toString();
  }

  public JsonNode parse(JsonNode root, List<String> path) throws IOException {
    ClassLoader classLoader = ParseOpenAPIsTests.class.getClassLoader();
    InputStream input = classLoader.getResourceAsStream("com/appian/guidewire/templates/claimsv2.json");
    String swaggerStr = IOUtils.toString(input, StandardCharsets.UTF_8);

    JsonNode openApi = new ObjectMapper().readValue(swaggerStr, JsonNode.class);


    JsonNode currNode = root;

    for (int i = 0; i < path.size(); i++) {
      String loc = path.get(i);
      currNode = currNode.get(loc);
      if (currNode == null) return null;

      if (currNode.has(REF)) {
        String newLoc = currNode.get(REF).asText().replace("#/", "/");
        currNode = openApi.at(newLoc);
      }
    }
    return currNode;
  }

  public List<JsonNode> getRefs(JsonNode arrOfRefStrs, JsonNode root) {
    if (arrOfRefStrs == null || arrOfRefStrs.size() == 0) return null;

    List<JsonNode> refNodeArr = new ArrayList<>();
    arrOfRefStrs.forEach(refNode -> {
      Optional.ofNullable(refNode.get("$ref"))
          .ifPresent(refStr -> {
            String refLocation = refStr.asText().replace("#/", "/");
            refNodeArr.add(root.at(refLocation));
          });
    });
    return refNodeArr;
  }

@Test
public void testingParsing() throws IOException {
  ClassLoader classLoader = ParseOpenAPIsTests.class.getClassLoader();
  InputStream input = classLoader.getResourceAsStream("com/appian/guidewire/templates/claimsv2.json");
  String swaggerStr = IOUtils.toString(input, StandardCharsets.UTF_8);

  JsonNode openApi = new ObjectMapper().readValue(swaggerStr, JsonNode.class);
  JsonNode paths = openApi.get("paths");
  String pathName = "/claims/{claimId}/documents/{documentId}/content";
  JsonNode get = parse(paths, Arrays.asList(pathName, GET));

  JsonNode data = parse(get, Arrays.asList(RESPONSES, "200", CONTENT, APPLICATION_JSON, SCHEMA, PROPERTIES, DATA));
  String hasDocs = parse(data, Arrays.asList(ITEMS)) == null ?
      parse(data, Arrays.asList(PROPERTIES, ATTRIBUTES, "title")).asText() :
      parse(data, Arrays.asList(ITEMS, PROPERTIES, ATTRIBUTES, "title")).asText();

  if (hasDocs.equals("Document")) {

  }


}

  @Test
  public void swagger() throws IOException  {

    ClassLoader classLoader = ParseOpenAPIsTests.class.getClassLoader();
    InputStream input = classLoader.getResourceAsStream("com/appian/guidewire/templates/claimsv2.json");
    String swaggerStr = IOUtils.toString(input, StandardCharsets.UTF_8);

    // Getting OpenAPI obj
    long startTime = System.nanoTime();
    OpenAPI openAPI = Util.getOpenAPI(swaggerStr);
    System.out.println("Getting OpenAPI obj: " + (System.nanoTime() - startTime)/1000000 + " milliseconds. ");



    // Parsing OpenAPI obj
    startTime = System.nanoTime();
    Optional<Schema> schema = Optional.ofNullable(openAPI.getPaths().get("/claims/{claimId}/activities"))
        .map(PathItem::getPost)
        .map(Operation::getRequestBody)
        .map(RequestBody::getContent)
        .map(content -> content.get("application/json"))
        .map(MediaType::getSchema)
        .map(Schema::getProperties)
        .map(properties -> properties.get("data"))
        .map(data -> ((ObjectSchema)data).getProperties())
        .map(dataMap -> dataMap.get("attributes"));
    System.out.println("Getting props: " + (System.nanoTime() - startTime)/1000000 + " milliseconds. ");


    // Getting Jackson parser obj
    ObjectMapper mapper = new ObjectMapper();
    startTime = System.nanoTime();
    JsonNode map = mapper.readValue(swaggerStr, JsonNode.class);
    System.out.println("My own parsing, getting openapi parser: " + (System.nanoTime() - startTime)/1000000 + " milliseconds. ");

    // Parsing Jackson parser obj with Optionals
    startTime = System.nanoTime();
    Optional<JsonNode> allSchemas = Optional.ofNullable(map)
        .map(c -> c.get("components"))
        .map(s -> s.get("schema"));

    Optional<JsonNode> schema1 = Optional.ofNullable(map)
        .map(p -> p.get("paths"))
        .map(p -> p.get("/claims/{claimId}/activities"))
        .map(p -> p.get("post"))
        .map(p -> p.get("requestBody"))
        .map(p -> p.get("content"))
        .map(p -> p.get("application/json"))
        .map(p -> p.get("schema"));

    if (allSchemas.isPresent()) {
      allSchemas
          .map(p -> p.get("ActivityRequest"))
          .map(p -> p.get("properties"))
          .map(p -> p.get("data"));
    }

    if (allSchemas.isPresent()) {
      allSchemas
          .map(p -> p.get("ActivityData"))
          .map(p -> p.get("properties"))
          .map(p -> p.get("attributes"));
    }
    System.out.println("My own parsing, getting props: " + (System.nanoTime() - startTime)/1000000 + " milliseconds. ");


    // Parsing Jackson obj with .at()
    startTime = System.nanoTime();
    String pathName = "/claims/{claimId}/activities";
    List<String> path = Arrays.asList(PATHS, pathName, POST, REQUEST_BODY, CONTENT, APPLICATION_JSON, SCHEMA, PROPERTIES, DATA,
        PROPERTIES, ATTRIBUTES);
    JsonNode res = parse(map, path);
    System.out.println("My own parsing2, getting props: " + (System.nanoTime() - startTime)/1000000 + " milliseconds. ");



    // Compress the string
    startTime = System.nanoTime();
    String compressed = Util.compress(swaggerStr);
    System.out.println("Compression Time: " + (System.nanoTime() - startTime)/1000000 + " milliseconds." + "Str Length:" + compressed.length());

    // Decompress the string
    startTime = System.nanoTime();
    String decompressed = Util.decompress(compressed);
    System.out.println("Decompression Time: " + (System.nanoTime() - startTime)/1000000 + " milliseconds" + "Str Length:" + decompressed.length());
  }




  @Test
  public void testPathVars() throws IOException {

    ClassLoader classLoader = ParseOpenAPIsTests.class.getClassLoader();
    InputStream input = classLoader.getResourceAsStream("com/appian/guidewire/templates/claimsv2.json");
    String swaggerStr = IOUtils.toString(input, StandardCharsets.UTF_8);

    JsonNode openAPI2 = new ObjectMapper().readValue(swaggerStr, JsonNode.class);
    JsonNode paths2 = Optional.ofNullable(openAPI2)
        .map(openapi -> openapi.get("paths"))
        .orElse(null);

    String pathName = "/claims/{claimId}/activities";
    Optional.ofNullable(parse(paths2, Arrays.asList(pathName, PARAMETERS)))
        .map(parameters -> getRefs(parameters, openAPI2))
        .ifPresent(refs -> refs.forEach(ref -> {
          System.out.println(ref.get("name"));
        }));
  }




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

}
