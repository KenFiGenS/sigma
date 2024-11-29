package tech.api_factory.sigma.parser.query;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Locale;

@Service
public class QueryHelper {

    private static final String uriString = "https://sigconverter.io/api/v1/1.0.4/convert";
    private static final String jsonBody =
            "{" +
                    "\"rule\":\"%s\"," +
                    "\"pipelineYml\":\"\"," +
                    "\"pipeline\":[]," +
                    "\"target\":\"opensearch_lucene\"," +
                    "\"format\":\"default\"" +
                    "}";



    public static String getQueryFromPost(String yamlBody) {
        String encodedYaml;
        encodedYaml = Base64.getEncoder().encodeToString(yamlBody.getBytes(Charset.defaultCharset()));
        String finalizedJsonBody = String.format(Locale.ROOT, jsonBody, encodedYaml);

//        System.out.println(finalizedJsonBody);

        HttpClient httpClient = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(uriString))
                    .header("Content-Type", "Application/Json")
                    .POST(HttpRequest.BodyPublishers.ofString(finalizedJsonBody))
                    .build();

            try{
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if(!response.body().isEmpty()) Thread.sleep(100);
                return response.body();
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
    }
}
