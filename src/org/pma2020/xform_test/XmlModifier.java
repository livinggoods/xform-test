package org.pma2020.xform_test;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Taken from: https://www.mkyong.com/java/how-to-modify-xml-file-in-java-dom-parser/ */
class XmlModifier {
    private String newFilePath;
//    private Document xmlDom;
    private String xmlString;

    XmlModifier(String filePath) throws IOException {
        newFilePath = filePath.substring(0, filePath.length() -4) + "-modified" + ".xml";
//        xmlDom = createXmlDom(filePath);
        xmlString = createXmlString(filePath);
    }

    private String createXmlString(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        }
    }

    void writeToFile() throws IOException {
        FileWriter file = new FileWriter(newFilePath);
        file.write(xmlString);
        file.flush();
        file.close();
    }

    /** Extracts function name from a given string.
     *
     * @param str The string containing function name; usually programming logic, containing func call.
     *
     * @return function name if present, else empty string */
    private String extractFunctionName(String str) {
        ArrayList<String> funcNameMatches = new ArrayList<>();

        Matcher funcNameMatcher = Pattern.compile("^[a-z]*\\(").matcher(str);
        while(funcNameMatcher.find())
            funcNameMatches.add(funcNameMatcher.group());

        if (funcNameMatches.size() < 1)
            return "";
        else {
            String match = funcNameMatches.get(0);
            return match.substring(0, match.length() - 1);
        }
    }

    private String handleFunctionPatterns(String broadMatch) {
        String funcName = extractFunctionName(broadMatch);

        if (funcName.equals(""))
            return broadMatch;
        else {
            StringBuilder match = new StringBuilder();
            String funcNamePlusParens = funcName + "(";
            match.append(funcNamePlusParens);
            int totOpenParens = 1;
            int totCloseParens = 0;

            for (int i = funcNamePlusParens.length(); i < broadMatch.length(); i++){
                char c = broadMatch.charAt(i);
                match.append(c);

                if (c == '(')
                    totOpenParens++;
                else if (c == ')')
                    totCloseParens++;
                if (totOpenParens == totCloseParens)
                    break;
            }

            return match.toString();
        }
    }

    boolean findReplace(String find, String replace) {
        // temp: https://www.regextester.com/97778
        // Matcher matcher = Pattern.compile("pulldata\\(.*\\)").matcher(xmlString);  // testing
        Matcher matcher = Pattern.compile(find, Pattern.DOTALL).matcher(xmlString);
        ArrayList<String> matches = new ArrayList<>();
        ArrayList<String> broadMatches = new ArrayList<>();

        while(matcher.find())
            broadMatches.add(matcher.group());

        // corrections - properly match single function
        for (String broadMatch : broadMatches) {
            String match = handleFunctionPatterns(broadMatch);
            matches.add(match);
        }

        // TODO - swap these? does it even matter?
//        ArrayList<String> matchesToRecurse = new ArrayList<>(matches);
        ArrayList<String> matchesToRecurse = matches;

        // replacements
//        for (String match : matches) {
        for (int i = 0; i < matches.size(); i++) {
            String match = matches.get(i);
            if (replace.equals("*")) {  // implementation of xform-test glob syntax for replace
                String funcWrapperToRemove = find.replace(".*", "")
                        .replace("\\", "")
                        .replace(")", "");
                String contentsToKeep = match.substring(0, match.length() - 1)
                        .replace(funcWrapperToRemove, "");
                xmlString = xmlString.replace(match, contentsToKeep);

            } else {
                // check if it is a calculation override, e.g. of the form 'pulldata(): VALUE'
                String calculateAssertionSuffix = "()";
                String funcName = extractFunctionName(match);
                boolean isCalculateAssertion = match.equals(funcName + calculateAssertionSuffix);
                if (isCalculateAssertion)
                    //noinspection SuspiciousListRemoveInLoop
                    matchesToRecurse.remove(i);
                else {
                    if (replace.equals("'" + find + "'")) {
                        xmlString = xmlString.replace(match, "'" + match + "'");  // encapsulate in string
                        //noinspection SuspiciousListRemoveInLoop
                        matchesToRecurse.remove(i);
                    }
                    xmlString = xmlString.replace(match, replace);  // literal find replace
                }
            }
            if (matchesToRecurse.size() > 0)
                findReplace(find, replace);
        }

        //noinspection UnnecessaryLocalVariable
        boolean modificationsMade = broadMatches.size() > 0;
        return modificationsMade;
    }

    String getnewFilePath() {
        return newFilePath;
    }
}
