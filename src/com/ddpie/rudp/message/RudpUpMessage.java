package com.ddpie.rudp.message;

import java.net.SocketAddress;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;

import com.ddpie.rudp.filter.IRudpFilterCallback;
import com.ddpie.rudp.session.IRudpSession;
import com.ddpie.rudp.util.ChannelBufferHelper;

/**
 * 上行RUDP消息。
 * 
 * @author caobao
 *
 */
public class RudpUpMessage extends AbstractRudpMessage {
	private Channel channel;				//Netty channel
	private Object content;					//消息实体
	private IRudpFilterCallback callback;	//所有RUDP的IRudpFilter操作结束后的回调
	
	/**
	 * 构造RUDP上行消息。
	 * 
	 * @param session RUDP会话
	 * @param remoteAddress 远端地址
	 * @param channel Netty Channel
	 * @param content 消息实体
	 */
	public RudpUpMessage(IRudpSession session, SocketAddress remoteAddress,
			Channel channel, Object content, IRudpFilterCallback callback) {
		super(session, remoteAddress);
		this.channel = channel;
		this.content = content;
		this.callback = callback;
	}
	
	@Override
	public RudpMessageType getType() {
		return RudpMessageType.UP;
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
	
	/**
	 * 获取所有RUDP的IRudpFilter操作结束后的回调对象
	 * 
	 * @return 回调对象
	 */
	public IRudpFilterCallback getCallback() {
		return callback;
	}
	
	@Override
	public String toString() {
		String contentStr = content.toString();
		if(content instanceof ChannelBuffer) {
			contentStr = ChannelBufferHelper.toString((ChannelBuffer) content);
		}
		return "RudpUpMessage [session=" + getSession()
				+ ", remoteAddress=" + getRemoteAddress() + ", channel="
				+ channel + ", content=" + contentStr + "]";
	}
}
