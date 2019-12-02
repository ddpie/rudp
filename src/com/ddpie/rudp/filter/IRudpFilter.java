package com.ddpie.rudp.filter;

import com.ddpie.rudp.processor.IRudpProcessor;
import com.ddpie.rudp.session.IRudpSession;

/**
 * 用来处理RUDP所有事件的一个节点。
 * 所有的方法调用均在{@link IRudpProcessor}线程中处理，所以对于单个会话来说是线程安全的。
 * 
 * @author caobao
 *
 */
public interface IRudpFilter {
	/**
	 * 一个RUDP连接打开了。
	 * 在{@link IRudpProcessor}线程中处理。
	 * 
	 * @param session RUDP会话
	 */
	void sessionOpened(IRudpSession session);
	
	/**
	 * 一个连接被挂起了，远程会话暂时断开了。
	 * 
	 * @param session RUDP会话
	 */
	void sessionSuspended(IRudpSession session);
	
	/**
	 * 一个连接恢复了，远程会话重新连上。
	 * 
	 * @param session RUDP会话
	 */
	void sessionResumed(IRudpSession session);
	
	/**
	 * 一个RUDP连接关闭了。
	 * 在{@link IRudpProcessor}线程中处理。
	 * 
	 * @param session RUDP连接
	 */
	void sessionClosed(IRudpSession session);
	
	/**
	 * 一个消息收到了。
	 * 在{@link IRudpProcessor}线程中处理。
	 * 
	 * @param session RUDP会话
	 * @param messageContent 收到的消息
	 * @return 传递给下一个Filter的消息
	 */
	Object messageReceived(IRudpSession session, Object messageContent);
	
	/**
	 * 一个写请求。
	 * 在{@link IRudpProcessor}线程中处理。
	 * 
	 * @param session RUDP会话
	 * @param messageContent 要写入的消息
	 * @return 传递给下一个Filter的消息
	 */
	Object writeRequested(IRudpSession session, Object messageContent);
}
