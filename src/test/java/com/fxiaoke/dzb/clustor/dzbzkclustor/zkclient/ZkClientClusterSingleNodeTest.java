package com.fxiaoke.dzb.clustor.dzbzkclustor.zkclient;

import com.fxiaoke.dzb.clustor.dzbzkclustor.serializer.MyZkSerializer;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
/***
 *@author lenovo
 *@date 2019/5/19 8:53
 *@Description:集群测试
 *@version 1.0
 */
public class ZkClientClusterSingleNodeTest {
    public static void main(String[] args) {
        // 创建一个zk客户端
        ZkClient client = new ZkClient("localhost:2182");
        client.setZkSerializer(new MyZkSerializer());
        try {
            client.createPersistent("/zk/a");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
		/*
		Random random = new Random();
		new Thread(()->{
			while(true) {
				int idx = random.nextInt(100);
				client.writeData("/zk/a", String.valueOf(idx));
				System.out.println(System.nanoTime()+" 修改/zk/a节点的数据："+idx);
				try {
					Thread.sleep(5000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		*/
        new Thread(() -> {
            while (true) {
                String data = client.readData("/zk/a");
                System.out.println(System.nanoTime() + " 读取到了/zk/a节点数据内容：" + data);
                try {
                    Thread.sleep(300L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        client.subscribeDataChanges("/zk/a", new IZkDataListener() {
            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                System.out.println(System.nanoTime() + " ----收到节点被删除了-------------");
            }

            @Override
            public void handleDataChange(String dataPath, Object data) throws Exception {
                System.out.println(System.nanoTime() + " ----收到节点数据变化：" + data + "-------------");
            }
        });

        try {
            Thread.sleep(1000 * 60 * 2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
