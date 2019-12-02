package com.ddpie.rudp.message;

/**
 * 可发送消息的发送类型。
 * 
 * @author caobao
 *
 */
public enum WritableMessageType {
	/** 可靠且有序 */
	RELIABLE_ORDERED,
	
	/** 可靠但无序 */
	RELIABLE_DISORDERED,
	
	/** 不可靠 */
	UNRELIABLE,
}
