package tech.api_factory.sigma.parser.query;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CustomSerializerForFieldQuery extends StdSerializer<String> {

    protected CustomSerializerForFieldQuery() {
        this(null);
    }

    protected CustomSerializerForFieldQuery(Class<String> t) {
        super(t);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        value = value.replaceAll("\\*", ".*");
//        value = value.replaceAll("\\\\\\.\\*", ".*");
//        if (value.contains(", "))  value = value.replaceAll(", ", " OR ");
        value = value.replaceAll("\\)" + "\\.\\*", ")");
        value = value.replaceAll("1 of ", "");
        value = value.replaceAll("all of ", "");
        value = value.replace("not", "NOT");
        value = value.replaceAll("\\\\\\\\", "\\\\");
        value = value.replaceAll("\\\\ ", " ");
//        System.out.println(value);
        gen.writeObject(value);
    }
}
