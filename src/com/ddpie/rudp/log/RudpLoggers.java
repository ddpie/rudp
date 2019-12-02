package com.ddpie.rudp.log;

import org.apache.log4j.Logger;

/**
 * RUDP日志集中类。
 * 
 * @author caobao
 *
 */
public class RudpLoggers {
	/** 基础日志 */
	public static final Logger rootLogger = Logger.getLogger("rudp_root");
	/** 上行心跳日志 */
	public static final Logger heartbeatUpLogger = Logger.getLogger("rudp_heartbeatUp");
	/** 下行心跳日志 */
	public static final Logger heartbeatDownLogger = Logger.getLogger("rudp_heartbeatDown");
	/** 会话日志 */
	public static final Logger sessionLogger = Logger.getLogger("rudp_session");
	/** 上行业务日志 */
	public static final Logger businessUpLogger = Logger.getLogger("rudp_businessUp");
	/** 下行业务日志 */
	public static final Logger businessDownLogger = Logger.getLogger("rudp_businessDown");
	/** 消息处理器日志 */
	public static final Logger processorLogger = Logger.getLogger("rudp_processor");
	/** 可靠性日志 */
	public static final Logger reliabilityLogger = Logger.getLogger("rudp_reliability");
	/** 编解码日志 */
	public static final Logger codecLogger = Logger.getLogger("rudp_codec");
	/** 洪水攻击日志 */
	public static final Logger floodLogger = Logger.getLogger("rudp_flood");
}
