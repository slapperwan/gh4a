package com.gh4a.utils;

import android.util.Log;

import com.gh4a.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FileUtils {
    private static final List<String> IMAGE_EXTS = Arrays.asList(
        "png", "gif", "jpeg", "jpg", "bmp", "ico"
    );

    private static final List<String> MARKDOWN_EXTS = Arrays.asList(
        "markdown", "md", "mdown", "mkdn", "mkd"
    );

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
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
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
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
            }
        }
        return false;
    }

    public static String getFileExtension(String filename) {
        int mid = filename.lastIndexOf(".");
        if (mid == -1) {
            return "";
        }

        return filename.substring(mid + 1, filename.length());
    }

    public static String getFileName(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        int mid = path.lastIndexOf("/");
        if (mid == -1) {
            return path;
        }
        return path.substring(mid + 1, path.length());
    }

    public static boolean isImage(String filename) {
        return isExtensionIn(filename, IMAGE_EXTS);
    }

    public static boolean isMarkdown(String filename) {
        return isExtensionIn(filename, MARKDOWN_EXTS);
    }

    private static boolean isExtensionIn(String filename, List<String> extensions) {
        String extension = filename == null ? null : getFileExtension(filename);
        if (StringUtils.isBlank(extension)) {
            return false;
        }
        return extensions.contains(extension.toLowerCase(Locale.US));
    }
}
