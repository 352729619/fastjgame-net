package com.wjybxx.fastjgame.eventloop;

import com.google.inject.Inject;

/**
 * NetEventLoop管理器，使得NetModule中的管理器可以获取运行环境。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class NetEventLoopManager {

	private NetEventLoopImp eventLoop;

	@Inject
	public NetEventLoopManager() {

	}
	/** 不允许外部调用，保证安全性 */
	void init(NetEventLoopImp eventLoop) {
		this.eventLoop = eventLoop;
	}

	public NetEventLoopImp eventLoop() {
		return eventLoop;
	}

	public boolean inEventLoop() {
		if (null == eventLoop) {
			throw new IllegalStateException();
		}
		return eventLoop.inEventLoop();
	}
}
