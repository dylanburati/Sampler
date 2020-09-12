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
import libre.sampler.utils.JsonParser;
import libre.sampler.utils.JsonTypes;
import libre.sampler.utils.MD5OutputStream;
import libre.sampler.utils.ProgressFraction;

public class InstrumentDeserializer {
    private Instrument toCreate;
    private Map<String, String> entriesToFilenames;
    private ZipInputStream zipFile;
    private JsonReader jsonReader;
    private final ProgressFraction progress;

    public InstrumentDeserializer(Instrument toCreate) {
        this(toCreate, null);
    }

    public InstrumentDeserializer(Instrument toCreate, ProgressFraction progress) {
        this.toCreate = toCreate;
        this.progress = progress;
        this.entriesToFilenames = new HashMap<>();
    }

    private Sample readSample(JsonTypes.Object jsonSample) throws IOException {
        String entryName = jsonSample.getNonNull("filename").getValueString();
        Sample sample = new Sample(Objects.requireNonNull(entriesToFilenames.get(entryName)));
        sample.setVolume(jsonSample.getNonNull("volume").getValueFloat());
        sample.setMinPitch(jsonSample.getNonNull("minPitch").getValueInt());
        sample.setMaxPitch(jsonSample.getNonNull("maxPitch").getValueInt());
        sample.setMinVelocity(jsonSample.getNonNull("minVelocity").getValueInt());
        sample.setMaxVelocity(jsonSample.getNonNull("maxVelocity").getValueInt());
        sample.setAttack(jsonSample.getNonNull("attack").getValueFloat());
        sample.setDecay(jsonSample.getNonNull("decay").getValueFloat());
        sample.setSustain(jsonSample.getNonNull("sustain").getValueFloat());
        sample.setRelease(jsonSample.getNonNull("release").getValueFloat());
        sample.setBasePitch(jsonSample.getNonNull("basePitch").getValueFloat());
        sample.setStartTime(jsonSample.getNonNull("startTime").getValueFloat());
        sample.setResumeTime(jsonSample.getNonNull("resumeTime").getValueFloat());
        sample.setEndTime(jsonSample.getNonNull("endTime").getValueFloat());
        sample.displayFlags = jsonSample.getNonNull("displayFlags").getValueInt();

        return sample;
    }

    private void readJson(JsonReader reader) throws IOException {
        JsonTypes.Object jsonInstrument = JsonParser.parseObject(reader);
        toCreate.setVolume(jsonInstrument.getNonNull("volume").getValueFloat());
        JsonTypes.Array jsonSampleArr = jsonInstrument.getNonNull("samples").getValueArray();
        for(JsonTypes.Any jsonSample : jsonSampleArr) {
            Sample s = readSample(jsonSample.getValueObject());
            toCreate.addSample(s);
        }
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

        if(this.progress != null) {
            this.progress.setProgressTotal(1);
        }
        float progressComplement = 1;
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
                                if(this.progress != null) {
                                    progressComplement *= 0.75;
                                    this.progress.setProgressCurrent(1 - progressComplement);
                                }
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
            if(this.progress != null) {
                this.progress.setProgressCurrent(1);
            }
        } finally {
            if(zipFile != null) zipFile.close();
            if(jsonInputStream != null) jsonInputStream.close();
            if(jsonReader != null) jsonReader.close();
        }
    }
}
