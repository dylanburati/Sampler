package libre.sampler;

import android.Manifest;
import android.util.Log;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import libre.sampler.io.InstrumentDeserializer;
import libre.sampler.io.InstrumentSerializer;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.models.Sample;
import libre.sampler.utils.IdStatus;
import libre.sampler.utils.ProgressFraction;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class InstrumentSerializationInstrumentedTest {
    private static String toHexString(byte[] array) {
        StringBuilder builder = new StringBuilder();
        for(byte b : array) {
            builder.append(String.format("%02x", b & 0xFF));
        }
        return builder.toString();
    }

    private static boolean deleteRecursively(File fileOrDir) {
        if(fileOrDir.isDirectory()) {
            return cleanDirectory(fileOrDir) && fileOrDir.delete();
        } else {
            return fileOrDir.delete();
        }
    }

    private static boolean cleanDirectory(File dir) {
        if(!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory");
        }
        File[] contents = dir.listFiles();
        if(contents == null) {
            return false;  // permission denied
        }

        for(File child : contents) {
            if(!deleteRecursively(child)) {
                return false;
            }
        }
        return true;
    }

    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void serialization_correct() {
        // initialize directory for samples and exported instrument
        File cacheDirectory = InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();
        IdStatus.setOutputEnabled(true);
        Project project = new Project(1, "project", 0L);
        File tmpDirectory = new File(cacheDirectory,"tmp");
        if(tmpDirectory.exists()) {
            deleteRecursively(tmpDirectory);
        }
        tmpDirectory.mkdir();

        String[] sampleFilenames = new String[]{"a.wav", "b.wav", "c.aiff"};
        Random random = new Random();
        byte[] buffer = new byte[32 * 1024];
        Map<String, byte[]> checksums = new HashMap<>();

        // write samples to original filenames, record checksums
        for(String filename : sampleFilenames) {
            File sampleFile = new File(tmpDirectory, filename);
            try {
                sampleFile.createNewFile();
                try(DigestOutputStream outputStream = new DigestOutputStream(
                        new FileOutputStream(sampleFile, false), MessageDigest.getInstance("MD5"))) {

                    random.nextBytes(buffer);
                    outputStream.write(buffer);
                    checksums.put(filename, outputStream.getMessageDigest().digest());
                }
            } catch(IOException e) {
                e.printStackTrace();
                fail("Could not create test files");
            } catch(NoSuchAlgorithmException e) {
                e.printStackTrace();
                fail("Could not get MD5 algorithm");
            }
        }

        // Create instrument from scratch
        Instrument instrument1 = new Instrument("name");
        project.registerInstrument(instrument1);
        project.addInstrument(instrument1);
        for(String filename : sampleFilenames) {
            Sample sample = instrument1.addSample(new File(tmpDirectory, filename).getAbsolutePath());
            sample.setMinPitch(random.nextInt(64));
            sample.setMaxPitch(random.nextInt(64) + 64);
            sample.setBasePitch(random.nextInt(64));
            sample.setMinVelocity(random.nextInt(64));
            sample.setMaxVelocity(random.nextInt(64) + 64);
            sample.setVolumeDecibels(random.nextFloat() * -100);
            sample.setAttack(random.nextFloat() * 100);
            sample.setDecay(random.nextFloat() * 100);
            sample.setSustainDecibels(random.nextFloat() * -100);
            sample.setRelease(random.nextFloat() * 100);
        }

        assertEquals("Wrong number of samples inserted", sampleFilenames.length, instrument1.getSamples().size());

        // Export to .zip
        ProgressFraction progressFraction = new ProgressFraction() {
            @Override
            public void setProgressTotal(int total) {
            }

            @Override
            public void setProgressCurrent(int current) {
            }
        };
        try(InstrumentSerializer serializer = new InstrumentSerializer(instrument1, progressFraction)) {
            serializer.write(new File(tmpDirectory, "zip.zip"), false);
        } catch(IOException e) {
            e.printStackTrace();
            fail("Serialization failed");
        }

        // Create instrument with exported properties and sample files
        Instrument instrument2 = new Instrument(null);
        project.registerInstrument(instrument2);
        project.addInstrument(instrument2);
        try {
            InstrumentDeserializer deserializer = new InstrumentDeserializer(instrument2);
            deserializer.read(new FileInputStream(new File(tmpDirectory, "zip.zip")), tmpDirectory);
        } catch(IOException e) {
            e.printStackTrace();
            fail("Deserialization failed");
        }

        // Ensure that all exported fields are identical, and the IDs used for the Room database
        // are in valid states.
        assertNotEquals("Deserialization gave wrong instrument ID - already used", instrument1.id, instrument2.id);
        assertEquals("Wrong number of samples deserialized", sampleFilenames.length, instrument2.getSamples().size());
        assertEquals("Properties of instruments differ", instrument1.getVolume(), instrument2.getVolume(), 1e-7);
        Set<Integer> sample2Ids = new HashSet<>(sampleFilenames.length);
        for(int i = 0; i < sampleFilenames.length; i++) {
            Sample sample1 = instrument1.getSamples().get(i);
            Sample sample2 = instrument2.getSamples().get(i);
            assertEquals("Deserialization gave wrong sample ID - instrument,sample mismatch", instrument2.id, sample2.instrumentId);
            assertTrue("Deserialization gave wrong sample ID - already used", sample2Ids.add(sample2.id));

            assertEquals("Properties of samples differ", sample1.getVolume(), sample2.getVolume(), 1e-7);
            assertEquals("Properties of samples differ", sample1.getMinPitch(), sample2.getMinPitch());
            assertEquals("Properties of samples differ", sample1.getMaxPitch(), sample2.getMaxPitch());
            assertEquals("Properties of samples differ", sample1.getMinVelocity(), sample2.getMinVelocity());
            assertEquals("Properties of samples differ", sample1.getMaxVelocity(), sample2.getMaxVelocity());
            assertEquals("Properties of samples differ", sample1.getAttack(), sample2.getAttack(), 1e-7);
            assertEquals("Properties of samples differ", sample1.getDecay(), sample2.getDecay(), 1e-7);
            assertEquals("Properties of samples differ", sample1.getSustain(), sample2.getSustain(), 1e-7);
            assertEquals("Properties of samples differ", sample1.getRelease(), sample2.getRelease(), 1e-7);
            assertEquals("Properties of samples differ", sample1.getBasePitch(), sample2.getBasePitch());
            assertEquals("Properties of samples differ", sample1.getStartTime(), sample2.getStartTime(), 1e-7);
            assertEquals("Properties of samples differ", sample1.getResumeTime(), sample2.getResumeTime(), 1e-7);
            assertEquals("Properties of samples differ", sample1.getEndTime(), sample2.getEndTime(), 1e-7);
            assertEquals("Properties of samples differ", sample1.displayFlags, sample2.displayFlags);

            // Ensure that the sample file was compressed and extracted correctly
            assertNotEquals("Deserialization overwrote a file from original instrument", sample1.filename, sample2.filename);
            File sample1File = new File(sample1.filename);
            File sample2File = new File(sample2.filename);
            try {
                try(DigestInputStream inputStream = new DigestInputStream(
                        new FileInputStream(sample2File), MessageDigest.getInstance("MD5"))) {

                    int readCount;
                    do {
                        readCount = inputStream.read(buffer);
                    } while(readCount != -1);

                    checksums.put(sample2File.getName(), inputStream.getMessageDigest().digest());
                }
            } catch(IOException e) {
                e.printStackTrace();
                fail("Could not create test files");
            } catch(NoSuchAlgorithmException e) {
                e.printStackTrace();
                fail("Could not get MD5 algorithm");
            }

            assertArrayEquals("Deserialization gave wrong sample content",
                    checksums.get(sample1File.getName()), checksums.get(sample2File.getName()));
        }

        // Cleanup and report
        if(!deleteRecursively(tmpDirectory)) {
            Log.w("InstrumentSerializationTest", tmpDirectory.getAbsolutePath() + " not deleted after test");
        }
        Log.i("InstrumentSerializationTest", String.format("%-16s%s", "Filename", "MD5"));
        for(Map.Entry<String, byte[]> entry : checksums.entrySet()) {
            Log.i("InstrumentSerializationTest", String.format("%-16s%s", entry.getKey(), toHexString(entry.getValue())));
        }
    }
}
