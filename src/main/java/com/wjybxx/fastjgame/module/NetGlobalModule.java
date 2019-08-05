package com.wjybxx.fastjgame.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.wjybxx.fastjgame.manager.NetConfigManager;

/**
 * Net模块全局单例。
 * 这里必须都是线程安全的类
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/5
 */
public class NetGlobalModule extends AbstractModule {

	@Override
	protected void configure() {
		binder().requireExplicitBindings();
		bind(NetConfigManager.class).in(Singleton.class);
	}
}
