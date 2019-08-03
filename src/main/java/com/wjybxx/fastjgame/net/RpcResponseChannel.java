package com.wjybxx.fastjgame.net;

/**
 * 返回rpc结果的通道
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface RpcResponseChannel {

	/**
	 * 返回rpc调用结果。
	 * 注意：该方法仅能调用一次，多次调用将抛出异常。
	 * @param rpcResponse rpc调用结果
	 */
	void write(RpcResponse rpcResponse);

	/**
	 * 返回rpc调用结果，立即发送，不存储在缓存中。
	 * 注意：该方法仅能调用一次，多次调用将抛出异常。
	 * @param rpcResponse rpc调用结果
	 */
	void writeAndFlush(RpcResponse rpcResponse);
}
