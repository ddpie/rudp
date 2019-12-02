package com.ddpie.rudp.filter.codec;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import com.ddpie.rudp.constant.RudpConstants;
import com.ddpie.rudp.filter.RudpFilterAdapter;
import com.ddpie.rudp.log.RudpLoggers;
import com.ddpie.rudp.message.WritableMessageWrapper;
import com.ddpie.rudp.session.IRudpSession;
import com.ddpie.rudp.util.ChannelBufferHelper;
import com.ddpie.rudp.util.MessageHelper;

/**
 * RUDP编解码Filter。
 * 
 * @author caobao
 *
 */
public abstract class RudpCodecFilter extends RudpFilterAdapter {

	@Override
	public Object messageReceived(IRudpSession session, Object messageContent) {
		if(messageContent == null) {
			return null;
		}
		if(messageContent instanceof ChannelBuffer == false) {
			RudpLoggers.codecLogger.error(
					"codec can only decode ChannelBuffer, cannot decode message from " + session + ", " + messageContent);
			return messageContent;
		}
		//去除消息头
		ChannelBuffer buf = (ChannelBuffer) messageContent;
		int index = MessageHelper.MESSAGE_HEAD_LENGTH;
		int length = buf.readableBytes() - index;
		
		Object decoded = decode(session, buf.slice(index, length));
		if(RudpLoggers.codecLogger.isDebugEnabled()) {
			RudpLoggers.codecLogger.debug("decoded finished, decoded=" + decoded + ", src=" + ChannelBufferHelper.toString(buf));
		}
		
		return decoded;
	}

	@Override
	public Object writeRequested(IRudpSession session, Object messageContent) {
		if(messageContent instanceof ChannelBuffer) {
			return messageContent;
		}
		byte headType = MessageHelper.RELIABLE_ORDERED_HEAD;
		Object content = messageContent;
		if(messageContent instanceof WritableMessageWrapper) {
			WritableMessageWrapper wrapper = (WritableMessageWrapper) messageContent;
			content = wrapper.getMessageContent();
			if(content instanceof ChannelBuffer) {
				return content;
			}
			
			switch(wrapper.getType()) {
			case RELIABLE_ORDERED:
				headType = MessageHelper.RELIABLE_ORDERED_HEAD;
				break;
			case RELIABLE_DISORDERED:
				headType = MessageHelper.RELIABLE_DISORDERED_HEAD;
				break;
			case UNRELIABLE:
				headType = MessageHelper.UNRELIABLE_HEAD;
				break;
			}
		}
		ChannelBuffer buf = encode(session, content);
		int readerIndex = buf.readerIndex();
		//编码器预留了消息头
		if(readerIndex >= MessageHelper.MESSAGE_HEAD_LENGTH) {
			readerIndex = readerIndex - MessageHelper.MESSAGE_HEAD_LENGTH;
			buf.readerIndex(readerIndex);
			buf.setZero(readerIndex, MessageHelper.MESSAGE_HEAD_LENGTH);
			buf.setByte(readerIndex, headType);
			if(RudpLoggers.codecLogger.isDebugEnabled()) {
				RudpLoggers.codecLogger.debug("encoded finished, encoded=" + ChannelBufferHelper.toString(buf) + ", src=" + content);
			}
			//不可靠消息太长，截断
			if(headType == MessageHelper.UNRELIABLE_HEAD && buf.readableBytes() > RudpConstants.MESSAGE_MAX_LENGTH) {
				RudpLoggers.businessDownLogger.warn("unreliable message is too large, so try to truncate it, " + ChannelBufferHelper.toString(buf));
				return buf.slice(readerIndex, RudpConstants.MESSAGE_MAX_LENGTH);
			}
			return buf;
		}
		//未预留消息头
		else {
			ChannelBuffer headBuf = ChannelBuffers.buffer(RudpConstants.BYTE_ORDER, MessageHelper.MESSAGE_HEAD_LENGTH);
			headBuf.setZero(0, MessageHelper.MESSAGE_HEAD_LENGTH);
			headBuf.setByte(0, headType);
			headBuf.writerIndex(MessageHelper.MESSAGE_HEAD_LENGTH);
			ChannelBuffer newBuf = ChannelBuffers.wrappedBuffer(headBuf, buf);
			if(RudpLoggers.codecLogger.isDebugEnabled()) {
				RudpLoggers.codecLogger.debug("encoded finished, encoded=" + ChannelBufferHelper.toString(newBuf) + ", src=" + content);
			}
			//不可靠消息太长，截断
			if(headType == MessageHelper.UNRELIABLE_HEAD && newBuf.readableBytes() > RudpConstants.MESSAGE_MAX_LENGTH) {
				RudpLoggers.businessDownLogger.warn("unreliable message is too large, so try to truncate it, " + ChannelBufferHelper.toString(newBuf));
				return newBuf.slice(readerIndex, RudpConstants.MESSAGE_MAX_LENGTH);
			}
			return newBuf;
		}
	}
	
	/**
	 * 将二进制的消息解码为消息对象。
	 * 
	 * @param session 关联的RUDP会话
	 * @param buf 要进行解码的消息
	 * @return 解码后的消息对象
	 */
	protected abstract Object decode(IRudpSession session, ChannelBuffer buf);
	
	/**
	 * 将消息对象编码为二进制的消息。
	 * 
	 * @param session 关联的RUDP会话
	 * @param messageContent 要进行编码的消息对象
	 * @return 编码后的二进制消息
	 */
	protected abstract ChannelBuffer encode(IRudpSession session, Object messageContent);

}
