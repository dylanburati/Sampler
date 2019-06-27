package libre.sampler.io;

import android.util.JsonReader;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import libre.sampler.models.Instrument;
import libre.sampler.models.Sample;

public class InstrumentDeserializer implements Closeable {
    private Instrument toCreate;
    private Map<String, String> entriesToFilenames;
    private ZipFile zipFile;
    private JsonReader jsonReader;

    public InstrumentDeserializer(Instrument toCreate) {
        this.toCreate = toCreate;
        this.entriesToFilenames = new HashMap<>();
    }

    private void readSample(JsonReader reader) throws IOException {
        Sample sample = new Sample("", 0);
        reader.beginObject();
        while(reader.hasNext()) {
            String nextName = reader.nextName();
            if(nextName.equals("filename")) {
                String entryName = reader.nextString();
                sample.filename = Objects.requireNonNull(entriesToFilenames.get(entryName));
            } else if(nextName.equals("volume")) {
                sample.setVolume((float) reader.nextDouble());
            } else if(nextName.equals("minPitch")) {
                sample.minPitch = reader.nextInt();
            } else if(nextName.equals("maxPitch")) {
                sample.maxPitch = reader.nextInt();
            } else if(nextName.equals("minVelocity")) {
                sample.minVelocity = reader.nextInt();
            } else if(nextName.equals("maxVelocity")) {
                sample.maxVelocity = reader.nextInt();
            } else if(nextName.equals("attack")) {
                sample.attack = (float) reader.nextDouble();
            } else if(nextName.equals("decay")) {
                sample.decay = (float) reader.nextDouble();
            } else if(nextName.equals("sustain")) {
                sample.sustain = (float) reader.nextDouble();
            } else if(nextName.equals("release")) {
                sample.release = (float) reader.nextDouble();
            } else if(nextName.equals("basePitch")) {
                sample.basePitch = reader.nextInt();
            } else if(nextName.equals("startTime")) {
                sample.startTime = (float) reader.nextDouble();
            } else if(nextName.equals("resumeTime")) {
                sample.resumeTime = (float) reader.nextDouble();
            } else if(nextName.equals("endTime")) {
                sample.endTime = (float) reader.nextDouble();
            } else if(nextName.equals("shouldUseDefaultLoopStart")) {
                sample.shouldUseDefaultLoopStart = reader.nextBoolean();
            } else if(nextName.equals("shouldUseDefaultLoopResume")) {
                sample.shouldUseDefaultLoopResume = reader.nextBoolean();
            } else if(nextName.equals("shouldUseDefaultLoopEnd")) {
                sample.shouldUseDefaultLoopEnd = reader.nextBoolean();
            } else if(nextName.equals("displayFlags")) {
                sample.displayFlags = reader.nextInt();
            }
        }
        reader.endObject();

        toCreate.addSample(sample);
    }

    private void readJson(JsonReader reader) throws IOException {
        reader.beginObject();
        while(reader.hasNext()) {
            String nextName = reader.nextName();
            if(nextName.equals("name")) {
                // toCreate.name = reader.nextString();
            } else if(nextName.equals("volume")) {
                toCreate.setVolume((float) reader.nextDouble());
            } else if(nextName.equals("samples")) {
                reader.beginArray();
                while(reader.hasNext()) {
                    readSample(reader);
                }
                reader.endArray();
            }
        }
        reader.endObject();
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[32 * 1024];
        int readCount;
        while((readCount = in.read(buffer)) != -1) {
            out.write(buffer, 0, readCount);
        }
    }

    public void read(File inFile, String extractPath) throws IOException {
        File extractDirectory = new File(extractPath);
        read(inFile, extractDirectory);
    }

    public void read(File inFile, File extractDirectory) throws IOException {
        extractDirectory.mkdir();
        if(!extractDirectory.isDirectory()) {
            throw new IOException(String.format("Extract directory does not exist (%s)",
                    extractDirectory.getAbsolutePath()));
        }
        this.zipFile = new ZipFile(inFile);

        ZipEntry jsonEntry = null;
        File[] extractPathFiles = extractDirectory.listFiles();
        List<String> extractPathFilenames = new ArrayList<>(extractPathFiles.length + zipFile.size());
        for(File f : extractPathFiles) {
            extractPathFilenames.add(f.getName());
        }

        for(Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); /* */) {
            ZipEntry entry = entries.nextElement();
            if("instrument.json".equals(entry.getName())) {
                jsonEntry = entry;
            } else {
                String basename = entry.getName();
                int i = 1;
                String filename = basename;
                while(extractPathFilenames.contains(filename)) {
                    filename = "" + i + basename;
                    i++;
                }
                extractPathFilenames.add(filename);

                File extractFile = new File(extractDirectory, filename);
                extractFile.createNewFile();
                entriesToFilenames.put(entry.getName(), extractFile.getAbsolutePath());
                try(OutputStream outputStream = new FileOutputStream(extractFile)) {
                    copy(zipFile.getInputStream(entry), outputStream);
                }
            }
        }

        InputStream jsonInputStream = this.zipFile.getInputStream(jsonEntry);
        this.jsonReader = new JsonReader(new InputStreamReader(jsonInputStream, StandardCharsets.UTF_8));
        readJson(this.jsonReader);
    }

    @Override
    public void close() throws IOException {
        if(this.jsonReader != null) {
            this.jsonReader.close();
        }
        if(this.zipFile != null) {
            this.zipFile.close();
        }
    }
}
