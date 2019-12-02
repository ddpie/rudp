package com.ddpie.rudp.config;

import java.net.SocketAddress;

import com.ddpie.rudp.filter.IRudpFilter;
import com.ddpie.rudp.processor.IRudpProcessorManager;
import com.ddpie.rudp.session.IRudpSessionManager;

/**
 * RUDP的配置信息接口。
 * 
 * @author caobao
 *
 */
public interface IRudpConfig {
	/**
	 * 是否是服务器。
	 */
	boolean isServer();
	
	/**
	 * 获取服务器地址。
	 * 仅对作为客户端的配置有效。
	 */
	SocketAddress getServerAddress();
	
	/**
	 * 获取会话管理器。
	 */
	IRudpSessionManager getSessionManager();
	
	/**
	 * 获取Processor管理器。
	 */
	IRudpProcessorManager getProcessorManager();
	
	/**
	 * 增加一个RUDP过滤器。
	 */
	void addRudpFilter(IRudpFilter filter);
	
	/**
	 * 销毁RUDP
	 */
	void destroy();
}
