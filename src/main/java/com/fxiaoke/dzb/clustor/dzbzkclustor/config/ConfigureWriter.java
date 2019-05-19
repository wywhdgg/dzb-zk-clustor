package com.fxiaoke.dzb.clustor.dzbzkclustor.config;

import java.util.Properties;

/**
 * ConfigureCenter
 * 
 */
public interface ConfigureWriter {
	/**
	 * 创建一个新的配置文件
	 * @param fileName 文件名称
	 * @param items 配置项
	 * @return 新文件的在zk上的路径
	 */
	String createCnfFile(String fileName, Properties items);
	/**
	 * 删除一个配置文件
	 * @param fileName
	 */
	void deleteCnfFile(String fileName);
	/**
	 * 修改一个配置文件
	 * @param fileName
	 * @param items
	 */
	void modifyCnfItem(String fileName, Properties items);
	/**
	 * 加载配置文件
	 * @param fileName
	 * @return
	 */
	Properties loadCnfFile(String fileName);
}

