package com.ddpie.rudp.util;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Netty的{@link ChannelBuffer}的一些辅助方法。
 * 
 * @author caobao
 *
 */
public final class ChannelBufferHelper {
	/**
	 * toString方法
	 * 
	 * @param buf
	 * @return
	 */
	public static String toString(ChannelBuffer buf) {
		if(buf == null) {
			return "null buffer";
		}
		StringBuilder result = new StringBuilder();
		result.append(buf.toString());
		result.append('[');
		byte[] bytes = new byte[buf.readableBytes()];
		buf.getBytes(buf.readerIndex(), bytes);
		for (int i = 0; i < bytes.length; i++) {
			if(i != 0) {
				result.append(' ');
			}
			String hex = Integer.toHexString(bytes[i]);
			if(hex.length() > 2) {
				hex = hex.substring(hex.length() - 2);
			}
			else if(hex.length() < 2) {
				result.append('0');
			}
			result.append(hex);
		}
		result.append(']');
		return result.toString();
	}
}
