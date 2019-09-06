package libre.sampler.models;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import androidx.annotation.NonNull;

public class FileSelectResult {
    private final int STRING = 1;
    private final int FILE = 2;
    private final int URI = 3;

    private String stringValue = "";
    private File fileValue;
    private Uri uriValue;
    private int type = STRING;

    public boolean isEmpty() {
        if(type == STRING) {
            return stringValue.isEmpty();
        }
        return (uriValue == null);
    }

    public boolean canRead() {
        if(type == STRING) {
            fileValue = new File(stringValue);
            type = FILE;
        }
        if(type == FILE) {
            return fileValue.isFile() && fileValue.canRead();
        }
        return true;
    }

    public InputStream openInputStream(Context ctx) throws FileNotFoundException {
        if(type == STRING) {
            fileValue = new File(stringValue);
            type = FILE;
        }

        if(type == FILE) {
            return new FileInputStream(fileValue);
        } else if(type == URI) {
            return ctx.getContentResolver().openInputStream(uriValue);
        } else {
            throw new FileNotFoundException("Could not open input stream");
        }
    }

    public void setStringValue(String stringValue) {
        this.type = STRING;
        this.stringValue = stringValue;
    }

    public void setFileValue(File fileValue) {
        this.type = FILE;
        this.fileValue = fileValue;
    }

    public void setUriValue(Uri uriValue) {
        this.type = URI;
        this.uriValue = uriValue;
    }

    @NonNull
    @Override
    public String toString() {
        if(type == STRING) {
            return stringValue;
        } else if(type == FILE) {
            return fileValue.getAbsolutePath();
        } else if(type == URI) {
            return uriValue.toString();
        }
        throw new RuntimeException("Invalid file select result type");
    }
}
