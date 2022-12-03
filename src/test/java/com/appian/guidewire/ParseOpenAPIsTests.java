package com.appian.guidewire;

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;

import com.appian.guidewire.templates.GuidewireCSP;
import com.appian.guidewire.templates.UIBuilders.ParseOpenAPI;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import std.ConstantKeys;
import std.Util;

public class ParseOpenAPIsTests {

  private static final String OPENAPI_SAMPLE = "openapis/petstore3.json";
  private static final String OPENAPI_SAMPLE_YAML = "openapis/petstore3.yaml";



  @Test
  public void plz() {
    OpenAPI openAPI = Util.getOpenApi("com/appian/guidewire/templates/claims.yaml");
    ParseOpenAPI.buildRequestBodyUI(openAPI, "");
  }

  @Test
  public void pets() {
    OpenAPI openAPI = Util.getOpenApi("com/appian/guidewire/templates/petstore3.yaml");
    Schema schema =
        openAPI.getPaths().get("/user/createWithList").getPost().getRequestBody().getContent().get("application/json").getSchema();
    System.out.println(schema.getItems());
  }

  @Test
  public void test_ParseMany() throws Exception {
      OpenAPI openAPI = Util.getOpenApi("com/appian/guidewire/templates/claims.yaml");
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

  @Test
  public void test_ParseResourcesOpenAPI() throws Exception {

    // Working parser of one path at a time
    OpenAPI openAPI = Util.getOpenApi("com/appian/guidewire/templates/claims.yaml");

    ObjectSchema schema = (ObjectSchema)openAPI.getPaths()
        .get("/claims/{claimId}/service-requests/{serviceRequestId}/invoices")
        .getPost()
        .getRequestBody()
        .getContent()
        .get("application/json")
        .getSchema()
        .getProperties()
        .get("data");


    schema.getProperties().get("attributes").getProperties().forEach((key, item) -> {
      Schema itemSchema = ((Schema)item);


      if (itemSchema.getType().equals("object")) {
        isObj(key, (Schema)item);
      } else if (itemSchema.getType().equals("array")) {

          System.out.println(key + " : " + itemSchema.getType());

          itemSchema.getItems().getProperties().forEach((innerKey, innerItem) -> {
              Schema innerItemSchema = ((Schema)innerItem);


              if (innerItemSchema.getType().equals("object")) {
                isObj(innerKey, (Schema)innerItem);
              } else {
                System.out.println("        " + innerKey + " : " + innerItemSchema.getType());
              }
          });
      } else {
        System.out.println(key + " : " + itemSchema.getType());
      }
    });


    /*        System.out.println(ParseOpenAPI.initializePaths(ConstantKeys.CLAIMS));*/

    // Checks if parser works on all post/patch paths
    // - Works for policies.yaml (which doesn't have patch paths for some reason, might have to try
    // getting the openapi 3.0 schema from that endpoint. Excludes document paths which have a
    // different parsing structure.
    // - Almost working for Claims, excluding documents and
    // /claims/{claimId}/service-requests/{serviceRequestId}/invoices has a different structure to work
    // out, Will come back to it once I actually need to build requestBodies
    /*  OpenAPI openAPI = Util.getOpenApi("com/appian/guidewire/templates/policies.yaml");
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
        });*/







    /*        assertNotNull(openAPI);*/

  }
}
