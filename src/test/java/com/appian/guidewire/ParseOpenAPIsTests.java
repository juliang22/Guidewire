package com.appian.guidewire;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.junit.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import std.Util;

public class ParseOpenAPIsTests {

    private static final String OPENAPI_SAMPLE = "openapis/petstore3.json";
    private static final String OPENAPI_SAMPLE_YAML = "openapis/petstore3.yaml";

    @Test
    public void test_ParseResourcesOpenAPI() throws Exception {

        OpenAPI openAPI = Util.getOpenApi("com/appian/guidewire/templates/Policies.yaml");
        // Get Paths
/*        openAPI.getPaths().entrySet().forEach(s -> System.out.println(s.getKey()));*/

        /*        String ref =
            openAPI.getPaths().get("/policies/{policyId}/activities").getPost().getRequestBody().getContent().get("application/json").getSchema().get$ref();
        String[] split = ref.split("/");
        ref = split[split.length-1];
        System.out.println(openAPI.getComponents().getSchemas().get(ref).getAllOf());*/
        ObjectSchema schema = (ObjectSchema)
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
        });



        assertNotNull(openAPI);


    }
}
