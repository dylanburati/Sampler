package libre.sampler.utils;

import android.util.JsonToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class JsonTypes {

    public static class Any {
        private JsonToken realType;

        private JsonTypes.Object valueObject;
        private JsonTypes.Array valueArray;
        private String valueString;
        private double valueDouble;
        private boolean valueBoolean;

        public Any(JsonTypes.Object valueObject) {
            this.realType = JsonToken.BEGIN_OBJECT;
            this.valueObject = valueObject;
        }

        public JsonTypes.Object getValueObject() throws IOException {
            if(realType != JsonToken.BEGIN_OBJECT) {
                throw new IOException(String.format("Json Type Error: expected %s, actual %s",
                        JsonToken.BEGIN_OBJECT.toString(), realType.toString()));
            }
            return valueObject;
        }

        public Any(JsonTypes.Array valueArray) {
            this.realType = JsonToken.BEGIN_ARRAY;
            this.valueArray = valueArray;
        }

        public JsonTypes.Array getValueArray() throws IOException {
            if(realType != JsonToken.BEGIN_ARRAY) {
                throw new IOException(String.format("Json Type Error: expected %s, actual %s",
                        JsonToken.BEGIN_ARRAY.toString(), realType.toString()));
            }
            return valueArray;
        }

        public Any(String valueString) {
            this.realType = JsonToken.STRING;
            this.valueString = valueString;
        }

        public String getValueString() throws IOException {
            if(realType != JsonToken.STRING) {
                throw new IOException(String.format("Json Type Error: expected %s, actual %s",
                        JsonToken.STRING.toString(), realType.toString()));
            }
            return valueString;
        }

        public Any(double valueDouble) {
            this.realType = JsonToken.NUMBER;
            this.valueDouble = valueDouble;
        }

        public double getValueDouble() throws IOException {
            if(realType != JsonToken.NUMBER) {
                throw new IOException(String.format("Json Type Error: expected %s, actual %s",
                        JsonToken.NUMBER.toString(), realType.toString()));
            }
            return valueDouble;
        }

        public float getValueFloat() throws IOException {
            if(realType != JsonToken.NUMBER) {
                throw new IOException(String.format("Json Type Error: expected %s, actual %s",
                        JsonToken.NUMBER.toString(), realType.toString()));
            }
            return (float) valueDouble;
        }

        public int getValueInt() throws IOException {
            if(realType != JsonToken.NUMBER) {
                throw new IOException(String.format("Json Type Error: expected %s, actual %s",
                        JsonToken.NUMBER.toString(), realType.toString()));
            }
            int valueInt = (int) valueDouble;
            if(Math.abs(valueInt - valueDouble) > 1e-14) {
                throw new IOException("Json Conversion Error: number is not an integer");
            }
            return valueInt;
        }

        public Any(boolean valueBoolean) {
            this.realType = JsonToken.BOOLEAN;
            this.valueBoolean = valueBoolean;
        }

        public boolean getValueBoolean() throws IOException {
            if(realType != JsonToken.BOOLEAN) {
                throw new IOException(String.format("Json Type Error: expected %s, actual %s",
                        JsonToken.BOOLEAN.toString(), realType.toString()));
            }
            return valueBoolean;
        }
    }

    public static class Object extends HashMap<String, JsonTypes.Any> {
        public JsonTypes.Any getNonNull(String key) throws IOException {
            JsonTypes.Any any = get(key);
            if(any == null) {
                throw new IOException(String.format("Json Key Error: object has no value for %s", key));
            }
            return any;
        }
    }

    public static class Array extends ArrayList<JsonTypes.Any> {
    }
}
