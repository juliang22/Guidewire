package std;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class Util implements ConstantKeys{

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

    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    public static String getExtensionFromContentType(String contentType) {
        switch (contentType) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "text/html":
                return ".html";
            case "application/json":
                return ".json";
            case "application/xml":
                return ".xml";
            case "text/css":
                return ".css";
            case "text/plain":
                return ".txt";
            default:
                return ""; // return default extension or null or throw exception
        }
    }

    public static Map<String,Map<String,String>> strToOpenAPIInfo(String openAPIInfoStr) {
        try {
             return new ObjectMapper().readValue(openAPIInfoStr, Map.class);
        } catch (IOException e) {
            // TODO: error handle
            e.printStackTrace();
        }
        return null;
    }

    public static OpenAPI getOpenAPI(String openAPIStr) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true); // implicit
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(false);
        return new OpenAPIV3Parser().readContents(openAPIStr, null, parseOptions).getOpenAPI();
    }

    // finds the longest common substring at the end of the first string and the beginning of the second string by starting at
    // the end of the first string and the start of the second string and moving towards the start of the first string and the
    // end of the second string respectively. If it finds characters that don't match, it breaks the loop and then constructs the
    // merged string by appending to the first string the substring of the second string starting from the end of the longest common substring.
    //Note: This function assumes that the common part is at the end of the first string and at the start of the second string.
    public static String mergeStrings(String str1, String str2) {
        // start from end of str1 and beginning of str2
        int i = str1.length() - 1;
        int j = 0;

        // find the start of overlap
        while(i >= 0 && !str1.substring(i).equals(str2.substring(0, str1.length() - i))) {
            i--;
        }

        // if overlap was found
        if (i != -1) {
            return str1 + str2.substring(str1.length() - i);
        } else {
            return str1 + str2;
        }
    }

    public static List<String> getPathVarsStr(String pathName) {
        Matcher m = Pattern.compile("[^{*}]+(?=})").matcher(pathName);
        List<String> pathVars = new ArrayList<>();

        while (m.find()) {
            pathVars.add(m.group());
        }
        return pathVars;
    }

    public static String camelCaseToTitleCase(String str) {
        return Pattern.compile("(?=[A-Z])").splitAsStream(str)
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.joining(" "));
    }

    public static String removeSpecialCharactersFromPathName(String pathName) {
        return pathName.replace("/", "").replace("{", "").replace("}", "");
    }

    public static OpenAPI getOpenApi(String api, ClassLoader classLoader) {
        try (InputStream input = classLoader.getResourceAsStream(api)) {
            String content = IOUtils.toString(input, StandardCharsets.UTF_8);
            ParseOptions parseOptions = new ParseOptions();
            parseOptions.setResolve(true); // implicit
            parseOptions.setResolveFully(true);
/*            parseOptions.setResolveCombinators(false);*/
            return new OpenAPIV3Parser().readContents(content, null, parseOptions).getOpenAPI();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String filterRules(String str) {
        return str == null ?
            null :
            str.replaceAll(" ", "%20").replaceAll(":","::");
    }

    public static String removeLastChar(String str) {
        return str.substring(0, str.length() - 1);
    }

    public static Operation getOperation(PathItem path, String restOperation) {
        Operation chosenOpenApiPath = null;
        switch(restOperation) {
            case GET:
                chosenOpenApiPath = path.getGet();
                break;
            case POST:
                chosenOpenApiPath = path.getPost();
                break;
            case PATCH:
                chosenOpenApiPath = path.getPatch();
                break;
            case DELETE:
                chosenOpenApiPath = path.getDelete();
                break;
        }
        return chosenOpenApiPath;
    }

    public static String getPathProperties(PathItem path, String restOperation, String property) {

        Operation chosenOpenApiPath = getOperation(path, restOperation);
        String result = "";
        switch (property) {
            case "description":
                result = chosenOpenApiPath.getDescription();
                break;
            case "summary":
                result = chosenOpenApiPath.getSummary();
                break;
        }
        return result;
    }

}
