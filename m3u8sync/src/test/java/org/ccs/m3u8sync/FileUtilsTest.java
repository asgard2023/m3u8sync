package org.ccs.m3u8sync;

import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;

class FileUtilsTest {

    @Test
    void listFiles2() throws Exception {
        String path = "D:\\app\\maoyun\\972735523";
        path = "C:\\Windows\\System32";
        Long time = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
//            Thread.sleep(10L);
            File file = new File(path);
            file.list();
//            System.out.println("lastModified="+file.list().length);
        }
        System.out.println("listFiles2=" + (System.currentTimeMillis() - time));
    }

    @Test
    void listFiles3() throws Exception {
        String path = "D:\\app\\maoyun\\972735523";
//        path="C:\\Windows\\System32";
        Long time = System.currentTimeMillis();

        File file = new File(path);
        String[] names = file.list();
        for (String name : names) {
            System.out.println("fileName=" + name);
        }
    }

    @Test
    public void del(){
        String path = "D:\\app\\maoyun\\dirTest";
        FileUtil.mkParentDirs(path);
        FileUtil.writeBytes("aaa".getBytes(), new File(path+"\\test.txt"));

        boolean isDel=FileUtil.del(path);
        System.out.println("---path="+path+" isDel="+isDel);
        Assertions.assertFalse(FileUtil.exist(path), "file unexist");
    }


    @Test
    void testQueue() throws Exception {
        ArrayBlockingQueue<String> task = new ArrayBlockingQueue<String>(2000);
        for (int i = 0; i < 1000; i++) {
            task.put("" + i);
        }

        int count = 0;
        while (!task.isEmpty()) {
            String v = task.take();
            count++;
        }

        System.out.println(count);

    }
}
