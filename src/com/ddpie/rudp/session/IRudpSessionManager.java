package com.ddpie.rudp.session;

import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;

/**
 * RUDP会话管理器，主要负责维护RUDP会话的连接状态。
 * 
 * @author caobao
 *
 */
public interface IRudpSessionManager {
	/**
	 * 会话连接成功后的回调。
	 * 仅由RUDP内部调用。
	 * 
	 * @param session RUDP会话
	 */
	void addSession(RudpSession session);
	
	/**
	 * 关闭RUDP会话，断开与远端的连接状态。
	 * 
	 * @param session 要断开的RUDP会话
	 */
	void removeSession(RudpSession session);
	
	/**
	 * 挂起RUDP会话。
	 * 
	 * @param session 要挂起的RUDP会话
	 */
	void suspendSession(RudpSession session);
	
	/**
	 * 恢复RUDP会话。
	 * 
	 * @param session 要恢复的RUDP会话
	 */
	void resumeSession(RudpSession session);
	
	/**
	 * 判断指定的{@link SocketAddress}是否处于连接状态。
	 * 
	 * @param remoteAddress 要判断的远端地址
	 * @return true，处于连接状态；false，不处于连接状态
	 */
	boolean isConnected(SocketAddress remoteAddress);
	
	/**
	 * 根据远端地址获取对应的RUDP会话
	 * 
	 * @param remoteAddress 远端地址
	 * @return RUDP会话
	 */
	IRudpSession getRudpSession(SocketAddress remoteAddress);
	
	/**
	 * 作为客户端连接服务器。
	 */
	void sendHandshake(Channel channel, SocketAddress remoteAddress);
	
	/**
	 * 关闭所有会话。
	 */
	void closeAllSession();
	
	/**
	 * 获得所有连接的总数
	 */
	int getTotalConnectionCount();
	
	/**
	 * 获得挂起的连接的数量（需要遍历，相对耗时）
	 */
	int getSuspendedConnectionCount();
}
