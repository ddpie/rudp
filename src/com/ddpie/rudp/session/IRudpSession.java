package com.ddpie.rudp.session;

import java.net.SocketAddress;

import com.ddpie.rudp.attribute.IAttributeContainer;

/**
 * 用来表示一个RUDP的远程会话。
 * 
 * @author caobao
 *
 */
public interface IRudpSession extends IAttributeContainer {
	/**
	 * 连接是否处于关闭状态。
	 * 
	 * @return true，关闭；false，未关闭
	 */
	boolean isClosed();
	
	/**
	 * 连接是否处于挂起状态。
	 * 
	 * @return true，挂起；false，非挂起（打开或关闭）
	 */
	boolean isSuspended();
	
	/**
	 * 获取该会话的远端地址。
	 * 
	 * @return 远端地址
	 */
	SocketAddress getRemoteAddress();
	
	/**
	 * 获取该会话的本地地址
	 * 
	 * @return 本地地址
	 */
	SocketAddress getLocalAddress();
	
	/**
	 * 向会话远端发送可靠且有序的消息。
	 * 
	 * @param message 要发送的消息
	 */
	void writeOrdered(Object message);
	
	/**
	 * 向会话远端发送可靠但无序的消息。
	 * 
	 * @param message 要发送的消息
	 */
	void writeDisordered(Object message);
	
	/**
	 * 向会话远端发送不可靠消息。
	 * 
	 * @param message 要发送的消息
	 */
	void writeUnreliable(Object message);
	
	/**
	 * 关闭会话
	 */
	void close();
	
	/**
	 * 强制子类重新实现{@link Object#hashCode()}方法
	 */
	public int hashCode();

	/**
	 * 强制子类重新实现{@link Object#equals(Object)}方法
	 */
	public boolean equals(Object obj);
	
}
