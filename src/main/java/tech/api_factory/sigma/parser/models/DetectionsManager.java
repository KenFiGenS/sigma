package tech.api_factory.sigma.parser.models;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DetectionsManager {
    final static Logger logger = LogManager.getLogger(DetectionsManager.class);

    private Map<String, SigmaDetections> detections = new LinkedHashMap<>();
    private Long windowTimeMS = 0L;

    public DetectionsManager() { }

    public void addDetections(String detectionName, SigmaDetections detectionList) {
        detections.put(detectionName, detectionList);
    }

    public SigmaDetections getDetectionsByName(String detectionName) {
        return detections.get(detectionName);
    }

    public Map<String, SigmaDetections> getAllDetections() {
        return detections;
    }

    public void convertWindowTime(String window) {
        /*
            15s  (15 seconds)
            30m  (30 minutes)
            12h  (12 hours)
            7d   (7 days)
            3M   (3 months)
         */
        Long time = 0L;
        if (StringUtils.contains(window, "s")) {
            time = Long.parseLong(StringUtils.substringBefore(window, "s"));
            setWindowTimeMS(TimeUnit.SECONDS.toMillis(time));
        } else if (StringUtils.contains(window, "m")) {
            time = Long.parseLong(StringUtils.substringBefore(window, "m"));
            setWindowTimeMS(TimeUnit.MINUTES.toMillis(time));
        } else if (StringUtils.contains(window, "h")) {
            time = Long.parseLong(StringUtils.substringBefore(window, "h"));
            setWindowTimeMS(TimeUnit.HOURS.toMillis(time));
        } else if (StringUtils.contains(window, "d")) {
            time = Long.parseLong(StringUtils.substringBefore(window, "d"));
            setWindowTimeMS(TimeUnit.DAYS.toMillis(time));
        } else if (StringUtils.contains(window, "M")) {
            time = Long.parseLong(StringUtils.substringBefore(window, "M"));
            setWindowTimeMS(TimeUnit.DAYS.toMillis(time * 30));
        }
   }

    public Long getWindowTimeMS() {
        return windowTimeMS;
    }

    public void setWindowTimeMS(Long windowTimeMS) {
        this.windowTimeMS = windowTimeMS;
    }
}
