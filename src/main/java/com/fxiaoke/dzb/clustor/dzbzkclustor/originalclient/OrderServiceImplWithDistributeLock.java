package com.fxiaoke.dzb.clustor.dzbzkclustor.originalclient;

import com.fxiaoke.dzb.clustor.dzbzkclustor.generator.OrderCodeGenerator;
import com.fxiaoke.dzb.clustor.dzbzkclustor.generator.OrderCodeGeneratorSingle;
import com.fxiaoke.dzb.clustor.dzbzkclustor.zkclient.ZkDistributeImproveLock;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;



/***
 *@author lenovo
 *@date 2019/5/19 8:53
 *@Description:
 *@version 1.0
 */
public class OrderServiceImplWithDistributeLock {
	
	private OrderCodeGenerator ocg = OrderCodeGeneratorSingle.getInstance();
	
	private Lock lock = new ZkDistributeImproveLock("/distributeLock");//OriginalClientDistributeImproveLock
	
	// 重复编号集合
	private static Set<String> codeSet = new HashSet<String>();

	// 创建订单接口
	public void createOrder() {

		String orderCode = null;
		try {
			lock.lock();
			// 获取订单号
			orderCode = ocg.getOrderCode();
			
			if(codeSet.contains(orderCode)) {
				System.err.println("重复编号："+orderCode);
			}else{
				codeSet.add(orderCode);
			}

		} finally {
			lock.unlock();
		}

		System.out.println(Thread.currentThread().getName() + " =============>" + orderCode);

		// ……业务代码，此处省略100行代码

	}

}
