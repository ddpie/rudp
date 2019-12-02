package com.ddpie.rudp.filter;

import com.ddpie.rudp.session.IRudpSession;

/**
 * RUDP Filter的默认实现。
 * 
 * @author caobao
 *
 */
public class RudpFilterAdapter implements IRudpFilter {

	@Override
	public void sessionOpened(IRudpSession session) {
		
	}
	
	@Override
	public void sessionSuspended(IRudpSession session) {
		
	}
	
	@Override
	public void sessionResumed(IRudpSession session) {
		
	}
	
	@Override
	public void sessionClosed(IRudpSession session) {
		
	}

	@Override
	public Object messageReceived(IRudpSession session, Object messageContent) {
		return messageContent;
	}

	@Override
	public Object writeRequested(IRudpSession session, Object messageContent) {
		return messageContent;
	}

}
