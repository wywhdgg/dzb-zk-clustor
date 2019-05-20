package com.fxiaoke.dzb.clustor.dzbzkclustor.master;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.WatchedEvent;

/***
 *@author lenovo
 *@date 2019/5/20 23:28
 *@Description:
        通过 Leader Election 选举方案进行 Master选举，需添加 LeaderSelectorListener 监听器对领导权进行控制，
        当节点被选为leader之后，将调用 takeLeadership 方法进行业务逻辑处理，处理完成会立即释放 leadship，
        重新进行Master选举，这样每个节点都有可能成为 leader。
        autoRequeue() 方法的调用确保此实例在释放领导权后还可能获得领导权。
 *@version 1.0
 */
public class LeaderSelectorMaster {

    private static final String zkServerIps = "127.0.0.1:2181";
    private static final String masterPath = "/testZK/leader_selector";

    public static void main(String[] args) {
        final int clientNums = 5;  // 客户端数量，用于模拟
        final CountDownLatch countDownLatch = new CountDownLatch(clientNums);
        List<LeaderSelector> selectorList = new CopyOnWriteArrayList();
        List<CuratorFramework> clientList = new CopyOnWriteArrayList();
        AtomicInteger atomicInteger = new AtomicInteger(1);
        try {
            for (int i = 0; i < clientNums; i++) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        /**创建客户端*/
                        CuratorFramework client = getClient();
                        clientList.add(client);
                        int number = atomicInteger.getAndIncrement();
                        final String name = "client#" + number;
                        final LeaderSelector selector = new LeaderSelector(client, masterPath, new LeaderSelectorListener() {
                            @Override
                            public void takeLeadership(CuratorFramework client) throws Exception {
                                System.out.println(name + ": 我现在被选举为Leader！我开始工作了...."+client.getData());
                                Thread.sleep(3000);
                                System.out.println(name+":休眠已经醒了");
                            }
                            @Override
                            public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                                System.out.println("数据状态改变...."+connectionState.name());
                            }
                        });
                        System.out.println("创建客户端：" + name);
                        try {
                            selector.autoRequeue();
                            selector.start();
                            selectorList.add(selector);
                            // 随机等待 number * 10秒，之后关闭客户端
                            System.out.println("随机数 number="+number);
                            Thread.sleep(number * 10000);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        } finally {
                            countDownLatch.countDown();
                            System.out.println("客户端 " + name + " 关闭");
                            CloseableUtils.closeQuietly(selector);
                            if (!client.getState().equals(CuratorFrameworkState.STOPPED)) {
                                CloseableUtils.closeQuietly(client);
                            }
                        }
                    }
                }).start();
            }
            countDownLatch.await(); // 等待，只有所有线程都退出
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized CuratorFramework getClient() {
        CuratorFramework client = CuratorFrameworkFactory.builder().connectString(zkServerIps)
            .sessionTimeoutMs(6000).connectionTimeoutMs(3000) //.namespace("LeaderLatchTest")
            .retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
        client.start();
        return client;
    }
}
