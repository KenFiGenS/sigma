package tech.api_factory.sigma.parser.models;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionsManager {
    final static Logger logger = LogManager.getLogger(ConditionsManager.class);
    private List<SigmaCondition> conditions = new ArrayList<>();

    public List<SigmaCondition> getConditions() {
        return this.conditions;
    }

    public void addCondition(SigmaCondition condition) {
        conditions.add(condition);
    }

    public Boolean hasAggregateCondition() {
        for (SigmaCondition c : conditions) {
            if (c.getAggregateCondition()) {
                return true;
            }
        }

        return false;
    }

    public SigmaCondition getAggregateCondition() {
        for (SigmaCondition c : conditions) {
            if (c.getAggregateCondition()) {
                return c;
            }
        }

        return null;
    }
}
