package com.fxiaoke.dzb.clustor.dzbzkclustor.curator;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.RetryNTimes;

/**
 * InterProcessMutex 
 * 
 */
public class InterProcessMutexDemo {
	
	private static final String ZK_ADDRESS = "localhost:2181";
	private static final String ZK_LOCK_PATH = "/curatorLock";
	
	
	public void doWithlock(CuratorFramework client) {
		InterProcessMutex lock = new InterProcessMutex(client, ZK_LOCK_PATH);
		try {
            if (lock.acquire(10 * 1000, TimeUnit.SECONDS)) {
                System.out.println(Thread.currentThread().getName() + " hold lock");
                Thread.sleep(5000L);
                System.out.println(Thread.currentThread().getName() + " release lock");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                lock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}
	
	public static void main(String[] args) {
		InterProcessMutexDemo demo = new InterProcessMutexDemo();
		CuratorFramework client = CuratorFrameworkFactory.newClient(
	            ZK_ADDRESS,
	            new RetryNTimes(10, 5000)
	    );
		client.start();
		
		demo.doWithlock(client);
		
	}
}

