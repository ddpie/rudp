package com.ddpie.rudp.config;

import java.net.SocketAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 作为客户端时的RUDP配置信息类。
 * 
 * @author caobao
 *
 */
public class RudpClientConfig extends RudpDefaultConfig {
	private SocketAddress serverAddress;
	
	/**
	 * 构造客户端配置信息
	 * 
	 * @param serverAddress 要连接的服务器地址
	 */
	public RudpClientConfig(SocketAddress serverAddress) {
		this(serverAddress, null);
	}
	
	/**
	 * 构造客户端配置信息
	 * 
	 * @param serverAddress 要连接的服务器地址
	 * @param sessionExecutor 会话定时器
	 */
	public RudpClientConfig(SocketAddress serverAddress, ScheduledExecutorService sessionExecutor) {
		super(false, 1, sessionExecutor);
		this.serverAddress = serverAddress;
	}
	
	@Override
	public SocketAddress getServerAddress() {
		return serverAddress;
	}
}
