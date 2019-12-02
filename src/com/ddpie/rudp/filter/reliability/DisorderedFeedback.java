package com.ddpie.rudp.filter.reliability;

/**
 * 可靠但无序消息反馈。
 * 
 * @author caobao
 *
 */
public class DisorderedFeedback {
	private int sequenceId;			//反馈中的最大消息序号
	private short bitFields;		//反馈中的消息bitfield
	private boolean duplicatedMsg;	//是否是重复的消息

	/**
	 * 根据最大消息序号和消息bitfield构造反馈。
	 * 
	 * @param sequenceId 最大消息序号
	 * @param bitFields 消息bitfield
	 */
	public DisorderedFeedback(int sequenceId, short bitFields) {
		this.sequenceId = sequenceId;
		this.bitFields = bitFields;
	}
	
	/**
	 * 获取反馈中的最大消息序号。
	 * 
	 * @return 最大消息序号
	 */
	public int getSequenceId() {
		return sequenceId;
	}
	
	/**
	 * 获取反馈中的消息bitfield
	 * 
	 * @return 消息bitfield
	 */
	public short getBitFields() {
		return bitFields;
	}

	/**
	 * 是否是重复消息。
	 * 若是重复消息，则不应该跑到应用层。
	 * 
	 * @return
	 */
	public boolean isDuplicatedMsg() {
		return duplicatedMsg;
	}

	/**
	 * 设置是否是重复消息。
	 * 
	 * @param duplicatedMsg
	 */
	public void setDuplicatedMsg(boolean duplicatedMsg) {
		this.duplicatedMsg = duplicatedMsg;
	}

	@Override
	public String toString() {
		return "ReliableDisorderedFeedback [sequenceId=" + sequenceId + ", bitFields="
				+ Integer.toBinaryString(bitFields) + "]";
	}
}
