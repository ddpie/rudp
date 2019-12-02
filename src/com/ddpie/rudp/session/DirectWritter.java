package com.ddpie.rudp.session;

import com.ddpie.rudp.log.RudpLoggers;

/**
 * 直接发送消息的辅助类
 * 
 * @author caobao
 *
 */
public class DirectWritter {
	
	/**
	 * 直接向远端发送消息。
	 */
	public static void write(IRudpSession session, Object message) {
		if(session instanceof RudpSession == false) {
			RudpLoggers.sessionLogger.error(
					"session is not RudpSession, so cannot send direct message, session=" + session);
			return;
		}
		RudpSession rudpSession = (RudpSession) session;
		rudpSession.channel.write(message, session.getRemoteAddress());
	}
}
