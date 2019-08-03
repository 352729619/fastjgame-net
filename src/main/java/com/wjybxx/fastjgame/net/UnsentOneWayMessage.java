package com.wjybxx.fastjgame.net;

/**
 * 还未发送的单向消息
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class UnsentOneWayMessage implements UnsentMessage{

	/** 单向消息的结构体，具体怎么解析，不做限制 */
	private final Object message;

	public UnsentOneWayMessage(Object message) {
		this.message = message;
	}

	@Override
	public NetMessage build(long sequence) {
		return new OneWayMessage(sequence, message);
	}
}
