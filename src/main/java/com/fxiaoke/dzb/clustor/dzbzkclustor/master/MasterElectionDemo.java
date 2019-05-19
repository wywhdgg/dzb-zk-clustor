package com.fxiaoke.dzb.clustor.dzbzkclustor.master;

import com.fxiaoke.dzb.clustor.dzbzkclustor.serializer.MyZkSerializer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
/***
 *@author lenovo
 *@date 2019/5/19 8:53
 *@Description:
 *@version 1.0
 */
public class MasterElectionDemo {
    // 代表服务器的一个类
    class Server {
        private String cluster, name, address;
        private final String masterPath, value;
        private String master;
        private ZkClient client;

        public Server(String cluster, String name, String address) {
            super();
            this.cluster = cluster;
            this.name = name;
            this.address = address;
            // master znode，这种方式就可以放多了集群
            masterPath = "/" + this.cluster + "/master";
            value = "name:" + this.name + " address:" + this.address;

            client = new ZkClient("localhost:2181");
            client.setZkSerializer(new MyZkSerializer());

            // 集群下所有服务信息存放Znode根目录
            String serversPath = "/" + this.cluster + "/servers";
            client.createPersistent(serversPath, true);

            // 具体服务Znode  临时节点
            String serverPath = serversPath + "/" + name;
            client.createEphemeral(serverPath, value);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    electionMaster(client);
                }
            }).start();
        }

        public void electionMaster(ZkClient client) {
            try {
                // 类似抢篮板，获得master权利
                client.createEphemeral(masterPath, value);
                // 获得了master权利，记录master节点信息
                master = client.readData(masterPath);
                System.out.println(value + "创建节点成功，成为Master");
            } catch (ZkNodeExistsException e) {
                // 没有抢到，把master信息读到自己的服务里面
                master = client.readData(masterPath);
                System.out.println("Master为：" + master);
            }

            // 为阻塞自己等待而用
            CountDownLatch cdl = new CountDownLatch(1);

            // 注册watcher
            IZkDataListener listener = new IZkDataListener() {
                @Override
                public void handleDataDeleted(String dataPath) throws Exception {
                    System.out.println("-----监听到节点被删除");
                    cdl.countDown();
                }

                @Override
                public void handleDataChange(String dataPath, Object data) throws Exception {

                }
            };

            client.subscribeDataChanges(masterPath, listener);

            // 让自己阻塞
            if (client.exists(masterPath)) {
                try {
                    cdl.await();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            // 醒来后，取消watcher
            client.unsubscribeDataChanges(masterPath, listener);
            // 递归调自己（下一次选举）
            electionMaster(client);
        }

        public void close() {
            client.close();
        }
    }

    public static void main(String[] args) {
        // 测试时，依次开启多个Server实例java线程，代替进程。然后停止的master的节点，看谁抢到Master
        Server s1 = new MasterElectionDemo().new Server("cluster1", "server1", "192.168.1.11:8991");
        Server s2 = new MasterElectionDemo().new Server("cluster1", "server2", "192.168.1.11:8992");
        Server s3 = new MasterElectionDemo().new Server("cluster1", "server3", "192.168.1.11:8993");
        Server s4 = new MasterElectionDemo().new Server("cluster1", "server4", "192.168.1.11:8994");

        ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(1);
        scheduled.schedule(() -> {
            System.out.println("关闭s1");
            s1.close();
        }, 5, TimeUnit.SECONDS);
        scheduled.schedule(() -> {
            System.out.println("关闭s2");
            s2.close();
        }, 10, TimeUnit.SECONDS);
        scheduled.schedule(() -> {
            System.out.println("关闭s3");
            s3.close();
        }, 15, TimeUnit.SECONDS);

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
