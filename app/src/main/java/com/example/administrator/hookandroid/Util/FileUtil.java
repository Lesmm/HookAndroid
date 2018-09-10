package com.example.administrator.hookandroid.Util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class FileUtil {

    public static String readFileToText(String fileName) {
        String encoding = "UTF-8";
        File file = new File(fileName);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(filecontent, encoding);
        } catch (UnsupportedEncodingException e) {
            System.err.println("The OS does not support " + encoding);
            e.printStackTrace();
            return null;
        }
    }

    public static boolean writeTextToFile(String strBuffer, String strFilename) {
        FileWriter fileWriter = null;
        try {
            File fileText = new File(strFilename);
            File fileParent = fileText.getParentFile();
            if (!fileParent.exists()) {
                fileParent.mkdirs();
            }
            fileWriter = new FileWriter(fileText, false);
            fileWriter.write(strBuffer);
            fileWriter.flush();
            fileWriter.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return false;
    }

    public static boolean appendTextToFile(String strBuffer, String strFilename) {
        FileWriter fileWriter = null;
        try {
            File fileText = new File(strFilename);
            File fileParent = fileText.getParentFile();
            if (!fileParent.exists()) {
                fileParent.mkdirs();
            }
            fileWriter = new FileWriter(fileText, true);
            fileWriter.write(strBuffer);
            fileWriter.flush();
            fileWriter.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return false;
    }

    public static void devideFile(File f) {
        try {
            int sizeOfFiles = (int) (f.length() / 1.8);
            byte[] buffer = new byte[sizeOfFiles];
            String fileName = f.getName();

            // try-with-resources to ensure closing stream
            int i = 0;
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));

            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                i++;
                // write each chunk of data into separate file with different number in name
                if (i == 2) {
                    File newFile = new File(f.getParent(), fileName);
                    FileOutputStream out = new FileOutputStream(newFile);
                    out.write(buffer, 0, bytesAmount);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
