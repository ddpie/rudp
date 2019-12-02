package com.ddpie.rudp.processor;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.netty.channel.Channel;

import com.ddpie.rudp.config.IRudpConfig;
import com.ddpie.rudp.filter.IRudpFilter;
import com.ddpie.rudp.filter.IRudpFilterCallback;
import com.ddpie.rudp.message.IRudpMessage;
import com.ddpie.rudp.message.RudpMessages;
import com.ddpie.rudp.session.IRudpSession;

/**
 * 负责RUDP消息处理器的管理工作。
 * 
 * @author caobao
 *
 */
public class RudpProcessorManager implements IRudpProcessorManager {
	private IRudpConfig rudpConfig;			//RUDP配置信息
	private IRudpProcessor[] processors;	//RUDP消息处理器集合
	private List<IRudpFilter> rudpFilters;	//RUDP Filter集合
	
	/**
	 * 根据消息处理器的数量构造管理器。
	 * 
	 * @param processorCount 消息处理器数量
	 */
	public RudpProcessorManager(IRudpConfig rudpConfig, int processorCount) {
		if(processorCount <= 0) {
			throw new IllegalArgumentException(
					"processorCount must more than zero, processorCount=" + processorCount);
		}
		this.rudpConfig = rudpConfig;
		rudpFilters = new CopyOnWriteArrayList<IRudpFilter>();
		processors = new IRudpProcessor[processorCount];
		for (int i = 0; i < processors.length; i++) {
			processors[i] = new RudpProcessor(rudpConfig, rudpFilters, 
					"RUDP Message Processor " + (i + 1) + "/" + processorCount);
		}
	}
	
	@Override
	public void addRudpFilter(IRudpFilter rudpFilter) {
		rudpFilters.add(rudpFilter);
	}
	
	/**
	 * 启动所有的消息处理器
	 */
	public void start() {
		if(processors != null) {
			for (IRudpProcessor processor : processors) {
				processor.start();
			}
		}
	}
	
	/**
	 * 停止所有的消息处理器
	 */
	public void stop() {
		if(processors != null) {
			for (IRudpProcessor processor : processors) {
				processor.stop();
			}
		}
	}
	
	@Override
	public void tickSession(IRudpSession session) {
		IRudpProcessor processor = pickAProcessor(session.getRemoteAddress());
		processor.tickSession(session);
	}
	
	@Override
	public void onMessageReceived(Channel channel, SocketAddress remoteAddress, Object message, IRudpFilterCallback callback) {
		IRudpMessage rudpMessage = RudpMessages.newUpMessage(
				rudpConfig.getSessionManager().getRudpSession(remoteAddress), channel, remoteAddress, message, callback);
		IRudpProcessor processor = pickAProcessor(remoteAddress);
		processor.put(rudpMessage);
	}
	
	@Override
	public void onWriteRequested(Channel channel, SocketAddress remoteAddress, Object message) {
		IRudpMessage rudpMessage = RudpMessages.newDownMessage(
				rudpConfig.getSessionManager().getRudpSession(remoteAddress), channel, remoteAddress, message);
		IRudpProcessor processor = pickAProcessor(remoteAddress);
		processor.put(rudpMessage);
	}
	
	@Override
	public void onSessionClosed(IRudpSession session, SocketAddress remoteAddress) {
		IRudpMessage rudpMessage = RudpMessages.newCloseMessage(session, remoteAddress);
		IRudpProcessor processor = pickAProcessor(remoteAddress);
		processor.put(rudpMessage);
	}
	
	@Override
	public void onSessionSuspended(IRudpSession session, SocketAddress remoteAddress) {
		IRudpMessage rudpMessage = RudpMessages.newSuspendMessage(session, remoteAddress);
		IRudpProcessor processor = pickAProcessor(remoteAddress);
		processor.put(rudpMessage);
	}
	
	@Override
	public void onSessionResumed(IRudpSession session, SocketAddress remoteAddress) {
		IRudpMessage rudpMessage = RudpMessages.newResumeMessage(session, remoteAddress);
		IRudpProcessor processor = pickAProcessor(remoteAddress);
		processor.put(rudpMessage);
	}
	
	//根据远端地址选择一个消息处理器
	private IRudpProcessor pickAProcessor(SocketAddress remoteAddress) {
		if(remoteAddress == null) {
			throw new IllegalArgumentException("remoteAddress cannot be null");
		}
		//如果只有一个消息处理器，则直接返回
		if(processors.length == 1) {
			return processors[0];
		}
		//根据hash与processCount求余，选择消息处理器
		int hash = remoteAddress.hashCode();
		int index = Math.abs(hash % processors.length);
		return processors[index];
	}
}
