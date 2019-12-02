package com.ddpie.rudp.processor;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;

import com.ddpie.rudp.config.IRudpConfig;
import com.ddpie.rudp.constant.RudpConstants;
import com.ddpie.rudp.filter.IRudpFilter;
import com.ddpie.rudp.filter.IRudpFilterCallback;
import com.ddpie.rudp.filter.reliability.SessionReliabilityManager;
import com.ddpie.rudp.log.RudpLoggers;
import com.ddpie.rudp.message.IRudpMessage;
import com.ddpie.rudp.message.RudpCloseMessage;
import com.ddpie.rudp.message.RudpDownMessage;
import com.ddpie.rudp.message.RudpResumeMessage;
import com.ddpie.rudp.message.RudpSuspendMessage;
import com.ddpie.rudp.message.RudpUpMessage;
import com.ddpie.rudp.session.DirectWritter;
import com.ddpie.rudp.session.IRudpSession;
import com.ddpie.rudp.session.IRudpSessionManager;
import com.ddpie.rudp.session.RudpSession;
import com.ddpie.rudp.util.ChannelBufferHelper;
import com.ddpie.rudp.util.MessageHelper;

/**
 * RUDP消息处理器。<br>
 * 每个消息处理器都只有一个线程。<br>
 * 对于同一个远端{@link SocketAddress}采用同一个消息处理器处理，这个规则由{@link RudpProcessorManager}保证。
 * 
 * @author caobao
 *
 */
public class RudpProcessor implements Runnable, IRudpProcessor {
	private IRudpConfig rudpConfig;						//RUDP配置信息
	private BlockingQueue<IRudpMessage> messageQueue;	//消息队列
	private Set<IRudpSession> tickableSessions;			//需要执行滴答操作的会话
	private Thread processorThread;						//消息处理线程
	private volatile boolean isRunning;					//是否处于运行状态
	private String processorName;						//消息处理器名称
	private List<IRudpFilter> rudpFilters;				//RUDP Filter集合
	
	public RudpProcessor(IRudpConfig rudpConfig, List<IRudpFilter> rudpFilters, String processorName) {
		if(rudpFilters == null) {
			throw new IllegalArgumentException("rudpFilters cannot be null");
		}
		this.rudpConfig = rudpConfig;
		messageQueue = new LinkedBlockingQueue<IRudpMessage>();
		tickableSessions = new HashSet<IRudpSession>();
		this.rudpFilters = rudpFilters;
		this.processorName = processorName;
	}

	@Override
	public synchronized void start() {
		if(isRunning) {
			RudpLoggers.processorLogger.error(processorName + " has already started.");
			return;
		}
		isRunning = true;
		if(processorThread == null) {
			processorThread = new Thread(this, processorName);
		}
		processorThread.start();
	}

	@Override
	public void stop() {
		isRunning = false;
		processorThread = null;
	}
	
	@Override
	public void tickSession(IRudpSession session) {
		synchronized(tickableSessions) {
			tickableSessions.add(session);
		}
	}

	@Override
	public void put(IRudpMessage message) {
		if(isRunning == false) {
			RudpLoggers.processorLogger.warn("process is not running, cannot put message, message=" + message);
			return;
		}
		try {
			messageQueue.put(message);
		} catch (InterruptedException e) {
			RudpLoggers.processorLogger.error("put rudp message error", e);
		}
	}

	@Override
	public void run() {
		while(isRunning) {
			IRudpMessage message = null;
			try {
				message = messageQueue.poll(RudpConstants.SESSION_TICK_INTERVAL, TimeUnit.MILLISECONDS);
				process(message);
				tickSessions();
			}
			catch(Exception e) {
				if(message != null) {
					//记录日志，含远程地址和异常信息
					if(message.getRemoteAddress() != null) {
						RudpLoggers.processorLogger.error("process rudp message error from " + message.getRemoteAddress(), e);
					}
					//关闭会话
					if(message.getSession() != null) {
						message.getSession().close();
					}
				}
				else {
					RudpLoggers.processorLogger.error("process rudp message error", e);
				}
				e.printStackTrace();
			}
		}
	}
	
	//执行所有会话的滴答操作
	private void tickSessions() {
		synchronized(tickableSessions) {
			if(tickableSessions.size() == 0) {
				return;
			}
			for(Iterator<IRudpSession> it = tickableSessions.iterator(); it.hasNext(); ) {
				IRudpSession session = it.next();
				if(session instanceof RudpSession) {
					((RudpSession) session).tick();
				}
				it.remove();
			}
		}
	}

	//处理消息
	private void process(IRudpMessage message) throws Exception{
		if(message == null) {
			return;
		}
		switch(message.getType()) {
		case UP:
			processUpMessage((RudpUpMessage) message);
			break;
		case DOWN:
			processDownMessage((RudpDownMessage) message);
			break;
		case SUSPEND:
			processSuspendMessage((RudpSuspendMessage) message);
			break;
		case RESUME:
			processResumeMessage((RudpResumeMessage) message);
			break;
		case CLOSE:
			processCloseMessage((RudpCloseMessage) message);
			break;
		default:
			RudpLoggers.processorLogger.error("unkown message: " + message);
		}
	}
	
	//处理上行消息
	private void processUpMessage(RudpUpMessage message) {
		//不是ChannelBuffer，则抛弃消息
		if(message.getContent() instanceof ChannelBuffer == false) {
			RudpLoggers.processorLogger.error("rudp processor cannot process the message, msg=" + message);
			return;
		}
		
		//作为服务器处理
		if(rudpConfig.isServer()) {
			processUpMessageAsServer(message);
		}
		//作为客户端处理
		else {
			processUpMessageAsClient(message);
		}
	}
	
	/*
	 * 客户端处理上行消息的流程：
	 * 
	 * 1、如果远端地址不是服务器地址，则直接抛弃；
	 * 2、如果会话已建立，则直接将数据抛给Filter集合；
	 * 3、如果会话未建立，则建立会话，然后把数据抛给Filter集合。
	 */
	private void processUpMessageAsClient(RudpUpMessage message) {
		ChannelBuffer buf = (ChannelBuffer) message.getContent();
		
		//关闭消息，则立即关闭会话
		if(MessageHelper.isClose(buf)) {
			IRudpSession session = message.getSession();
			if(session != null && !session.isClosed()) {
				if(RudpLoggers.sessionLogger.isInfoEnabled()) {
					RudpLoggers.sessionLogger.info("receive close message from " + session.getRemoteAddress() + ", so close the session");
				}
				session.close();
			}
			return;
		}
		
		if(rudpConfig.getServerAddress().equals(message.getRemoteAddress()) == false) {
			return;
		}

		//记录日志
		if(RudpLoggers.heartbeatUpLogger.isDebugEnabled()) {
			if(MessageHelper.isHeartbeat((ChannelBuffer) message.getContent())) {
				RudpLoggers.heartbeatUpLogger.debug(
						"received heartbeat from " + message.getRemoteAddress() + ", " + ChannelBufferHelper.toString(buf));
			}
			else if(MessageHelper.isHandshake((ChannelBuffer) message.getContent())) {
				RudpLoggers.heartbeatUpLogger.debug(
						"received handshake from " + message.getRemoteAddress() + ", " + ChannelBufferHelper.toString(buf));
			}
		}

		//如果session处于连接状态
		IRudpSessionManager sessionMgr = rudpConfig.getSessionManager();
		if(sessionMgr.isConnected(message.getRemoteAddress())) {
			IRudpSession session = sessionMgr.getRudpSession(message.getRemoteAddress());
			if(MessageHelper.isHeartbeat(buf) && session instanceof RudpSession) {
				((RudpSession) session).onHeartbeatReceived();
			}
			else {
				if(RudpLoggers.businessUpLogger.isDebugEnabled()) {
					RudpLoggers.businessUpLogger.debug(
							"received business message from " + message.getRemoteAddress() + 
							", msg=" + ChannelBufferHelper.toString(buf));
				}
				if(session instanceof RudpSession) {
					((RudpSession) session).onMessageReceived();
				}
			}
			notifyMessageReceived(session, message, message.getCallback());
			message.getCallback().callback(message.getContent());
			return;
		}
		
		//创建会话并连接
		RudpSession session = new RudpSession(rudpConfig, message.getChannel(), message.getRemoteAddress());
		session.onHeartbeatReceived();
		sessionMgr.addSession(session);

		//通知监听器列表
		notifySessionOpened(session);
		
		//抛给Filter集合
		if(MessageHelper.isHeartbeat(buf) == false) {
			if(RudpLoggers.businessUpLogger.isDebugEnabled()) {
				RudpLoggers.businessUpLogger.debug(
						"received business message from " + message.getRemoteAddress() + 
						", msg=" + ChannelBufferHelper.toString(buf));
			}
		}
		notifyMessageReceived(session, message, message.getCallback());
		message.getCallback().callback(message.getContent());
	}
	
	/*
	 * 服务器处理上行消息的流程：
	 * 
	 * 1、如果会话已建立，则直接将数据抛给Filter集合；
	 * 2、如果会话未建立，则判断收到的消息是否是握手。如果是握手则建立会话，否则抛弃消息。
	 */
	private void processUpMessageAsServer(RudpUpMessage message) {
		ChannelBuffer buf = (ChannelBuffer) message.getContent();
		
		//关闭消息，则立即挂起会话
		if(MessageHelper.isClose(buf)) {
			IRudpSession session = message.getSession();
			if(session != null && !session.isClosed() && !session.isSuspended()) {
				if(RudpLoggers.sessionLogger.isInfoEnabled()) {
					RudpLoggers.sessionLogger.info("receive close message from " + session.getRemoteAddress() + ", so suspend the session");
				}
				((RudpSession) session).suspend();
			}
			return;
		}
		
		IRudpSessionManager sessionMgr = rudpConfig.getSessionManager();
		//如果session处于连接或挂起状态
		if(sessionMgr.isConnected(message.getRemoteAddress())) {
			IRudpSession session = sessionMgr.getRudpSession(message.getRemoteAddress());
			//挂起状态
			if(session.isSuspended()) {
				//不是握手消息，则抛弃消息
				if(MessageHelper.isHandshake(buf) == false) {
					return;
				}
				if(RudpLoggers.sessionLogger.isInfoEnabled()) {
					RudpLoggers.sessionLogger.info("session resumed, session: " + session);
				}
				((RudpSession) session).resume();
			}
			//记录日志
			if(RudpLoggers.heartbeatUpLogger.isDebugEnabled()) {
				if(MessageHelper.isHeartbeat((ChannelBuffer) message.getContent())) {
					RudpLoggers.heartbeatUpLogger.debug(
							"received heartbeat from " + message.getRemoteAddress() + ", " + ChannelBufferHelper.toString(buf));
				}
				else if(MessageHelper.isHandshake((ChannelBuffer) message.getContent())) {
					RudpLoggers.heartbeatUpLogger.debug(
							"received handshake from " + message.getRemoteAddress() + ", " + ChannelBufferHelper.toString(buf));
				}
			}

			//心跳消息
			if(MessageHelper.isHeartbeat(buf)) {
				if(session instanceof RudpSession) {
					((RudpSession) session).onHeartbeatReceived();
				}
				notifyMessageReceived(session, message, message.getCallback());
			}
			//握手消息，有可能是客户端挂起恢复，反馈心跳
			else if(MessageHelper.isHandshake(buf)) {
				if(RudpLoggers.heartbeatUpLogger.isDebugEnabled()) {
					RudpLoggers.heartbeatUpLogger.debug(
							"connection is already established, received useless handshake, " + message.getRemoteAddress());
				}
				for (int i = 0; i < RudpConstants.HANDSHAKE_RESPONSE_MESSAGE_COUNT; i++) {
					SessionReliabilityManager mgr = (SessionReliabilityManager) session.getAttribute("reliability");
					if(mgr != null) {
						mgr.sendHeartbeat();
					}
				}
			}
			//业务消息
			else {
				if(RudpLoggers.businessUpLogger.isDebugEnabled()) {
					RudpLoggers.businessUpLogger.debug(
							"received business message from " + message.getRemoteAddress() + 
							", msg=" + ChannelBufferHelper.toString(buf));
				}
				notifyMessageReceived(session, message, message.getCallback());
				if(session instanceof RudpSession) {
					((RudpSession) session).onMessageReceived();
				}
			}
			return;
		}
		
		//不是握手消息，则抛弃消息
		if(MessageHelper.isHandshake(buf) == false) {
			return;
		}
		
		//是握手消息，创建会话并连接
		RudpSession newSession = new RudpSession(rudpConfig, message.getChannel(), message.getRemoteAddress());
		newSession.onHeartbeatReceived();
		sessionMgr.addSession(newSession);
		
		//通知监听器列表
		notifySessionOpened(newSession);
		
		//给客户端发送连接响应
		for (int i = 0; i < RudpConstants.HANDSHAKE_RESPONSE_MESSAGE_COUNT; i++) {
			MessageHelper.sendHeartbeat(newSession, 0, (short)0, 0, (short)0);
		}
	}

	//处理下行消息
	private void processDownMessage(RudpDownMessage message) {
		IRudpSessionManager sessionMgr = rudpConfig.getSessionManager();
		IRudpSession session = sessionMgr.getRudpSession(message.getRemoteAddress());
		if(session != null) {
			notifyWriteRequested(session, message);
		}
		else {
			RudpLoggers.processorLogger.warn("cannot find session when process down message " + message);
		}
	}

	//处理内部关闭消息
	private void processCloseMessage(RudpCloseMessage message) {
		notifySessionClosed(message.getSession());
	}
	
	//处理内部挂起消息
	private void processSuspendMessage(RudpSuspendMessage message) {
		notifySessionSuspended(message.getSession());
	}
	
	//处理内部恢复消息
	private void processResumeMessage(RudpResumeMessage message) {
		notifySessionResumed(message.getSession());
	}
	
	//通知所有RUDP Filter，一个RUDP连接打开了。
	private void notifySessionOpened(IRudpSession session) {
		for (IRudpFilter rudpFilter : rudpFilters) {
			rudpFilter.sessionOpened(session);
		}
	}
	
	//通知所有RUDP Filter，一个RUDP会话挂起了。
	private void notifySessionSuspended(IRudpSession session) {
		for (IRudpFilter rudpFilter : rudpFilters) {
			rudpFilter.sessionSuspended(session);
		}
	}
	
	//通知所有RUDP Filter，一个RUDP会话恢复了。
	private void notifySessionResumed(IRudpSession session) {
		for (IRudpFilter rudpFilter : rudpFilters) {
			rudpFilter.sessionResumed(session);
		}
	}
	
	//通知所有RUDP Filter，一个RUDP连接关闭了。
	private void notifySessionClosed(IRudpSession session) {
		for (IRudpFilter rudpFilter : rudpFilters) {
			rudpFilter.sessionClosed(session);
		}
	}
	
	//通知所有RUDP Filter，一个消息收到了。
	private void notifyMessageReceived(IRudpSession session, RudpUpMessage message, IRudpFilterCallback callback) {
		notifyOneFilterMessageReceived(0, session, message.getContent(), callback);
	}
	
	//从当前Filter节点开始，根据操作结果，递归向后面的节点传递消息
	private void notifyOneFilterMessageReceived(int filterIndex, IRudpSession session, Object messageContent, IRudpFilterCallback callback) {
		if(filterIndex < 0 || filterIndex >= rudpFilters.size()) {
			return;
		}
		IRudpFilter rudpFilter = rudpFilters.get(filterIndex);
		Object resultContent = rudpFilter.messageReceived(session, messageContent);
		//当前Filter禁止消息继续传递，直接返回
		if(resultContent == null) {
			return;
		}

		//最后一个Filter
		if(filterIndex == rudpFilters.size() - 1) {
			//若结果是数组，则依次调用回调
			if(resultContent instanceof Object[]) {
				Object[] resultContentArray = (Object[]) resultContent;
				for (int i = 0; i < resultContentArray.length; i++) {
					callback.callback(resultContentArray[i]);
				}
			}
			//不是数组，直接调用回调
			else {
				callback.callback(resultContent);
			}
		}
		//不是最后一个Filter，需要继续传递
		else {
			//若结果是数组，则依次调用下个Filter
			if(resultContent instanceof Object[]) {
				Object[] resultContentArray = (Object[]) resultContent;
				for (int i = 0; i < resultContentArray.length; i++) {
					notifyOneFilterMessageReceived(filterIndex + 1, session, resultContentArray[i], callback);
				}
			}
			//不是数组，直接调用下个Filter
			else {
				notifyOneFilterMessageReceived(filterIndex + 1, session, resultContent, callback);
			}
		}
	}

	//通知所有RUDP Filter，一个写请求。
	private void notifyWriteRequested(IRudpSession session, RudpDownMessage message) {
		notifyOneFilterWriteRequested(rudpFilters.size() - 1, session, message.getContent());
	}
	
	//从当前Filter节点开始，根据操作结果，递归向后面的节点传递消息
	private void notifyOneFilterWriteRequested(int filterIndex, IRudpSession session, Object messageContent) {
		if(filterIndex < 0 || filterIndex >= rudpFilters.size()) {
			return;
		}
		IRudpFilter rudpFilter = rudpFilters.get(filterIndex);
		Object resultContent = rudpFilter.writeRequested(session, messageContent);
		//当前Filter禁止消息继续传递，直接返回
		if(resultContent == null) {
			return;
		}
		
		//最后一个Filter
		if(filterIndex == 0) {
			//若结果是数组，则依次发送消息
			if(resultContent instanceof Object[]) {
				Object[] resultContentArray = (Object[]) resultContent;
				for (int i = 0; i < resultContentArray.length; i++) {
					DirectWritter.write(session, resultContentArray[i]);
					if(RudpLoggers.businessDownLogger.isDebugEnabled()) {
						ChannelBuffer buf = (ChannelBuffer) resultContentArray[i];
						RudpLoggers.businessDownLogger.debug(
								"sent business message to " + session.getRemoteAddress() + ", msg=" + ChannelBufferHelper.toString(buf));
					}
				}
			}
			//不是数组，直接发送消息
			else {
				DirectWritter.write(session, resultContent);
				if(RudpLoggers.businessDownLogger.isDebugEnabled()) {
					ChannelBuffer buf = (ChannelBuffer) resultContent;
					RudpLoggers.businessDownLogger.debug(
							"sent business message to " + session.getRemoteAddress() + ", msg=" + ChannelBufferHelper.toString(buf));
				}
			}
		}
		//不是最后一个Filter，需要继续传递
		else {
			//若结果是数组，则依次调用下个Filter
			if(resultContent instanceof Object[]) {
				Object[] resultContentArray = (Object[]) resultContent;
				for (int i = 0; i < resultContentArray.length; i++) {
					notifyOneFilterWriteRequested(filterIndex - 1, session, resultContentArray[i]);
				}
			}
			//不是数组，直接调用下个Filter
			else {
				notifyOneFilterWriteRequested(filterIndex - 1, session, resultContent);
			}
		}
	}
	
}
