package libre.sampler.io;

import android.util.JsonWriter;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import libre.sampler.models.Instrument;
import libre.sampler.models.Sample;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.ProgressFraction;

public class InstrumentSerializer implements Closeable {
    private Instrument instrument;
    private Map<String, String> filenamesToEntries;
    private ZipOutputStream zipOutputStream;
    private JsonWriter jsonWriter;
    private final ProgressFraction progress;

    public InstrumentSerializer(Instrument instrument, ProgressFraction progress) {
        this.instrument = instrument;
        this.progress = progress;
        this.filenamesToEntries = new HashMap<>(instrument.getSamples().size());
    }

    private void writeSample(JsonWriter writer, Sample sample) throws IOException {
        String baseName = new File(sample.filename).getName();
        int i = 1;
        String entryName = baseName;
        while(filenamesToEntries.containsValue(entryName)) {
            entryName = "" + i + baseName;
            i++;
        }
        filenamesToEntries.put(sample.filename, entryName);

        writer.beginObject();
        writer.name("filename").value(entryName);
        writer.name("volume").value(sample.getVolume());
        writer.name("minPitch").value(sample.minPitch);
        writer.name("maxPitch").value(sample.maxPitch);
        writer.name("minVelocity").value(sample.minVelocity);
        writer.name("maxVelocity").value(sample.maxVelocity);
        writer.name("attack").value(sample.attack);
        writer.name("decay").value(sample.decay);
        writer.name("sustain").value(sample.sustain);
        writer.name("release").value(sample.release);
        writer.name("basePitch").value(sample.basePitch);
        writer.name("startTime").value(sample.startTime);
        writer.name("resumeTime").value(sample.resumeTime);
        writer.name("endTime").value(sample.endTime);
        writer.name("shouldUseDefaultLoopStart").value(sample.shouldUseDefaultLoopStart);
        writer.name("shouldUseDefaultLoopResume").value(sample.shouldUseDefaultLoopResume);
        writer.name("shouldUseDefaultLoopEnd").value(sample.shouldUseDefaultLoopEnd);
        writer.name("displayFlags").value(sample.displayFlags);
        writer.endObject();
    }

    private void writeJson(JsonWriter writer) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry("instrument.json"));
        writer.setIndent("");
        writer.beginObject();
        writer.name("name").value(instrument.name);
        writer.name("volume").value(instrument.getVolume());
        writer.name("samples");
        writer.beginArray();
        for(Sample s : this.instrument.getSamples()) {
            writeSample(writer, s);
        }
        writer.endArray();
        writer.endObject();
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[32 * 1024];
        int readCount;
        while((readCount = in.read(buffer)) != -1) {
            out.write(buffer, 0, readCount);
        }
    }

    public void write(File outFile, boolean overwrite) throws IOException {
        if(!outFile.createNewFile() && !overwrite) {
            throw new IOException(AppConstants.ERROR_EXPORT_ZIP_EXISTS);
        }
        this.zipOutputStream = new ZipOutputStream(new FileOutputStream(outFile, false));
        this.jsonWriter = new JsonWriter(new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8));

        writeJson(this.jsonWriter);
        jsonWriter.flush();

        int i = 0;
        progress.setProgressTotal(filenamesToEntries.size());
        for(Map.Entry<String, String> kv : filenamesToEntries.entrySet()) {
            zipOutputStream.putNextEntry(new ZipEntry(kv.getValue()));
            try(InputStream inputStream = new FileInputStream(new File(kv.getKey()))) {
                copy(inputStream, zipOutputStream);
            }
            progress.setProgressCurrent(++i);
        }
    }

    @Override
    public void close() throws IOException {
        if(this.jsonWriter != null) {
            this.jsonWriter.close();
        } else if(this.zipOutputStream != null) {
            this.zipOutputStream.close();
        }
    }
}
