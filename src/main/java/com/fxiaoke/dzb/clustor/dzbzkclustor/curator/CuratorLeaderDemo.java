package com.fxiaoke.dzb.clustor.dzbzkclustor.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.RetryNTimes;

/**
 * CuratorLeaderDemo
 * 
 */
public class CuratorLeaderDemo {
	private static final String ZK_ADDRESS = "localhost:2181";
	private static final String ZK_PATH = "/curatorLeader";
	/*
	 * Curator提供了LeaderSelector监听器实现Leader选举功能。
	 * 同一时刻，只有一个Listener会进入takeLeadership()方法，说明它是当前的Leader。
	 */
    public void registerListener(LeaderSelectorListener listener) {
        CuratorFramework client = CuratorFrameworkFactory.newClient(
                ZK_ADDRESS,
                new RetryNTimes(10, 5000)
        );
        client.start();

        // 创建目录
        try {
        	if(client.checkExists().forPath(ZK_PATH) == null) {
        		client.create().creatingParentsIfNeeded().forPath(ZK_PATH);
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 注册监听器
        LeaderSelector selector = new LeaderSelector(client, ZK_PATH, listener);
        // autoRequeue()方法使放弃Leadership的Listener有机会重新获得Leadership，
        // 如果不设置的话放弃了的Listener是不会再变成Leader的
        selector.autoRequeue();
        selector.start();
    }
    
    public static void main(String[] args) throws InterruptedException {
        LeaderSelectorListener listener = new LeaderSelectorListener() {
            @Override
            public void takeLeadership(CuratorFramework client) throws Exception {
            	// 同一时刻，只有一个Listener会进入takeLeadership()方法，说明它是当前的Leader。
                System.out.println(Thread.currentThread().getName() + " 获得领导权!");

                Thread.sleep(5000L);
                
                // 当Listener从takeLeadership()退出时就说明它放弃了“Leader身份”
                System.out.println(Thread.currentThread().getName() + " 放弃领导权!");
            }
            
			@Override
			public void stateChanged(CuratorFramework client, ConnectionState newState) {
				
			}
        };
        CuratorLeaderDemo demo = new CuratorLeaderDemo();
        new Thread(() -> {
        	demo.registerListener(listener);
        }).start();

        new Thread(() -> {
        	demo.registerListener(listener);
        }).start();

        new Thread(() -> {
        	demo.registerListener(listener);
        }).start();

        Thread.currentThread().join();
    }
}

