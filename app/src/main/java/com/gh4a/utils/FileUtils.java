package com.gh4a.utils;

import android.util.Log;
import android.webkit.MimeTypeMap;

import com.gh4a.Gh4Application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class FileUtils {
    private static final List<String> MARKDOWN_EXTS = Arrays.asList(
        "markdown", "md", "mdown", "mkdn", "mkd"
    );

    private static final HashMap<String, String> MIME_TYPE_OVERRIDES = new HashMap<>();
    static {
        // .ts can be both a TypeScript file and a MPEG2 transport stream file. As the former is the
        // more likely case for us and the framework returns the latter, override to assume a text file.
        MIME_TYPE_OVERRIDES.put("ts", "text/x-typescript");
        // there is no general MIME type mapping for Java properties files, but they're text
        MIME_TYPE_OVERRIDES.put("properties", "text/x-java-properties");
        // JavaScript can be resolved to both text/javascript and application/javascript,
        // for our purposes it's text in any case
        MIME_TYPE_OVERRIDES.put("js", "text/javascript");
        // Same for Ruby, LaTeX, SQL, JSON
        MIME_TYPE_OVERRIDES.put("rb", "text/x-ruby");
        MIME_TYPE_OVERRIDES.put("latex", "text/x-latex");
        MIME_TYPE_OVERRIDES.put("sql", "text/x-sql");
        MIME_TYPE_OVERRIDES.put("json", "text/x-json");
        // Also treat batch files as plain text (we want to open them in our text viewer,
        // and they can't be run within Android anyway)
        MIME_TYPE_OVERRIDES.put("bat", "text/plain");
    }

    public static boolean save(File file, InputStream inputStream) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            int read;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            return true;
        } catch (IOException e) {
            Log.e(Gh4Application.LOG_TAG, e.getMessage(), e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (IOException e) {
                Log.e(Gh4Application.LOG_TAG, e.getMessage(), e);
            }
        }
        return false;
    }

    public static String getFileExtension(String filename) {
        int mid = filename.lastIndexOf(".");
        if (mid == -1) {
            return "";
        }

        return filename.substring(mid + 1);
    }

    public static String getFileName(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        int mid = path.lastIndexOf("/");
        if (mid == -1) {
            return path;
        }
        return path.substring(mid + 1);
    }

    public static boolean isImage(String filename) {
        String mime = getMimeTypeFor(filename);
        return mime != null && mime.startsWith("image/");
    }

    public static boolean isBinaryFormat(String filename) {
        String mime = getMimeTypeFor(filename);
        return mime != null && !mime.startsWith("text/")
                // cover cases like application/xhtml+xml or image/svg+xml
                && !mime.endsWith("xml");
    }

    public static boolean isMarkdown(String filename) {
        return isExtensionIn(filename, MARKDOWN_EXTS);
    }

    public static String getMimeTypeFor(String filename) {
        String extension = filename == null ? null : getFileExtension(filename);
        if (StringUtils.isBlank(extension)) {
            return null;
        }
        if (MIME_TYPE_OVERRIDES.containsKey(extension)) {
            return MIME_TYPE_OVERRIDES.get(extension);
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    private static boolean isExtensionIn(String filename, List<String> extensions) {
        String extension = filename == null ? null : getFileExtension(filename);
        if (StringUtils.isBlank(extension)) {
            return false;
        }
        return extensions.contains(extension.toLowerCase(Locale.US));
    }
}
