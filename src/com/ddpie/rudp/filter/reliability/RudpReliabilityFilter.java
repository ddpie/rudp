package com.ddpie.rudp.filter.reliability;

import org.jboss.netty.buffer.ChannelBuffer;

import com.ddpie.rudp.filter.IRudpFilter;
import com.ddpie.rudp.log.RudpLoggers;
import com.ddpie.rudp.session.IRudpSession;
import com.ddpie.rudp.util.ChannelBufferHelper;
import com.ddpie.rudp.util.MessageHelper;

/**
 * 实现RUDP可靠性的Filter。
 * 
 * @author caobao
 *
 */
public class RudpReliabilityFilter implements IRudpFilter {

	@Override
	public void sessionOpened(IRudpSession session) {
		SessionReliabilityManager mgr = new SessionReliabilityManager(session);
		session.setAttribute("reliability", mgr);
	}
	
	@Override
	public void sessionSuspended(IRudpSession session) {
		
	}
	
	@Override
	public void sessionResumed(IRudpSession session) {
		
	}

	@Override
	public void sessionClosed(IRudpSession session) {
		session.removeAttribute("reliability");
	}

	@Override
	public Object messageReceived(IRudpSession session, Object messageContent) {
		SessionReliabilityManager mgr = (SessionReliabilityManager) session.getAttribute("reliability");
		ChannelBuffer buf = (ChannelBuffer) messageContent;
		
		switch(buf.getByte(buf.readerIndex())) {
		case MessageHelper.HEARTBEAT_HEAD:
			mgr.onHeartbeat(buf);
			return null;
		case MessageHelper.RELIABLE_DISORDERED_HEAD:
			return mgr.onDisorderedReceived(buf);
		case MessageHelper.RELIABLE_ORDERED_HEAD:
			return mgr.onOrderedReceived(buf);
		case MessageHelper.UNRELIABLE_HEAD:
			return buf;
		default:
			RudpLoggers.reliabilityLogger.warn(
					"unrecgnized message " + ChannelBufferHelper.toString(buf) + ", received from session " + session + ", so close session.");
			session.close();
			return null;
		}
	}

	@Override
	public Object writeRequested(IRudpSession session, Object messageContent) {
		SessionReliabilityManager mgr = (SessionReliabilityManager) session.getAttribute("reliability");
		ChannelBuffer buf = (ChannelBuffer) messageContent;
		
		switch(buf.getByte(buf.readerIndex())) {
		case MessageHelper.RELIABLE_DISORDERED_HEAD:
			mgr.onDisorderedWrite(buf);
			break;
		case MessageHelper.RELIABLE_ORDERED_HEAD:
			mgr.onOrderedWrite(buf);
			break;
		}
		
		return messageContent;
	}

}
