/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 heimuheimu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.heimuheimu.naivecache.memcached.listener;

import com.heimuheimu.naivecache.memcached.cluster.MemcachedClusterClientListenerSkeleton;
import com.heimuheimu.naivemonitor.MonitorUtil;
import com.heimuheimu.naivemonitor.alarm.NaiveServiceAlarm;
import com.heimuheimu.naivemonitor.alarm.ServiceAlarmMessageNotifier;
import com.heimuheimu.naivemonitor.alarm.ServiceContext;

import java.util.List;
import java.util.Map;

/**
 * 该监听器可用于当 Memcached 集群客户端发生 Memcached 服务不可用或者从不可用状态恢复时，进行实时通知
 *
 * @author heimuheimu
 * @see NaiveServiceAlarm
 */
public class NoticeableMemcachedClusterClientListener extends MemcachedClusterClientListenerSkeleton {

    /**
     * 调用 Memcached 服务的项目名称
     */
    private final String project;

    /**
     * 调用 Memcached 服务的主机名称
     */
    private final String host;

    /**
     * 服务不可用报警器
     */
    private final NaiveServiceAlarm naiveServiceAlarm;

    /**
     * 构造一个 Memcached 集群客户端监听器，可在 Memcached 服务不可用或者从不可用状态恢复时，进行实时通知
     *
     * @param project 调用 Memcached 服务的项目名称
     * @param notifierList 服务不可用或从不可用状态恢复的报警消息通知器列表，不允许 {@code null} 或空
     * @throws IllegalArgumentException 如果消息通知器列表为 {@code null} 或空时，抛出此异常
     */
    public NoticeableMemcachedClusterClientListener(String project, List<ServiceAlarmMessageNotifier> notifierList) {
        this(project, notifierList, null);
    }

    /**
     * 构造一个 Memcached 集群客户端监听器，可在 Memcached 服务不可用或者从不可用状态恢复时，进行实时通知
     *
     * @param project 调用 Memcached 服务的项目名称
     * @param notifierList 服务不可用或从不可用状态恢复的报警消息通知器列表，不允许 {@code null} 或空
     * @param hostAliasMap 别名 Map，Key 为机器名， Value 为别名，允许为 {@code null}
     * @throws IllegalArgumentException 如果消息通知器列表为 {@code null} 或空时，抛出此异常
     */
    public NoticeableMemcachedClusterClientListener(String project, List<ServiceAlarmMessageNotifier> notifierList,
                                                    Map<String, String> hostAliasMap) throws IllegalArgumentException {
        this.project = project;
        this.naiveServiceAlarm = new NaiveServiceAlarm(notifierList);
        String host = MonitorUtil.getLocalHostName();
        if (hostAliasMap != null && hostAliasMap.containsKey(host)) {
            this.host = hostAliasMap.get(host);
        } else {
            this.host = host;
        }
    }

    @Override
    public void onRecovered(String host) {
        naiveServiceAlarm.onCrashed(getServiceContext(host));
    }

    @Override
    public void onClosed(String host) {
        naiveServiceAlarm.onRecovered(getServiceContext(host));
    }

    /**
     * 根据 Memcached 服务主机地址构造一个服务及服务所在的运行环境信息
     *
     * @param memcachedHost Memcached 服务主机地址
     * @return 服务及服务所在的运行环境信息
     */
    protected ServiceContext getServiceContext(String memcachedHost) {
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setName("Memcached");
        serviceContext.setHost(host);
        serviceContext.setProject(project);
        serviceContext.setRemoteHost(memcachedHost);
        return serviceContext;
    }
}
