package com.fxiaoke.dzb.clustor.dzbzkclustor.originalclient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 *@author lenovo
 *@date 2019/5/19 8:53
 *@Description:  OriginalClientDistributeImproveLock
 *@version 1.0
 */
public class OriginalClientDistributeImproveLock implements Lock {
	
	private ZooKeeper zkClient;
	private String lockPath;
	private ThreadLocal<String> currentPath = new ThreadLocal<String>();
	private ThreadLocal<String> previousPath = new ThreadLocal<String>();
	
	private static final String CONNECT_STRING = "127.0.0.1:2181";
	private static final int SESSION_TIMEOUT = 3000;

	public OriginalClientDistributeImproveLock(String path) {
		if(path == null) {
			throw new NullPointerException("参数不能为null");
		}
		// 初始化
		try {
			ZooKeeper zk = new ZooKeeper(CONNECT_STRING, SESSION_TIMEOUT, null);
			this.zkClient = zk;
			this.lockPath = path;
			if(zkClient.exists(lockPath, false) == null) {
				zkClient.create(lockPath, "locked".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void lock() {
		if(!tryLock()) { // 尝试获取锁
			// 阻塞等待前一个节点删除消息
			waitForLock();
			
			lock();
		}
	}

	private void waitForLock() {
		// 未获得锁的线程，通过栅栏进行控制 阻塞—>唤醒动作
		CountDownLatch cdl = new CountDownLatch(1);
		Watcher watcher = new Watcher() {
			@Override
			public void process(WatchedEvent event) {
				if(Event.EventType.NodeDeleted.equals(event.getType())) {
					// 监听到节点被删除，唤醒阻塞中的线程。
					cdl.countDown();
					System.out.println("节点被删除，去唤醒阻塞的线程"+Thread.currentThread().getName());
				}
			}
		};
		try {
			Stat exiStat = zkClient.exists(previousPath.get(), watcher);
			if(exiStat != null) {
				cdl.await();	// 为获得锁，进入阻塞
			}
		} catch (KeeperException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public boolean tryLock() {
		String rootDir = lockPath+"/";
		try {
			if(currentPath.get() == null || zkClient.exists(currentPath.get(), null) == null) {
				// 创建临时顺序节点
				String newPath = zkClient.create(rootDir, "locked".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
				currentPath.set(newPath);
			}
			// 获取节点下的子节点
			List<String> children = zkClient.getChildren(lockPath, null);
			// 排序子节点，如果自己是最小的节点，则表示获取到了锁
			List<String> sortedChildren = children.stream().sorted().collect(Collectors.toList());
			String curPath = currentPath.get().substring(rootDir.length());
			if(curPath.equals(sortedChildren.get(0))) {
				return true;
			}else {// 否则获取前一个节点信息，记录它，等待它的删除动作（释放锁）信号
				int currentIdx = sortedChildren.indexOf(curPath);
				String prevPath = sortedChildren.get(currentIdx - 1);
				previousPath.set(rootDir+prevPath);
			}
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return false;
	}

	@Override
	public void unlock() {
		try {
			if(currentPath.get() != null) {
				zkClient.delete(currentPath.get(), -1);
				currentPath.set(null);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (KeeperException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void lockInterruptibly() throws InterruptedException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Condition newCondition() {
		// TODO Auto-generated method stub
		return null;
	}
	
}

