package com.ddpie.rudp.message;

import java.net.SocketAddress;

import com.ddpie.rudp.session.IRudpSession;

/**
 * RUDP会话恢复消息。
 * 
 * @author caobao
 *
 */
public class RudpResumeMessage extends AbstractRudpMessage {

	public RudpResumeMessage(IRudpSession session, SocketAddress remoteAddress) {
		super(session, remoteAddress);
	}

	@Override
	public RudpMessageType getType() {
		return RudpMessageType.RESUME;
	}

	@Override
	public String toString() {
		return "RudpResumeMessage [session=" + getSession()
				+ ", remoteAddress=" + getRemoteAddress() + "]";
	}

}
