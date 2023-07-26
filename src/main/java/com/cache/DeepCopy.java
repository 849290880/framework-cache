package com.cache;

import java.io.*;

public class DeepCopy {
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T copy(T obj) {
        T cloneObj = null;
        try {
            // 写入字节流
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(obj);

            // 分配内存，写入原始对象，生成新对象
            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
            ObjectInputStream in =new ObjectInputStream(byteIn);
            // 返回生成的新对象
            cloneObj = (T) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return cloneObj;
    }


    public static Object copy(Object obj) {
        Object cloneObj = null;
        try {
            // 写入字节流
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(obj);

            // 分配内存，写入原始对象，生成新对象
            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
            ObjectInputStream in =new ObjectInputStream(byteIn);
            // 返回生成的新对象
            cloneObj = in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return cloneObj;
    }
}