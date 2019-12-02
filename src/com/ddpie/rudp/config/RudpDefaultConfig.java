package com.ddpie.rudp.config;

import java.net.SocketAddress;
import java.util.concurrent.ScheduledExecutorService;

import com.ddpie.rudp.filter.IRudpFilter;
import com.ddpie.rudp.filter.fragment.RudpFragmentFilter;
import com.ddpie.rudp.filter.reliability.RudpReliabilityFilter;
import com.ddpie.rudp.processor.IRudpProcessorManager;
import com.ddpie.rudp.processor.RudpProcessorManager;
import com.ddpie.rudp.session.IRudpSessionManager;
import com.ddpie.rudp.session.RudpSessionManager;

/**
 * RUDP的配置信息
 * 
 * @author caobao
 *
 */
public class RudpDefaultConfig implements IRudpConfig {
	private volatile boolean isServer;			//是否是服务器
	private RudpSessionManager sessionMgr;		//会话管理器
	private RudpProcessorManager processorMgr;	//Processor管理器
	
	/**
	 * 构造RUDP配置信息。
	 * 作为服务器，并且Processor数量为CPU核数*2。
	 */
	public RudpDefaultConfig() {
		this(true, Runtime.getRuntime().availableProcessors() * 2);
	}
	
	/**
	 * 构造RUDP配置信息。
	 * 
	 * @param isServer 是否是服务器
	 * @param processorCount Processor的数量
	 */
	public RudpDefaultConfig(boolean isServer, int processorCount) {
		this(isServer, processorCount, null);
	}
	
	/**
	 * 构造RUDP配置信息。
	 * 
	 * @param isServer 是否是服务器
	 * @param processorCount Processor的数量
	 * @param sessionExecutor 会话定时器
	 */
	public RudpDefaultConfig(boolean isServer, int processorCount, ScheduledExecutorService sessionExecutor) {
		this.isServer = isServer;
		processorMgr = new RudpProcessorManager(this, processorCount);
		sessionMgr = new RudpSessionManager(this, sessionExecutor);
		
		init();
	}

	private void init() {
		addRudpFilter(new RudpReliabilityFilter());
		addRudpFilter(new RudpFragmentFilter());
		
		processorMgr.start();
		sessionMgr.start();
	}

	@Override
	public boolean isServer() {
		return isServer;
	}
	
	@Override
	public SocketAddress getServerAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IRudpSessionManager getSessionManager() {
		return sessionMgr;
	}

	@Override
	public IRudpProcessorManager getProcessorManager() {
		return processorMgr;
	}

	@Override
	public void addRudpFilter(IRudpFilter filter) {
		processorMgr.addRudpFilter(filter);
	}

	@Override
	public void destroy() {
		if(sessionMgr != null) {
			sessionMgr.stop();
		}
		if(processorMgr != null) {
			processorMgr.stop();
		}
	}
}
