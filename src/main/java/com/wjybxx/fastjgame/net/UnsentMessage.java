/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.net;

/**
 * 未发送的消息，它只由网络层使用，并且不会共享，因此字段不必final.
 * (final会增加一定的消耗)
 *
 * Q: 为什么要有这样一个抽象？
 * A: 使得未发送的包不占用sequence，未发送的不会占用资源！
 *    这样可以支持消息插队，尤其是在有同步调用的时候，可以将数据包插到未发送的消息的最前面，甚至直接发送。
 *    Rpc同步调用可以排在rpc异步调用前面，rpc调用又可以排在单向消息前面。 不同的消息具有不同的紧迫性。
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
