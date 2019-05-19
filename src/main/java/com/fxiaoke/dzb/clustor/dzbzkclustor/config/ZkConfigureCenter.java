package com.fxiaoke.dzb.clustor.dzbzkclustor.config;

import com.fxiaoke.dzb.clustor.dzbzkclustor.serializer.MyZkSerializer;
import com.fxiaoke.dzb.clustor.dzbzkclustor.zkclient.ZkDistributeImproveLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;

/**
 * ConfigureCenter 配置中心实现
 */
public class ZkConfigureCenter implements ConfigureWriter, ConfigureReader {
    private String confRootPath;
    private String confFilePath;
    private String fileLockPath;
    private static final String default_confRootPath = "/distributeConfigure";
    private ZkClient client;

    public ZkConfigureCenter() {
        this(default_confRootPath);
    }

    public ZkConfigureCenter(String path) {
        if (path == null || path.trim().equals("")) {
            throw new IllegalArgumentException("patch不能为空字符串");
        }
        confRootPath = path;
        confFilePath = confRootPath + "/cnfFile";
        fileLockPath = confRootPath + "/writeLock";
        client = new ZkClient("localhost:2181");
        client.setZkSerializer(new MyZkSerializer());
        if (!this.client.exists(confFilePath)) {
            try {
                this.client.createPersistent(confFilePath, true);
            } catch (ZkNodeExistsException e) {

            }
        }
    }

    private void checkElement(String v) {
        if (v == null) {
            throw new NullPointerException();
        }
        if ("".equals(v.trim())) {
            throw new IllegalArgumentException("不能使用空格");
        }
        if (v.startsWith(" ") || v.endsWith(" ")) {
            throw new IllegalArgumentException("前后不能包含空格");
        }
    }

    @Override
    public String createCnfFile(String fileName, Properties items) {
        checkElement(fileName);
        // 创建配置文件Node
        String cfgNode = confFilePath + "/" + fileName;
        if (client.exists(cfgNode)) {
            throw new IllegalArgumentException("[" + fileName + "]文件已存在！");
        }
        client.createPersistent(cfgNode, true);
        // 创建配置文件中的配置项
        if (items == null) {
            return cfgNode;
        }
        Lock distributeWriteLock = new ZkDistributeImproveLock(fileLockPath + "/" + fileName);
        distributeWriteLock.lock();
        try {
            items.keySet().iterator();
            Set<Map.Entry<Object, Object>> entrySet = items.entrySet();
            for (Map.Entry<Object, Object> entry : entrySet) {
                System.out.println(entry.getKey() + "=" + entry.getValue());
                String cfgItemNode = cfgNode + "/" + entry.getKey().toString();
                client.createPersistent(cfgItemNode, entry.getValue());
            }
        } finally {
            distributeWriteLock.unlock();
        }
        return cfgNode;
    }

    @Override
    public void deleteCnfFile(String fileName) {
        checkElement(fileName);
        String cfgNode = confFilePath + "/" + fileName;
        // 创建锁
        Lock distributeWriteLock = new ZkDistributeImproveLock(fileLockPath + "/" + fileName);
        // 获得锁
        distributeWriteLock.lock();
        try {
            client.deleteRecursive(cfgNode);
        } finally {
            // 释放锁
            distributeWriteLock.unlock();
        }
    }

    @Override
    public void modifyCnfItem(String fileName, Properties items) {
        checkElement(fileName);
        // 获取子节点信息
        String cfgNode = confFilePath + "/" + fileName;
        // 简单粗暴的实现
        if (items == null) {
            throw new NullPointerException("要修改的配置项不能为空");
        }
        items.keySet().iterator();
        Set<Map.Entry<Object, Object>> entrySet = items.entrySet();
        Lock distributeWriteLock = new ZkDistributeImproveLock(fileLockPath + "/" + fileName);
        distributeWriteLock.lock();
        try {
            // 获取zk中已存在的配置信息
            List<String> itemNodes = client.getChildren(cfgNode);
            Set<String> existentItemSet = itemNodes.stream().collect(Collectors.toSet());

            for (Map.Entry<Object, Object> entry : entrySet) {
                System.out.println(entry.getKey() + "=" + entry.getValue());
                String itemName = entry.getKey().toString();
                String itemData = entry.getValue().toString();

                String cfgItemNode = cfgNode + "/" + itemName;
                if (existentItemSet.contains(itemName)) {// zk中存在的配置项
                    String itemNodeData = client.readData(cfgItemNode);
                    if (!eql(itemNodeData, itemData)) { // 数据不一致才需要修改
                        client.writeData(cfgItemNode, itemData);
                    }
                    existentItemSet.remove(itemName);    // 剩下的就是需要删除的配置项
                } else { // zk中不存在的配置项，新的配置项
                    client.createPersistent(cfgItemNode, itemData);
                }
            }

            // existentItemSet中剩下的就是需要删除的
            if (!existentItemSet.isEmpty()) {
                for (String itemName : existentItemSet) {
                    String cfgItemNode = cfgNode + "/" + itemName;
                    client.delete(cfgItemNode);
                }
            }
        } finally {
            distributeWriteLock.unlock();
        }
    }

    private boolean eql(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a != null) {
            return a.equals(b);
        }
        if (b != null) {
            return b.equals(a);
        }
        return false;
    }

    @Override
    public Properties loadCnfFile(String fileName) {
        if (!fileName.startsWith("/")) {
            fileName = confFilePath + "/" + fileName;
        }
        return loadNodeCnfFile(fileName);
    }

    private Properties loadNodeCnfFile(String cfgNode) {
        checkElement(cfgNode);
        if (!client.exists(cfgNode)) {
            throw new ZkNoNodeException(cfgNode);
        }
        // 获取子节点信息
        List<String> itemNodes = client.getChildren(cfgNode);

        // 读取配置信息，并装载到Properties中
        if (itemNodes == null || itemNodes.isEmpty()) {
            return new Properties();
        }
        Properties file = new Properties();
        itemNodes.stream().forEach((e) -> {
            String itemNameNode = cfgNode + "/" + e;
            String data = client.readData(itemNameNode, true);
            file.put(e, data);
        });
        return file;
    }

    @Override
    public void watchCnfFile(String fileName, ChangeHandler changeHandler) {
        if (!fileName.startsWith("/")) {
            fileName = confFilePath + "/" + fileName;
        }
        final String fileNodePath = fileName;
        // 读取文件
        Properties p = loadNodeCnfFile(fileNodePath);
        if (p != null) {
            // 合并5秒配置项变化，5秒内变化只触发一次处理事件
            int waitTime = 5;
            final ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(1);
            scheduled.setRemoveOnCancelPolicy(true);
            final List<ScheduledFuture<?>> futureList = new ArrayList<ScheduledFuture<?>>();
            Set<Map.Entry<Object, Object>> entrySet = p.entrySet();
            for (Map.Entry<Object, Object> entry : entrySet) {
                System.out.println("监控：" + fileNodePath + "/" + entry.getKey().toString());
                client.subscribeDataChanges(fileNodePath + "/" + entry.getKey().toString(), new IZkDataListener() {
                    @Override
                    public void handleDataDeleted(String dataPath) throws Exception {
                        System.out.println("触发删除：" + dataPath);
                        triggerHandler(futureList, scheduled, waitTime, fileNodePath, changeHandler);
                    }

                    @Override
                    public void handleDataChange(String dataPath, Object data) throws Exception {
                        System.out.println("触发修改：" + dataPath);
                        triggerHandler(futureList, scheduled, waitTime, fileNodePath, changeHandler);
                    }
                });
            }
            client.subscribeChildChanges(fileNodePath, new IZkChildListener() {
                @Override
                public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                    System.out.println("触发子节点：" + parentPath);
                    triggerHandler(futureList, scheduled, waitTime, fileNodePath, changeHandler);
                }
            });
        }
    }

    /**
     * 合并修改变化事件，5秒钟内发生变化的合并到一个事件进行
     *
     * @param futureList 装有定时触发任务的列表
     * @param scheduled 定时任务执行器
     * @param waitTime 延迟时间，单位秒
     * @param fileName zk配置文件的节点
     * @param changeHandler 事件处理器
     */
    private void triggerHandler(List<ScheduledFuture<?>> futureList, ScheduledThreadPoolExecutor scheduled, int waitTime, String fileName, ChangeHandler changeHandler) {
        if (futureList != null && !futureList.isEmpty()) {
            for (int i = 0; i < futureList.size(); i++) {
                ScheduledFuture<?> future = futureList.get(i);
                if (future != null && !future.isCancelled() && !future.isDone()) {
                    future.cancel(true);
                    futureList.remove(future);
                    i--;
                }
            }
        }
        ScheduledFuture<?> future = scheduled.schedule(() -> {
            Properties p = loadCnfFile(fileName);
            changeHandler.itemChange(p);
        }, waitTime, TimeUnit.SECONDS);
        futureList.add(future);
    }
}

