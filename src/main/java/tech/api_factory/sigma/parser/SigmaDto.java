package tech.api_factory.sigma.parser;


import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import tech.api_factory.sigma.parser.query.CustomSerializerForFieldQuery;

import java.util.List;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SigmaDto {
    private String name;
    private String body;
    private String query;
}
