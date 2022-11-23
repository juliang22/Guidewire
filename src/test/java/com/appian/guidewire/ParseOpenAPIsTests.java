package com.appian.guidewire;

import com.appian.guidewire.templates.claims.GetClaimsIntegrationTemplate;
import com.google.common.io.Resources;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ParseOpenAPIsTests {

    private static final String OPENAPI_SAMPLE = "openapis/petstore3.json";
    private static final String OPENAPI_SAMPLE_YAML = "openapis/petstore3.yaml";

    @Test
    public void test_ParseResourcesOpenAPI() throws Exception {

        try (InputStream input = GetClaimsIntegrationTemplate.class.getClassLoader()
            .getResourceAsStream("openapis/Policies.yaml")) {
            String content = IOUtils.toString(input, "utf-8");
            /*      System.out.println(content);*/
            OpenAPI openAPI = new OpenAPIV3Parser().readContents(content).getOpenAPI();
/*            System.out.println(openAPI.getPaths());*/
            assertNotNull(openAPI);
            assertEquals(openAPI.getOpenapi(), "3.0.1");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
