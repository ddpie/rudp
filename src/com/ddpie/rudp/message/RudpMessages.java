package com.ddpie.rudp.message;

import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;

import com.ddpie.rudp.filter.IRudpFilter;
import com.ddpie.rudp.filter.IRudpFilterCallback;
import com.ddpie.rudp.session.IRudpSession;

/**
 * RUDP消息生产工厂。
 * 
 * @author caobao
 *
 */
public class RudpMessages {
	
	/**
	 * 构造上行RUDP消息。
	 * 
	 * @param session RUDP会话
	 * @param channel Netty Channel
	 * @param remoteAddress 远端地址
	 * @param content 消息实体
	 * @param callback 所有RUDP的{@link IRudpFilter}操作结束后的回调对象
	 * 
	 * @return 构造完成的RUDP上行消息
	 */
	public static IRudpMessage newUpMessage(IRudpSession session, Channel channel, SocketAddress remoteAddress, Object content, IRudpFilterCallback callback) {
		return new RudpUpMessage(session, remoteAddress, channel, content, callback);
	}
	
	/**
	 * 构造下行RUDP消息。
	 * 
	 * @param session RUDP会话
	 * @param channel Netty Channel
	 * @param remoteAddress 远端地址
	 * @param message 消息实体
	 * @param callback 所有RUDP的{@link IRudpFilter}操作结束后的回调对象
	 * 
	 * @return 构造完成的RUDP下行消息
	 */
	public static IRudpMessage newDownMessage(IRudpSession session, Channel channel, SocketAddress remoteAddress, Object message) {
		return new RudpDownMessage(session, remoteAddress, channel, message);
	}
	
	/**
	 * 构造会话挂起消息。
	 * 
	 * @param session RUDP会话
	 * @param remoteAddress
	 * 
	 * @return 构造完成的RUDP挂起消息
	 */
	public static IRudpMessage newSuspendMessage(IRudpSession session, SocketAddress remoteAddress) {
		return new RudpSuspendMessage(session, remoteAddress);
	}
	
	/**
	 * 构造会话恢复消息。
	 * 
	 * @param session RUDP会话
	 * @param remoteAddress
	 * 
	 * @return 构造完成的RUDP恢复消息
	 */
	public static IRudpMessage newResumeMessage(IRudpSession session, SocketAddress remoteAddress) {
		return new RudpResumeMessage(session, remoteAddress);
	}
	
	/**
	 * 构造会话关闭消息。
	 * 
	 * @param session RUDP会话
	 * @param remoteAddress
	 * 
	 * @return 构造完成的RUDP关闭消息
	 */
	public static IRudpMessage newCloseMessage(IRudpSession session, SocketAddress remoteAddress) {
		return new RudpCloseMessage(session, remoteAddress);
	}
	
}
