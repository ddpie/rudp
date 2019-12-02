package com.ddpie.rudp.filter.reliability;

import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * 可靠且连续消息的接收状态。
 * 用来维护本地接收到的远程消息序号集合。
 * 
 * @author caobao
 * 
 */
public class OrderedUpStatus {
	private BitSet bitSet;						//存储消息状态的BitSet
	private int size;							//所能存储消息状态的数量
	private int curSequenceId = -1;				//最新接收到的消息序号
	private int curBitIndex = -1;				//最新接收到的消息在BitSet中的位置
	private int lastFeedbackSequenceId = -1;	//上次反馈的最大消息序号
	private int startSequenceId = 1;			//连续消息的起始ID
	private Map<Integer, ChannelBuffer> bufMap;	//消息缓存
	
	public OrderedUpStatus(int size) {
		this.size = size;
		bitSet = new BitSet(size);
		bufMap = new ConcurrentHashMap<Integer, ChannelBuffer>();
	}
	
	/**
	 * 获取消息缓存大小
	 * 
	 * @return
	 */
	public int getCacheSize() {
		return bufMap.size();
	}
	
	/**
	 * 收到新消息后的回调。
	 * 
	 * @param sequenceId 收到的消息的序号
	 * @return 若不为null，则需要向远端反馈
	 */
	public OrderedFeedback onMessageReceived(int sequenceId, ChannelBuffer buf) {
		//很老的消息了
		//很可能是由于之前发给远端的反馈心跳消息丢失，导致远端重复发送
		//也有可能是该消息确实一直未收到，远端一直重发，现在收到了，但此时消息已失去时效性
		//无论如何，都应该给远端发送心跳，告知本消息已经收到
		if(sequenceId < curSequenceId - size + 1) {
			return new OrderedFeedback(sequenceId, (short) 1);
		}
		
		int bitIndex = sequenceId - curSequenceId + curBitIndex;
		while(bitIndex < 0) {
			bitIndex += size * 10;
		}
		if(bitIndex >= size) {
			bitIndex = bitIndex % size;
		}
		
		//这条消息曾经收到过
		//很可能是由于之前发给远端的反馈心跳消息丢失，导致远端重复发送
		//需要告知远端，本消息已经收到了
		//这个消息前后消息的反馈也有可能丢失了，所以也把前后消息的接收状态带上（不会浪费空间）
		if(bitSet.get(bitIndex) && sequenceId <= curSequenceId) {
			return buildFeedback(sequenceId - 7, sequenceId + 8);
		}
		
		//新消息，如果是跳跃性的，则需要重置跨过的bit
		if(curSequenceId < sequenceId) {
			resetBits(sequenceId);
			curSequenceId = sequenceId;
			curBitIndex = bitIndex;
		}
				
		bitSet.set(bitIndex);
		bufMap.put(sequenceId, buf);
		
		//检查连续性
		ChannelBuffer[] orderedMsgs = getOrderedMessages();
		
		//比较老的消息了，上次发送反馈的时候该消息还未收到
		//需要给远端发送反馈
		if(sequenceId < lastFeedbackSequenceId) {
			OrderedFeedback feedback = buildFeedback(sequenceId - 7, sequenceId + 8);
			feedback.setMsgArray(orderedMsgs);
			return feedback;
		}
		//够了16条消息了，需要反馈了
		else if(curSequenceId - lastFeedbackSequenceId >= 16) {
			OrderedFeedback feedback = buildFeedback(curSequenceId - 15, curSequenceId);
			lastFeedbackSequenceId = curSequenceId;
			feedback.setMsgArray(orderedMsgs);
			return feedback;
		}
		else if(orderedMsgs != null) {
			OrderedFeedback feedback = new OrderedFeedback(0, (short) 0);
			feedback.setMsgArray(orderedMsgs);
			return feedback;
		}
		
		return null;
	}
	
	//检查消息是否连续
	private ChannelBuffer[] getOrderedMessages() {
		int startIndex = getIndex(startSequenceId);
		if(startIndex < 0) {
			return null;
		}
		int endIndex = bitSet.nextClearBit(startIndex);
		//到头了，要从头开始
		if(endIndex == size) {
			int newEndIndex = bitSet.nextClearBit(0);
			//如果全部都是1
			if(newEndIndex == size) {
				endIndex = curBitIndex + 1;
			}
			else if(newEndIndex > 0) {
				endIndex = newEndIndex;
			}
		}
		while(endIndex < startIndex) {
			endIndex += size;
		}
		int endSequenceId = startSequenceId + endIndex - startIndex;
		
		if(endSequenceId <= startSequenceId) {
			return null;
		}
		
		ChannelBuffer[] msgArray = new ChannelBuffer[endSequenceId - startSequenceId];
		for (int i = 0; i < msgArray.length; i++) {
			int sequenceId = startSequenceId + i;
			msgArray[i] = bufMap.remove(sequenceId);
		}
		startSequenceId = endSequenceId;

		return msgArray;
	}
	
	/**
	 * 获取当前反馈
	 * 
	 * @return
	 */
	public OrderedFeedback getFeedback() {
		if(curSequenceId > lastFeedbackSequenceId) {
			lastFeedbackSequenceId = curSequenceId;
			return buildFeedback(curSequenceId - 15, curSequenceId);
		}
		else {
			return new OrderedFeedback(0, (short) 0);
		}
	}
	
	//构造从起始消息到结束消息的反馈
	private OrderedFeedback buildFeedback(int startSequenceId, int endSequenceId) {
		if(startSequenceId < 1) {
			startSequenceId = 1;
		}
		if(endSequenceId > curSequenceId) {
			endSequenceId = curSequenceId;
		}
		
		int bitFields = 0;
		for(int sequenceId = startSequenceId; sequenceId <= endSequenceId; sequenceId++) {
			if(getStatus(sequenceId)) {
				bitFields |= 1;
			}
			bitFields = bitFields << 1;
		}
		bitFields = bitFields >> 1;
		return new OrderedFeedback(endSequenceId, (short) bitFields);
	}
	
	//获取指定消息对应的索引
	private int getIndex(int sequenceId) {
		//太老的消息了，已经丢失状态了
		if(sequenceId < curSequenceId - size + 1) {
			return -1;
		}
		//太新的消息，尚未得知其状态
		if(sequenceId > curSequenceId) {
			return -1;
		}
		int bitIndex = sequenceId - curSequenceId + curBitIndex;
		while(bitIndex < 0) {
			bitIndex += size * 10;
		}
		if(bitIndex >= size) {
			bitIndex = bitIndex % size;
		}
		return bitIndex;
	}
	
	//获取指定消息的接收状态
	private boolean getStatus(int sequenceId) {
		int bitIndex = getIndex(sequenceId);
		if(bitIndex >= 0) {
			return bitSet.get(bitIndex);
		}
		return false;
	}
	
	//重置从当前index到目标消息id对应的index之间的bit为false。
	private void resetBits(int toSequenceId) {
		for(int sequenceId = curSequenceId + 1; sequenceId < toSequenceId; sequenceId++) {
			int bitIndex = sequenceId - curSequenceId + curBitIndex;
			while(bitIndex < 0) {
				bitIndex += size * 10;
			}
			if(bitIndex >= size) {
				bitIndex = bitIndex % size;
			}
			bitSet.clear(bitIndex);
		}
	}

	@Override
	public String toString() {
		return "MessageReceiveStatus [curSequenceId=" + curSequenceId
				+ ", curBitIndex=" + curBitIndex + ", bitSet=" + bitSet
				+ ", size=" + size + "]";
	}
}
