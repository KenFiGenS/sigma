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

    public String getRuleName(String body) {
        String name;
        try (BufferedReader bf = new BufferedReader(new StringReader(body))){
            name = bf.readLine().replaceAll("title: ", "");
        } catch (IOException e) {
            return "Unknown rule name";
        }
        return name;
    }

    public String getRuleLevel(String path) {
        String level;
        try (BufferedReader bf = new BufferedReader(new StringReader(readFile(path)))){
            level = bf.lines().filter(l -> l.startsWith("level: ")).findFirst().get().replace("level: ", "");
        } catch (IOException e) {
            return "Unknown rule name";
        }
        switch (level) {
            case "low": level = "1"; break;
            case "medium": level = "2"; break;
            case "high": level = "3"; break;
            case "critical": level = "4"; break;
        }
        return level;
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

}
