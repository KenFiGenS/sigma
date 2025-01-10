package tech.api_factory.sigma.parser.query;

import org.springframework.stereotype.Service;
import tech.api_factory.sigma.parser.exceptions.InvalidSigmaRuleException;
import tech.api_factory.sigma.parser.exceptions.SigmaRuleParserException;
import tech.api_factory.sigma.parser.models.DetectionsManager;
import tech.api_factory.sigma.parser.models.SigmaDetection;
import tech.api_factory.sigma.parser.models.SigmaRule;
import tech.api_factory.sigma.parser.parsers.SigmaRuleParser;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QueryBuilder {
    static final Pattern PATTERN_FOR_CONDITION_LINE = Pattern.compile("_\\*|\\*");
    static final Pattern PATTERN_FOR_CONDITION_LINE2 = Pattern.compile("\\(|\\)");
    static final Pattern PATTERN_WITH_SPACE = Pattern.compile("\\s");
    static final Pattern PATTERN_WITHOUT_ALPHABET_AND_NUMBERS = Pattern.compile("\\W");
    static final String SPACE = " ";
    static final String TEMP_STRING = "";
    static final String CONDITION_ALL = "all";
    static final String CONDITION_ONE = "1";
    static final String OPERATOR_AND = " AND ";
    static final String OPERATOR_OR = " OR ";
    public static String yaml = "";

    public static SigmaRuleParser ruleParser;

    public QueryBuilder() {ruleParser = new SigmaRuleParser();}

    public String buildQuery(String yamlSource) throws IOException, InvalidSigmaRuleException, SigmaRuleParserException, InterruptedException, URISyntaxException {
        yaml = yamlSource;
        SigmaRule sigmaRule;
        try {
            sigmaRule = ruleParser.parseRule(yamlSource);
        } catch (RuntimeException e) {
           throw new IllegalArgumentException("Ошибка парсинга в Java-объект! \n " + e.getMessage() + "\n" + e + "\n" + " Отправлен POST запрос");
        }

        // Получаем строку condition непосредственно из String Yaml
        String condition = getConditionLineFromYaml(yamlSource);
//        System.out.println(condition);
        return getConditionResult(sigmaRule, condition);
    }

    public String getConditionResult(SigmaRule sigmaRule, String conditionLine) {
        StringBuilder conditionResult = new StringBuilder();
        List<String> conditions = Arrays.asList(conditionLine.split(SPACE));
        List<String> detectionNamesInConditionLine = List.copyOf(sigmaRule.getDetectionsManager().getAllDetections().keySet());
        boolean isAll = false;
        for (int i = 0; i < conditions.size(); i++) {
            String currenConditionLineElement = conditions.get(i);
            switch (currenConditionLineElement) {
                case CONDITION_ONE:
                    isAll = false;
                    conditionResult.append(currenConditionLineElement).append(SPACE);
                    break;
                case CONDITION_ALL:
                    isAll = true;
                    conditionResult.append(currenConditionLineElement).append(SPACE);
                    break;
                default:
                    Matcher matcher = PATTERN_FOR_CONDITION_LINE.matcher(currenConditionLineElement);
                    if (matcher.find()) {
                        currenConditionLineElement = currenConditionLineElement.replace(matcher.group(), TEMP_STRING);
                        conditionResult.append(getAggregationDetectionNamesByConditionName(currenConditionLineElement, detectionNamesInConditionLine, isAll).append(SPACE));
                    } else {
                        conditionResult.append(currenConditionLineElement).append(SPACE);
                    }
            }
        }
//        System.out.println(conditionResult.toString().trim());
        String aggregatedDetectionName = conditionResult.toString().trim();
        return getQueryFromSigmaRule(aggregatedDetectionName, sigmaRule);
    }

    public StringBuilder getAggregationDetectionNamesByConditionName(String currenConditionLineElement, List<String> detectionNamesInConditionLine, boolean isAll) {
        StringBuilder builder = new StringBuilder();
        List<String> detectionNamesByConditionName = detectionNamesInConditionLine.stream()
                .filter(s -> s.contains(currenConditionLineElement))
                .toList();
        for (int i = 0; i < detectionNamesByConditionName.size(); i++) {
            String currentName = detectionNamesByConditionName.get(i);
            if (isAll) {
                if (i + 1 != detectionNamesByConditionName.size()) {
                    builder.append(currentName).append(OPERATOR_AND);
                } else {
                    builder.append(currentName);
                }
            } else {
                if (i + 1 != detectionNamesByConditionName.size()) {
                    builder.append(currentName).append(OPERATOR_OR);
                } else {
                    builder.append(currentName);
                }
            }
        }
        return builder;
    }

    public String getQueryFromSigmaRule(String aggregatedDetectionName, SigmaRule sigmaRule) {
        DetectionsManager detectionsManager = sigmaRule.getDetectionsManager();

        List<String> allDetectionName = Arrays.stream(aggregatedDetectionName.split(SPACE)).toList().stream()
                .filter(s -> (s.length() > 3) || (s.equals("cmd")))
                .map(s -> {
                    Matcher matcher = PATTERN_FOR_CONDITION_LINE2.matcher(s);
                    if (matcher.find()) {
                        s = s.replace(matcher.group(), "");
                    }
                    return s;
                }).toList();

        return getQueryFromSimpleSigmaRule(aggregatedDetectionName, allDetectionName, detectionsManager);
    }

    public String getQueryFromSimpleSigmaRule(String result, List<String> detectionNamesInConditionLine, DetectionsManager detectionsManager) {
        Iterator<String> detectionNamesIterator = detectionNamesInConditionLine.iterator();
        String valueResult;
        while (detectionNamesIterator.hasNext()) {
            String currentDetectionName = detectionNamesIterator.next();
//            System.out.println(currentDetectionName);
            List<SigmaDetection> detections = detectionsManager.getDetectionsByName(currentDetectionName).getDetections();
            StringBuilder keyValueByDetectionName = new StringBuilder();
            for (SigmaDetection d : detections) {
                valueResult = d.getValues().toString();
                if (d.getMatchAll()) {
                    valueResult = valueResult.replaceAll(", ", OPERATOR_AND + d.getName() + ":");
                } else {
                    valueResult = valueResult.replaceAll(", ", OPERATOR_OR);
                }
                if (currentDetectionName.equals("keywords")) {
                    keyValueByDetectionName.append(d.getName()).append(OPERATOR_OR);
                    continue;
                }
                if (isList(yaml, currentDetectionName)) {
                    keyValueByDetectionName.append(d.getName()).append(":").append(valueResult).append(OPERATOR_OR);
                } else {
                    keyValueByDetectionName.append(d.getName()).append(":").append(valueResult).append(OPERATOR_AND);

                }
            }
            valueResult = valueFormat(keyValueByDetectionName.toString());
            result = result.replaceAll(currentDetectionName + PATTERN_WITH_SPACE, "(" + valueResult + ") ");
            result = result.replaceAll(currentDetectionName + PATTERN_WITHOUT_ALPHABET_AND_NUMBERS, "(" + valueResult + ")");
            if (!detectionNamesIterator.hasNext())result = result.replaceAll(currentDetectionName, "(" + valueResult + ")");
        }
        return result;
    }

    public String valueFormat(String value) {
        StringBuilder queryBuilder = new StringBuilder();
        String[] splitValues = value.split(SPACE);
        for (int i = 0; i < splitValues.length; i++) {
            String currentSplitValue = splitValues[i];
            StringBuilder currentValueBuilder = new StringBuilder(currentSplitValue);
            if (currentSplitValue.contains(":[")) {
                currentValueBuilder = new StringBuilder(currentSplitValue.replaceFirst("\\[", ""));
            }
            if (currentSplitValue.endsWith("]")) {
                currentValueBuilder.delete(currentValueBuilder.length() - 1, currentValueBuilder.length());
            }
            if ((currentSplitValue.equals("AND") || currentSplitValue.equals("OR")) && i == splitValues.length - 1) {
                continue;
            }
            queryBuilder.append(currentValueBuilder).append(" ");
        }
        value = queryBuilder.toString().trim();
        if (value.contains("$")) value = value.replace("$", "\\$");
        return value;
    }

    public String getConditionLineFromYaml(String yaml) {
        String condition = "";
        try (BufferedReader br = new BufferedReader(new StringReader(yaml))) {
            Iterator<String> iterator = br.lines().iterator();
            while (iterator.hasNext()) {
                String currentLine = iterator.next();
                if (currentLine.contains("condition")) condition = currentLine.replace("condition: ", "").trim();
            }
            condition = condition.replaceAll(" and ", " AND ");
            condition = condition.replaceAll(" or ", " OR ");
            return condition;
        } catch (IOException e) {
        }
        return condition;
    }

    public String getSigmaRuleFromFile(String file) throws IOException, InvalidSigmaRuleException, SigmaRuleParserException {
        StringBuilder newYaml = new StringBuilder();
        try (BufferedReader br = (new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.defaultCharset())))) {
            Iterator<String> iterator = br.lines().iterator();
            while (iterator.hasNext()) {
                newYaml.append(iterator.next());
                newYaml.append("\n");
            }
        } catch (IOException e) {
        }
        return newYaml.toString();
    }

    private boolean isList(String yaml, String detectionName) {
        try (BufferedReader br = (new BufferedReader(new StringReader(yaml)))) {
            Iterator<String> iterator = br.lines().iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (line.contains(detectionName)) {
                    String nextLine = iterator.next().trim();
                    if (nextLine.startsWith("- ")) return true;
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
}
