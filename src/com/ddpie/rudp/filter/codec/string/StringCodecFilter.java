package com.ddpie.rudp.filter.codec.string;

import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import com.ddpie.rudp.constant.RudpConstants;
import com.ddpie.rudp.filter.codec.RudpCodecFilter;
import com.ddpie.rudp.session.IRudpSession;

/**
 * 字符串编解码过滤器。
 * 
 * @author caobao
 *
 */
public class StringCodecFilter extends RudpCodecFilter {
	
	private final Charset charset;
	
	/**
	 * 根据utf8字符集构造字符串编解码器。
	 * 
	 */
	public StringCodecFilter() {
        this(Charset.forName("utf-8"));
    }

	/**
	 * 根据指定的字符集构造字符串编解码器。
	 * 
	 * @param charset
	 */
    public StringCodecFilter(Charset charset) {
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.charset = charset;
    }

	@Override
	protected Object decode(IRudpSession session, ChannelBuffer buf) {
		return buf.toString(charset);
	}

	@Override
	protected ChannelBuffer encode(IRudpSession session, Object messageContent) {
		return ChannelBuffers.copiedBuffer(RudpConstants.BYTE_ORDER, (String) messageContent, charset);
	}

}
