package com.gh4a.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.util.Log;

import com.gh4a.Constants;

public class FileUtils {

    static List<String> imageExts = new ArrayList<String>();
    
    static {
        imageExts.add("png");
        imageExts.add("gif");
        imageExts.add("jpeg");
        imageExts.add("jpg");
        imageExts.add("bmp");
    }

    public static boolean save(File file, InputStream inputStream) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            int read = 0;
            byte[] bytes = new byte[1024];
         
            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            return true;
        } catch (FileNotFoundException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return false;
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return false;
        }
        finally {
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
                return false;
            }
        }
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
        if (StringUtils.isBlank(filename)) {
            return false;
        }
        String ext = getFileExtension(filename);
        if (StringUtils.isBlank(ext)) {
            return false;
        }

        return imageExts.contains(ext.toLowerCase(Locale.US));
    }
}
