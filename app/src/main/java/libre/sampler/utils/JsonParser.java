package libre.sampler.utils;

import android.util.JsonReader;

import java.io.IOException;

public class JsonParser {
    public static JsonTypes.Any parse(JsonReader reader) throws IOException {
        switch(reader.peek()) {
            case BEGIN_OBJECT:
                return new JsonTypes.Any(parseObject(reader));
            case BEGIN_ARRAY:
                return new JsonTypes.Any(parseArray(reader));
            case STRING:
                return new JsonTypes.Any(reader.nextString());
            case NUMBER:
                return new JsonTypes.Any(reader.nextDouble());
            case BOOLEAN:
                return new JsonTypes.Any(reader.nextBoolean());
            default:
                throw new IOException("Json Type Error: parser does not support null");
        }
    }

    public static JsonTypes.Object parseObject(JsonReader reader) throws IOException {
        JsonTypes.Object object = new JsonTypes.Object();
        reader.beginObject();
        while(reader.hasNext()) {
            object.put(reader.nextName(), parse(reader));
        }
        reader.endObject();
        return object;
    }

    public static JsonTypes.Array parseArray(JsonReader reader) throws IOException {
        JsonTypes.Array array = new JsonTypes.Array();
        reader.beginArray();
        while(reader.hasNext()) {
            array.add(parse(reader));
        }
        reader.endArray();
        return array;
    }
}
