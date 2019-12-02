package com.ddpie.rudp.session;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.netty.channel.Channel;

import com.ddpie.rudp.attribute.DefaultAttributeContainer;
import com.ddpie.rudp.attribute.IAttributeContainer;
import com.ddpie.rudp.config.IRudpConfig;
import com.ddpie.rudp.constant.RudpConstants;
import com.ddpie.rudp.message.WritableMessageWrapper;
import com.ddpie.rudp.util.MessageHelper;


/**
 * RUDP会话，代表一个与远端的连接。
 * 
 * @author caobao
 *
 */
public class RudpSession implements IRudpSession {
	private IRudpConfig rudpConfig;				//RUDP配置信息
	Channel channel;							//Netty Channel
	private SocketAddress remoteAddress;		//远端地址
	private volatile SessionStatusType status;	//会话状态
	volatile long lastReceiveMessageTime;		//上次收到远端消息时间，毫秒
	volatile long lastReceiveUnFeedbackMsgTime;	//上次收到尚未反馈的消息的时间
	volatile long lastSendHeartbeatTime;		//上次给远端发送消息的时间，毫秒
	volatile long suspendTime;					//上次挂起的时间
	private IAttributeContainer attrContainer;	//属性容器
	private List<Runnable> tickListeners;		//滴答监听器列表
	
	/**
	 * 构造一个Netty会话。
	 * 
	 * @param channel 
	 * @param remoteAddress 远端地址
	 */
	public RudpSession(IRudpConfig rudpConfig, Channel channel, SocketAddress remoteAddress) {
		if(remoteAddress == null) {
			throw new IllegalArgumentException("remoteAddress cannot be null");
		}
		this.rudpConfig = rudpConfig;
		this.channel = channel;
		this.remoteAddress = remoteAddress;
		
		attrContainer = new DefaultAttributeContainer();
		tickListeners = new CopyOnWriteArrayList<Runnable>();
	}
	
	/**
	 * 设置会话的连接状态。
	 * 为保证正确性，仅由RUDP内部调用。
	 * 
	 * @param connected 连接状态
	 */
	void setStatus(SessionStatusType status) {
		this.status = status;
	}
	
	@Override
	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}
	
	@Override
	public SocketAddress getLocalAddress() {
		return channel.getLocalAddress();
	}
	
	@Override
	public boolean isSuspended() {
		return status == SessionStatusType.SUSPENDED;
	}
	
	@Override
	public boolean isClosed() {
		return status == SessionStatusType.CLOSED;
	}
	
	@Override
	public void writeOrdered(Object message) {
		if(status != SessionStatusType.CLOSED) {
			if(message instanceof WritableMessageWrapper == false) {
				message = WritableMessageWrapper.newReliableOrdered(message);
			}
			rudpConfig.getProcessorManager().onWriteRequested(channel, remoteAddress, message);
		}
	}
	
	@Override
	public void writeDisordered(Object message) {
		if(status != SessionStatusType.CLOSED) {
			if(message instanceof WritableMessageWrapper == false) {
				message = WritableMessageWrapper.newReliableDisordered(message);
			}
			rudpConfig.getProcessorManager().onWriteRequested(channel, remoteAddress, message);
		}
	}
	
	@Override
	public void writeUnreliable(Object message) {
		if(status != SessionStatusType.CLOSED) {
			if(message instanceof WritableMessageWrapper == false) {
				message = WritableMessageWrapper.newUnreliable(message);
			}
			rudpConfig.getProcessorManager().onWriteRequested(channel, remoteAddress, message);
		}
	}
	
	public void suspend() {
		rudpConfig.getSessionManager().suspendSession(this);
		suspendTime = System.currentTimeMillis();
	}
	
	public void resume() {
		rudpConfig.getSessionManager().resumeSession(this);
		lastReceiveMessageTime = System.currentTimeMillis();
	}
	
	@Override
	public void close() {
		for (int i = 0; i < RudpConstants.CLOSE_MESSAGE_COUNT; i++) {
			channel.write(MessageHelper.getClose(), remoteAddress);
		}
		rudpConfig.getSessionManager().removeSession(this);
	}
	
	@Override
	public Object setAttribute(Object key, Object value) {
		return attrContainer.setAttribute(key, value);
	}

	@Override
	public Object getAttribute(Object key) {
		return attrContainer.getAttribute(key);
	}

	@Override
	public Object removeAttribute(Object key) {
		return attrContainer.removeAttribute(key);
	}
	
	/**
	 * 滴答
	 */
	public void tick() {
		for (Runnable listener : tickListeners) {
			listener.run();
		}
	}

	/**
	 * 增加一个滴答监听器。
	 * 
	 * @param listener 心跳监听器
	 */
	public void addTickListener(Runnable listener) {
		tickListeners.add(listener);
	}
	
	/**
	 * 收到心跳消息后的回调。
	 */
	public void onHeartbeatReceived() {
		lastReceiveMessageTime = System.currentTimeMillis();
	}
	
	/**
	 * 发送完心跳消息后的回调。
	 */
	public void onHeartbeatSent() {
		lastSendHeartbeatTime = System.currentTimeMillis();
		lastReceiveUnFeedbackMsgTime = 0;
	}
	
	/**
	 * 收到非心跳非握手消息后的回调
	 */
	public void onMessageReceived() {
		lastReceiveMessageTime = System.currentTimeMillis();
		if(lastReceiveUnFeedbackMsgTime == 0) {
			lastReceiveUnFeedbackMsgTime = System.currentTimeMillis();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((remoteAddress == null) ? 0 : remoteAddress.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RudpSession other = (RudpSession) obj;
		if (remoteAddress == null) {
			if (other.remoteAddress != null)
				return false;
		} else if (!remoteAddress.equals(other.remoteAddress))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RudpSession [remoteAddress=" + remoteAddress + ", localAddress=" + getLocalAddress() + ", status=" + status + "]";
	}

}
