package tech.api_factory.sigma.REST;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.api_factory.sigma.parser.SigmaDto;
import tech.api_factory.sigma.parser.SigmaManager;

import java.io.IOException;

@RestController
@AllArgsConstructor
@Validated
@RequestMapping(path = "/sigma")
public class SigmaConverterController {

    SigmaManager manager;

    @GetMapping
    public ResponseEntity<ObjectNode> getFileStructure() {
        ObjectNode node;
        try {
            node = manager.getFileStructure();
        } catch(Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>((node), null, HttpStatus.OK);
    }

    @GetMapping("/sigmaPath")
    @JsonInclude
    public  ResponseEntity<SigmaDto> getParsedSigma(@RequestParam("sigmaPath") String sigmaPath) throws JsonProcessingException {
        System.out.println(sigmaPath);
        if (!sigmaPath.startsWith("rules") || !sigmaPath.endsWith(".yml") || sigmaPath.contains("..")) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        SigmaManager manager = new SigmaManager();
        SigmaDto dto;
        try{
            dto = manager.getSigmaDto(sigmaPath);
        }
        catch (Exception e) {
            SigmaDto sigmaDto = new SigmaDto(e.getMessage(), e.toString(), e.getLocalizedMessage());
            return new ResponseEntity<>(sigmaDto, null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(dto, null, HttpStatus.OK);
    }

    @GetMapping("/test")
    public ResponseEntity<Object> getDefinitionFromSigmaTest() throws IOException {
        SigmaManager manager = new SigmaManager();
        return new ResponseEntity<>(manager.getSigmaDtoForTest(), null, HttpStatus.OK);
    }
}
