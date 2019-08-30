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
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
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
                sample.setMinPitch(reader.nextInt());
            } else if(nextName.equals("maxPitch")) {
                sample.setMaxPitch(reader.nextInt());
            } else if(nextName.equals("minVelocity")) {
                sample.setMinVelocity(reader.nextInt());
            } else if(nextName.equals("maxVelocity")) {
                sample.setMaxVelocity(reader.nextInt());
            } else if(nextName.equals("attack")) {
                sample.setAttack((float) reader.nextDouble());
            } else if(nextName.equals("decay")) {
                sample.setDecay((float) reader.nextDouble());
            } else if(nextName.equals("sustain")) {
                sample.setSustain((float) reader.nextDouble());
            } else if(nextName.equals("release")) {
                sample.setRelease((float) reader.nextDouble());
            } else if(nextName.equals("basePitch")) {
                sample.setBasePitch(reader.nextInt());
            } else if(nextName.equals("startTime")) {
                sample.setStartTime((float) reader.nextDouble());
            } else if(nextName.equals("resumeTime")) {
                sample.setResumeTime((float) reader.nextDouble());
            } else if(nextName.equals("endTime")) {
                sample.setResumeTime((float) reader.nextDouble());
            } else if(nextName.equals("displayFlags")) {
                sample.displayFlags = reader.nextInt();
            } else {
                reader.skipValue();
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
                /*discard*/ reader.nextString();
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

    private final Random random = new Random();
    private static String bytesToHexString(byte[] array, int stringLength) {
        StringBuilder builder = new StringBuilder();
        int end = (stringLength + 1) / 2;
        for(int i = 0; i < end; i++) {
            builder.append(String.format("%02x", array[i] & 0xFF));
        }
        builder.setLength(stringLength);
        return builder.toString();
    }

    private String checksum(InputStream in) throws IOException {
        try(DigestOutputStream outputStream = new DigestOutputStream(new NoOpOutputStream(), MessageDigest.getInstance("MD5"))) {
            copy(in, outputStream);
            outputStream.flush();
            byte[] digest = outputStream.getMessageDigest().digest();
            return "--" + bytesToHexString(digest, 6);
        } catch(NoSuchAlgorithmException e) {
            byte[] rand = new byte[6];
            random.nextBytes(rand);
            return "--" + bytesToHexString(rand, 6);
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

        for(Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); /* */) {
            ZipEntry entry = entries.nextElement();
            if("instrument.json".equals(entry.getName())) {
                jsonEntry = entry;
            } else {
                StringBuilder filenameBuilder = new StringBuilder(entry.getName());
                int extensionIdx = filenameBuilder.lastIndexOf(".");
                if(extensionIdx < 0) {
                    extensionIdx = 0;
                }
                filenameBuilder.insert(extensionIdx, checksum(zipFile.getInputStream(entry)));
                String filename = filenameBuilder.toString();

                File extractFile = new File(extractDirectory, filename);
                entriesToFilenames.put(entry.getName(), extractFile.getAbsolutePath());
                if(extractFile.createNewFile()) {
                    try(OutputStream outputStream = new FileOutputStream(extractFile, false)) {
                        copy(zipFile.getInputStream(entry), outputStream);
                    }
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

    private static class NoOpOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            // Prevent allocating extra memory for checksum calc.
        }
    }
}
