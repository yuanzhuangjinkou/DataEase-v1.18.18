package io.dataease.plugins.common.util;

import io.dataease.plugins.common.exception.DataEaseException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil {

    public static byte[] readBytes(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            DataEaseException.throwException("文件不存在");
        }
        byte[] bytes = null;
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            bytes = bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static void writeBytes(File file, byte[] bytes) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    public static String getSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }


    public static boolean exist(String path) {
        return new File(path).exists();
    }

    public static List<String> listFileNames(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) return null;
        File[] files = file.listFiles();
        assert files != null;
        return Arrays.stream(files).map(File::getName).collect(Collectors.toList());
    }

    public static String getPrefix(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    public static void del(File file) {
        if (!file.exists()) return;
        file.delete();
    }

    public static void del(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    public static boolean isFile(File file) {
        return file.isFile();
    }

    public static void move(File file, File target, boolean replace) {
        if (!file.exists()) return;
        try {
            String parentPath = target.getParent();
            File parentDir = new File(parentPath);
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
