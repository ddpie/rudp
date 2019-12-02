package com.ddpie.rudp.filter.fragment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import com.ddpie.rudp.constant.RudpConstants;
import com.ddpie.rudp.log.RudpLoggers;
import com.ddpie.rudp.session.IRudpSession;
import com.ddpie.rudp.session.RudpSession;
import com.ddpie.rudp.util.ChannelBufferHelper;
import com.ddpie.rudp.util.MessageHelper;

/**
 * RUDP封包管理器。<br>
 * 将隶属于一个大数据的多个小数据包，合并成一个大数据。
 * 与RUDP会话一一对应。
 * 
 * @author caobao
 *
 */
public class SessionFragmentManager {
	private IRudpSession session;											//关联的RUDP会话
	private Map<Integer, MessageFragment> orderedFragmentMap;		//有序消息的封包表<第一个包的序号, 封包类>
	private Map<Integer, MessageFragment> disorderedFragmentMap;	//无序消息的封包表<第一个包的序号, 封包类>

	/**
	 * 构造风暴管理器。
	 * 
	 * @param session
	 */
	public SessionFragmentManager(IRudpSession session) {
		this.session = session;
		orderedFragmentMap = new ConcurrentHashMap<Integer, SessionFragmentManager.MessageFragment>();
		disorderedFragmentMap = new ConcurrentHashMap<Integer, SessionFragmentManager.MessageFragment>();
		initSessionScanner();
	}
	
	private void initSessionScanner() {
		if(session instanceof RudpSession == false) {
			return;
		}
		((RudpSession) session).addTickListener(new Runnable() {
			@Override
			public void run() {
				if(orderedFragmentMap.size() + disorderedFragmentMap.size() > RudpConstants.MAX_FRAGMENT_CACHE_SIZE) {
					RudpLoggers.rootLogger.warn("fragment cache too large, so close the session, " + session);
					session.close();
				}
			}
		});
	}

	//获取本组封包中第一个消息包的消息序号
	private int getFirstFragmentSequenceId(ChannelBuffer message) {
		int currentSequenceId = MessageHelper.getSequenceId(message);
		int currentFragmentIndex = MessageHelper.getFragmentIndex(message);
		return currentSequenceId - currentFragmentIndex;
	}
	
	/**
	 * 收到单个消息时的回调。
	 * 
	 * @param message
	 * @return
	 */
	public ChannelBuffer onMessageReceived(ChannelBuffer message) {
		//没有封包，无需合并
		int fragmentCount = MessageHelper.getFragmentCount(message);
		if(fragmentCount <= 0) {
			return message;
		}
		
		int firstSequenceId = getFirstFragmentSequenceId(message);
		//有序消息
		if(MessageHelper.isReliableOrdered(message)) {
			MessageFragment messageFragment = orderedFragmentMap.get(firstSequenceId);
			if(messageFragment == null) {
				messageFragment = new MessageFragment(message);
				orderedFragmentMap.put(firstSequenceId, messageFragment);
				return null;
			}
			else {
				ChannelBuffer mergedMessage = messageFragment.addMessage(message);
				if(mergedMessage != null) {
					orderedFragmentMap.remove(firstSequenceId);
					if(RudpLoggers.businessUpLogger.isDebugEnabled()) {
						RudpLoggers.businessUpLogger.debug(
								"merged a ordered message from " + session.getRemoteAddress() + ", msg=" + ChannelBufferHelper.toString(mergedMessage));
					}
				}
				return mergedMessage;
			}
		}
		//无序消息
		else {
			MessageFragment messageFragment = disorderedFragmentMap.get(firstSequenceId);
			if(messageFragment == null) {
				messageFragment = new MessageFragment(message);
				disorderedFragmentMap.put(firstSequenceId, messageFragment);
				return null;
			}
			else {
				ChannelBuffer mergedMessage = messageFragment.addMessage(message);
				if(mergedMessage != null) {
					disorderedFragmentMap.remove(firstSequenceId);
					if(RudpLoggers.businessUpLogger.isDebugEnabled()) {
						RudpLoggers.businessUpLogger.debug(
								"merged a disordered message from " + session.getRemoteAddress() + ", msg=" + ChannelBufferHelper.toString(mergedMessage));
					}
				}
				return mergedMessage;
			}
		}
	}
	
	/**
	 * 消息封包抽象类。
	 * 
	 * @author caobao
	 *
	 */
	private static class MessageFragment {
		private ChannelBuffer[] bufArray;	//消息缓存
		private int currentCount = 0;		//当前消息数量
		private int totalCount = 0;			//消息总数量
		
		public MessageFragment(ChannelBuffer msg) {
			totalCount = MessageHelper.getFragmentCount(msg);
			bufArray = new ChannelBuffer[totalCount];
			addMessage(msg);
		}
		
		/**
		 * 增加一个消息
		 * 
		 * @param msg
		 * @return
		 */
		public ChannelBuffer addMessage(ChannelBuffer msg) {
			int fragmentIndex = MessageHelper.getFragmentIndex(msg);
			if(bufArray[fragmentIndex] != null) {
				return null;
			}
			bufArray[fragmentIndex] = msg;
			currentCount++;
			if(currentCount == totalCount) {
				return mergeFragments();
			}
			return null;
		}

		//合并封包
		private ChannelBuffer mergeFragments() {
			int headLength = MessageHelper.MESSAGE_HEAD_LENGTH;
			ChannelBuffer[] slicedBuffers = new ChannelBuffer[bufArray.length];
			for (int i = 0; i < slicedBuffers.length; i++) {
				if(i == 0) {
					slicedBuffers[0] = bufArray[0];
				}
				else {
					slicedBuffers[i] = bufArray[i].slice(
							bufArray[i].readerIndex() + headLength, 
							bufArray[i].readableBytes() - headLength);
				}
			}
			//生成消息
			ChannelBuffer wholeMsg = ChannelBuffers.wrappedBuffer(slicedBuffers);
			//清空封包数量和封包索引
			MessageHelper.setFragmentCount(wholeMsg, 0);
			MessageHelper.setFragmentIndex(wholeMsg, 0);
			return wholeMsg;
		}
	}
	
}
