package tech.api_factory.sigma.parser.models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.api_factory.sigma.parser.parsers.ParsedSigmaRule;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SigmaRule {
    private String title;
    private String description;
    private String id;
    private String author;
    private List<String> references = new ArrayList<>();
    private LogSource logsource = new LogSource();
    private DetectionsManager detectionsManager = new DetectionsManager();
    private ConditionsManager conditionsManager = new ConditionsManager();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public DetectionsManager getDetectionsManager() { return detectionsManager; }

    public void setDetection(DetectionsManager detectionsManager) { this.detectionsManager = detectionsManager; }

    public ConditionsManager getConditionsManager() { return conditionsManager; }

    public void setConditionsManager(ConditionsManager conditionsManager) { this.conditionsManager = conditionsManager; }

    public LogSource getLogsource() {
        return logsource;
    }

    public void setLogsource(LogSource logsource) {
        this.logsource = logsource;
    }

    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public void copyParsedSigmaRule(ParsedSigmaRule parsedSigmaRule) {
        this.title = parsedSigmaRule.getTitle();
        this.description = parsedSigmaRule.getDescription();
        this.id = parsedSigmaRule.getId();
        this.author = parsedSigmaRule.getAuthor();
        this.references = parsedSigmaRule.getReferences();
        this.logsource = parsedSigmaRule.getLogsource();
    }

}
