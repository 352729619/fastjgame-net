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

import com.wjybxx.fastjgame.manager.NetTimeManager;
import com.wjybxx.fastjgame.trigger.SystemTimeHelper;
import com.wjybxx.fastjgame.trigger.Timer;
import com.wjybxx.fastjgame.trigger.TriggerSystemImp;

/**
 * WorldTriggerSystem的实现
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/2
 * github - https://github.com/hl845740757
 */
public class NetTriggerSystemImp implements NetTriggerSystem {

	private final TriggerSystemImp triggerSystemImp = new TriggerSystemImp();

	private final SystemTimeHelper systemTimeHelper;

	public NetTriggerSystemImp(SystemTimeHelper systemTimeHelper) {
		this.systemTimeHelper = systemTimeHelper;
	}

	/**
	 * 添加一个定时器
	 * @param timer 定时器
	 */
	public void addTimer(Timer timer) {
		triggerSystemImp.addTimer(timer, systemTimeHelper.getSystemMillTime());
	}

	/**
	 * 检查timer执行
	 */
	public void tickTrigger() {
		triggerSystemImp.tickTrigger(systemTimeHelper.getSystemMillTime());
	}

	@Override
	public void priorityChanged(Timer timer) {
		triggerSystemImp.priorityChanged(timer);
	}
}
