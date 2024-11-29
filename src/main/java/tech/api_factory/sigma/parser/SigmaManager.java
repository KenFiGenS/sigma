package tech.api_factory.sigma.parser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.api_factory.sigma.parser.parsers.DetectionParser;
import tech.api_factory.sigma.parser.query.QueryHelper;

@Service
public class SigmaManager {

    private DetectionParser detectionParser;
    private static final Logger LOG = LoggerFactory.getLogger(SigmaManager.class);
    private final Path rootPath = SigmaReader.SIGMA_DIR;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SigmaReader reader = new SigmaReader();

    public ObjectNode getFileStructure() throws IOException {
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("name", rootPath.getFileName().toString());
        rootNode.putArray("children");

        Deque<ObjectNode> nodeStack = new ArrayDeque<>();
        nodeStack.push(rootNode);

        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(rootPath)) {
                    ObjectNode dirNode = mapper.createObjectNode();
                    dirNode.put("name", dir.getFileName().toString());
                    dirNode.putArray("children");

                    ObjectNode parentNode = nodeStack.peek();
                    parentNode.withArray("children").add(dirNode);

                    nodeStack.push(dirNode);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                ObjectNode fileNode = mapper.createObjectNode();
                fileNode.put("name", file.getFileName().toString());

                ObjectNode parentNode = nodeStack.peek();
                parentNode.withArray("children").add(fileNode);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                if (!dir.equals(rootPath)) {
                    nodeStack.pop();
                }
                return FileVisitResult.CONTINUE;
            }
        });

        LOG.debug("Created sigmaNode: {}", rootNode); //TODO убрать, когда директория будет здоровой
        return rootNode;
    }
    
    public SigmaDto getSigmaDto(String path) throws IOException {
        String body = reader.readFile(path);
        String name = reader.getRuleName(body);
        String query;
        try {
            query = reader.getQueryString(path);
//            throw new IOException();
        } catch (Exception e) {
            query = QueryHelper.getQueryFromPost(body);
        }
        SigmaDto dto = new SigmaDto(name, body, getQueryFormat(query));
        return dto;
    }

    public List<String> getSigmaDtoForTest() throws IOException {
        Map<SigmaDto, String> allSigmaDto = new HashMap<>();
        List<String> paths = reader.getAllPaths();
        Iterator<String> iterator = paths.iterator();
        while (iterator.hasNext()) {
            String sigmaPath = iterator.next();
            sigmaPath = sigmaPath.replace("sigma\\", "");
            if (!sigmaPath.startsWith("rules") || !sigmaPath.endsWith(".yml") || sigmaPath.contains("..")) {
                continue;
            }
            allSigmaDto.put(getSigmaDto(sigmaPath), reader.getRuleLevel(sigmaPath));
        }
        return getJsonFromSigmaDto(allSigmaDto);
    }

    public List<String> getJsonFromSigmaDto(Map<SigmaDto, String> allSigmaDto) throws IOException {
        List<String> jsonResponseList = new ArrayList<>();
        for (SigmaDto sigmaDto : allSigmaDto.keySet()) {
            String currentJson = "{" +
                    "\"title\":\"%s\"," +
                    "\"description\":\"\"," +
                    "\"priority\":\"%s\"," +
                    "\"config\":" +
                        "{\"query\":\"%s\"," +
                        "\"query_parameters\":[]," +
                        "\"streams\":[]," +
                        "\"stream_categories\":[]," +
                        "\"filters\":[]," +
                        "\"search_within_ms\":\"300000\"," +
                        "\"execute_every_ms\":\"300000\"," +
                        "\"event_limit\":\"100\"," +
                        "\"use_cron_scheduling\":\"false\"," +
                        "\"group_by\":[]," +
                        "\"series\":[]," +
                        "\"conditions\":{}," +
                        "\"type\":\"sigma-v1\"}," +
                    "\"field_spec\":{}," +
                    "\"key_spec\":[]," +
                    "\"notification_settings\":" +
                        "{\"grace_period_ms\":\"300000\"," +
                        "\"backlog_size\":\"null\"}," +
                    "\"notifications\":[]," +
                    "\"alert\":\"false\"" +
        "}";
            String query = getQueryFormat(sigmaDto.getQuery());
//            query = Base64.getEncoder().encodeToString(query.getBytes(Charset.defaultCharset()));
            String jsonBody = String.format(currentJson, sigmaDto.getName(), allSigmaDto.get(sigmaDto), query);

            jsonResponseList.add(jsonBody);
        }
        List<String> result = new ArrayList<>();
        String oneRule = jsonResponseList.get(1988);
        System.out.println(oneRule);
        String body = requestToCreate(oneRule);
        result.add(body);
        return result;
    }

    public static String requestToCreate(String jsonBody) {
        String uriString = "http://10.200.0.182:9000/api/events/definitions?schedule=true";

        HttpClient httpClient = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(uriString))
                    .header("Content-Type", "Application/Json")
                    .header("Cookie", "27d7a78a-cd97-4af1-9eef-eb2145e62093")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            try{
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println(response);
                return response.toString();
            }
            catch (IOException e) {
                return "IOException has been thrown while requesting secondary sigma parsing resource";
            }
            catch (InterruptedException e) {
                return "InterruptedException has been thrown while requesting secondary sigma parsing resource";
            }
        }
        catch (URISyntaxException e){
            return "Impossible error (or sigmaConvert endpoint has been changed...)";
        }
    }

    public String getQueryFormat(String value) throws IOException {
        value = value.replaceAll("\\*", ".*");
        value = value.replaceAll("\\)" + "\\.\\*", ")");
        value = value.replaceAll("1 of ", "");
        value = value.replaceAll("all of ", "");
        value = value.replace("not", "NOT");
        value = value.replaceAll("\\\\\\\\", "\\\\");
        value = value.replaceAll("\\\\ ", " ");
        value = value.replaceAll("\\\\", "\\\\\\\\");
        return value;
    }
}