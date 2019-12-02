package com.ddpie.rudp.message;

import java.net.SocketAddress;

import com.ddpie.rudp.session.IRudpSession;

/**
 * RUDP消息基础类。
 * 
 * @author caobao
 *
 */
public abstract class AbstractRudpMessage implements IRudpMessage {
	private IRudpSession session;			//RUDP会话
	private SocketAddress remoteAddress;	//远端地址
	
	/**
	 * 构造RUDP消息。
	 * 
	 * @param session RUDP会话
	 * @param remoteAddress 远端地址
	 */
	public AbstractRudpMessage(IRudpSession session, SocketAddress remoteAddress) {
		super();
		this.session = session;
		this.remoteAddress = remoteAddress;
	}

	@Override
	public IRudpSession getSession() {
		return session;
	}
	
	@Override
	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}
}
