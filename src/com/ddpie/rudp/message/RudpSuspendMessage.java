package com.ddpie.rudp.message;

import java.net.SocketAddress;

import com.ddpie.rudp.session.IRudpSession;

/**
 * RUDP会话挂起消息。
 * 
 * @author caobao
 *
 */
public class RudpSuspendMessage extends AbstractRudpMessage {

	public RudpSuspendMessage(IRudpSession session, SocketAddress remoteAddress) {
		super(session, remoteAddress);
	}

	@Override
	public RudpMessageType getType() {
		return RudpMessageType.SUSPEND;
	}

	@Override
	public String toString() {
		return "RudpSuspendMessage [session=" + getSession()
				+ ", remoteAddress=" + getRemoteAddress() + "]";
	}

}
