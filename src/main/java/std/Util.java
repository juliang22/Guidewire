package std;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;

public class Util implements ConstantKeys{

    private static List<Integer> responseCode = Arrays.asList(200, 201, 202, 207, 204);

    public static IntegrationResponse buildError(String title, String errorMessage, String errorDetail) {
        return IntegrationResponse.forError(
            new IntegrationError.IntegrationErrorBuilder()
                .title(title)
                .message(errorMessage == null ? "" : errorMessage)
                .detail(errorDetail == null ? "" : errorDetail)
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

    public static String filterRules(String str) {
        return str == null ?
            null :
            str.replaceAll(" ", "%20").replaceAll(":","::");
    }

    public static String removeLastChar(String str) {
        return str.substring(0, str.length() - 1);
    }

    public static String pascalCaseToTileCase(String path) {
        // Extract substring after last "/"
        String lastPart = path.substring(path.lastIndexOf("/") + 1);

        // Replace "_" with " " and add a space between two capital letters
        String withSpaces = lastPart.replaceAll("_", " ")
            .replaceAll("(?<=[a-z])(?=[A-Z])", " ");

        // Convert to title case
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : withSpaces.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }

    public static String compress(String str) throws IOException {
        if ((str == null) || (str.length() == 0)) {
            return str;
        }
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(obj)) {
            gzip.write(str.getBytes("UTF-8"));
        }
        return Base64.getEncoder().encodeToString(obj.toByteArray());
    }

    public static String decompress(String str) throws IOException {
        if ((str == null) || (str.length() == 0)) {
            return str;
        }
        String outStr = "";
        byte[] compressed = Base64.getDecoder().decode(str);
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
            ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[256];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            outStr = bos.toString("UTF-8");
        }
        return outStr;
    }
}
