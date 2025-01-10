package tech.api_factory.sigma.parser.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import tech.api_factory.sigma.parser.SigmaManager;
import tech.api_factory.sigma.parser.SigmaReader;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class QueryBuilderTest {
    private SigmaReader reader;
    private SigmaManager manager;
    private Pattern PATTERN_FOR_AND;
    private Pattern PATTERN_FOR_OR;
    private Pattern PATTERN_FOR_NOT;
    private Pattern PATTERN_FOR_BACKSLASH;
    private int andCountFromParser;
    private int orCountFromParser;
    private int notCountFromParser;
    private int backSlashCountFromParser;
    private int andCountFromConverter;
    private int orCountFromConverter;
    private int notCountFromConverter;
    private int backSlashCountFromConverter;

    @BeforeEach
    void setUp() {
        reader = new SigmaReader();
        manager = new SigmaManager();
        PATTERN_FOR_AND = Pattern.compile("AND");
        PATTERN_FOR_OR = Pattern.compile("OR");
        PATTERN_FOR_NOT = Pattern.compile("NOT");
        PATTERN_FOR_BACKSLASH = Pattern.compile("\\\\");
        andCountFromParser = 0;
        orCountFromParser = 0;
        notCountFromParser = 0;
        backSlashCountFromParser = 0;
        andCountFromConverter = 0;
        orCountFromConverter = 0;
        notCountFromConverter = 0;
        backSlashCountFromConverter = 0;
    }

    @Test
    public void queryTest() throws InterruptedException {
        Map<String, String> rulesWithError = new HashMap<>();
        List<String> paths = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("allRulesPaths.txt"))) { // allRulesPaths or rulesList
            Iterator iterator = br.lines().iterator();
            while (iterator.hasNext()) {
                String line = iterator.next().toString();
                paths.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int counter = 0;
        for (String path : paths) {
            counter++;
            andCountFromParser = 0;
            orCountFromParser = 0;
            notCountFromParser = 0;
            backSlashCountFromParser = 0;
            andCountFromConverter = 0;
            orCountFromConverter = 0;
            notCountFromConverter = 0;
            backSlashCountFromConverter = 0;
            System.out.println(path);
            String queryFromParser = "";
            String queryFromSigConverter = "";
//            try {
                queryFromParser = manager.getQueryFormat(reader.getQueryString(path));
                queryFromSigConverter = QueryHelper.getQueryFromPost(reader.readFile(path));
//            } catch (RuntimeException e) {
//                rulesWithError.put(path, e.getMessage());
//                continue;
//            }




            Matcher matcher = PATTERN_FOR_AND.matcher(queryFromParser);
            while (matcher.find()) {
                andCountFromParser++;
            }
            matcher = PATTERN_FOR_OR.matcher(queryFromParser);
            while (matcher.find()) {
                orCountFromParser++;
            }
            matcher = PATTERN_FOR_NOT.matcher(queryFromParser);
            while (matcher.find()) {
                notCountFromParser++;
            }
            matcher = PATTERN_FOR_BACKSLASH.matcher(queryFromParser);
            while (matcher.find()) {
                backSlashCountFromParser++;
            }

            matcher = PATTERN_FOR_AND.matcher(queryFromSigConverter);
            while (matcher.find()) {
                andCountFromConverter++;
            }
            matcher = PATTERN_FOR_OR.matcher(queryFromSigConverter);
            while (matcher.find()) {
                orCountFromConverter++;
            }
            matcher = PATTERN_FOR_NOT.matcher(queryFromSigConverter);
            while (matcher.find()) {
                notCountFromConverter++;
            }
            matcher = PATTERN_FOR_BACKSLASH.matcher(queryFromSigConverter);
            while (matcher.find()) {
                backSlashCountFromConverter++;
            }


//            if (andCountFromParser != andCountFromConverter) {
//                rulesWithError.put(path, "AND");
//                continue;
//            }
//            if (orCountFromParser != orCountFromConverter) {
//                rulesWithError.put(path, "OR");
//                continue;
//            }
//            if (notCountFromParser != notCountFromConverter) {
//                rulesWithError.put(path, "NOT");
//                continue;
//            }
//            if (backSlashCountFromParser != backSlashCountFromConverter) {
//                rulesWithError.put(path, "backSlash");
//                continue;
//            }


            Assert.isTrue(andCountFromParser == andCountFromConverter, "ERROR in rules with path: " + path);
            Assert.isTrue(orCountFromParser == orCountFromConverter, "ERROR in rules with path: " + path);
            Assert.isTrue(notCountFromParser == notCountFromConverter, "ERROR in rules with path: " + path);
            Assert.isTrue(backSlashCountFromParser == backSlashCountFromConverter, "ERROR in rules with path: " + path);
            System.out.println(counter);
            Thread.sleep(500);
        }
//        rulesWithError.entrySet().stream().forEach(stringStringEntry -> System.out.println(stringStringEntry.getKey() + ": " + stringStringEntry.getValue()));
//        System.out.println(rulesWithError.size());
    }
}