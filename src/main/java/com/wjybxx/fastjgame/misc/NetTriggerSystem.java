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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.trigger.Timer;

/**
 * World使用的触发器系统
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/2
 * github - https://github.com/hl845740757
 */
public interface NetTriggerSystem {

	/**
	 * 添加一个timer。
	 * 由子类选择如何存储timer。
	 * @param timer 定时器
	 */
	void addTimer(Timer timer);

	/**
	 * 检查timer执行
	 */
	void tickTrigger();

	/**
	 * 指定timer的执行间隔被修改了，优先级发生了改变。
	 * 它的拥有者可能需要进行一些操作，以保证timer优先级的正确性。
	 * @param timer 该定时器的延迟时间被修改；
	 */
	void priorityChanged(Timer timer);
}
