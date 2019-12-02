package com.ddpie.rudp.channelhandler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.channel.WriteCompletionEvent;

import com.ddpie.rudp.config.IRudpConfig;
import com.ddpie.rudp.filter.IRudpFilterCallback;
import com.ddpie.rudp.log.RudpLoggers;
import com.ddpie.rudp.util.ChannelBufferHelper;

/**
 * RUDP中Netty的Handler。
 * 放到Netty的pipeline中使用。
 * 
 * @author caobao
 *
 */
public class RudpNettyHandler extends SimpleChannelHandler {
	private IRudpConfig rudpConfig;	//RUDP配置信息
	
	/**
	 * 构造Netty的一个Handler。
	 */
	public RudpNettyHandler(IRudpConfig rudpConfig) {
		this.rudpConfig = rudpConfig;
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		rudpConfig.getProcessorManager().onMessageReceived(
				ctx.getChannel(), e.getRemoteAddress(), e.getMessage(), new IRudpFilterCallback() {
					@Override
					public void callback(Object messageContent) {
						if(messageContent != null) {
							MessageEvent event = new UpstreamMessageEvent(ctx.getChannel(), messageContent, e.getRemoteAddress());
							ctx.sendUpstream(event);
						}
					}
				});
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		RudpLoggers.rootLogger.error("exception occurred, addr=" + ctx.getChannel().getRemoteAddress(), e.getCause());
    }
	
	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object message = e.getMessage();
		if(RudpLoggers.rootLogger.isDebugEnabled()) {
			if(message instanceof ChannelBuffer) {
				ChannelBuffer buffer = (ChannelBuffer) message;
				RudpLoggers.rootLogger.debug("write requested in netty, message: " + ChannelBufferHelper.toString(buffer));
			}
			else {
				RudpLoggers.rootLogger.debug("write requested in netty, message: " + message);
			}
		}
        ctx.sendDownstream(e);
    }
	
	@Override
	public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
		if(RudpLoggers.rootLogger.isDebugEnabled()) {
			RudpLoggers.rootLogger.debug("write completed in netty, written amount: " + e.getWrittenAmount());
		}
        ctx.sendUpstream(e);
    }
}
