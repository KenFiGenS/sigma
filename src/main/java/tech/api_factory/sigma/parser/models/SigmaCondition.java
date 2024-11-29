package tech.api_factory.sigma.parser.models;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SigmaCondition {
    final static Logger logger = LogManager.getLogger(SigmaCondition.class);

    private String conditionName = "";
    private String operator = "";
    private Boolean notCondition = false;
    private SigmaCondition pairedCondition = null;
    private Boolean aggregateCondition = false;
    private AggregateValues aggregateValues = null;

    public SigmaCondition() {}

    public SigmaCondition(String conditionName) {
        this.conditionName = conditionName;
    }

    public AggregateValues getAggregateValues() {
        return aggregateValues;
    }

    public void setAggregateValues(AggregateValues aggregateValues) {
        this.aggregateValues = aggregateValues;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public Boolean getNotCondition() {
        return notCondition;
    }

    public void setNotCondition(Boolean notCondition) {
        this.notCondition = notCondition;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public SigmaCondition getPairedCondition() {
        return pairedCondition;
    }

    public void setPairedCondition(SigmaCondition pairedCondition) {
        this.pairedCondition = pairedCondition;
    }

    public Boolean getAggregateCondition() {
        return aggregateCondition;
    }

    public void setAggregateCondition(Boolean aggregateCondition) {
        this.aggregateCondition = aggregateCondition;
    }

    @Override
    public String toString() {
        String condition = new String(conditionName);

        if (operator != null) {
            condition += " " + operator;
        }

        if (notCondition == true) {
            condition += " NOT"; 
        }

        if (pairedCondition != null) {
            condition += " " + pairedCondition.toString();
        }

        return condition;
    }
}
