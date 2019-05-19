package com.fxiaoke.dzb.clustor.dzbzkclustor.generator;

import java.text.SimpleDateFormat;
import java.util.Date;

/***
 *@author lenovo
 *@date 2019/5/19 8:53
 *@Description:订单编号生成类
 *@version 1.0
 */
public class OrderCodeGenerator {
	// 自增长序列
	private int i = 1;

	// 按照“年-月-日-小时-分钟-秒-自增长序列”的规则生成订单编号
	public String getOrderCode() {
		Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-");
		return sdf.format(now) + i++;
	}
	
	

	public static void main(String[] args) {
		OrderCodeGenerator ong = new OrderCodeGenerator();
		for (int i = 0; i < 10; i++) {
			System.out.println(ong.getOrderCode());
		}
	}

}
