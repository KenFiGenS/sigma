package tech.api_factory.sigma.parser.models;

import java.util.HashMap;
import java.util.Map;

public enum ModifierType {

    BEGINS_WITH("beginswith"),
    STARTS_WITH("startswith"),
    ENDS_WITH("endswith"),
    CONTAINS("contains"),
    REGEX("re"),
    GREATER_THAN("greater_than"),
    LESS_THAN("less_than"),
    BASE64("base64offset"),
    WINDASH("windash"),
    ALL("all");

    private final String value;


    ModifierType(String val) {
        value = val;

    }
    private static final Map<String, ModifierType> lookup = new HashMap<String, ModifierType>();

    //Populate the lookup table on loading time
    static
    {
        for(ModifierType t: ModifierType.values())
        {
            lookup.put(t.value, t);
        }
    }

    public static ModifierType getEnum(String val)
    {
        return lookup.get(val);
    }
}
