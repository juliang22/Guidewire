package com.appian.guidewire.templates.UI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.DocumentPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.ParagraphHeight;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptorBuilder;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.guidewire.templates.apis.GuidewireIntegrationTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import std.ConstantKeys;
import std.Util;

public abstract class UIBuilder implements ConstantKeys {

  protected String api;
  protected String subApi;
  protected String pathName;
  protected String restOperation;
  protected List<PropertyDescriptor<?>> pathVarsUI = new ArrayList<>();
  protected OpenAPI openAPI = null;
  protected JsonNode openAPI2;

  protected Paths paths;
  protected JsonNode paths2;
/*  protected List<String> choicesForSearch = new ArrayList<>();
  protected List<Choice> defaultChoices = new ArrayList<>();*/
  protected SimpleIntegrationTemplate simpleIntegrationTemplate;
  protected SimpleConfiguration integrationConfiguration;
  protected SimpleConfiguration connectedSystemConfiguration;
  protected PropertyPath propertyPath;
  protected ObjectMapper objectMapper = new ObjectMapper();

  public UIBuilder(GuidewireIntegrationTemplate simpleIntegrationTemplate,
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      PropertyPath propertyPath) {
    this.simpleIntegrationTemplate = simpleIntegrationTemplate;
    this.integrationConfiguration = integrationConfiguration;
    this.connectedSystemConfiguration = connectedSystemConfiguration;
    this.propertyPath = propertyPath;
  }

  // Methods to implement when building out the API specific details of each request
  public abstract void buildRestCall(String restOperation, List<PropertyDescriptor<?>> result, String pathName);

  public abstract void buildGet(List<PropertyDescriptor<?>> result);

  public abstract void buildPost(List<PropertyDescriptor<?>> result) throws IOException;

  public abstract void buildPatch(List<PropertyDescriptor<?>> result);

  public abstract void buildDelete(List<PropertyDescriptor<?>> result);

  public void setPathName(String pathName) {
    this.pathName = pathName;
  }

  public void setRestOperation(String restOperation) {
    this.restOperation = restOperation;
  }

  public void setApi(String api) {this.api = api;}

  public void setSubApi(String subApi) {this.subApi = subApi;}

  public void setOpenAPI(OpenAPI openAPI) {
    this.openAPI = openAPI;
    this.paths = openAPI.getPaths();
  }
  public void setOpenAPI2(String swaggerStr) throws JsonProcessingException {
    try {
      this.openAPI2 = objectMapper.readValue(swaggerStr, JsonNode.class);
      this.paths2 = Optional.ofNullable(openAPI2)
          .map(openapi -> openapi.get("paths"))
          .orElse(null);

      if (paths2 == null) {
        integrationConfiguration.setErrors(Arrays.asList("Unable to fetch API information. Check that connected system " +
            "credentials are properly formatted"));
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public List<JsonNode> getRefs(JsonNode arrOfRefStrs) {
    if (arrOfRefStrs == null || arrOfRefStrs.size() == 0) return null;

    List<JsonNode> refNodeArr = new ArrayList<>();
    arrOfRefStrs.forEach(refNode -> {
      Optional.ofNullable(refNode.get("$ref"))
          .ifPresent(refStr -> {
            String refLocation = refStr.asText().replace("#/", "/");
            refNodeArr.add(openAPI2.at(refLocation));
          });
    });
    return refNodeArr;
  }

  public JsonNode getRefIfPresent(JsonNode currNode) {
    // If no ref or null, just return currNode
    if (currNode == null || !currNode.has(REF)) return currNode;

    // Get Ref if it exists
    String newLoc = currNode.get(REF).asText().replace("#/", "/");
    JsonNode newNode = openAPI2.at(newLoc);
    if (newNode == null || newNode.isMissingNode()) return null;
    else return newNode;
  }

  // Parse through the OpenAPI spec starting at root node and traversing down path
  public JsonNode parse(JsonNode root, List<String> path) {
    if (root == null || path.size() <= 0) return null;

    JsonNode currNode = root;
    for (int i = 0; i < path.size(); i++) {
      String loc = path.get(i);
      currNode = currNode.get(loc);
      currNode = getRefIfPresent(currNode);
      if (currNode == null) return null;
    }

    currNode = getRefIfPresent(currNode);
    return currNode;
  }



  // Find all occurrences of variables inside path (ex. {claimId})
/*  protected void setPathVarsUI() {

    List<Parameter> pathParams = Util.getOperation(paths.get(pathName), restOperation).getParameters();
    if (pathParams == null)  return;

    pathParams.forEach(param -> {
      if (param instanceof PathParameter) {
        TextPropertyDescriptor ui = simpleIntegrationTemplate.textProperty(param.getName())
            .instructionText("")
            .isRequired(true)
            .isExpressionable(true)
*//*            .refresh(RefreshPolicy.ALWAYS)*//*
            .placeholder("1")
            .label(Util.camelCaseToTitleCase(param.getName()))
            .instructionText(param.getDescription() != null ? param.getDescription() : "")
            .build();
        pathVarsUI.add(ui);
      }
    });

  }*/

  public List<PropertyDescriptor<?>> getPathVarsUI() {
    return pathVarsUI;
  }
  protected void setPathVarsUI() {

    Optional.ofNullable(parse(paths2, Arrays.asList(pathName, PARAMETERS)))
        .map(this::getRefs)
        .ifPresent(refs -> refs.forEach(ref -> {
          String paramName = ref.get(NAME).asText();
          String paramDescription = ref.get(DESCRIPTION).asText();
          TextPropertyDescriptor ui = simpleIntegrationTemplate.textProperty(paramName)
              .instructionText("")
              .isRequired(true)
              .isExpressionable(true)
              .placeholder("1")
              .label(Util.camelCaseToTitleCase(paramName))
              .instructionText(paramDescription != null ? paramDescription : "")
              .build();
          pathVarsUI.add(ui);
        }));
  }





  public void ReqBodyUIBuilder2(List<PropertyDescriptor<?>> result,
      JsonNode reqBodyPropertiesNode,
      JsonNode required,
      String restOperation) {

    LocalTypeDescriptor.Builder builder = simpleIntegrationTemplate.localType(REQ_BODY_PROPERTIES);
    reqBodyPropertiesNode.fields().forEachRemaining(entry -> {
      if (entry.getValue() == null) return;

      JsonNode itemSchema = entry.getValue();
      String keyStr = entry.getKey();

      // If that property has required fields create a new hashset of those required fields
      // TODO: check all required logic
      Set<String> requiredProperties = new HashSet<>();
      if (required == null && itemSchema.get(REQUIRED) != null) {
        itemSchema.get(REQUIRED).forEach(req -> requiredProperties.add(req.asText()));
      }

      // If the property is a document. This creates the property outside of the request body to use the native Appian
      // document/file picker
      if (itemSchema.get(FORMAT) != null && itemSchema.get(FORMAT).equals("binary")) {
        DocumentPropertyDescriptor document = simpleIntegrationTemplate.documentProperty(keyStr)
            .label("Document " + Character.toUpperCase(keyStr.charAt(0)) + keyStr.substring(1))
            .isRequired(requiredProperties == null ? false : requiredProperties.contains(keyStr))
            .isExpressionable(true)
/*            .refresh(RefreshPolicy.ALWAYS)*/
            .instructionText(itemSchema.get(DESCRIPTION).asText())
            .build();
        result.add(document);
      } else {
        LocalTypeDescriptor property = parseRequestBody(keyStr, itemSchema, requiredProperties, restOperation);
        if (property != null) {
          builder.properties(property.getProperties());
        }
      }
    });

    result.add(
        simpleIntegrationTemplate.localTypeProperty(builder.build())
            .key(Util.removeSpecialCharactersFromPathName(pathName))
            .displayHint(DisplayHint.NORMAL)
            .isExpressionable(true)

            .label("Request Body")
            .description("Enter values to the properties below to send new or updated data to Guidewire. Not all properties are " +
                "required. Make sure to remove any unnecessary autogenerated properties. By default, null values will not be added " +
                "to the request. Use a space between apostrophes for sending empty text.")
            .instructionText(AUTOGENERATED_ERROR_DETAIL)
/*            .refresh(RefreshPolicy.ALWAYS)*/

            .build()
    );
  }


  public LocalTypeDescriptor parseRequestBody(String key,
      JsonNode item,
      Set<String> requiredProperties,
      String restOperation) {

    // Skip if the field is a read-only value
    if (item.get("readOnly") != null && item.get("readOnly").asBoolean()) return null;

    item = getRefIfPresent(item);

    // For POSTs, gw sets required properties required to create a post in their extensions instead of in the required section
    JsonNode requiredForCreate = item.get("x-gw-requiredForCreate");
    if (restOperation.equals(POST) && requiredForCreate != null ) {
      for (JsonNode req : requiredForCreate) {
        requiredProperties.add(req.asText());
      }
    }

    // Fields that are marked as createOnly are allowed in POSTs but not in PATCHes
    JsonNode createOnly = item.get("x-gw-createOnly");
    if (restOperation.equals(PATCH) && createOnly != null && createOnly.asBoolean()) {
      return null;
    }

    LocalTypeDescriptor.Builder builder = simpleIntegrationTemplate.localType(key);
    String type = item.get("type").asText();
    if (type.equals("object")) {

      if (item.get(PROPERTIES) == null)
        return null;

      item.get(PROPERTIES).fields().forEachRemaining(entry -> {
        String innerKey = entry.getKey();
        JsonNode innerItem = entry.getValue();

        // TODO: fix required
/*        requiredProperties = innerItem.get(REQUIRED) != null ? new HashSet<>(innerItem.get(REQUIRED)) : requiredProperties;*/

        LocalTypeDescriptor nested = parseRequestBody(innerKey, innerItem, requiredProperties, restOperation);
        if (nested != null) {
          builder.properties(nested.getProperties());
        }
      });

      String description = item.get(DESCRIPTION) != null ?
          item.get(DESCRIPTION).asText().replaceAll("\n", "") :
          "";
      return simpleIntegrationTemplate.localType(key + "Builder")
          .properties(simpleIntegrationTemplate.localTypeProperty(builder.build())
              .label(key)
              .description(description)
              .refresh(RefreshPolicy.ALWAYS)
              .isRequired(requiredProperties != null && requiredProperties.contains(key))
              .build())
          .build();

    }

  /*  else if (type.equals("array")) {

      if (item.get(ITEMS) == null || item.get(ITEMS).get(REF) == null)
        return null;

      item = parse(item, Arrays.asList(REF));

      item.get(PROPERTIES).fields().forEachRemaining(entry -> {
        String innerKey = entry.getKey();
        JsonNode innerItem = entry.getValue();

        Set<String> innerRequiredProperties = new HashSet<>();
        if (innerItem.getRequired() != null) innerRequiredProperties.addAll(innerItem.getRequired());
        else if (item.getItems().getRequired() != null) innerRequiredProperties.addAll(item.getItems().getRequired());

        LocalTypeDescriptor nested = parseRequestBody(innerKey, innerItem, innerRequiredProperties,
            removeFieldsFromReqBody, restOperation);
        if (nested != null) {
          builder.properties(nested.getProperties());
        }
      });


      String description = item.getDescription() != null ?
          item.getDescription().replaceAll("\n", "") :
          "";

      // The listProperty needs a typeReference to a localProperty set by localTypeProperty, but not actually displayed
      LocalTypeDescriptor built = builder.build();
      simpleIntegrationTemplate.localTypeProperty(built, key + "hidden");
      return simpleIntegrationTemplate.localType(key + "Builder").properties(
          simpleIntegrationTemplate.listTypeProperty(key)
              .label(key)
              .isHidden(false)
              .refresh(RefreshPolicy.ALWAYS)

              .description(description)
              .displayHint(DisplayHint.NORMAL)
              .isExpressionable(true)
              .isRequired(requiredProperties != null && requiredProperties.contains(key))
              .itemType(TypeReference.from(built))
              .build()
      ).build();

    }*/
    else {
      String isRequired = requiredProperties != null && requiredProperties.contains(key) ? "(Required) ": "";
      String description = item.get(DESCRIPTION) != null ?
          isRequired + item.get(DESCRIPTION).asText().replaceAll("\n", "") :
          "";

      // Base case: Create new property field depending on the type
      PropertyDescriptorBuilder<?> newProperty;
      switch (type) {
        case ("boolean"):
/*          newProperty = simpleIntegrationTemplate.booleanProperty(key).displayMode(BooleanDisplayMode.CHECKBOX);*/

          // Boolean properties are automatically marked as false, need to know when a user actually selects false. Using
          // dropdown instead
          newProperty = simpleIntegrationTemplate.textProperty(key).choices(
              Choice.builder().name("True").value("true").build(),
              Choice.builder().name("False").value("false").build()
          );
          break;
        case ("integer"):
          newProperty = simpleIntegrationTemplate.integerProperty(key);
          break;
        case ("number"):
          newProperty = simpleIntegrationTemplate.doubleProperty(key);
          break;
        default:
          newProperty = simpleIntegrationTemplate
              .paragraphProperty(key)
              .height(description.length() >= 125 ? ParagraphHeight.TALL : ParagraphHeight.MEDIUM);
          break;
      }

      return simpleIntegrationTemplate.localType(key + "Container")
          .property(newProperty
              .label(key)
              .isRequired(requiredProperties != null && requiredProperties.contains(key))
              .isExpressionable(true)
              .refresh(RefreshPolicy.ALWAYS)

              .placeholder(description)
              .instructionText(description)

              .description(description)

              .build())
          .build();
    }
  }

/*
  public void ReqBodyUIBuilder(List<PropertyDescriptor<?>> result,
      Map<?,?> properties,
      Set<String> required,
      Map<String, List<String>> removeFieldsFromReqBody,
      String httpCall) {
    LocalTypeDescriptor.Builder builder = simpleIntegrationTemplate.localType(REQ_BODY_PROPERTIES);
    properties.forEach((key, item) -> {
      Schema<?> itemSchema = (Schema<?>)item;
      String keyStr = key.toString();

      //
      Set<String> requiredProperties = required == null && itemSchema.getRequired() != null ?
              new HashSet<>(itemSchema.getRequired()) :
              required;

      // If the property is a document. This creates the property outside of the request body to use the native Appian
      // document/file picker
      if (itemSchema.getFormat() != null && itemSchema.getFormat().equals("binary")) {
        DocumentPropertyDescriptor document = simpleIntegrationTemplate.documentProperty(keyStr)
            .label("Document " + Character.toUpperCase(keyStr.charAt(0)) + keyStr.substring(1))
            .isRequired(requiredProperties == null ? false : requiredProperties.contains(keyStr))
            .isExpressionable(true)
*//*            .refresh(RefreshPolicy.ALWAYS)*//*
            .instructionText(itemSchema.getDescription())
            .build();
        result.add(document);
      } else {
        LocalTypeDescriptor property = parseRequestBody(keyStr, itemSchema, requiredProperties, removeFieldsFromReqBody, httpCall);
        if (property != null) {
          builder.properties(property.getProperties());
        }
      }
    });

    result.add(
        simpleIntegrationTemplate.localTypeProperty(builder.build())
        .key(Util.removeSpecialCharactersFromPathName(pathName))
        .displayHint(DisplayHint.NORMAL)
*//*        .isExpressionable(true)*//*
        .label("Request Body")
        .description("Enter values to the properties below to send new or updated data to Guidewire. Not all properties are " +
            "required. Make sure to remove any unnecessary autogenerated properties. By default, null values will not be added " +
            "to the request. Use a space between apostrophes for sending empty text.")
        .instructionText(AUTOGENERATED_ERROR_DETAIL)
*//*        .refresh(RefreshPolicy.ALWAYS)*//*
        .build()
    );
  }

  public StringBuilder parseOneOf(Schema<?> property) {
    StringBuilder propertyType = new StringBuilder();
    if (property instanceof StringSchema) {
      propertyType.append("String");
      propertyType.append(property.getExample() != null ?
          "\n   Example: " + "'" + property.getExample().toString().replaceAll("\n", "") + "'" :
          "");
    } else if (property instanceof IntegerSchema) {
      propertyType.append("Integer");
      propertyType.append(
          property.getExample() != null ? "\n   Example: " + property.getExample().toString().replaceAll("\n", "") : "");
    } else if (property instanceof BooleanSchema) {
      propertyType.append("Boolean");
      propertyType.append(
          property.getExample() != null ? "\n   Example: " + property.getExample().toString().replaceAll("\n", "") : "");
    } else if (property instanceof ArraySchema) {
      propertyType.append("Array of ").append(parseOneOf(property.getItems()));
      propertyType.append(
          property.getExample() != null ? "\n   Example: " + property.getExample().toString().replaceAll("\n", "") : "");
    }
    return propertyType;
  }

  public LocalTypeDescriptor parseRequestBody(String key,
      Schema<?> item,
      Set<String> requiredProperties,
      Map<String, List<String>> removeFieldsFromReqBody,
      String httpCall) {

    // Skip if the field is a read-only value
    if (item.getReadOnly() != null && item.getReadOnly()) return null;

    // Control fields that you don't want to show on specific paths
    if (removeFieldsFromReqBody != null &&
        removeFieldsFromReqBody.keySet().contains(pathName) &&
        removeFieldsFromReqBody.get(pathName).contains(key)) {
      return null;
    }

    Optional<Object> extensions = Optional.ofNullable(item.getExtensions()).map(extensionMap -> extensionMap.get("x-gw-extensions"));

    // For POSTs, gw sets required properties required to create a post in their extensions instead of in the required section
    Optional<Object> requiredForCreate = extensions.map(requiredMap -> ((Map)requiredMap).get("requiredForCreate"));
    if (httpCall.equals(POST) && requiredForCreate.isPresent() && requiredForCreate.get().equals(true)) {
      if (requiredProperties == null) {
        requiredProperties = new HashSet<>();
      }
      requiredProperties.add(key);
    }

    // Fields that are marked as createOnly are allowed in POSTs but not in PATCHes
    Optional<Object> createOnly = extensions.map(requiredMap -> ((Map)requiredMap).get("createOnly"));
    if (httpCall.equals(PATCH) && createOnly.isPresent() && createOnly.get().equals(true)) {
      return null;
    }

    LocalTypeDescriptor.Builder builder = simpleIntegrationTemplate.localType(key);

    if (item.getOneOf() != null) {    // property could be one of many things
      String description = item.getDescription() != null ? item.getDescription().replaceAll("\\n", "") : "";
      StringBuilder oneOfStrBuilder = new StringBuilder(
          description + "\n" + "'" + key + "'" + " can be one of the following " + "types: ");
      int propertyNum = 1;
      for (Schema<?> property : item.getOneOf()) {
        oneOfStrBuilder.append("\n").append(propertyNum++).append(". ").append(parseOneOf(property));
      }
      return builder.property(simpleIntegrationTemplate.textProperty(key)
          .placeholder(oneOfStrBuilder.toString())
          .isExpressionable(true)
*//*          .refresh(RefreshPolicy.ALWAYS)*//*
          .build()).build();
    } else if (item.getType().equals("object")) {

      if (item.getProperties() == null)
        return null;

      for (Map.Entry<String,Schema> entry : item.getProperties().entrySet()) {
        String innerKey = entry.getKey();
        Schema<?> innerItem = entry.getValue();
        requiredProperties = innerItem.getRequired() != null ? new HashSet<>(innerItem.getRequired()) : requiredProperties;
        LocalTypeDescriptor nested = parseRequestBody(innerKey, innerItem, requiredProperties, removeFieldsFromReqBody, httpCall);
        if (nested != null) {
          builder.properties(nested.getProperties());
        }
      }

      String description = item.getDescription() != null ?
          item.getDescription().replaceAll("\n", "") :
          "";
      return simpleIntegrationTemplate.localType(key + "Builder")
          .properties(simpleIntegrationTemplate.localTypeProperty(builder.build())
              .label(key)
              .description(description)
*//*              .refresh(RefreshPolicy.ALWAYS)*//*
              .isRequired(requiredProperties != null && requiredProperties.contains(key))
              .build())
          .build();

    } else if (item.getType().equals("array")) {

      if (item.getItems() == null || item.getItems().getProperties() == null)
        return null;

      for (Map.Entry<String,Schema> entry : item.getItems().getProperties().entrySet()) {
        String innerKey = entry.getKey();
        Schema<?> innerItem = entry.getValue();

        Set<String> innerRequiredProperties = new HashSet<>();
        if (innerItem.getRequired() != null) innerRequiredProperties.addAll(innerItem.getRequired());
        else if (item.getItems().getRequired() != null) innerRequiredProperties.addAll(item.getItems().getRequired());

        LocalTypeDescriptor nested = parseRequestBody(innerKey, innerItem, innerRequiredProperties,
            removeFieldsFromReqBody, httpCall);
        if (nested != null) {
          builder.properties(nested.getProperties());
        }
      }

      String description = item.getDescription() != null ?
          item.getDescription().replaceAll("\n", "") :
          "";

      // The listProperty needs a typeReference to a localProperty set by localTypeProperty, but not actually displayed
      LocalTypeDescriptor built = builder.build();
      simpleIntegrationTemplate.localTypeProperty(built, key + "hidden");
      return simpleIntegrationTemplate.localType(key + "Builder").properties(
          simpleIntegrationTemplate.listTypeProperty(key)
              .label(key)
              .isHidden(false)
*//*              .refresh(RefreshPolicy.ALWAYS)*//*
              .description(description)
              .displayHint(DisplayHint.NORMAL)
              .isExpressionable(true)
              .isRequired(requiredProperties != null && requiredProperties.contains(key))
              .itemType(TypeReference.from(built))
              .build()
      ).build();

    } else {
      String isRequired = requiredProperties != null && requiredProperties.contains(key) ? "(Required) ": "";
      String description = item.getDescription() != null ?
          isRequired + item.getDescription().replaceAll("\n", "") :
          "";

      // Base case: Create new property field depending on the type
      PropertyDescriptorBuilder<?> newProperty;
      switch (item.getType()) {
        case ("boolean"):
*//*          newProperty = simpleIntegrationTemplate.booleanProperty(key).displayMode(BooleanDisplayMode.CHECKBOX);*//*
          // Boolean properties are automatically marked as false, need to know when a user actually selects false. Using
          // dropdown instead
          newProperty = simpleIntegrationTemplate.textProperty(key).choices(
              Choice.builder().name("True").value("true").build(),
              Choice.builder().name("False").value("false").build()
          );
          break;
        case ("integer"):
          newProperty = simpleIntegrationTemplate.integerProperty(key);
          break;
        case ("number"):
          newProperty = simpleIntegrationTemplate.doubleProperty(key);
          break;
        default:
          newProperty = simpleIntegrationTemplate
              .paragraphProperty(key)
              .height(description.length() >= 125 ? ParagraphHeight.TALL : ParagraphHeight.MEDIUM);
          break;
      }

      return simpleIntegrationTemplate.localType(key + "Container")
          .property(newProperty
              .label(key)
              .isRequired(requiredProperties != null && requiredProperties.contains(key))
              .isExpressionable(true)
*//*              .refresh(RefreshPolicy.ALWAYS)*//*
              .placeholder(description)
*//*              .instructionText(description)*//*
  *//*            .description(description)*//*
              .build())
          .build();
    }
  }*/

}
