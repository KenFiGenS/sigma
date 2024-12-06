package tech.api_factory.sigma.parser;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SigmaDto {
    private String name;
    private String body;
    private String query;
}
