package com.fxiaoke.dzb.clustor.dzbzkclustor.barriers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/***
 *@author lenovo
 *@date 2019/5/20 8:53
 *@Description: zookeeper 官网实例
 * http://zookeeper.apache.org/doc/current/zookeeperTutorial.html
 *@version 1.0
 */
public class SyncPrimitive implements Watcher {
    static ZooKeeper zk = null;
    static Integer mutex;
    String root;
    String address = "127.0.0.1:2181";
    Integer size = 0;

    SyncPrimitive(String address) {
        if (zk == null) {
            try {
                System.out.println("Starting ZK:");
                zk = new ZooKeeper(address, 3000, this);
                mutex = new Integer(-1);
                System.out.println("Finished starting ZK: " + zk);
            } catch (IOException e) {
                System.out.println(e.toString());
                zk = null;
            }
        }
        //else mutex = new Integer(-1);
    }

    synchronized public void process(WatchedEvent event) {
        synchronized (mutex) {
            //System.out.println("Process: " + event.getType());
            mutex.notify();
        }
    }

    /**
     * Barrier
     */
    static public class Barrier extends SyncPrimitive {
        int size;
        String name;

        /**
         * Barrier constructor
         */
        Barrier(String address, String root, int size) {
            super(address);
            this.root = root;
            this.size = size;

            // Create barrier node
            if (zk != null) {
                try {
                    Stat s = zk.exists(root, false);
                    if (s == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    }
                } catch (KeeperException e) {
                    System.out.println("Keeper exception when instantiating queue: " + e.toString());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception");
                }
            }

            // My node name
            try {
                name = new String(InetAddress.getLocalHost().getCanonicalHostName().toString());
                System.out.println("getCanonicalHostName=" + name);
            } catch (UnknownHostException e) {
                System.out.println(e.toString());
            }
        }

        /**
         * Join barrier
         *
         * @throws KeeperException
         * @throws InterruptedException
         */
        boolean enter() throws KeeperException, InterruptedException {
            zk.create(root + "/" + name, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            while (true) {
                synchronized (mutex) {
                    List<String> list = zk.getChildren(root, true);
                    if (list.size() < size) {
                        mutex.wait();
                    } else {
                        return true;
                    }
                }
            }
        }

        /**
         * Wait until all reach barrier
         *
         * @throws KeeperException
         * @throws InterruptedException
         */
        boolean leave() throws KeeperException, InterruptedException {
            zk.delete(root + "/" + name, 0);
            while (true) {
                synchronized (mutex) {
                    List<String> list = zk.getChildren(root, true);
                    if (list.size() > 0) {
                        mutex.wait();
                    } else {
                        return true;
                    }
                }
            }
        }
    }

    /**
     * Producer-Consumer queue
     */
    static public class Queue extends SyncPrimitive {
        /**
         * Constructor of producer-consumer queue
         */
        Queue(String address, String name) {
            super(address);
            this.root = name;
            // Create ZK node name
            if (zk != null) {
                try {
                    Stat s = zk.exists(root, false);
                    if (s == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    }
                } catch (KeeperException e) {
                    System.out.println("Keeper exception when instantiating queue: " + e.toString());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception");
                }
            }
        }

        /**
         * Add element to the queue.
         */
        boolean produce(int i) throws KeeperException, InterruptedException {
            ByteBuffer b = ByteBuffer.allocate(4);
            byte[] value;
            // Add child with value i
            b.putInt(i);
            value = b.array();
            zk.create(root + "/element", value, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
            return true;
        }

        /**
         * Remove first element from the queue.
         *
         * @throws KeeperException
         * @throws InterruptedException
         */
        int consume() throws KeeperException, InterruptedException {
            int retvalue = -1;
            Stat stat = null;
            // Get the first element available
            while (true) {
                synchronized (mutex) {
                    List<String> list = zk.getChildren(root, true);
                    if (list.size() == 0) {
                        System.out.println("Going to wait");
                        mutex.wait();
                    } else {
                        Integer min = new Integer(list.get(0).substring(7));
                        String minNode = list.get(0);
                        System.out.println("min="+min+"-->minNode="+minNode);
                        for (String s : list) {
                            Integer tempValue = new Integer(s.substring(7));
                            System.out.println("Temporary value: " + tempValue);
                            if (tempValue < min) {
                                min = tempValue;
                                minNode = s;
                            }
                        }
                        System.out.println("Temporary value: " + root + "/" + minNode);
                        byte[] b = zk.getData(root + "/" + minNode, false, stat);
                        zk.delete(root + "/" + minNode, 0);
                        ByteBuffer buffer = ByteBuffer.wrap(b);
                        retvalue = buffer.getInt();
                        return retvalue;
                    }
                }
            }
        }
    }

    public static void main(String args[]) {
        //queueTest("p");
        queueTest("");
        //barrierTest();
    }

    public static void queueTest(String pc) {
        Queue q = new Queue("127.0.0.1:2181", "/app1");
        System.out.println("Input: " + "127.0.0.1:2181");
        int i;
        Integer max = new Integer(10);
        if (pc.equals("p")) {
            System.out.println("Producer");
            for (i = 0; i < max; i++) {
                try {
                    q.produce(10 + i);
                } catch (KeeperException e) {

                } catch (InterruptedException e) {

                }
            }
        } else {
            System.out.println("Consumer");
            for (i = 0; i < max; i++) {
                try {
                    int r = q.consume();
                    System.out.println("Item: " + r);
                } catch (KeeperException e) {
                    i--;
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public static void barrierTest() {
        Integer count = new Integer("10");
        Barrier b = new Barrier("127.0.0.1:2181", "/b1", count);
        try {
            boolean flag = b.enter();
            System.out.println("Entered barrier: " + count);
            if (!flag) {
                System.out.println("Error when entering the barrier");
            }
        } catch (KeeperException e) {
        } catch (InterruptedException e) {
        }

        // Generate random integer
        Random rand = new Random();
        int r = rand.nextInt(10);
        // Loop for rand iterations
        for (int i = 0; i < r; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        try {
            b.leave();
        } catch (KeeperException e) {

        } catch (InterruptedException e) {

        }
        System.out.println("Left barrier");
    }
}