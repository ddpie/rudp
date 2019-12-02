package com.ddpie.rudp.message;

import java.net.SocketAddress;

import com.ddpie.rudp.session.IRudpSession;

/**
 * RUDP消息
 * 
 * @author caobao
 *
 */
public interface IRudpMessage {
	
	/**
	 * 获取RUDP消息类型
	 * 
	 * @return RUDP消息类型
	 */
	RudpMessageType getType();
	
	/**
	 * 获取RUDP会话
	 * 
	 * @return RUDP会话
	 */
	IRudpSession getSession();
	
	/**
	 * 获取远端地址。
	 * 
	 * @return 远端地址
	 */
	SocketAddress getRemoteAddress();
	
}
