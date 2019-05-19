package com.fxiaoke.dzb.clustor.dzbzkclustor.config;

import com.fxiaoke.dzb.clustor.dzbzkclustor.config.ConfigureReader.ChangeHandler;
import java.util.Properties;

/***
 *@author lenovo
 *@date 2019/5/19 8:53
 *@Description:
 *@version 1.0
 */
public class ConfigureTest {
    public static void main(String[] args) {
        // 模拟运维人员创建配置文件，引用ConfigureWriter接口操作
        ConfigureWriter writer = new ZkConfigureCenter();
        String fileName = "trade-application.properties";
        writer.deleteCnfFile(fileName);    // 测试，确保配置中心没有这个问题

        Properties items = new Properties();
        items.put("abc.gc.a", "123");
        items.put("abc.gc.b", "3456");
        // 创建配置文件，内容为 properties items的内容。
        String znodePath = writer.createCnfFile(fileName, items);
        System.out.println("new file: " + znodePath);

        new Thread(() -> {
            readCnf();
        }).start();

        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 3秒后修改文件内容，有新增、有删除、有修改
        items.put("abc.gc.a", "haha");    // 修改
        items.put("abc.gc.c", "xx");    // 新增
        items.remove("abc.gc.b"); // 删除
        writer.modifyCnfItem(fileName, items);

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 模拟应用程序加载配置文件，监听配置文件的变化
     */
    public static void readCnf() {
        // 应用引用ConfigureReader接口进行操作
        System.out.println("读取并监听配置文件");
        ConfigureReader reader = new ZkConfigureCenter();
        String fileName = "trade-application.properties";
        Properties p = reader.loadCnfFile(fileName);        // 读取配置文件
        System.out.println(p);

        // 监听配置文件
        reader.watchCnfFile(fileName, new ChangeHandler() {
            @Override
            public void itemChange(Properties newProp) {
                System.out.println("发现数据发生变化：" + newProp);
            }
        });

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

