package com.ddpie.rudp.message;

import java.net.SocketAddress;

import com.ddpie.rudp.session.IRudpSession;

/**
 * RUDP关闭消息。
 * 
 * @author caobao
 *
 */
public class RudpCloseMessage extends AbstractRudpMessage {

	public RudpCloseMessage(IRudpSession session, SocketAddress remoteAddress) {
		super(session, remoteAddress);
	}

	@Override
	public RudpMessageType getType() {
		return RudpMessageType.CLOSE;
	}

	@Override
	public String toString() {
		return "RudpCloseMessage [session=" + getSession()
				+ ", remoteAddress=" + getRemoteAddress() + "]";
	}

}
