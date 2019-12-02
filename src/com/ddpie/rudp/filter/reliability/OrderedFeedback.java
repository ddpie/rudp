package com.ddpie.rudp.filter.reliability;

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * 可靠且有序消息反馈。
 * 
 * @author caobao
 *
 */
public class OrderedFeedback {
	private int sequenceId;				//反馈中的最大消息序号
	private short bitFields;			//反馈中的消息bitfield
	private ChannelBuffer[] msgArray;	//反馈中的连续消息列表

	/**
	 * 根据最大消息序号和消息bitfield构造反馈。
	 * 
	 * @param sequenceId 最大消息序号
	 * @param bitFields 消息bitfield
	 * @param msgArray 连续消息列表
	 */
	public OrderedFeedback(int sequenceId, short bitFields) {
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
	 * 设置反馈中的连续消息列表
	 * 
	 * @param msgArray 连续消息列表
	 */
	public void setMsgArray(ChannelBuffer[] msgArray) {
		this.msgArray = msgArray;
	}

	/**
	 * 获取反馈中的连续消息列表
	 * 
	 * @return 连续消息列表
	 */
	public ChannelBuffer[] getMsgArray() {
		return msgArray;
	}

	@Override
	public String toString() {
		return "ReliableOrderedFeedback [sequenceId=" + sequenceId
				+ ", bitFields=" + Long.toBinaryString(bitFields) + ", msgArray="
				+ Arrays.toString(msgArray) + "]";
	}
}
