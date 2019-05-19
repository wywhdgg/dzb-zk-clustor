package com.fxiaoke.dzb.clustor.dzbzkclustor.consumer;


import com.fxiaoke.dzb.clustor.dzbzkclustor.zkclient.ZkDistributeQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
/***
 *@author lenovo
 *@date 2019/5/19 8:53
 *@Description: 队列消费
 *@version 1.0
 */
public class DistributeQueueConsumerTest {

    public static final String queueRootNode = "/distributeQueue";

    public static final String zkConnUrl = "localhost:2181";

    public static final int capacity = 20;

    public static void main(String[] args) {
        satrtConsumer();
    }

    public static void satrtConsumer() {
        // 服务集群数
        int service = 2;
        // 并发数
        int requestSize = 2;

        CyclicBarrier requestBarrier = new CyclicBarrier(requestSize * service);

        // 多线程模拟分布式环境下消费者
        for (int i = 0; i < service; i++) {
            new Thread(new Runnable() {    // 进程模拟线程
                public void run() {
                    // 模拟分布式集群的场景
                    BlockingQueue<String> queue = new ZkDistributeQueue(zkConnUrl, queueRootNode, capacity);

                    System.out.println(Thread.currentThread().getName() + "---------消费者服务器，已准备好---------------");

                    for (int i = 0; i < requestSize; i++) {    // 操作模拟线程
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // 等待service台服务，requestSize个请求 一起出发
                                    requestBarrier.await();
                                } catch (InterruptedException | BrokenBarrierException e) {
                                    e.printStackTrace();
                                }
                                while (true) {
                                    try {
                                        queue.take();
                                        System.out.println(Thread.currentThread().getName() + "-----进入睡眠状态");
                                        TimeUnit.SECONDS.sleep(3);
                                        System.out.println(Thread.currentThread().getName() + "-----睡眠状态，醒来");
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }, Thread.currentThread().getName() + "-request-" + i).start();
                    }
                }
            }, "consummerServer-" + i).start();
        }

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
