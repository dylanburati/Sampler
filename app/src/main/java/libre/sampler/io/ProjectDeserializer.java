package libre.sampler.io;

import android.util.JsonReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.utils.JsonParser;
import libre.sampler.utils.JsonTypes;
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

    private void readJson(JsonReader reader) throws IOException {
        JsonTypes.Object jsonProject = JsonParser.parseObject(reader);
        JsonTypes.Array jsonInstrumentArr = jsonProject.getNonNull("instruments").getValueArray();
        if(requireProjectIdCallback != null) {
            requireProjectIdCallback.run();
        }

        progress.setProgressTotal(jsonInstrumentArr.size());
        int index = 0;
        JsonTypes.Object jsonInstrument;
        for(JsonTypes.Any aJsonInstrument : jsonInstrumentArr) {
            jsonInstrument = aJsonInstrument.getValueObject();
            Instrument instrument = new Instrument(jsonInstrument.getNonNull("name").getValueString());
            toCreate.registerInstrument(instrument);

            URL download = new URL(jsonInstrument.getNonNull("download").getValueString());
            final int indexCopy = index;
            InstrumentDeserializer deserializer = new InstrumentDeserializer(instrument, new ProgressFraction() {
                float total;
                @Override
                public void setProgressTotal(float total) {
                    this.total = total;
                }

                @Override
                public void setProgressCurrent(float current) {
                    progress.setProgressCurrent(indexCopy + (current / total));
                }
            });
            deserializer.read(download.openConnection().getInputStream(), extractDirectory);
            toCreate.addInstrument(instrument);
            index++;
        }
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
