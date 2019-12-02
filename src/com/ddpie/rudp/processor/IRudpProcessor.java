package com.ddpie.rudp.processor;

import com.ddpie.rudp.message.IRudpMessage;
import com.ddpie.rudp.session.IRudpSession;

/**
 * RUDP消息处理器
 * 
 * @author caobao
 *
 */
public interface IRudpProcessor {
	
	/**
	 * 启动消息处理器。
	 */
	void start();
	
	/**
	 * 停止消息处理器。
	 */
	void stop();
	
	/**
	 * 向消息处理器中投递需要处理的RUDP消息。
	 * 
	 * @param message RUDP消息
	 */
	void put(IRudpMessage message);
	
	/**
	 * 执行指定RUDP会话的tick操作。
	 * 
	 * @param session
	 */
	void tickSession(IRudpSession session);
}
