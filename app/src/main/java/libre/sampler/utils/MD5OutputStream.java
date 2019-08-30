package libre.sampler.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5OutputStream extends DigestOutputStream {
    public MD5OutputStream() throws NoSuchAlgorithmException {
        super(new NoOpOutputStream(), MessageDigest.getInstance("MD5"));
    }

    public void writeInt(int i) throws IOException {
        write(i >> 24);
        write((i >> 16) & 0xFF);
        write((i >> 8) & 0xFF);
        write(i & 0xFF);
    }

    private static class NoOpOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            // Prevent allocating extra memory for checksum calc.
        }
    }
}
