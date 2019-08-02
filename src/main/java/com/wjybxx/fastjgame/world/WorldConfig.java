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

package com.wjybxx.fastjgame.world;

import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;

/**
 * 游戏世界配置
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/28
 * github - https://github.com/hl845740757
 */
public interface WorldConfig {

	/**
	 * World的帧率，最小1，最大1000
	 * @return 初始时指定的帧率，真实帧率应该是小于该值的。
	 */
	int framesPerSecond();

	/**
	 * 游戏世界的配置，通常指的是配置文件中读取的数据
	 * @return properties
	 */
	ConfigWrapper properties();
}
