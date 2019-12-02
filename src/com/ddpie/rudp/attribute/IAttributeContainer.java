package com.ddpie.rudp.attribute;

/**
 * 属性容器。
 * 
 * @author caobao
 *
 */
public interface IAttributeContainer {
	
	/**
	 * 设置一个属性。
	 * 
	 * @param key 属性对应的键
	 * @param value 属性
	 * @return 与 key关联的先前属性；如果 key没有映射关系，则返回 null
	 */
	Object setAttribute(Object key, Object value);
	
	/**
	 * 获取一个属性。
	 * 
	 * @param key 属性对应的键
	 * @return 属性
	 */
	Object getAttribute(Object key);
	
	/**
	 * 移除一个属性。
	 * 
	 * @param key 属性对应的键
	 * @return 与 key关联的先前属性；如果 key没有映射关系，则返回 null
	 */
	Object removeAttribute(Object key);
}
