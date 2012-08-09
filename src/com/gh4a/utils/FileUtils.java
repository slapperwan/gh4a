package com.gh4a.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
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
    
    public static boolean save(String filename, String data) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            BufferedWriter out = null;
            FileWriter fstream = null;
            try {
                File rootDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/download");
                if (!rootDir.exists()) {
                    rootDir.mkdirs();
                }
                
                filename = filename.replaceAll("\\s", "_");
                File file = new File(rootDir, filename);
                
                fstream = new FileWriter(file);
                
                out = new BufferedWriter(fstream);
                out.write(data);
                
                return true;
            }
            catch (IOException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
                return false;
            }
            finally {
                if (out != null) {
                    try {
                        out.close();
                    }
                    catch (IOException e) {
                        Log.e(Constants.LOG_TAG, e.getMessage(), e);
                        return false;
                    }
                }
                if (fstream != null) {
                    try {
                        fstream.close();
                    }
                    catch (IOException e) {
                        Log.e(Constants.LOG_TAG, e.getMessage(), e);
                        return false;
                    }
                }
            }
        }
        else {
            return false;
        }
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
                inputStream.close();
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
        if (mid != -1) {
            return filename.substring(mid + 1, filename.length());
        }
        else {
            return "";
        }
    }
    
    public static String getFileName(String path) {
        if (!StringUtils.isBlank(path)) {
            int mid = path.lastIndexOf("/");
            if (mid != -1) {
                return path.substring(mid + 1, path.length());
            }
            else {
                return path;
            }
        }
        return "";
    }
    
    public static boolean isImage(String filename) {
        if (!StringUtils.isBlank(filename)) {
            String ext = getFileExtension(filename);
            
            if (!StringUtils.isBlank(ext)) {
                if (imageExts.contains(ext.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
}
