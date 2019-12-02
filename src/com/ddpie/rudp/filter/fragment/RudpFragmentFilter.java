package com.ddpie.rudp.filter.fragment;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import com.ddpie.rudp.constant.RudpConstants;
import com.ddpie.rudp.filter.IRudpFilter;
import com.ddpie.rudp.log.RudpLoggers;
import com.ddpie.rudp.session.IRudpSession;
import com.ddpie.rudp.util.ChannelBufferHelper;
import com.ddpie.rudp.util.MessageHelper;

/**
 * RUDP大数据封包Filter。
 * 
 * @author caobao
 *
 */
public class RudpFragmentFilter implements IRudpFilter {

	@Override
	public void sessionOpened(IRudpSession session) {
		SessionFragmentManager mgr = new SessionFragmentManager(session);
		session.setAttribute("fragment", mgr);
	}
	
	@Override
	public void sessionSuspended(IRudpSession session) {
		
	}
	
	@Override
	public void sessionResumed(IRudpSession session) {
		
	}

	@Override
	public void sessionClosed(IRudpSession session) {
		session.removeAttribute("fragment");
	}

	@Override
	public Object messageReceived(IRudpSession session, Object messageContent) {
		ChannelBuffer buf = (ChannelBuffer) messageContent;
		//不可靠消息，无需处理封包
		if(MessageHelper.isUnreliable(buf)) {
			return messageContent;
		}
		SessionFragmentManager mgr = (SessionFragmentManager) session.getAttribute("fragment");
		return mgr.onMessageReceived(buf);
	}

	@Override
	public Object writeRequested(IRudpSession session, Object messageContent) {
		ChannelBuffer buf = (ChannelBuffer) messageContent;
		if(buf.readableBytes() > RudpConstants.MESSAGE_MAX_LENGTH) {
			if(RudpLoggers.businessDownLogger.isDebugEnabled()) {
				RudpLoggers.businessDownLogger.debug(
						"the message sent to " + session.getRemoteAddress() + " is too large, so split it, msg=" + ChannelBufferHelper.toString(buf));
			}
			return splitMessage(session, buf);
		}
		
		return messageContent;
	}
	
	//拆分消息
	private ChannelBuffer[] splitMessage(IRudpSession session, ChannelBuffer buf) {
		int startIndex = buf.readerIndex();
		int endIndex = buf.writerIndex();
		int headLength = MessageHelper.MESSAGE_HEAD_LENGTH;
		int maxLength = RudpConstants.MESSAGE_MAX_LENGTH - headLength;
		int currentIndex = startIndex;
		
		int totalBodyLength = endIndex - startIndex - headLength;
		//封包数量
		int segmentCount = totalBodyLength / maxLength;
		if(totalBodyLength % maxLength != 0) {
			segmentCount += 1;
		}
		
		ChannelBuffer[] result = new ChannelBuffer[segmentCount];
		
		//跳过消息头
		currentIndex += headLength;
		
		//封包索引
		int segmentIndex = 0;
		//拆分消息体
		while(currentIndex < endIndex) {
			int segmentBodyLength = maxLength;
			if(currentIndex + segmentBodyLength > endIndex) {
				segmentBodyLength = endIndex - currentIndex;
			}
			ChannelBuffer segmentBody = buf.slice(currentIndex, segmentBodyLength);
			//消息头
			ChannelBuffer headBuffer = null;
			//第一条封包消息，复用大消息的消息头，减少一次内存数组复制
			if(segmentIndex == 0) {
				headBuffer = buf.slice(startIndex, headLength);
			}
			else {
				headBuffer = buf.copy(startIndex, headLength);
			}
			//生成消息
			ChannelBuffer segmentBuf = ChannelBuffers.wrappedBuffer(headBuffer, segmentBody);
			//设置封包数量和封包索引
			MessageHelper.setFragmentCount(segmentBuf, segmentCount);
			MessageHelper.setFragmentIndex(segmentBuf, segmentIndex);
			//存储消息
			result[segmentIndex] = segmentBuf;
			
			//跳过本消息体片段
			currentIndex += segmentBodyLength;
			//更新封包索引
			segmentIndex += 1;
		}
		
		return result;
	}

}
