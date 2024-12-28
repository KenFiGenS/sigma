package tech.api_factory.sigma.parser.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import tech.api_factory.sigma.parser.SigmaManager;
import tech.api_factory.sigma.parser.SigmaReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public void queryTest() {
        List<String> paths = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("rulesList.txt"))) {
            Iterator iterator = br.lines().iterator();
            while (iterator.hasNext()) {
                String line = iterator.next().toString();
                paths.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String path : paths) {
            andCountFromParser = 0;
            orCountFromParser = 0;
            notCountFromParser = 0;
            backSlashCountFromParser = 0;
            andCountFromConverter = 0;
            orCountFromConverter = 0;
            notCountFromConverter = 0;
            backSlashCountFromConverter = 0;
//            System.out.println(path);

            String queryFromParser = manager.getQueryFormat(reader.getQueryString(path));
            String queryFromSigConverter = QueryHelper.getQueryFromPost(reader.readFile(path));
//            System.out.println(queryFromSigConverter);

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

//            System.out.println(queryFromParser + " - " + queryFromSigConverter);
//            System.out.println(andCountFromParser + " - " + andCountFromConverter);
//            System.out.println(orCountFromParser + " - " + orCountFromConverter);
//            System.out.println(notCountFromParser + " - " + notCountFromConverter);
//            System.out.println(backSlashCountFromParser + " - " + backSlashCountFromConverter);
            Assert.isTrue(andCountFromParser == andCountFromConverter, "ERROR in rules with path: " + path);
            Assert.isTrue(orCountFromParser == orCountFromConverter, "ERROR in rules with path: " + path);
            Assert.isTrue(notCountFromParser == notCountFromConverter, "ERROR in rules with path: " + path);
            Assert.isTrue(backSlashCountFromParser == backSlashCountFromConverter, "ERROR in rules with path: " + path);
        }
    }
}