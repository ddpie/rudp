package com.ddpie.rudp.message;

/**
 * RUDP消息枚举类。
 * 
 * @author caobao
 *
 */
public enum RudpMessageType {
	/** 上行消息 */
	UP,
	
	/** 下行消息 */
	DOWN,
	
	/** 会话挂起消息 */
	SUSPEND,
	
	/** 会话恢复消息 */
	RESUME,
	
	/** 会话关闭消息 */
	CLOSE,
}
