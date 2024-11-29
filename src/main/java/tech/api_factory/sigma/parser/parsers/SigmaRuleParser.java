package tech.api_factory.sigma.parser.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.api_factory.sigma.parser.exceptions.InvalidSigmaRuleException;
import tech.api_factory.sigma.parser.exceptions.SigmaRuleParserException;
import tech.api_factory.sigma.parser.models.SigmaRule;

public class SigmaRuleParser {
    final static Logger logger = LogManager.getLogger(SigmaRuleParser.class);
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private DetectionParser detectionParser;
    private ConditionParser conditionParser;

    public SigmaRuleParser() {
        detectionParser = new DetectionParser();
        conditionParser = new ConditionParser();
    }

    public SigmaRule parseRule(String rule)
        throws IOException, InvalidSigmaRuleException, SigmaRuleParserException {
        ParsedSigmaRule parsedSigmaRule = yamlMapper.readValue(rule, ParsedSigmaRule.class);

        return parseRule(parsedSigmaRule);
    }

    public SigmaRule parseRule(ParsedSigmaRule parsedSigmaRule)
        throws InvalidSigmaRuleException, SigmaRuleParserException {
        SigmaRule sigmaRule = new SigmaRule();
        sigmaRule.copyParsedSigmaRule(parsedSigmaRule);

        sigmaRule.setDetection(detectionParser.parseDetections(parsedSigmaRule));
        sigmaRule.setConditionsManager(conditionParser.parseCondition(parsedSigmaRule));

        return sigmaRule;
    }


}
