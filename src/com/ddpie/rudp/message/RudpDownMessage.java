package com.ddpie.rudp.message;

import java.net.SocketAddress;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;

import com.ddpie.rudp.session.IRudpSession;
import com.ddpie.rudp.util.ChannelBufferHelper;

/**
 * 下行RUDP消息。
 * 
 * @author caobao
 *
 */
public class RudpDownMessage extends AbstractRudpMessage {
	private Channel channel;				//Netty channel
	private Object content;					//消息实体
	
	/**
	 * 构造RUDP下行消息。
	 * 
	 * @param session RUDP会话
	 * @param remoteAddress 远端地址
	 * @param channel Netty Channel
	 * @param content 消息实体
	 */
	public RudpDownMessage(IRudpSession session, SocketAddress remoteAddress,
			Channel channel, Object content) {
		super(session, remoteAddress);
		this.channel = channel;
		this.content = content;
	}
	
	@Override
	public RudpMessageType getType() {
		return RudpMessageType.DOWN;
	}

	/**
	 * 获取Netty Channel
	 * 
	 * @return Netty Channel
	 */
	public Channel getChannel() {
		return channel;
	}
	
	/**
	 * 获取RUDP消息的消息实体。
	 * 
	 * @return 消息实体
	 */
	public Object getContent() {
		return content;
	}

	@Override
	public String toString() {
		String contentStr = content.toString();
		if(content instanceof ChannelBuffer) {
			contentStr = ChannelBufferHelper.toString((ChannelBuffer) content);
		}
		return "RudpDownMessage [session=" + getSession()
				+ ", remoteAddress=" + getRemoteAddress() + ", channel="
				+ channel + ", content=" + contentStr + "]";
	}
}
