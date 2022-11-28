package com.appian.guidewire;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import std.ConstantKeys;
import std.ParseOpenAPI;

public class ParseOpenAPIsTests {

    private static final String OPENAPI_SAMPLE = "openapis/petstore3.json";
    private static final String OPENAPI_SAMPLE_YAML = "openapis/petstore3.yaml";

    @Test
    public void test_ParseResourcesOpenAPI() throws Exception {


/*        System.out.println(ParseOpenAPI.initializePaths(ConstantKeys.CLAIMS));*/

        // Checks if parser works on all post/patch paths
            // - Works for policies.yaml (which doesn't have patch paths for some reason, might have to try
            // getting the openapi 3.0 schema from that endpoint. Excludes document paths which have a
            // different parsing structure.
            // - Almost working for Claims, excluding documents and
        // /claims/{claimId}/service-requests/{serviceRequestId}/invoices has a different structure to work
        // out, Will come back to it once I actually need to build requestBodies
/*        openAPI.getPaths().entrySet().forEach(s -> {
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



        // Working parser of one path at a time
/*        ObjectSchema schema = (ObjectSchema)
            openAPI.getPaths().get("/policies/{policyId}/activities").getPost().getRequestBody().getContent().get("application/json").getSchema().getProperties().get("data");
        schema.getProperties().get("attributes").getProperties().forEach((key, item) -> {
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
        });*/



/*        assertNotNull(openAPI);*/


    }
}
