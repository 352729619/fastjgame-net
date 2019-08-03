package com.wjybxx.fastjgame.net;

/**
 * 未发送的ack心跳包
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class UnsentAckPingPong implements UnsentMessage{

	public UnsentAckPingPong() {

	}

	@Override
	public AckPingPongMessage build(long sequence) {
		return new AckPingPongMessage(sequence);
	}
}
