package com.ddpie.rudp.message;

/**
 * 可发送的消息封装类。
 * 包含发送消息体和要采用的发送类型（有序、无序或不可靠）。
 * 
 * @author caobao
 *
 */
public final class WritableMessageWrapper {
	private Object messageContent;		//消息实体
	private WritableMessageType type;	//发送类型
	
	private WritableMessageWrapper(Object messageContent,
			WritableMessageType type) {
		super();
		this.messageContent = messageContent;
		this.type = type;
	}
	
	/**
	 * 获取消息实体
	 * 
	 * @return
	 */
	public Object getMessageContent() {
		return messageContent;
	}
	
	/**
	 * 获取发送类型
	 * 
	 * @return
	 */
	public WritableMessageType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return "WritableMessageWrapper [type=" + type + ", messageContent="
				+ messageContent + "]";
	}

	/**
	 * 包装可靠且有序的消息。
	 * 
	 * @param messageContent
	 * @return
	 */
	public static WritableMessageWrapper newReliableOrdered(Object messageContent) {
		if(messageContent instanceof WritableMessageWrapper) {
			throw new RuntimeException("recursive wrap: " + messageContent);
		}
		return new WritableMessageWrapper(messageContent, WritableMessageType.RELIABLE_ORDERED);
	}
	
	/**
	 * 包装可靠但无序的消息。
	 * 
	 * @param messageContent
	 * @return
	 */
	public static WritableMessageWrapper newReliableDisordered(Object messageContent) {
		if(messageContent instanceof WritableMessageWrapper) {
			throw new RuntimeException("recursive wrap: " + messageContent);
		}
		return new WritableMessageWrapper(messageContent, WritableMessageType.RELIABLE_DISORDERED);
	}
	
	/**
	 * 包装不可靠消息。
	 * 
	 * @param messageContent
	 * @return
	 */
	public static WritableMessageWrapper newUnreliable(Object messageContent) {
		if(messageContent instanceof WritableMessageWrapper) {
			throw new RuntimeException("recursive wrap: " + messageContent);
		}
		return new WritableMessageWrapper(messageContent, WritableMessageType.UNRELIABLE);
	}
	
}
