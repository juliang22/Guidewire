package std;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.guidewire.templates.claims.GetClaimsIntegrationTemplate;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

public class Util {

    private static List<Integer> responseCode = Arrays.asList(200, 201, 202, 207, 204);

    public static Boolean isSuccess(HttpResponse response) {
        return (Boolean) (response.getStatusCode() == 200 && response.getResponse().get("access_token") != null);
    }

    public static String getToken(SimpleConfiguration simpleConfiguration) {
/*        HttpResponse response = Http.authPost(simpleConfiguration);
        if (response.getStatusCode() == 200 && response.getResponse().get("access_token") != null) {
            return response.getResponse().get("access_token").toString();
        } else return null;*/
        return "";
    }

    public static String fieldsQueryBuilder(ArrayList<String> fieldList){

        ArrayList<String> elements= new ArrayList<>();
        fieldList.forEach((entity)->
                elements.add("element="+entity.toString()));
        return String.join("^OR",(String[])elements.toArray(new String[0]));
    }

    public static IntegrationResponse buildResult(Diagnostics diagnostics, HttpResponse result) {
        final IntegrationDesignerDiagnostic diagnostic = IntegrationDesignerDiagnostic.builder()
                .addExecutionTimeDiagnostic(diagnostics.getTiming())
                .addRequestDiagnostic(diagnostics.getDiagnostics())
                .addResponseDiagnostic(result.getResponse())
                .build();
        if (responseCode.contains(result.getStatusCode())) {
            return IntegrationResponse
                    .forSuccess(result.getResponse())
                    .withDiagnostic(diagnostic)
                    .build();
        } else {
            return IntegrationResponse.forError(
                            new IntegrationError.IntegrationErrorBuilder()
                                    .title("Error Code " + result.getStatusCode())
                                    .message(result.getStatusLine())
                                    .detail(result.getResponse().toString())
                                    .build())
                    .withDiagnostic(diagnostic).build();
        }
    }

    public static IntegrationResponse buildError(String title, String errorMessage) {
        return IntegrationResponse.forError(
                        new IntegrationError.IntegrationErrorBuilder()
                                .title(title)
                                .message(errorMessage)
                                .build())
                .build();
    }

    public static OpenAPI getOpenApi(String api) {
        try (InputStream input = GetClaimsIntegrationTemplate.class.getClassLoader().getResourceAsStream(api)) {
            String content = IOUtils.toString(input, "utf-8");
            ParseOptions parseOptions = new ParseOptions();
            parseOptions.setResolve(true); // implicit
            parseOptions.setResolveFully(true);
            OpenAPI openAPI = new OpenAPIV3Parser().readContents(content, null, parseOptions).getOpenAPI();
            return openAPI;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
