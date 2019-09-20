package libre.sampler.io;

import android.util.JsonReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.utils.ProgressFraction;

public class ProjectDeserializer {
    private Project toCreate;
    private JsonReader jsonReader;
    private File extractDirectory;
    private final ProgressFraction progress;
    private final Runnable requireProjectIdCallback;

    public ProjectDeserializer(Project toCreate, File extractDirectory, ProgressFraction progress,
                               Runnable requireProjectIdCallback) {
        this.toCreate = toCreate;
        this.extractDirectory = extractDirectory;
        this.progress = progress;
        this.requireProjectIdCallback = requireProjectIdCallback;
    }

    private static class JsonInstrument {
        public String name;
        public String download;

        public JsonInstrument(String name, String download) {
            this.name = name;
            this.download = download;
        }
    }
    private JsonInstrument readInstrument(JsonReader reader) throws IOException {
        JsonInstrument instrument = new JsonInstrument("", "");

        reader.beginObject();
        while(reader.hasNext()) {
            String nextName = reader.nextName();
            if(nextName.equals("name")) {
                instrument.name = reader.nextString();
            } else if(nextName.equals("download")) {
                instrument.download = reader.nextString();
            }
        }
        reader.endObject();

        return instrument;
    }

    private void readJson(JsonReader reader) throws IOException {
        reader.beginObject();
        while(reader.hasNext()) {
            String nextName = reader.nextName();
            if(nextName.equals("name")) {
                /*discard*/ reader.nextString();
            } else if(nextName.equals("instruments")) {
                reader.beginArray();

                if(this.requireProjectIdCallback != null) {
                    this.requireProjectIdCallback.run();
                }
                List<JsonInstrument> instrumentsMeta = new ArrayList<>(10);
                while(reader.hasNext()) {
                    instrumentsMeta.add(readInstrument(reader));
                }
                reader.endArray();

                progress.setProgressTotal(instrumentsMeta.size());
                for(int i = 0; i < instrumentsMeta.size(); i++) {
                    JsonInstrument jsonT = instrumentsMeta.get(i);
                    Instrument instrument = new Instrument(jsonT.name);
                    toCreate.registerInstrument(instrument);
                    URL download = new URL(jsonT.download);
                    final int index = i;
                    InstrumentDeserializer deserializer = new InstrumentDeserializer(instrument, new ProgressFraction() {
                        float total;
                        @Override
                        public void setProgressTotal(float total) {
                            this.total = total;
                        }

                        @Override
                        public void setProgressCurrent(float current) {
                            progress.setProgressCurrent(index + (current / total));
                        }
                    });
                    deserializer.read(download.openConnection().getInputStream(), extractDirectory);
                    toCreate.addInstrument(instrument);
                }
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    public void read(InputStream inputStream) throws IOException {
        extractDirectory.mkdir();
        if(!extractDirectory.isDirectory()) {
            throw new IOException(String.format("Extract directory does not exist (%s)",
                    extractDirectory.getAbsolutePath()));
        }

        try {
            this.jsonReader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            readJson(this.jsonReader);
        } finally {
            if(jsonReader != null) jsonReader.close();
        }
    }
}
