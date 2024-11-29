package tech.api_factory.sigma.parser.models;

import java.util.ArrayList;
import java.util.List;

public class SigmaDetections {
    private List<SigmaDetection> detections = new ArrayList<>();

    public List<SigmaDetection> getDetections() {
        return detections;
    }

    public void setDetections(List<SigmaDetection> detections) {
        this.detections = detections;
    }

    public void addDetection(SigmaDetection detection) {
        this.detections.add(detection);
    }
}
