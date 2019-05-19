package com.fxiaoke.dzb.clustor.dzbzkclustor.originalclient;

import java.util.List;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 *@author lenovo
 *@date 2019/5/19 8:53
 *@Description:OriginalClient 原生Client示例
 *@version 1.0
 */
public class OriginalClient {
	private static final String CONNECT_STRING = "127.0.0.1:2181";
	private static final int SESSION_TIMEOUT = 3000;
	private static final String DEMO_PATH = "/dongnao";
	
	// 定义一个监控所有节点变化的Watcher
	Watcher allChangeWatcher = new Watcher() {
		@Override
		public void process(WatchedEvent event) {
			System.out.println("接收到了WatchedEvent changed path: " + event.getPath()
					+ "; changed type: " + event.getType().name());
		}
	};
	
	public void runDemo() throws Exception {
		// 初始化一个与ZK连接。三个参数：
		// 1、要连接的服务器地址，"IP:port"格式；
		// 2、会话超时时间
		// 3、节点变化监视器
		ZooKeeper zk = new ZooKeeper(CONNECT_STRING, SESSION_TIMEOUT, allChangeWatcher);
 
		// 新建节点。四个参数：1、节点路径；2、节点数据；3、节点权限；4、创建模式
		//String newPath = zk.create(DEMO_PATH+"/data", "123".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		//System.out.println("create new node '"+newPath+"'");
 
		// 判断某路径是否存在。两个参数：1、节点路径；2、是否监控（Watcher即初始化ZooKeeper时传入的Watcher）
		Stat beforSstat = zk.exists(DEMO_PATH, true);
		System.out.println("Stat of "+DEMO_PATH+" before change : " + beforSstat);
 
		// 修改节点数据。三个参数：1、节点路径；2、新数据；3、版本，如果为-1，则匹配任何版本
		Stat afterStat = zk.setData(DEMO_PATH, "456".getBytes(), -1);
		System.out.println("Stat of "+DEMO_PATH+" after change: " + afterStat);
 
		// 获取所有子节点。两个参数：1、节点路径；2、是否监控该节点
		List<String> children = zk.getChildren("/", true);
		System.out.println("children of path '/': " + children.toString());
 
		// 获取节点数据。三个参数：1、节点路径；2、书否监控该节点；3、版本等信息可以通过一个Stat对象来指定
		byte[] nameByte = zk.getData(DEMO_PATH, true, null);
		String name = new String(nameByte, "UTF-8");
		System.out.println("get data from "+DEMO_PATH+": " + name);
 
		// 删除节点。两个参数：1、节点路径；2、 版本，-1可以匹配任何版本，会删除所有数据
		zk.delete(DEMO_PATH, -1);
		System.out.println("delete "+DEMO_PATH);
 
		zk.close();
	}
	
	public static void main(String[] args) {
		OriginalClient oc = new OriginalClient();
		try {
			oc.runDemo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

