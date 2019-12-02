package com.ddpie.rudp.client;

import java.net.SocketAddress;
import java.util.Timer;
import java.util.TimerTask;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.ddpie.rudp.config.IRudpConfig;
import com.ddpie.rudp.constant.RudpConstants;
import com.ddpie.rudp.session.IRudpSession;
import com.ddpie.rudp.session.IRudpSessionManager;

/**
 * Rudp客户端连接辅助类。
 * 
 * @author caobao
 *
 */
public class RudpClientConnector {

	/**
	 * 连接服务器。
	 * 
	 * @param bootstrap Netty UDP连接辅助类
	 * @param rudpConfig RUDP配置信息
	 */
	public static void connect(ConnectionlessBootstrap bootstrap, IRudpConfig rudpConfig) {
		final SocketAddress serverAddress = rudpConfig.getServerAddress();
		final IRudpSessionManager sessionManager = rudpConfig.getSessionManager();
		ChannelFuture future = bootstrap.connect(serverAddress);
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					for (int i = 0; i < RudpConstants.HANDSHAKE_MESSAGE_COUNT; i++) {
						sessionManager.sendHandshake(future.getChannel(), serverAddress);
					}
				}
			}
		});
	}
	
	/**
	 * 连接服务器，连接失败后有自动重连。
	 * 
	 * @param bootstrap Netty UDP连接辅助类
	 * @param rudpConfig RUDP配置信息
	 * @param reconnectInterval 自动重连的时间间隔，毫秒
	 * @param maxReconnectTimes 最多尝试次数
	 * @param timeoutCallback 连接超时的回调
	 * @param failedCallback 尝试次数满仍未成功的回调
	 */
	public static void connect(
			final ConnectionlessBootstrap bootstrap, 
			final IRudpConfig rudpConfig, 
			final int reconnectInterval,
			final int maxConnectTimes,
			final Runnable timeoutCallback,
			final Runnable failedCallback) {
		connect(bootstrap, rudpConfig);
		final SocketAddress serverAddress = rudpConfig.getServerAddress();
		final IRudpSessionManager sessionMgr = rudpConfig.getSessionManager();
		final Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				timer.cancel();
				IRudpSession session = sessionMgr.getRudpSession(serverAddress);
				if(session == null || session.isClosed()) {
					if(timeoutCallback != null) {
						timeoutCallback.run();
					}
					if(maxConnectTimes <= 1 && failedCallback != null) {
						failedCallback.run();
						return;
					}
					connect(bootstrap, rudpConfig, reconnectInterval, maxConnectTimes - 1, timeoutCallback, failedCallback);
				}
			}
		}, reconnectInterval);
	}
}
