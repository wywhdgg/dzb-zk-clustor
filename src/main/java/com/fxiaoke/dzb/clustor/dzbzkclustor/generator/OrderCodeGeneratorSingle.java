package com.fxiaoke.dzb.clustor.dzbzkclustor.generator;


/***
 *@author lenovo
 *@date 2019/5/19 8:53
 *@Description:订单编号生成类
 *@version 1.0
 */
public class OrderCodeGeneratorSingle {
	
	static class InstanceHolder {
		private static OrderCodeGenerator instance = new OrderCodeGenerator();
	}
	
	public static OrderCodeGenerator getInstance() {
		return InstanceHolder.instance;
	}
}
