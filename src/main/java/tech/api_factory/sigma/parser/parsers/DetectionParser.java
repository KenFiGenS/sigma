package tech.api_factory.sigma.parser.parsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import tech.api_factory.sigma.parser.exceptions.InvalidSigmaRuleException;
import tech.api_factory.sigma.parser.exceptions.SigmaRuleParserException;
import tech.api_factory.sigma.parser.models.DetectionsManager;
import tech.api_factory.sigma.parser.models.ModifierType;
import tech.api_factory.sigma.parser.models.SigmaDetection;
import tech.api_factory.sigma.parser.models.SigmaDetections;

@Service
public class DetectionParser {
    final static Logger logger = LogManager.getLogger(DetectionParser.class);

    static final String ESCAPED_CHARACTERS = "+ - && || / ! = { } [ ] ^ \" ~ * ? : \\";
    static final String ESCAPE = "\\\\";
    static final String OPEN_BRACKET = "{";
    static final String CLOSE_BRACKET = "}";
    static final String OPEN_ARRAY = "[";
    static final String CLOSE_ARRAY = "]";
    static final String EQUALS = "=";
    static final String SEPERATOR = "|";
    static final String COMMA_SEP = ",";

    public DetectionParser() {}

    public DetectionsManager parseDetections(ParsedSigmaRule sigmaRule)
        throws InvalidSigmaRuleException, SigmaRuleParserException {
        DetectionsManager detectionsManager = new DetectionsManager();

        // loop through list of detections - search identifier are the keys
        // the values can be either lists or maps (key / value pairs)
        // See https://github.com/SigmaHQ/sigma/wiki/Specification#detection
        for (Map.Entry<String, Object> entry : sigmaRule.getDetection().entrySet()) {
            String detectionName = entry.getKey();
            Object searchIdentifiers = entry.getValue();

            if (detectionName.equals("condition") || detectionName.equals("timeframe") ||
                detectionName.equals("fields")) {
                // handle separately
            } else if (detectionName.equals("keywords")) {
                List<String> names = (List<String>) searchIdentifiers;
                List<SigmaDetection> sigmaDetectionList = new ArrayList<>();
                for (String s : names) {
                    StringBuilder buildKeyWord = new StringBuilder();
                    SigmaDetection sigmaDetection = new SigmaDetection();
                    sigmaDetection.setName( buildKeyWord.append("*").append(s).append("*").toString());
                    sigmaDetectionList.add(sigmaDetection);
                }
               SigmaDetections sigmaDetections = new SigmaDetections();
               sigmaDetections.setDetections(sigmaDetectionList);
               detectionsManager.addDetections(detectionName,sigmaDetections);
            } else {
                detectionsManager.addDetections(detectionName, parseDetection(searchIdentifiers));
            }
        }

        if (sigmaRule.getDetection().containsKey("timeframe")) {
            detectionsManager.convertWindowTime(sigmaRule.getDetection().get("timeframe").toString());
        }

        return detectionsManager;
    }

    private void parseMap(SigmaDetections parsedDetections, LinkedHashMap<String, Object> searchIdMap)
        throws InvalidSigmaRuleException {

        for (Map.Entry<String, Object> searchId : searchIdMap.entrySet()) {
            if (searchId.getValue() instanceof ArrayList) {
                List<Object> searchArray = (ArrayList<Object>)searchId.getValue();
                parseList(parsedDetections, searchId.getKey(), searchArray);
            } else if (searchId.getValue() instanceof LinkedHashMap) {
                LinkedHashMap<String, Object> searchIdInnerMap = (LinkedHashMap<String, Object>) searchId.getValue();
                parseMap(parsedDetections, searchIdInnerMap);
            } else { // key is the detection name
                SigmaDetection detectionModel = new SigmaDetection();
                parseName(detectionModel, searchId.getKey());
                parseValue(detectionModel, searchId.getValue().toString());

                parsedDetections.addDetection(detectionModel);
            }
        }
    }

    private void parseList(SigmaDetections parsedDetections, String name, List<Object> searchIdValues)
        throws InvalidSigmaRuleException {

        SigmaDetection detectionModel = null;
        if (name != null) {
            detectionModel = new SigmaDetection();
            parseName(detectionModel, name);
        }

        for (Object v : searchIdValues) {
            if ((v instanceof LinkedHashMap) || (name == null)) {
                LinkedHashMap<String, Object> searchIdMap = (LinkedHashMap<String, Object>)v;
                parseMap(parsedDetections, searchIdMap);
            } else {
                parseValue(detectionModel, v.toString());
            }
        }

        if ((detectionModel != null) && (detectionModel.getValues().size() > 0)) {
            parsedDetections.addDetection(detectionModel);
        }
    }

    private SigmaDetections parseDetection(Object searchIdentifiers)
        throws InvalidSigmaRuleException, SigmaRuleParserException {
        SigmaDetections parsedDetections = new SigmaDetections();

        // check if the search identifier is a list or a map
        if (searchIdentifiers instanceof LinkedHashMap) {
            LinkedHashMap<String, Object> searchIdMap = (LinkedHashMap<String, Object>) searchIdentifiers;
            parseMap(parsedDetections, searchIdMap);
        } else if (searchIdentifiers instanceof ArrayList) {
            // Array list contains a map of key/values and parsed by the parseMap function eventually
            List<Object> searchArray = (ArrayList<Object>)searchIdentifiers;
            parseList(parsedDetections, null, searchArray);
        } else {
            logger.error("unknown type: " + searchIdentifiers.getClass() + " value: " + searchIdentifiers);
            throw new SigmaRuleParserException("Unknown type: " + searchIdentifiers.getClass() +
                " value: " + searchIdentifiers);
        }
        return parsedDetections;
    }

    private void parseName(SigmaDetection detectionModel, String name) {
        String parsedName = StringUtils.substringBefore(name, SEPERATOR);

        detectionModel.setSigmaName(parsedName);
        detectionModel.setName(parsedName);

        // handles the case where the modifier is piped with the name (ex. field|endswith)
        // modifiers can be chained together
        if (StringUtils.contains(name, SEPERATOR)) {
            String[] modifiers = StringUtils.split(name, SEPERATOR);

            Iterator<String> iterator = Arrays.stream(modifiers).iterator();
            while(iterator.hasNext()) {
                ModifierType modifier = ModifierType.getEnum(iterator.next());
                if (modifier == ModifierType.ALL) {
                    detectionModel.setMatchAll(true);
                } else {
                    detectionModel.addModifier(modifier);
                }
            }
        }
    }


    private void parseValue(SigmaDetection detectionModel, String value) throws InvalidSigmaRuleException {
        if (detectionModel.getModifiers().size() > 0) {
            for (ModifierType modifier : detectionModel.getModifiers()) {
                detectionModel.addValue(buildStringWithModifier(value, modifier));
            }
        }
        else {
            detectionModel.addValue(sigmaWildcardToRegex(value));
        }
    }

    // TODO We need to handle escaping in sigma
    private String buildStringWithModifier(String value, ModifierType modifier) throws InvalidSigmaRuleException {

        // Sigma spec isn't clear on what to do with wildcard characters when they are in values with a "transformation"
        // which we are calling operator
        if (modifier != null) {
            switch (modifier) {
                case STARTS_WITH:
                case BEGINS_WITH:
                    return sigmaWildcardToRegex(value) + "*";
                case CONTAINS:
                    return "*" + sigmaWildcardToRegex(value) + "*";
                case ENDS_WITH:
                    return "*" + sigmaWildcardToRegex(value);
                case WINDASH:
                    return buildStringWithModifier(value.replace("-", "/"), ModifierType.CONTAINS);
                case REGEX:
                    if (!validRegex(value))
                        throw new InvalidSigmaRuleException("Regular expression operator specified " +
                                "but pattern did not compile for value = " + value);
                    return value;
            }
        }

        return sigmaWildcardToRegex(value);
    }

    private boolean validRegex(String regex) {
        try {
            // check if pattern is already a regex and do nothing
            Pattern.compile(regex);
            return true;
          } catch (PatternSyntaxException e) {
           return false;
          }
    }

    /**
     * This function takes a sigma expression which allows the typical search wildcards and converts it into a java regex
     * pattern.  If there are no sigma wildcards then nothing will change
     * @param value sigma pattern value
     * @return java regex pattern
     */
    private String sigmaWildcardToRegex(String value) {
        StringBuilder out = new StringBuilder();
        for(int i = 0; i < value.length(); ++i) {
            final char c = value.charAt(i);
            if (ESCAPED_CHARACTERS.contains(String.valueOf(c))) {
                out.append(ESCAPE).append(c);
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
