package com.wjybxx.fastjgame.net;

/**
 * 未发送的消息。
 * Q: 为什么要有这样一个抽象？
 * A: 使得未发送的包不占用sequence，未发送的不会占用资源！
 *    这样可以支持消息插队，尤其是在有同步调用的时候，可以将数据包插到未发送的消息的最前面，甚至直接发送。
 *    Rpc同步调用可以排在rpc异步调用前面，rpc调用又可以排在单向消息前面。 不同的消息具有不同的紧迫性。
 *    (当然这个看需求)
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface UnsentMessage {

	/**
	 * 构建为正式的发送消息。
	 * 该方法只会被调用一次，会在将要发送的时候调用。
	 *
	 * @param sequence 该包指定的编号，消息的序号必须为该编号。
	 * @return 用于真正发送的消息体结构。
	 */
	NetMessage build(long sequence);
}
