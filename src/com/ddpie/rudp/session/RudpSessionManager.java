package com.ddpie.rudp.session;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.Channel;

import com.ddpie.rudp.config.IRudpConfig;
import com.ddpie.rudp.constant.RudpConstants;
import com.ddpie.rudp.filter.reliability.SessionReliabilityManager;
import com.ddpie.rudp.log.RudpLoggers;
import com.ddpie.rudp.util.MessageHelper;

/**
 * RUDP会话管理器，主要负责维护RUDP会话的连接状态。
 * 
 * @author caobao
 *
 */
public class RudpSessionManager implements IRudpSessionManager, Runnable {
	//RUDP配置信息
	private IRudpConfig rudpConfig;
	//处于连接状态的会话集合<远端地址, RUDP会话实例>。该集合是线程安全的。
	private Map<SocketAddress, RudpSession> connectedSessions;
	//定时服务
	private ScheduledExecutorService scheduledExecutor;
	//是否处于运行状态
	private volatile boolean isRunning = false;
	
	/**
	 * 构造RUDP会话管理器。
	 * 
	 * @param rudpConfig RUDP配置信息
	 */
	public RudpSessionManager(IRudpConfig rudpConfig, ScheduledExecutorService scheduledSvc) {
		this.rudpConfig = rudpConfig;
		connectedSessions = new ConcurrentHashMap<SocketAddress, RudpSession>();
		if(scheduledSvc == null) {
			this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "SessionScanner");
					return t;
				}
			});
		}
		else {
			this.scheduledExecutor = scheduledSvc;
		}
		this.scheduledExecutor.scheduleAtFixedRate(this, 0, RudpConstants.SESSION_TICK_INTERVAL, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * 启动会话扫描线程
	 */
	public void start() {
		if(isRunning) {
			RudpLoggers.sessionLogger.error("rudp session manager has already started.");
			return;
		}
		isRunning = true;
	}
	
	/**
	 * 停止会话扫描线程
	 */
	public void stop() {
		isRunning = false;
		closeAllSession();
	}
	
	/**
	 * 检查连接状态的后台线程。
	 * 仅由系统调用。
	 * 
	 */
	public void run() {
		if(isRunning == false) {
			return;
		}
		//遍历所有连接，向Processor中抛掷滴答操作
		for(IRudpSession session : connectedSessions.values()) {
			rudpConfig.getProcessorManager().tickSession(session);
		}
	}
	
	@Override
	public void sendHandshake(Channel channel, SocketAddress remoteAddress) {
		channel.write(MessageHelper.getHandshake(), remoteAddress);
		if(RudpLoggers.heartbeatDownLogger.isDebugEnabled()) {
			RudpLoggers.heartbeatDownLogger.debug("sent handshake to " + remoteAddress);
		}
	}
	
	@Override
	public void addSession(final RudpSession session) {
		if(session == null) {
			throw new IllegalArgumentException("session cannot be null");
		}
		SocketAddress remoteAddress = session.getRemoteAddress();
		if(remoteAddress == null) {
			throw new IllegalStateException("session.remoteAddress cannot be null");
		}

		session.setStatus(SessionStatusType.OPENED);
		connectedSessions.put(remoteAddress, session);

		session.addTickListener(new Runnable() {
			@Override
			public void run() {
				long currentTime = System.currentTimeMillis();
				//挂起
				if(session.isSuspended()) {
					//挂起超时
					if(currentTime - session.suspendTime > RudpConstants.SUSPEND_TIMEOUT) {
						if(RudpLoggers.rootLogger.isDebugEnabled()) {
							RudpLoggers.rootLogger.debug("suspend timeout, so close the session, " + session);
						}
						session.close();
					}
				}
				//心跳超时
				else if(currentTime - session.lastReceiveMessageTime > RudpConstants.HEARTBEAT_TIMEOUT) {
					if(RudpLoggers.rootLogger.isDebugEnabled()) {
						RudpLoggers.rootLogger.debug("heartbeat timeout, so suspend the session, " + session);
					}
					((RudpSession) session).suspend();
				}
				//发送心跳消息
				//满足两个条件之一就要发送心跳消息：
				//1、距离上次心跳达到最大心跳间隔；
				//2、距离最老未反馈的消息到达时间超过指定阀值。
				else {
					boolean heartbeatIntervalReached = (currentTime - session.lastSendHeartbeatTime) > RudpConstants.HEARTBEAT_INTERVAL;
					boolean ackTimeReached = session.lastReceiveUnFeedbackMsgTime != 0 && (currentTime - session.lastReceiveUnFeedbackMsgTime) > RudpConstants.MESSAGE_ACK_MAX_WAIT_TIME;
					if(heartbeatIntervalReached || ackTimeReached) {
						SessionReliabilityManager mgr = (SessionReliabilityManager) session.getAttribute("reliability");
						if(mgr != null) {
							mgr.sendHeartbeat();
						}
					}
				}
			}
		});

		//记录日志
		if(RudpLoggers.sessionLogger.isInfoEnabled()) {
			RudpLoggers.sessionLogger.info("session opened, addr=" + remoteAddress);
		}
	}
	
	@Override 
	public void suspendSession(RudpSession session) {
		if(session == null) {
			throw new IllegalArgumentException("session cannot be null");
		}
		session.setStatus(SessionStatusType.SUSPENDED);
		rudpConfig.getProcessorManager().onSessionSuspended(session, session.getRemoteAddress());
	}
	
	@Override
	public void resumeSession(RudpSession session) {
		if(session == null) {
			throw new IllegalArgumentException("session cannot be null");
		}
		session.setStatus(SessionStatusType.OPENED);
		rudpConfig.getProcessorManager().onSessionResumed(session, session.getRemoteAddress());
	}
	
	@Override
	public void removeSession(RudpSession session) {
		if(session == null) {
			throw new IllegalArgumentException("session cannot be null");
		}
		SocketAddress remoteAddress = session.getRemoteAddress();
		if(remoteAddress == null) {
			throw new IllegalStateException("session.remoteAddress cannot be null");
		}
		synchronized(session) {
			if(!session.isClosed() && connectedSessions.containsKey(remoteAddress)) {
				session.setStatus(SessionStatusType.CLOSED);
				connectedSessions.remove(remoteAddress);

				//发送关闭消息
				rudpConfig.getProcessorManager().onSessionClosed(session, remoteAddress);

				//记录日志
				if(RudpLoggers.sessionLogger.isInfoEnabled()) {
					RudpLoggers.sessionLogger.info("session closed, addr=" + remoteAddress);
				}
			}
			else if(session.isClosed() == connectedSessions.containsKey(remoteAddress)) {
				RudpLoggers.sessionLogger.error("illegal session state， session: " + session + 
						", session.isClosed: " + session.isClosed() + 
						", connectedSessions.contains: " + connectedSessions.containsKey(remoteAddress));
			}
		}
	}
	
	@Override
	public boolean isConnected(SocketAddress remoteAddress) {
		if(remoteAddress == null) {
			throw new IllegalArgumentException("remoteAddress cannot be null");
		}
		RudpSession session = connectedSessions.get(remoteAddress);
		if(session != null && !session.isClosed()) {
			return true;
		}
		return false;
	}
	
	@Override
	public IRudpSession getRudpSession(SocketAddress remoteAddress) {
		if(remoteAddress == null) {
			throw new IllegalArgumentException("remoteAddress cannot be null");
		}
		return connectedSessions.get(remoteAddress);
	}

	@Override
	public void closeAllSession() {
		for(IRudpSession session : connectedSessions.values()) {
			if(session.isClosed() == false) {
				session.close();
			}
		}
	}

	@Override
	public int getTotalConnectionCount() {
		return connectedSessions.size();
	}

	@Override
	public int getSuspendedConnectionCount() {
		int count = 0;
		for (RudpSession session : connectedSessions.values()) {
			if(session.isSuspended()) {
				count += 1;
			}
		}
		return count;
	}
}
