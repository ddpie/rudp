package com.ddpie.rudp.filter;


/**
 * RUDP所有{@link IRudpFilter}操作结束后的回调。
 * 
 * @author caobao
 *
 */
public interface IRudpFilterCallback {
	
	/**
	 * RUDP所有{@link IRudpFilter}操作结束后的回调。
	 * 
	 * @param 处理完的消息
	 */
	void callback(Object messageContent);
}
