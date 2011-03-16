package com.gh4a.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.gh4a.Constants;

import android.os.Environment;
import android.util.Log;

public class FileUtils {

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
}
