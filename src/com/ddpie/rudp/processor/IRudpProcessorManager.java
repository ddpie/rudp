package com.ddpie.rudp.processor;

import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;

import com.ddpie.rudp.filter.IRudpFilter;
import com.ddpie.rudp.filter.IRudpFilterCallback;
import com.ddpie.rudp.session.IRudpSession;

/**
 * 负责RUDP消息处理器的管理工作。
 * 
 * @author caobao
 *
 */
public interface IRudpProcessorManager {
	
	/**
	 * 增加一个RUDP Filter。
	 */
	void addRudpFilter(IRudpFilter rudpFilter);
	
	/**
	 * 滴答一个会话
	 * 
	 * @param session 要进行滴答的会话
	 */
	void tickSession(IRudpSession session);
	
	/**
	 * 网络层收到消息后的回调。
	 * 
	 * @param channel Netty channel 
	 * @param remoteAddress 远端地址
	 * @param message 消息
	 * @param callback 所有RUDP的{@link IRudpFilter}操作结束后的回调对象
	 */
	void onMessageReceived(Channel channel, SocketAddress remoteAddress, Object message, IRudpFilterCallback callback);
	
	/**
	 * 向网络层发送消息时的回调。
	 * 
	 * @param channel 通道
	 * @param remoteAddress 远端地址
	 * @param message 消息
	 */
	void onWriteRequested(Channel channel, SocketAddress remoteAddress, Object message);
	
	/**
	 * 当检测到会话挂起时的回调
	 * 
	 * @param session 会话
	 * @param remoteAddress 远端地址
	 */
	void onSessionSuspended(IRudpSession session, SocketAddress remoteAddress);
	
	/**
	 * 当检测到会话恢复时的回调
	 * 
	 * @param session 会话
	 * @param remoteAddress 远端地址
	 */
	void onSessionResumed(IRudpSession session, SocketAddress remoteAddress);
	
	/**
	 * 当检测到会话关闭时的回调
	 * 
	 * @param session 会话
	 * @param remoteAddress 远端地址
	 */
	void onSessionClosed(IRudpSession session, SocketAddress remoteAddress);
}
