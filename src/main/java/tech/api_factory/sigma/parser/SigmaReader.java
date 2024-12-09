package tech.api_factory.sigma.parser;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.api_factory.sigma.parser.exceptions.InvalidSigmaRuleException;
import tech.api_factory.sigma.parser.exceptions.SigmaRuleParserException;
import tech.api_factory.sigma.parser.query.QueryBuilder;

@Service
public class SigmaReader {

    private static final Logger LOG = LoggerFactory.getLogger(SigmaReader.class);

//    public final static Path DATA_DIR = Paths.get("/usr/share/graylog-server/data"); //TODO вынести в переменную среды и прихуячить к конфигу
    public final static Path SIGMA_DIR = Paths.get("sigma");

    private final QueryBuilder qb = new QueryBuilder();

    public String readFile(String pathToFile) {
        String path = SIGMA_DIR.resolve(pathToFile).toString();
        try {
            return qb.getSigmaRuleFromFile(path);
        }
        catch (IOException ex) {
            LOG.error("IOException in SigmaReader: {}", ex.getMessage());
            return "Unable to read body:(";
        } catch (InvalidSigmaRuleException e) {
            throw new RuntimeException(e);
        } catch (SigmaRuleParserException e) {
            throw new RuntimeException(e);
        }
    }

    public String getQueryString(String pathToFile) {
        Path path = SIGMA_DIR.resolve(pathToFile);
        try {
            String yamlBody = qb.getSigmaRuleFromFile(path.toString());
            return qb.buildQuery(yamlBody);
        }
        catch (IOException | InterruptedException | URISyntaxException | InvalidSigmaRuleException |
               SigmaRuleParserException ex) {
            LOG.error("Exception in SigmaReader getQueryString: {}", ex.getMessage());
            return "Unable to read body:(";
        }
    }

    public String getQueryString(Path path) {
        try {
            String yamlBody = qb.getSigmaRuleFromFile(path.toString());
            return qb.buildQuery(yamlBody);
        }
        catch (IOException | InterruptedException | URISyntaxException ex) {
            LOG.error("Exception in SigmaReader: {}", ex.getMessage());
            return "Unable to read body:(";
        } catch (SigmaRuleParserException e) {
            throw new RuntimeException(e);
        } catch (InvalidSigmaRuleException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getAllPaths() {
        List<String> filePaths = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get("sigma"))) {
            filePaths = paths.map(Path::toString).collect(Collectors.toList());
        } catch (IOException e) {
            filePaths.add(e.getMessage());
        }
        return filePaths;
    }

    public Map<String, String> getSigmaFields(String body) {
        Map<String, String> sigmaFields = new HashMap();
        try (BufferedReader bf = new BufferedReader(new StringReader(body))) {

            String line = "";
            StringBuilder result = new StringBuilder();
            boolean isDescription = false;

            sigmaFields.put("title", bf.lines().filter(l -> l.startsWith("title: ")).findFirst().get().replace("title: ", ""));

            while (( line = bf.readLine()) != null) {
                if(line.startsWith("description: ")){
                    isDescription = true;
                    result.append(line.replace("description: ", ""));
                }
                else if(line.startsWith("    ") && isDescription){
                    result.append(line.replace("    ", " "));
                }
                else if(isDescription) {
                    break;
                }
            }

            sigmaFields.put("description", result.toString());

            String levelFromSigma = bf.lines().filter(l -> l.startsWith("level: ")).findFirst().get().replace("level: ", "");
            int level;
            long scheduleTime;
            switch (levelFromSigma) {
                case "medium" -> {
                    level = 2;
                    scheduleTime = 600000L;
                }
                case "high", "critical" -> {
                    level = 3;
                    scheduleTime = 300000L;
                }
                default -> {
                    level = 1;
                    scheduleTime = 900000L;
                }
            }
            sigmaFields.put("level", Integer.toString(level));
            sigmaFields.put("scheduleTime", Long.toString(scheduleTime));
        } catch (IOException e) {
            return new HashMap();
        }
        return sigmaFields;
    }

    private static String getDescription(String body) {
        Iterator<String> iterator = body.lines().iterator();
        StringBuilder description = new StringBuilder();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line.startsWith("description: ")) {
                if (line.equals("description: |")) {
                    String nextLine = iterator.next();
                    while (nextLine.startsWith(" ")) {
                        description.append(nextLine.trim());
                        description.append(" ");
                        nextLine = iterator.next();
                    }
                } else {
                    description.append(line.replace("description: ", ""));
                }
            }
        }
        return description.toString().trim();
    }
}
