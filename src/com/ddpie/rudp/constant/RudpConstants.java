package com.ddpie.rudp.constant;

import java.nio.ByteOrder;

/**
 * RUDP的一些常量信息
 * 
 * @author caobao
 *
 */
public class RudpConstants {
	/** 消息包的字节顺序 */
	public static ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
	/** 消息包的最大长度（含消息头），超过此长度消息会分包发送 */
	public static int MESSAGE_MAX_LENGTH = 576;
	
	/** 每个会话发送缓存的最大容量，超过此容量，则会断开连接 */
	public static int MAX_WRITE_CACHE_SIZE = 10240;
	/** 每个会话读取缓存的最大容量，超过此容量，则会断开连接 */
	public static int MAX_READ_CACHE_SIZE = 256;
	/** 每个会话封包缓存最大容量，超过此容量，则会断开连接 */
	public static int MAX_FRAGMENT_CACHE_SIZE = 32;
	
	/** 会话滴答时间间隔，毫秒 */
	public static long SESSION_TICK_INTERVAL = 100;
	/** 会话活跃超时时间，毫秒 */
	public static long HEARTBEAT_TIMEOUT = 65000;
	/** 发送心跳的最大时间间隔，毫秒 */
	public static long HEARTBEAT_INTERVAL = 10000;
	/** 挂起状态持续的最大时间，超过该时间后，将关闭会话 ，毫秒*/
	public static long SUSPEND_TIMEOUT = 10 * 1000;
	/** 收到新消息后，从收到消息开始到发出反馈的最大等待时间，毫秒 */
	public static long MESSAGE_ACK_MAX_WAIT_TIME = 300;
	/** 消息重发的等待时间，毫秒 */
	public static long MESSAGE_RETRANSMIT_TIME = 500;
	/** 消息最大重发次数 */
	public static int MESSAGE_RETRANSMIT_MAX_COUNT = 20;
	
	/** 消息接收状态中存储的最大消息数量 */
	public static int RECEIVE_STATUS_MAX_MESSAGE_COUNT = 1024;
	
	/** 扫描RUDP连接Future的时间间隔，毫秒 */
	public static long SCAN_CONNECT_FUTURE_INTERVAL = 200;
	/** RUDP连接Future超时时间，毫秒 */
	public static long CONNECT_FUTURE_TIMEOUT = 1500;
	
	/** 尝试建立连接时，发送的握手消息的个数 */
	public static int HANDSHAKE_MESSAGE_COUNT = 5;
	/** 接到客户端连接请求后，发送反馈心跳消息的个数 */
	public static int HANDSHAKE_RESPONSE_MESSAGE_COUNT = 5;
	/** 尝试终止连接时，发送的关闭消息的个数 */
	public static int CLOSE_MESSAGE_COUNT = 5;
}
