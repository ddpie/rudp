package com.ddpie.rudp.channelhandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.ddpie.rudp.log.RudpLoggers;

/**
 * 防止洪水攻击的Handler
 * 
 * @author caobao
 *
 */
public class FloodAttackHandler extends SimpleChannelUpstreamHandler {
	//每秒最大包数量 
	private static final int MAX_SECOND_PACKS = 64;
	//每分钟最大包数量
	private static final int MAX_MINUTE_PACKS = 512;
	//检测到多少次洪水包后, 禁止链接
	private static final int BLOCK_DETECT_COUNT = 5;
	//检测到洪水包后IP禁用时间毫秒
	private static final int BLOCK_IP_TIME = 1000 * 60 * 30;
	
	//MIS 系统 IP 地址
	private Set<String> misIpSet;
	//洪水攻击嫌疑列表<IP地址，嫌疑次数>
	private Map<String, AtomicInteger> possibleIps = new ConcurrentHashMap<String, AtomicInteger>();
	//被阻塞的IP列表<IP地址，Block的时间戳>
	private Map<String, Long> blockedIps = new ConcurrentHashMap<String, Long>();
	//洪水攻击记录列表<IP地址，洪水攻击记录>
	private Map<String, FloodRecord> recordMap = new ConcurrentHashMap<String, FloodRecord>();
	//定时器
	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("flood detector");
			return t;
		}
	});
	
	/**
	 * 构造防止洪水攻击的Handler
	 * 
	 * @param misIps
	 */
	public FloodAttackHandler(String misIps) {
		misIpSet = new HashSet<String>();
		if(misIps != null) {
			String[] misIpArray = misIps.split(",");
			for (String misIp : misIpArray) {
				misIpSet.add(misIp.trim());
			}
		}
		
		scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				long now = System.currentTimeMillis();
				for(Map.Entry<String, Long> entry : blockedIps.entrySet()) {
					String ip = entry.getKey();
					long blockedTime = entry.getValue();
					if(now > blockedTime + BLOCK_IP_TIME) {
						blockedIps.remove(ip);
						RudpLoggers.floodLogger.warn("remove flood attack blocked ip " + ip);
					}
				}
			}
		}, 0, 1, TimeUnit.MINUTES);
	}
	
	public void destroy() {
		scheduler.shutdown();
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		SocketAddress address = (SocketAddress)e.getRemoteAddress();
    	InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
		String ip = inetSocketAddress.getAddress().getHostAddress();
		
		if(isBlocked(ip)) {
			if(RudpLoggers.floodLogger.isInfoEnabled()) {
				RudpLoggers.floodLogger.info("ignore message from flood attack blocked ip " + ip);
			}
			return;
		}
		
        FloodRecord record = recordMap.get(ip);
        if(record == null) {
        	record = new FloodRecord();
        	recordMap.put(ip, record);
        }
        else {
        	//递增
        	record.lastSecondPacks++;
        	record.lastMinutePacks++;
        	
        	//当前秒
            long curSec = System.currentTimeMillis() / 1000;
            //当前分钟
            long curMin = curSec / 60;
            //上次秒
        	long lastSec = record.lastPackTime / 1000;
        	//上次分钟
        	long lastMin = lastSec / 60;
        	
        	//不是同一秒了，要清空秒计数器
        	if(lastSec != curSec) {
        		record.lastSecondPacks = 0;
        	}
        	//不是同一分钟了，要清空分钟计数器
        	if(lastMin != curMin) {
        		record.lastMinutePacks = 0;
        	}
        	
        	//更新时间戳
        	record.lastPackTime = System.currentTimeMillis();
        	
        	//秒计数器或分钟计数器超过指定阀值，则尝试Block
        	if(record.lastSecondPacks > MAX_SECOND_PACKS || record.lastMinutePacks > MAX_MINUTE_PACKS) {
        		if(isMisIp(ip)) {
    				RudpLoggers.floodLogger.warn("mis packet frequency too high " + address);
    			}
    			else {
    				RudpLoggers.floodLogger.warn("client packet frequency too high, add possible block, " + address);
    				addPossible(ip);
    				record.lastSecondPacks = 0;
    	        	record.lastMinutePacks = 0;
    				return;
    			}
        	}
        }
        
        if(RudpLoggers.floodLogger.isDebugEnabled()) {
        	RudpLoggers.floodLogger.debug(
        			String.format(
        					"received packets - second:%s, minute:%s", 
        					record.lastSecondPacks, 
        					record.lastMinutePacks));
        }
        ctx.sendUpstream(e);
    }
	
	/**
	 * 验证是否为允许访问的 MIS (后台)系统 IP 地址
	 * 
	 * @param ip
	 * @return
	 */
	private boolean isMisIp(String ip) {
		if (ip == null) {
			return false;
		}

		return misIpSet.contains(ip);
	}
	
	/**
	 * 已BLOCK次数
	 * 
	 * @param ip
	 * @return
	 */
	private boolean isBlocked(String ip) {
		return blockedIps.containsKey(ip);
	}

	/**
	 * 添加禁用IP
	 * 
	 * @param ip
	 */
	private void addPossible(String ip) {
		AtomicInteger blocks = possibleIps.get(ip);
		if (blocks == null) {
			blocks = new AtomicInteger();
			possibleIps.put(ip, blocks);
		}
		int count = blocks.incrementAndGet();
		if(count > BLOCK_DETECT_COUNT) {
			RudpLoggers.floodLogger.warn(
					"possible block times is larger than [" + BLOCK_DETECT_COUNT + "], so block it, " + ip);
			possibleIps.remove(ip);
			recordMap.remove(ip);
			blockedIps.put(ip, System.currentTimeMillis());
		}
	}
	
	/**
	 * 洪水记录
	 * 
	 * @author caobao
	 *
	 */
	private static class FloodRecord {
		/**
		 * 上次检查命令次数的时间戳
		 */
		volatile long lastPackTime = 0;
		/**
		 * 秒累计数据包数量
		 */
		volatile int lastSecondPacks = 0;
		/**
		 * 分累计数据包数量
		 */
		volatile int lastMinutePacks = 0;
	}
}
