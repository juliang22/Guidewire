package com.appian.guidewire;

import com.google.common.io.Resources;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ParseOpenAPIsTests {

    private static final String OPENAPI_SAMPLE = "openapis/petstore3.json";

    @Test
    public void test_ParseResourcesOpenAPI() throws Exception {
        try {
            String content = Resources.toString(Resources.getResource(OPENAPI_SAMPLE),
                    StandardCharsets.UTF_8);
            OpenAPI openAPI = new OpenAPIV3Parser().readContents(content).getOpenAPI();
            assertNotNull(openAPI);
            assertEquals(openAPI.getOpenapi(), "3.0.2");
        } catch (
                IOException e) {
            throw new RuntimeException("Invalid OpenAPI source file: " + OPENAPI_SAMPLE, e);
        }
    }
}
