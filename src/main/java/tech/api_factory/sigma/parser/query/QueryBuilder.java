package tech.api_factory.sigma.parser.query;

import org.springframework.stereotype.Service;
import tech.api_factory.sigma.parser.exceptions.InvalidSigmaRuleException;
import tech.api_factory.sigma.parser.exceptions.SigmaRuleParserException;
import tech.api_factory.sigma.parser.models.DetectionsManager;
import tech.api_factory.sigma.parser.models.SigmaCondition;
import tech.api_factory.sigma.parser.models.SigmaDetection;
import tech.api_factory.sigma.parser.models.SigmaRule;
import tech.api_factory.sigma.parser.parsers.SigmaRuleParser;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class QueryBuilder {
    static final Pattern PATTERN_FOR_CONDITION_LINE = Pattern.compile("_\\*|\\*");
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
        System.out.println("===============================================================================");
        // Получаем строку condition непосредственно из String Yaml
        String condition = getConditionLineFromYaml(yamlSource);
        System.out.println(condition);
        return getConditionResult(sigmaRule, condition);
//        System.out.println("===============================================================================");
//        // Получаем все Conditions и Detections из объекта SigmaRule
//        List<SigmaCondition> sigmaConditions = sigmaRule.getConditionsManager().getConditions();
//        DetectionsManager detectionsManager = sigmaRule.getDetectionsManager();
//
//        // Вычленяем имена Detection из поля Condition в объекте SigmaRule
//        List<String> detectionNamesInConditionLine = new ArrayList<>();
//        for (SigmaCondition sigmaCondition : sigmaConditions) {
//            recursiveInspectConditionNames(sigmaCondition, detectionNamesInConditionLine);
//        }
//
////        System.out.println(condition);
//
//        if (condition.equals(ONE_OF_SELECTION_1) || condition.equals(ALL_OF_SELECTION_1) ||
//                condition.equals(ONE_OF_SELECTION_2) || condition.equals(ALL_OF_SELECTION_2) ||
//                condition.contains(ONE_OF_SELECTION1) || condition.contains(ALL_OF_SELECTION1)) {
////            System.out.println(condition);
//            detectionNamesInConditionLine = List.copyOf(detectionsManager.getAllDetections().keySet());
//            condition = getConditionLine(condition, detectionNamesInConditionLine);
//        }
////        detectionNamesInConditionLine.forEach(s -> System.out.println(s));
////        System.out.println(condition);
//        return getHardConditionLine(condition, detectionNamesInConditionLine, detectionsManager);
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
        System.out.println(conditionResult.toString().trim());
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
                .filter(s -> s.length() > 3).toList();

        return getQueryFromSimpleSigmaRule(aggregatedDetectionName, allDetectionName, detectionsManager);
    }

    public String getQueryFromSimpleSigmaRule(String result, List<String> detectionNamesInConditionLine, DetectionsManager detectionsManager) {
        Iterator<String> detectionNamesIterator = detectionNamesInConditionLine.iterator();
        String valueResult;
        while (detectionNamesIterator.hasNext()) {
            String currentDetectionName = detectionNamesIterator.next();
            List<SigmaDetection> detections = detectionsManager.getDetectionsByName(currentDetectionName).getDetections();
            StringBuilder keyValueByDetectionName = new StringBuilder();
            for (SigmaDetection d : detections) {
                valueResult = d.getValues().toString();
                if (d.getMatchAll()) {
                    valueResult = valueResult.replaceAll(", ", OPERATOR_AND + d.getName() + ":");
                } else {
                    valueResult = valueResult.replaceAll(", ", OPERATOR_OR);
                }
                if (isList(yaml, currentDetectionName)) {
                    keyValueByDetectionName.append(d.getName()).append(":").append(valueResult).append(OPERATOR_OR);
                } else {
                    keyValueByDetectionName.append(d.getName()).append(":").append(valueResult).append(OPERATOR_AND);
                }
            }
            valueResult = valueFormat(keyValueByDetectionName.toString());
            result = result.replaceAll(currentDetectionName, "(" + valueResult + ")");
        }
        return result;
    }

//    public String getHardConditionLine(String result, List<String> conditionNames, DetectionsManager detectionsManager) {
//        List<String> detectionName = detectionsManager.getAllDetections().keySet().stream().toList();
//        Map<String, String> keyValue = new HashMap<>();
//        boolean isAndConditions;
//        for (String currentDetectionName : detectionName) {
//            for (String conditionName : conditionNames) {
//                isAndConditions = conditionName.equals("all");
//                if (currentDetectionName.contains(conditionName.replace("_*", "")) && !conditionName.equals("of") && !conditionName.equals("1")) {
//                    StringBuilder keyValueByDetectionName = new StringBuilder();
//                    List<SigmaDetection> detections = detectionsManager.getDetectionsByName(currentDetectionName).getDetections();
//                    for (SigmaDetection d : detections) {
//                        String currentValue = d.getValues().toString();
//                        if (!d.getMatchAll()) {
//                            currentValue = currentValue.replaceAll(", ", " OR ");
//                        } else {
//                            currentValue = currentValue.replaceAll(", ", " AND " + d.getName() + ":");
//                        }
//                        if (currentDetectionName.equals("selection")) {
//                            keyValueByDetectionName.append(d.getName()).append(":").append(currentValue).append(" AND ");
//                        } else if (isListForHardRules(yaml, currentDetectionName)) {
//                            if (isList(yaml, currentDetectionName)) {
//                                keyValueByDetectionName.append(d.getName()).append(":").append(currentValue).append(" OR ");
//                            } else {
//                                keyValueByDetectionName.append(d.getName()).append(":").append(currentValue).append(" AND ");
//                            }
//                        } else {
//                            keyValueByDetectionName.append(d.getName()).append(":").append(currentValue).append(" AND ");
//                        }
//                    }
//                    keyValueByDetectionName = new StringBuilder(valueFormat(keyValueByDetectionName.toString()));
//                    if (!keyValue.containsKey(conditionName)) {
//                        keyValue.put(conditionName, keyValueByDetectionName.toString());
//                    } else if (currentDetectionName.contains("selection") || isAndConditions) {
//                        StringBuilder builder = new StringBuilder(keyValue.get(conditionName));
//                        builder.append(" AND ").append(keyValueByDetectionName);
//                        keyValue.put(conditionName, builder.toString());
//                    } else {
//                        StringBuilder builder = new StringBuilder(keyValue.get(conditionName));
//                        builder.append(" OR ").append(keyValueByDetectionName);
//                        keyValue.put(conditionName, builder.toString());
//                    }
//                }
//            }
//        }
//        for (String k : keyValue.keySet()) {
//            String value = keyValue.get(k);
//            result = result.replaceAll(Pattern.quote(k), "(" + value + ")");
//        }
//        return result;
//    }

    public String valueFormat(String value) {
        System.out.println(value);
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

//    public String getConditionLine(String condition, List<String> detectionsNames) {
//        StringBuilder conditionResult = new StringBuilder();
//        for (int i = 0; i < detectionsNames.size(); i++) {
//            if (i + 1 < detectionsNames.size()) {
//                if (condition.contains("all")) {
//                    conditionResult
//                            .append(detectionsNames.get(i))
//                            .append(" AND ");
//                } else if (condition.contains("1")){
//                    conditionResult
//                            .append(detectionsNames.get(i))
//                            .append(" OR ");
//                }
//            } else {
//                conditionResult.append(detectionsNames.get(i));
//            }
//        }
//        return conditionResult.toString();
//    }

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

//    private boolean isListForHardRules(String yaml, String detectionName) {
//
//        try (BufferedReader br = (new BufferedReader(new StringReader(yaml)))) {
//            Iterator<String> iterator = br.lines().iterator();
//            while (iterator.hasNext()) {
//                String line = iterator.next();
//                if (line.contains(detectionName)) {
//                    if (iterator.hasNext()) iterator.next();
//                    if (iterator.hasNext()) {
//                        String nextLine = iterator.next().trim();
//                        if (nextLine.startsWith("- ")) return true;
//                    }
//                }
//            }
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        }
//        return false;
//    }
}
