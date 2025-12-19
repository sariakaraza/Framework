package util;

import java.io.IOException;
import jakarta.servlet.http.Part;

public class UploadedFile {
    public final String filename;
    public final String contentType;
    public final byte[] bytes;

    public UploadedFile(String filename, String contentType, byte[] bytes) {
        this.filename = filename;
        this.contentType = contentType;
        this.bytes = bytes;
    }

    public long size() {
        return bytes == null ? 0L : bytes.length;
    }

    public static UploadedFile fromPart(Part p) throws IOException {
        if (p == null) return null;
        String fn = p.getSubmittedFileName();
        String ct = p.getContentType();
        byte[] b = p.getInputStream().readAllBytes();
        return new UploadedFile(fn, ct, b);
    }
}