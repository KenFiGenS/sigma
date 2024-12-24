package tech.api_factory.sigma.parser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final String TWIN_BACK_SLASH = "\\\\";
    private final String NOT_FOR_QUERY = "NOT";
    private final String ONE_OF = "1 of ";
    private final String ALL_OF = "all of ";
    private final String NOT_FROM_CONDITION ="not";
    private final Pattern BACK_SLASH = Pattern.compile("\\\\");
    private static final Pattern BACK_SLASH_WITH_SYMBOLS = Pattern.compile("[^-|\\s|=]\\\\[^-|\\s|:|=|(|)|<|>|/|\\[|\\]|!|\\|]");

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
        Map<String, String> sigmaFields = reader.getSigmaFields(body);
        String name = sigmaFields.get("title");
        String description = sigmaFields.get("description");
        int priority = Integer.parseInt(sigmaFields.get("level"));
        String query;
        try {
            query = getQueryFormat(reader.getQueryString(path));
        } catch (Exception e) {
            System.out.println("Ошибка парсинга! \n " + e.getMessage() + "\n" + e + " Отправлен POST запрос");
            query = QueryHelper.getQueryFromPost(body);
        }
//        System.out.println(description);
        SigmaDto dto = new SigmaDto(name, body, query);
        System.out.println(dto.getQuery());
//        queryValidate(List.of(dto.getQuery()));
        return dto;
    }

    public Map<String, String> getSigmaDtoForTest() throws IOException {
        Map<String, String> allSigmaQuery = new HashMap<>();
        List<String> paths = reader.getAllPaths();
        Iterator<String> iterator = paths.iterator();
        List<String> all = new ArrayList<>();
        while (iterator.hasNext()) {
            String sigmaPath = iterator.next();
            sigmaPath = sigmaPath.replace("sigma\\", "");
            if (!sigmaPath.startsWith("rules") || !sigmaPath.endsWith(".yml") || sigmaPath.contains("..")) {
                continue;
            }
            SigmaDto currentSigma = getSigmaDto(sigmaPath);
            allSigmaQuery.put(currentSigma.getName(), currentSigma.getQuery());
            all.add(currentSigma.getQuery());
        }
//        allSigmaQuery.keySet().stream().forEach(t -> System.out.println(allSigmaQuery.get(t)));

//        queryValidate(all);
        return allSigmaQuery;
    }

    public String getQueryFormat(String value) {
        System.out.println(value);
        value = value.replaceAll(ONE_OF, "");
        value = value.replaceAll(ALL_OF, "");
        value = value.replace(NOT_FROM_CONDITION, NOT_FOR_QUERY);
        value = backSlashHandler(value);
        return value;
    }

    public String backSlashHandler(String value) {
        StringBuilder finalQueryBuilder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            String currentSymbol = String.valueOf(value.charAt(i));
            Matcher matcher = BACK_SLASH.matcher(currentSymbol);
            if (matcher.find()) {
                String nextSymbol = (i + 1 != value.length()) ? String.valueOf(value.charAt(i + 1)) : "";
                String previousSymbol = (i - 1 >= 0) ? String.valueOf(value.charAt(i - 1)) : "";
                String treeSymbols = String.join("", previousSymbol, currentSymbol, nextSymbol);
                Matcher matcher2 = BACK_SLASH_WITH_SYMBOLS.matcher(treeSymbols);
                if (matcher2.find()) {
                    finalQueryBuilder.append(TWIN_BACK_SLASH);
                } else {
                    finalQueryBuilder.append(currentSymbol);
                }
            } else {
                finalQueryBuilder.append(currentSymbol);
            }
        }
        return finalQueryBuilder.toString();
    }

    public String queryValidate(List<String> allQuery) {
        String uriString = "http://10.200.0.182:9000/api/search/validate";
        String jsonBody = "{\"query\": \"%s\"}";

        String value = allQuery.get(0);


        String finalizedJsonBody = String.format(jsonBody, value);

        System.out.println(finalizedJsonBody);
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofSeconds(2)) // время ожидания соединения
                .build();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(uriString))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic YWRtaW46bnE4VWxpQW5yTExtN0ZOVlpwMjhINldnVUxweFY0RXlieG1UZmQ5T0tRZXQxNXZsSW1GMUlaR2w5Y25tbDc0MU5RZnJsY3BaSUdGWmxXclByaFJTMlB3MVlPZEFZS3dQ")
                    .header("X-Requested-By", "Java TestScript")
                    .POST(HttpRequest.BodyPublishers.ofString(finalizedJsonBody))
                    .build();

            try{
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println(response);
                System.out.println(response.statusCode() +" : " + response.body());
            }
            catch (IOException e) {
                return "IOException has been thrown while requesting secondary sigma parsing resource";
            }
            catch (InterruptedException e) {
                return "InterruptedException has been thrown while requesting secondary sigma parsing resource";
            }
        }
        catch (URISyntaxException e){
            return "Impossible error (or sigmaconvert endpoint has been changed...)";
        }
        return uriString;
    }
}