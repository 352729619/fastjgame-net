package com.wjybxx.fastjgame.misc;

import io.netty.channel.Channel;

/**
 * 绑定端口结果
 * @author houlei
 * @version 1.0
 * date - 2019/8/5
 */
public class BindResult {
	/** 绑定到的channel */
	private final Channel channel;
	/** 成功绑定的端口 */
	private final HostAndPort hostAndPort;

	public BindResult(Channel channel, HostAndPort hostAndPort) {
		this.channel = channel;
		this.hostAndPort = hostAndPort;
	}

	public Channel getChannel() {
		return channel;
	}

	public HostAndPort getHostAndPort() {
		return hostAndPort;
	}
}
