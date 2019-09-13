package libre.sampler.io;

import android.util.JsonReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import libre.sampler.models.Instrument;
import libre.sampler.models.Sample;
import libre.sampler.utils.MD5OutputStream;

public class InstrumentDeserializer {
    private Instrument toCreate;
    private Map<String, String> entriesToFilenames;
    private ZipInputStream zipFile;
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
                sample.setBasePitch((float) reader.nextDouble());
            } else if(nextName.equals("startTime")) {
                sample.setStartTime((float) reader.nextDouble());
            } else if(nextName.equals("resumeTime")) {
                sample.setResumeTime((float) reader.nextDouble());
            } else if(nextName.equals("endTime")) {
                sample.setEndTime((float) reader.nextDouble());
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
        int end = Math.min(array.length, (stringLength + 1) / 2);
        for(int i = 0; i < end; i++) {
            builder.append(String.format("%02x", array[i] & 0xFF));
        }
        builder.setLength(stringLength);
        return builder.toString();
    }

    private String checksum(ByteArrayOutputStream in) throws IOException {
        try(DigestOutputStream outputStream = new MD5OutputStream()) {
            in.writeTo(outputStream);
            outputStream.flush();
            byte[] digest = outputStream.getMessageDigest().digest();
            return "--" + bytesToHexString(digest, 6);
        } catch(NoSuchAlgorithmException e) {
            byte[] rand = new byte[6];
            random.nextBytes(rand);
            return "--" + bytesToHexString(rand, 6);
        }
    }

    public void read(InputStream inputStream, File extractDirectory) throws IOException {
        extractDirectory.mkdir();
        if(!extractDirectory.isDirectory()) {
            throw new IOException(String.format("Extract directory does not exist (%s)",
                    extractDirectory.getAbsolutePath()));
        }

        this.zipFile = new ZipInputStream(inputStream);
        ByteArrayInputStream jsonInputStream = null;
        try {
            ZipEntry entry = null;
            while((entry = zipFile.getNextEntry()) != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                copy(zipFile, byteArrayOutputStream);
                try {
                    if("instrument.json".equals(entry.getName())) {
                        jsonInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                    } else {
                        StringBuilder filenameBuilder = new StringBuilder(entry.getName());
                        int extensionIdx = filenameBuilder.lastIndexOf(".");
                        if(extensionIdx < 0) {
                            extensionIdx = 0;
                        }
                        filenameBuilder.insert(extensionIdx, checksum(byteArrayOutputStream));
                        String filename = filenameBuilder.toString();

                        File extractFile = new File(extractDirectory, filename);
                        entriesToFilenames.put(entry.getName(), extractFile.getAbsolutePath());
                        if(extractFile.createNewFile()) {
                            try(OutputStream outputStream = new FileOutputStream(extractFile, false)) {
                                byteArrayOutputStream.writeTo(outputStream);
                            }
                        }
                    }
                } finally {
                    zipFile.closeEntry();
                    byteArrayOutputStream.close();
                }
            }

            this.jsonReader = new JsonReader(new InputStreamReader(jsonInputStream, StandardCharsets.UTF_8));
            readJson(this.jsonReader);
        } finally {
            if(zipFile != null) zipFile.close();
            if(jsonInputStream != null) jsonInputStream.close();
            if(jsonReader != null) jsonReader.close();
        }
    }
}
