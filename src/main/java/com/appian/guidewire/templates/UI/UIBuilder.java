package com.appian.guidewire.templates.UI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.BooleanDisplayMode;
import com.appian.connectedsystems.templateframework.sdk.configuration.Choice;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.DocumentPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.ListTypePropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.ParagraphHeight;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyDescriptorBuilder;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.configuration.SystemType;
import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.TypeReference;
import com.appian.guidewire.templates.apis.GuidewireIntegrationTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import std.ConstantKeys;
import std.Util;

public abstract class UIBuilder implements ConstantKeys {

  protected String api;
  protected String subApi;
  protected String pathName;
  protected String restOperation;
  protected List<PropertyDescriptor<?>> pathVarsUI = new ArrayList<>();
  protected JsonNode openAPI;
  protected JsonNode paths;
  protected SimpleIntegrationTemplate simpleIntegrationTemplate;
  protected SimpleConfiguration integrationConfiguration;
  protected SimpleConfiguration connectedSystemConfiguration;
  protected PropertyPath propertyPath;
  protected ObjectMapper objectMapper = new ObjectMapper();
  List<PropertyDescriptor<?>> properties = new ArrayList<>(); // build properties to pass into .setProperties()
  Map<String, String> values = new HashMap<>(); // build values to pass into .setValues()

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
  public abstract void buildRestCall(String restOperation, String pathName) throws JsonProcessingException;

  public abstract void buildGet();

  public abstract void buildPost() throws IOException;

  public abstract void buildPatch() throws JsonProcessingException;

  public abstract void buildDelete();

  public void setPathName(String pathName) {
    this.pathName = pathName;
  }

  public void setRestOperation(String restOperation) {
    this.restOperation = restOperation;
  }

  public void setApi(String api) {this.api = api;}

  public void setSubApi(String subApi) {this.subApi = subApi;}

  public void setOpenAPI(String swaggerStr) throws JsonProcessingException {
    long startTime = System.nanoTime();

    this.openAPI = objectMapper.readValue(swaggerStr, JsonNode.class);
    this.paths = parse(openAPI, Arrays.asList(PATHS));

    if (openAPI == null || paths == null) {
      integrationConfiguration.setErrors(
          Arrays.asList("Unable to fetch API information. Check that connected system credentials are properly formatted")
      );
    }

    System.out.println("Getting OpenAPI obj: " + (System.nanoTime() - startTime)/1000000 + " milliseconds. ");
  }

  public SimpleConfiguration setPropertiesAndValues() {
    integrationConfiguration.setProperties(properties.toArray(new PropertyDescriptor<?>[0]));
    if (values != null && values.size() > 0) {
      values.forEach((key, val) -> {
        integrationConfiguration.setValue(key, val);
      });
    }
    return integrationConfiguration;
  }

  public List<JsonNode> getRefs(JsonNode arrOfRefStrs) {
    if (arrOfRefStrs == null || arrOfRefStrs.size() == 0) return null;

    List<JsonNode> refNodeArr = new ArrayList<>();
    arrOfRefStrs.forEach(refNode -> {
      Optional.ofNullable(refNode.get("$ref"))
          .ifPresent(refStr -> {
            String refLocation = refStr.asText().replace("#/", "/");
            refNodeArr.add(openAPI.at(refLocation));
          });
    });
    return refNodeArr;
  }

  public JsonNode getRefIfPresent(JsonNode currNode) {
    // If no ref or null, just return currNode
    if (currNode == null || !currNode.has(REF)) return currNode;

    // Get Ref if it exists
    String newLoc = currNode.get(REF).asText().replace("#/", "/");
    JsonNode newNode = openAPI.at(newLoc);
    if (newNode == null || newNode.isMissingNode()) return null;
    else return newNode;
  }

  // Parse through the OpenAPI spec starting at root node and traversing down path
  public JsonNode parse(JsonNode root, String path) {
    return parse(root, Arrays.asList(path));
  }

  public JsonNode parse(JsonNode root, List<String> path) {
    if (root == null || path.size() <= 0) return null;

    JsonNode currNode = root;
    for (int i = 0; i < path.size(); i++) {
      String loc = path.get(i);
      currNode = currNode.get(loc);
      currNode = getRefIfPresent(currNode);
      if (currNode == null) return null;
    }

    return getRefIfPresent(currNode);
  }

  public List<PropertyDescriptor<?>> getPathVarsUI() {
    return pathVarsUI;
  }
  protected void setPathVarsUI() {

    Optional.ofNullable(parse(paths, Arrays.asList(pathName, PARAMETERS)))
        .map(this::getRefs)
        .ifPresent(refs -> refs.forEach(ref -> {
          String paramName = ref.get(NAME).asText();
          String paramNameTitleCase = Util.camelCaseToTitleCase(paramName);
          String paramDescription = ref.get(DESCRIPTION).asText();
          TextPropertyDescriptor ui = simpleIntegrationTemplate.textProperty(paramName)
              .isRequired(true)
              .isExpressionable(true)
              .placeholder("Insert " + paramNameTitleCase)
              .label(paramNameTitleCase)
              .instructionText(paramDescription != null ? paramDescription : "")
              .build();
          pathVarsUI.add(ui);
        }));
  }

  public void ReqBodyUIBuilder(JsonNode reqBodyPropertiesNode,
      JsonNode requiredNode,
      String restOperation) {

    LocalTypeDescriptor.Builder builder = simpleIntegrationTemplate.localType(REQ_BODY_PROPERTIES);
    StringBuilder rootInstructionText = new StringBuilder("Nested Property Descriptions: \n");
    reqBodyPropertiesNode.fields().forEachRemaining(entry -> {
      if (entry.getValue() == null) return;

      JsonNode itemSchema = entry.getValue();
      String keyStr = entry.getKey();

      // If that property has required fields create a new hashset of those required fields
      // TODO: check all required logic
      Set<String> requiredProperties = new HashSet<>();
      if (itemSchema.get(REQUIRED) != null) {
        itemSchema.get(REQUIRED).forEach(req -> requiredProperties.add(req.asText()));
      } else if (requiredNode != null) {
        requiredNode.forEach(req -> requiredProperties.add(req.asText()));
      }

      // If the property is a document. This creates the property outside of the request body to use the native Appian
      // document/file picker
      if (itemSchema.get(FORMAT) != null && itemSchema.get(FORMAT).equals("binary")) {
        DocumentPropertyDescriptor document = simpleIntegrationTemplate.documentProperty(keyStr)
            .label("Document " + Character.toUpperCase(keyStr.charAt(0)) + keyStr.substring(1))
            .isRequired(requiredProperties.contains(keyStr))
            .isExpressionable(true)
            .instructionText(itemSchema.get(DESCRIPTION).asText())
            .build();
        properties.add(document);
      } else {
        LocalTypeDescriptor property = parseRequestBody(keyStr, itemSchema, requiredProperties, restOperation, -1,
            rootInstructionText);
        if (property != null) {
          builder.properties(property.getProperties());
        }
      }
    });

    properties.add(
        simpleIntegrationTemplate.localTypeProperty(builder.build())
            .key(Util.removeSpecialCharactersFromPathName(pathName))
            .displayHint(DisplayHint.NORMAL)
            .isExpressionable(true)
            .label("Request Body")
            .instructionText("Enter values to the properties below to send new or updated data to Guidewire. Not all properties are " +
                "required. Make sure to remove any unnecessary autogenerated properties. By default, null values will not be added " +
                "to the request. Use a space between apostrophes for sending empty text.")
            .description(rootInstructionText.toString())
            .build()
    );
  }


  public LocalTypeDescriptor parseRequestBody(String key, JsonNode item, Set<String> requiredProperties, String restOperation,
      int nestingLevel, StringBuilder rootInstructionText) {

    // Skip if the field is a read-only value
    if (item.get("readOnly") != null && item.get("readOnly").asBoolean()) return null;

    nestingLevel++;
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

    // TODO: fix oneOf
    if (item.has("oneOf")) return null;

    LocalTypeDescriptor.Builder builder = simpleIntegrationTemplate.localType(key);
    String type = item.get("type").asText();
    String isRequired = requiredProperties != null && requiredProperties.size() > 0 && requiredProperties.contains(key) ?
        "(Required) ":
        "";
    String description = item.get(DESCRIPTION) != null ?
        isRequired + item.get(DESCRIPTION).asText().replaceAll("\n", "") :
        "";
    if (nestingLevel == 0 && !NOT_NESTED_SET.contains(type)) { // If top-level property that's an object or array, add description
      rootInstructionText.append(key + ": " + description + ". ");
    }

    if (type.equals("object")) {

      if (item.get(PROPERTIES) == null) return null;

      // Set required properties for the properties within the object
      Set<String> innerRequiredProps = new HashSet<>();
      JsonNode innerPropsNode =  item.get(REQUIRED);
      if (item.get(REQUIRED) != null && item.get(REQUIRED).size() > 0) {
        innerPropsNode.forEach(prop -> innerRequiredProps.add(prop.asText()));
      }

      Iterator<Map.Entry<String, JsonNode>> fieldsIterator = item.get(PROPERTIES).fields();
      while (fieldsIterator.hasNext()) {
        Map.Entry<String, JsonNode> entry = fieldsIterator.next();
        String innerKey = entry.getKey();
        JsonNode innerItem = entry.getValue();

        LocalTypeDescriptor nested = parseRequestBody(innerKey, innerItem, innerRequiredProps, restOperation, nestingLevel, rootInstructionText);
        if (nested != null) {
          builder.properties(nested.getProperties());
        }
      }

      return simpleIntegrationTemplate.localType(key + "Builder")
          .properties(simpleIntegrationTemplate.localTypeProperty(builder.build())
              .label(key)
              .displayHint(DisplayHint.NORMAL)
              .isRequired(requiredProperties != null && requiredProperties.contains(key))
              .build())
          .build();
    }

    else if (type.equals("array")) {

      if (item.get(ITEMS) == null) return null;

      item = item.get(ITEMS);
      ListTypePropertyDescriptor.Builder listProperty = simpleIntegrationTemplate.listTypeProperty(key)
          .label(key)
          .displayHint(DisplayHint.NORMAL)
          .isExpressionable(true)
          .isRequired(requiredProperties != null && requiredProperties.contains(key));
      if (item.has(REF)) {
        item = getRefIfPresent(item);

        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = item.get(PROPERTIES).fields();
        while (fieldsIterator.hasNext()) {
          Map.Entry<String, JsonNode> entry = fieldsIterator.next();
          String innerKey = entry.getKey();
          JsonNode innerItem = entry.getValue();

          LocalTypeDescriptor nested = parseRequestBody(innerKey, innerItem, new HashSet<>(), restOperation,
              nestingLevel, rootInstructionText);
          if (nested != null) {
            builder.properties(nested.getProperties());
          }
        }
        // Register this list of objects
        // The listProperty needs a typeReference to a localProperty set by localTypeProperty, but not actually displayed
        LocalTypeDescriptor built = builder.build();
        simpleIntegrationTemplate.localTypeProperty(built, key + "hidden");
        listProperty.itemType(TypeReference.from(built));
      } else { // List is of flat elements, not objects
        if (item.get(TYPE) == null) return null;

        String flatArrPropertyType = item.get(TYPE).asText();
        switch (flatArrPropertyType) {
          case ("boolean"):
            listProperty.itemType(SystemType.BOOLEAN);
            break;
          case ("integer"):
            listProperty.itemType(SystemType.INTEGER);
            break;
          case ("number"):
            listProperty.itemType(SystemType.DOUBLE);
            break;
          default:
            listProperty.itemType(SystemType.STRING);
            break;
        }
      }
      return simpleIntegrationTemplate.localType(key + "Builder").properties(listProperty.build()).build();
    } else {

      // Base case: Create new property field depending on the type
      PropertyDescriptorBuilder<?> newProperty;
      switch (type) {
        case ("boolean"):
          // Boolean properties are automatically marked as false, even if a user doesn't interact with it. Since we need to know
          // when a user actually selects false, I use a dropdown for top-level booleans. Nested boolean properties inside of localTypes
          // aren't automatically set to false so using normal boolean property so that they have a description
          newProperty = nestingLevel == 0 ?
              simpleIntegrationTemplate.textProperty(key).choices(
                  Choice.builder().name("True").value("true").build(),
                  Choice.builder().name("False").value("false").build()) :
              simpleIntegrationTemplate.booleanProperty(key).displayMode(BooleanDisplayMode.CHECKBOX);
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
              .placeholder(description)
              .build())
          .build();
    }
  }

}
