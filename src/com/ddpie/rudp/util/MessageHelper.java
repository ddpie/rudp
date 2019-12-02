package com.ddpie.rudp.util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import com.ddpie.rudp.constant.RudpConstants;
import com.ddpie.rudp.log.RudpLoggers;
import com.ddpie.rudp.session.DirectWritter;
import com.ddpie.rudp.session.IRudpSession;
import com.ddpie.rudp.session.RudpSession;

/**
 * 消息的一些辅助方法。<br>
 * <br>
 * 包消息头为1个字节，目前分为五种：<br>
 * 1、0x00 握手消息<br>
 * 2、0x01 心跳消息（携带反馈）<br>
 * 3、0x03 可靠且有序的消息<br>
 * 4、0x04 可靠但无序的消息<br>
 * 5、0x05 不可靠也无序的消息<br>
 * <br>
 * 各种消息的结构如下：<br>
 * 1、握手消息（8字节）：				【8字节】<br>
 * 2、关闭消息（8字节）：				【8字节】<br>
 * 3、心跳消息（13字节）：			【消息类型|1字节】【有序消息反馈序号|4字节】【有序消息反馈bitfield|2字节】【无序消息反馈序号|4字节】【无序消息反馈bitfield|2字节】<br>
 * 4、可靠且有序的消息头（7字节）：	【消息类型|1字节】【消息序号|4字节】【封包数量|1字节】【封包索引|1字节】<br>
 * 5、可靠但无序的消息头（7字节）：	【消息类型|1字节】【消息标识|4字节】【封包数量|1字节】【封包索引|1字节】<br>
 * 6、不可靠也无序的消息头（7字节）：	【消息类型|1字节】【填充字节|6字节】<br>
 * 
 * @author caobao
 *
 */
public class MessageHelper {
	/** 握手消息（0x00000000FFFFFFFF） */
	private static final byte[] HAND_SHAKE = new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
	/** 关闭消息 */
	private static final byte[] CLOSE = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00};
	/** 心跳消息头（携带反馈） */
	public static final byte HEARTBEAT_HEAD = 0x01;
	/** 可靠且有序消息头 */
	public static final byte RELIABLE_ORDERED_HEAD = 0x02;
	/** 可靠但无序消息头 */
	public static final byte RELIABLE_DISORDERED_HEAD = 0x03;
	/** 不可靠也无序消息头 */
	public static final byte UNRELIABLE_HEAD = 0x04;
	/** 消息头长度 */
	public static final int MESSAGE_HEAD_LENGTH = 7;
	
	/**
	 * 判断消息是否是握手消息。
	 * 
	 * @param buf
	 * @return
	 */
	public static boolean isHandshake(ChannelBuffer buf) {
		if(buf.readableBytes() != HAND_SHAKE.length) {
			return false;
		}
		for (int i = 0; i < HAND_SHAKE.length; i++) {
			if(buf.getByte(buf.readerIndex() + i) != HAND_SHAKE[i]) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 获取握手消息。
	 * 
	 * @return
	 */
	public static ChannelBuffer getHandshake() {
		return ChannelBuffers.wrappedBuffer(RudpConstants.BYTE_ORDER, HAND_SHAKE);
	}
	
	/**
	 * 判断消息是否是关闭消息。
	 * 
	 * @param buf
	 * @return
	 */
	public static boolean isClose(ChannelBuffer buf) {
		if(buf.readableBytes() != CLOSE.length) {
			return false;
		}
		for (int i = 0; i < CLOSE.length; i++) {
			if(buf.getByte(buf.readerIndex() + i) != CLOSE[i]) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 获取关闭消息。
	 * 
	 * @return
	 */
	public static ChannelBuffer getClose() {
		return ChannelBuffers.wrappedBuffer(RudpConstants.BYTE_ORDER, CLOSE);
	}
	
	/**
	 * 判断{@link ChannelBuffer}消息是否为心跳消息。
	 * 
	 * @param buf 要判断的消息
	 * @return true，是心跳消息；false，不是心跳消息
	 */
	public static boolean isHeartbeat(ChannelBuffer buf) {
		/*
		 * 判断Heartbeat消息步骤如下：
		 * 1、消息长度正确；
		 * 2、消息头正确；
		 */
		if(buf.readableBytes() != 13) {
			return false;
		}
		if(buf.getByte(0) != HEARTBEAT_HEAD) {
			return false;
		}
		return true;
	}
	
	/**
	 * 获取心跳消息。
	 * 
	 * @return
	 */
	public static ChannelBuffer getHeartbeat(int orderedSequence, short orderedBitfield, int disorderedSequence, short disorderedBitfield) {
		ChannelBuffer buf = ChannelBuffers.buffer(RudpConstants.BYTE_ORDER, 13);
		buf.writeByte(HEARTBEAT_HEAD);
		buf.writeInt(orderedSequence);
		buf.writeShort(orderedBitfield);
		buf.writeInt(disorderedSequence);
		buf.writeShort(disorderedBitfield);
		return buf;
	}
	
	/**
	 * 是否是可靠且有序的消息
	 * 
	 * @param message
	 * @return
	 */
	public static boolean isReliableOrdered(ChannelBuffer message) {
		return message.getByte(message.readerIndex()) == RELIABLE_ORDERED_HEAD;
	}
	
	/**
	 * 是否是可靠但无序的消息
	 * 
	 * @param message
	 * @return
	 */
	public static boolean isReliableDisordered(ChannelBuffer message) {
		return message.getByte(message.readerIndex()) == RELIABLE_DISORDERED_HEAD;
	}
	
	/**
	 * 判断是否是不可靠消息
	 * 
	 * @param message
	 * @return
	 */
	public static boolean isUnreliable(ChannelBuffer message) {
		return message.getByte(message.readerIndex()) == UNRELIABLE_HEAD;
	}
	
	/**
	 * 获取消息序号。
	 * 
	 * @param message
	 * @return
	 */
	public static int getSequenceId(ChannelBuffer message) {
		return message.getInt(1);
	}
	
	/**
	 * 设置消息序号。
	 * 
	 * @param message
	 * @param sequenceId
	 */
	public static void setSequenceId(ChannelBuffer message, int sequenceId) {
		message.setInt(1, sequenceId);
	}
	
	/**
	 * 获取封包数量
	 * 
	 * @param message
	 * @return
	 */
	public static byte getFragmentCount(ChannelBuffer message) {
		return message.getByte(message.readerIndex() + 5);
	}
	
	/**
	 * 设置封包数量
	 * 
	 * @param message
	 * @param count
	 */
	public static void setFragmentCount(ChannelBuffer message, int count) {
		message.setByte(5, count);
	}
	
	/**
	 * 设置封包索引
	 * 
	 * @param message
	 * @param count
	 */
	public static void setFragmentIndex(ChannelBuffer message, int index) {
		message.setByte(6, index);
	}
	
	/**
	 * 获取封包索引
	 * 
	 * @param message
	 * @return
	 */
	public static byte getFragmentIndex(ChannelBuffer message) {
		return message.getByte(message.readerIndex() + 6);
	}
	
	/**
	 * 向远端发送心跳消息
	 * 
	 * @param session RUDP会话
	 * @param orderedSequence 有序消息序号
	 * @param orderedBitfield 有序消息bitfield
	 * @param disorderedSequence 无序消息标识
	 * @param disorderedBitfield 无序消息bitfield
	 */
	public static void sendHeartbeat(
			IRudpSession session, 
			int orderedSequence, short orderedBitfield, 
			int disorderedSequence, short disorderedBitfield) {
		ChannelBuffer hearbeat = MessageHelper.getHeartbeat(
				orderedSequence, orderedBitfield, 
				disorderedSequence, disorderedBitfield);
		DirectWritter.write(session, hearbeat);
		if(session instanceof RudpSession) {
			((RudpSession) session).onHeartbeatSent();
		}
		if(RudpLoggers.heartbeatDownLogger.isDebugEnabled()) {
			RudpLoggers.heartbeatDownLogger.debug(
					"sent heartbeat to " + session.getRemoteAddress() + ", " + ChannelBufferHelper.toString(hearbeat));
		}
	}
	
}
