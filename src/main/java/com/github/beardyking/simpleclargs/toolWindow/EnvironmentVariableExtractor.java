package com.github.beardyking.simpleclargs.toolWindow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvironmentVariableExtractor {
    public static String extractEnvironmentVariables(String input) {
        // pattern to match "<...>"
        Pattern pattern = Pattern.compile("<(.*?)>");

        Matcher matcher = pattern.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String variableValue = System.getenv(variableName);

            if (variableValue != null) {

                String replacement = Matcher.quoteReplacement(variableValue);
                matcher.appendReplacement(result, replacement);
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
