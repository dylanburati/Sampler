package libre.sampler.databases;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import androidx.room.TypeConverter;
import libre.sampler.utils.AppConstants;

public class ProjectSettingsConverter {
    @TypeConverter
    public static String serializeSettings(Map<String, Object> map) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.beginObject();

            Object defaultSamplePath = map.get(AppConstants.PREF_DEFAULT_SAMPLE_PATH);
            if(defaultSamplePath instanceof String) {
                writer.name(AppConstants.PREF_DEFAULT_SAMPLE_PATH).value((String) defaultSamplePath);
            }

            Object defaultExportPath = map.get(AppConstants.PREF_DEFAULT_INSTRUMENT_EXPORT_PATH);
            if(defaultExportPath instanceof String) {
                writer.name(AppConstants.PREF_DEFAULT_INSTRUMENT_EXPORT_PATH).value((String) defaultExportPath);
            }

            writer.endObject();
            writer.close();
            return out.toString("UTF-8");
        } catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @TypeConverter
    public static Map<String, Object> deserializeSettings(String str) {
        Map<String, Object> map = new HashMap<>();
        try {
            JsonReader reader = new JsonReader(new StringReader(str));
            reader.beginObject();

            while(reader.hasNext()) {
                String nextName = reader.nextName();
                if(nextName.equals(AppConstants.PREF_DEFAULT_SAMPLE_PATH)) {
                    map.put(AppConstants.PREF_DEFAULT_SAMPLE_PATH, reader.nextString());
                } else if(nextName.equals(AppConstants.PREF_DEFAULT_INSTRUMENT_EXPORT_PATH)) {
                    map.put(AppConstants.PREF_DEFAULT_INSTRUMENT_EXPORT_PATH, reader.nextString());
                } else {
                    reader.skipValue();
                }
            }

            reader.endObject();
            reader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return map;
    }
}
