package com.ddpie.rudp.attribute;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的属性容器。
 * 该实现是线程安全的。
 * 
 * @author caobao
 *
 */
public final class DefaultAttributeContainer implements IAttributeContainer {
	private final Map<Object, Object> attrMap = new ConcurrentHashMap<Object, Object>(4);

	@Override
	public Object setAttribute(Object key, Object value) {
		return attrMap.put(key, value);
	}

	@Override
	public Object getAttribute(Object key) {
		return attrMap.get(key);
	}

	@Override
	public Object removeAttribute(Object key) {
		return attrMap.remove(key);
	}

}
