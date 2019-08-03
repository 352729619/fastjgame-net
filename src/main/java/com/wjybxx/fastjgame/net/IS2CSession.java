package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.misc.HostAndPort;

/**
 * 监听接口创建的session
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface IS2CSession extends Session{

	/**
	 * 绑定的本地端口
	 */
	HostAndPort localAddress();
}
