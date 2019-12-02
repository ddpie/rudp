package com.ddpie.rudp.filter.reliability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.buffer.ChannelBuffer;

import com.ddpie.rudp.constant.RudpConstants;
import com.ddpie.rudp.log.RudpLoggers;
import com.ddpie.rudp.session.DirectWritter;
import com.ddpie.rudp.session.IRudpSession;
import com.ddpie.rudp.session.RudpSession;
import com.ddpie.rudp.util.ChannelBufferHelper;
import com.ddpie.rudp.util.MessageHelper;

/**
 * 维持RUDP可靠性的管理器。<br>
 * 包括本地发送消息的状态和收到远端消息的状态。
 * 与RUDP会话一一对应。
 * 
 * @author caobao
 *
 */
public class SessionReliabilityManager {
	private IRudpSession session;							//绑定的RUDP会话
	private Map<Integer, MessageEntry> orderedDownMap;		//可靠且连续消息发送缓存<消息序号, 消息>
	private Map<Integer, MessageEntry> disorderedDownMap;	//可靠但不连续消息发送缓存<消息序号, 消息>
	private OrderedUpStatus orderedUpStatus;				//可靠且连续消息的接收状态
	private DisorderedUpStatus disorderedUpStatus;			//可靠但不连续消息的接收状态
	private int orderedWriteSequenceId = 1;					//可靠且连续消息的当前序号
	private int disorderedWriteSequenceId = 1;				//可靠但不连续消息的当前序号
	
	/**
	 * 根据RUDP会话构造维持其可靠性的管理器。
	 * 
	 * @param session
	 */
	public SessionReliabilityManager(IRudpSession session) {
		this.session = session;
		orderedDownMap = new ConcurrentHashMap<Integer, MessageEntry>();
		disorderedDownMap = new ConcurrentHashMap<Integer, MessageEntry>();
		orderedUpStatus = new OrderedUpStatus(RudpConstants.RECEIVE_STATUS_MAX_MESSAGE_COUNT);
		disorderedUpStatus = new DisorderedUpStatus(RudpConstants.RECEIVE_STATUS_MAX_MESSAGE_COUNT);
		initSessionScanner();
	}
	
	private void initSessionScanner() {
		if(session instanceof RudpSession == false) {
			return;
		}
		((RudpSession) session).addTickListener(new Runnable() {
			@Override
			public void run() {
				//挂起状态的会话，不检测超时、缓冲区溢出等
				if(session.isSuspended()) {
					return;
				}
				//扫描缓存中的消息，并将超时的消息重发
				scanTimeoutMessages();
				
				//检测发送缓存是否超过容量，若超过容量，则断连接
				checkWriteCacheSize();
				
				//检测读取缓存是否超过容量，若超过容量，则断连接
				checkReadCacheSize();
			}
		});
	}
	
	//检测发送缓存是否超过容量，若超过容量，则断连接
	private void checkWriteCacheSize() {
		if(orderedDownMap.size() + disorderedDownMap.size() > RudpConstants.MAX_WRITE_CACHE_SIZE) {
			RudpLoggers.rootLogger.warn("write cache too large, so close the session, " + session);
			session.close();
		}
	}
	
	//检测读取缓存是否超过容量，若超过容量，则断连接
	private void checkReadCacheSize() {
		if(orderedUpStatus.getCacheSize() > RudpConstants.MAX_READ_CACHE_SIZE) {
			RudpLoggers.rootLogger.warn("read cache too large, so close the session, " + session);
			session.close();
		}
	}
	
	//扫描缓存中的消息，并将超时的消息重发
	private void scanTimeoutMessages() {
		long currentTime = System.currentTimeMillis();
		
		//有序消息
		for(MessageEntry messageEntry : orderedDownMap.values()) {
			long expireTime = messageEntry.expireTime;
			ChannelBuffer message = messageEntry.message;
			//超时了
			if(currentTime >= expireTime) {
				if(RudpLoggers.reliabilityLogger.isDebugEnabled()) {
					RudpLoggers.reliabilityLogger.debug("ordered message write timeout, so retransmit, " + ChannelBufferHelper.toString(message));
				}
				messageEntry.expireTime = currentTime + RudpConstants.MESSAGE_RETRANSMIT_TIME;
				messageEntry.expireCount++;
				if(messageEntry.expireCount >= RudpConstants.MESSAGE_RETRANSMIT_MAX_COUNT) {
					RudpLoggers.processorLogger.error(
							"ordered message retransmit max count has been reached, so suspend the session, session=" + session + 
							", message=" + ChannelBufferHelper.toString(message));
					((RudpSession) session).suspend();
					return;
				}
				DirectWritter.write(session, message);
			}
		}
		
		//无序消息
		for(MessageEntry messageEntry : disorderedDownMap.values()) {
			long expireTime = messageEntry.expireTime;
			ChannelBuffer message = messageEntry.message;
			//超时了
			if(currentTime >= expireTime) {
				if(RudpLoggers.reliabilityLogger.isDebugEnabled()) {
					RudpLoggers.reliabilityLogger.debug("disordered message write timeout, so retransmit, " + ChannelBufferHelper.toString(message));
				}
				messageEntry.expireTime = currentTime + RudpConstants.MESSAGE_RETRANSMIT_TIME;
				messageEntry.expireCount++;
				if(messageEntry.expireCount >= RudpConstants.MESSAGE_RETRANSMIT_MAX_COUNT) {
					RudpLoggers.processorLogger.error(
							"disordered message retransmit max count has been reached, so close the session, session=" + session + 
							", message=" + ChannelBufferHelper.toString(message));
					session.close();
					return;
				}
				DirectWritter.write(session, message);
			}
		}
		
	}

	/**
	 * 当收到远端可靠且连续消息时的回调。
	 * 
	 * @param message
	 * @return
	 */
	public ChannelBuffer[] onOrderedReceived(ChannelBuffer message) {
		int sequenceId = MessageHelper.getSequenceId(message);
		OrderedFeedback feedback = orderedUpStatus.onMessageReceived(sequenceId, message);
		if(feedback == null) {
			return null;
		}
		
		int feedbackSequenceId = feedback.getSequenceId();
		short feedbackBitfields = feedback.getBitFields();
		ChannelBuffer[] msgArray = feedback.getMsgArray();
		
		//需要向远端反馈心跳
		if(feedbackSequenceId != 0) {
			MessageHelper.sendHeartbeat(session, feedbackSequenceId, feedbackBitfields, 0, (short) 0);
		}
		
		return msgArray;
	}
	
	/**
	 * 当收到远端可靠但不连续消息时的回调。
	 * 
	 * @param message
	 * @return
	 */
	public ChannelBuffer onDisorderedReceived(ChannelBuffer message) {
		int sequenceId = MessageHelper.getSequenceId(message);
		DisorderedFeedback feedback = disorderedUpStatus.onMessageReceived(sequenceId);
		if(feedback == null) {
			return message;
		}
		
		int feedbackSequenceId = feedback.getSequenceId();
		short feedbackBitfields = feedback.getBitFields();

		//需要向远端反馈心跳
		if(feedbackSequenceId != 0) {
			MessageHelper.sendHeartbeat(session, 0, (short) 0, feedbackSequenceId, feedbackBitfields);
		}
		
		//重复消息，不能跑到应用层
		if(feedback.isDuplicatedMsg()) {
			if(RudpLoggers.businessUpLogger.isDebugEnabled()) {
				RudpLoggers.businessUpLogger.debug(
						"duplicated disordered message received, ignore it, msg=" + ChannelBufferHelper.toString(message));
			}
			return null;
		}
		return message;
	}
	
	/**
	 * 收到心跳消息时的回调
	 * 
	 * @param heartbeat
	 */
	public void onHeartbeat(ChannelBuffer heartbeat) {
		int orderedSequenceId = heartbeat.getInt(1);
		short orderedBitfields = heartbeat.getShort(5);
		int disorderedSequenceId = heartbeat.getInt(7);
		short disorderedBitfields = heartbeat.getShort(11);
		
		onOrderedHearbeat(orderedSequenceId, orderedBitfields);
		onDisorderedHearbeat(disorderedSequenceId, disorderedBitfields);
	}
	
	//当收到远端可靠且连续消息的心跳时的回调
	private void onOrderedHearbeat(int sequenceId, short bitfields) {
		if(sequenceId <= 0) {
			return;
		}
		List<Integer> sequenceIdList = getFeedbackSequenceIds(sequenceId, bitfields);
		if(RudpLoggers.reliabilityLogger.isDebugEnabled()) {
			RudpLoggers.reliabilityLogger.debug(session.getRemoteAddress() + " has received ordered messages: " + sequenceIdList);
		}
		for (Integer id : sequenceIdList) {
			orderedDownMap.remove(id);
		}
	}
	
	//当收到远端可靠但不连续消息的心跳时的回调
	private void onDisorderedHearbeat(int sequenceId, short bitfields) {
		if(sequenceId <= 0) {
			return;
		}
		List<Integer> sequenceIdList = getFeedbackSequenceIds(sequenceId, bitfields);
		if(RudpLoggers.reliabilityLogger.isDebugEnabled()) {
			RudpLoggers.reliabilityLogger.debug(session.getRemoteAddress() + " has received disordered messages: " + sequenceIdList);
		}
		for (Integer id : sequenceIdList) {
			disorderedDownMap.remove(id);
		}
	}
	
	//获取反馈中的消息ID列表
	private List<Integer> getFeedbackSequenceIds(int sequenceId, short bitfields) {
		List<Integer> result = new ArrayList<Integer>(16);
		for (int i = 0; i < 16; i++) {
			if((bitfields & 1) != 0) {
				result.add(sequenceId - i);
			}
			bitfields = (short) (bitfields >> 1);
		}
		return result;
	}
	
	/**
	 * 发送有序消息时的回调。
	 * 
	 * @param message
	 */
	public void onOrderedWrite(ChannelBuffer message) {
		/*
		 * 过程：
		 * 1、读取消息的序号，如果不是0，则说明是重发的数据；
		 * 2、如有必要（重发数据无需更新消息序号），则更新消息序号；
		 * 3、将消息加入到重发缓冲区。
		 */
		int sequenceId = MessageHelper.getSequenceId(message);
		if(sequenceId == 0) {
			//更新消息序号
			sequenceId = orderedWriteSequenceId++;
			MessageHelper.setSequenceId(message, sequenceId);
		}
		
		//将消息加入到重发缓冲区
		long expireTime = System.currentTimeMillis() + RudpConstants.MESSAGE_RETRANSMIT_TIME;
		orderedDownMap.put(sequenceId, new MessageEntry(message, expireTime));
	}
	
	/**
	 * 发送无序消息时的回调。
	 * 
	 * @param message
	 */
	public void onDisorderedWrite(ChannelBuffer message) {
		/*
		 * 过程：
		 * 1、读取消息的序号，如果不是0，则说明是重发的数据；
		 * 2、如有必要（重发数据无需更新消息序号），则更新消息序号；
		 * 3、将消息加入到重发缓冲区。
		 */
		int sequenceId = MessageHelper.getSequenceId(message);
		if(sequenceId == 0) {
			//更新消息序号
			sequenceId = disorderedWriteSequenceId++;
			MessageHelper.setSequenceId(message, sequenceId);
		}
		
		//将消息加入到重发缓冲区
		long expireTime = System.currentTimeMillis() + RudpConstants.MESSAGE_RETRANSMIT_TIME;
		disorderedDownMap.put(sequenceId, new MessageEntry(message, expireTime));
	}
	
	/**
	 * 向远端发送心跳。
	 * 
	 */
	public void sendHeartbeat() {
		OrderedFeedback orderedFeedback = orderedUpStatus.getFeedback();
		DisorderedFeedback disorderedFeedback = disorderedUpStatus.getFeedback();
		int orderedSequence = orderedFeedback.getSequenceId();
		short orderedBitfield = orderedFeedback.getBitFields();
		int disorderedSequence = disorderedFeedback.getSequenceId();
		short disorderedBitfield = disorderedFeedback.getBitFields();
		if(orderedSequence < 0) {
			orderedSequence = 0;
		}
		if(disorderedSequence < 0) {
			disorderedSequence = 0;
		}
		MessageHelper.sendHeartbeat(session, orderedSequence, orderedBitfield, disorderedSequence, disorderedBitfield);
	}
	
	/**
	 * 消息和超时时间的映射关系类
	 * 
	 * @author caobao
	 *
	 */
	private static final class MessageEntry {
		private ChannelBuffer message;	//消息
		private long expireTime;		//超时时间
		private long expireCount;		//超时次数
		
		public MessageEntry(ChannelBuffer buf, long expireTime) {
			super();
			this.message = buf;
			this.expireTime = expireTime;
			this.expireCount = 0;
		}
		
		@Override
		public String toString() {
			return "Entry [buf=" + ChannelBufferHelper.toString(message) + ", expireTime=" + expireTime + ", expireCount=" + expireCount +"]";
		}
		
	}
	
}
