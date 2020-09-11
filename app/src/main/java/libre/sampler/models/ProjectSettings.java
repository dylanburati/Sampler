package libre.sampler.models;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import androidx.room.TypeConverter;

public class ProjectSettings {
    TouchVelocitySource touchVelocitySource = TouchVelocitySource.NONE;
    String defaultSamplePath;
    String defaultInstrumentExportPath;

    @Override
    public int hashCode() {
        int hashCode = touchVelocitySource.hashCode();
        if (defaultSamplePath != null) hashCode ^= defaultSamplePath.hashCode();
        if (defaultInstrumentExportPath != null) hashCode ^= defaultInstrumentExportPath.hashCode();
        return hashCode;
    }

    public static class JsonConverter {
        private static final String jsonKey_touchVelocitySource = "touch_velocity_source";
        private static final String jsonKey_defaultSamplePath = "default_sample_path";
        private static final String jsonKey_defaultInstrumentExportPath = "default_instrument_export_path";

        @TypeConverter
        public static String toJson(ProjectSettings settings) {
            StringWriter out = new StringWriter();
            try(JsonWriter writer = new JsonWriter(out)) {
                writer.beginObject();
                writer.name(jsonKey_touchVelocitySource).value(settings.touchVelocitySource.toString());
                if (settings.defaultSamplePath != null) {
                    writer.name(jsonKey_defaultSamplePath).value(settings.defaultSamplePath);
                }
                if (settings.defaultInstrumentExportPath != null) {
                    writer.name(jsonKey_defaultInstrumentExportPath).value(settings.defaultInstrumentExportPath);
                }
                writer.endObject();
            } catch(IOException e) {
                e.printStackTrace();
                return null;
            }
            return out.toString();
        }

        @TypeConverter
        public static ProjectSettings fromJson(String json) {
            ProjectSettings settings = new ProjectSettings();
            try(JsonReader reader = new JsonReader(new StringReader(json))) {
                reader.beginObject();

                while(reader.hasNext()) {
                    String nextName = reader.nextName();
                    if (reader.peek() == JsonToken.NULL) {
                        reader.skipValue();
                    } else {
                        switch(nextName) {
                            case jsonKey_touchVelocitySource:
                                settings.touchVelocitySource = TouchVelocitySource.valueOf(reader.nextString());
                                break;
                            case jsonKey_defaultSamplePath:
                                settings.defaultSamplePath = reader.nextString();
                                break;
                            case jsonKey_defaultInstrumentExportPath:
                                settings.defaultInstrumentExportPath = reader.nextString();
                                break;
                            default:
                                reader.skipValue();
                                break;
                        }
                    }
                }

                reader.endObject();
            } catch(IOException e) {
                e.printStackTrace();
            }
            return settings;
        }
    }
}
