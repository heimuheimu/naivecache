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

package com.heimuheimu.naivecache.monitor.reporter;

import com.heimuheimu.naivecache.constant.BeanStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 将监控数据 push 到 Falcon 系统抽象类，关于 Falcon 系统的更多信息请参考文档：
 * <p>
 *     <a href="https://book.open-falcon.org/zh/usage/data-push.html">https://book.open-falcon.org/zh/usage/data-push.html</a>
 * </p>
 *
 * @author heimuheimu
 */
public abstract class AbstractFalconReporter implements Closeable {

    /**
     * 上报周期时间，默认为 15 秒
     */
    protected static final int REPORT_INTERVAL_SECONDS = 15;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * 当前机器名
     */
    private final String endpoint;

    /**
     * 用于接收监控数据的 Falcon 接口 URL 地址
     */
    private final String pushUrl;

    private BeanStatusEnum state = BeanStatusEnum.UNINITIALIZED;

    /**
     * 构造一个基于 Falcon 系统的监控数据上报服务，不使用别名 Map
     *
     * @param pushUrl 用于接收监控数据的 Falcon 接口 URL 地址
     */
    public AbstractFalconReporter(String pushUrl) {
        this (pushUrl, null);
    }

    /**
     * 构造一个基于 Falcon 系统的监控数据上报服务
     *
     * @param pushUrl 用于接收监控数据的 Falcon 接口 URL 地址
     * @param endpointAliasMap Endpoint 别名 Map，Key 为机器名， Value 为别名，允许为 {@code null}
     */
    public AbstractFalconReporter(String pushUrl, Map<String, String> endpointAliasMap) {
        String endpoint = "unknown";
        try {
            InetAddress localInetAddress = InetAddress.getLocalHost();
            String hostName = localInetAddress.getHostName();
            if (endpointAliasMap != null && endpointAliasMap.containsKey(hostName)) {
                endpoint = endpointAliasMap.get(hostName);
            } else {
                endpoint = hostName;
            }
            logger.info("Endpoint: `{}`. Hostname: `{}`. Alias Map: `{}`.", endpoint, hostName, endpointAliasMap);
        } catch (Exception e) {//ignore exception
            logger.error("Get endpoint failed.", e);
        } finally {
            this.endpoint = endpoint;
        }
        this.pushUrl = pushUrl;
    }

    /**
     * 初始化当前 基于 Falcon 系统的监控数据上报服务
     */
    public synchronized void init() {
        if (state == BeanStatusEnum.UNINITIALIZED) {
            //noinspection Convert2Lambda
            executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    List<FalconData> pushDataList;
                    String pushJsonData = "";
                    try {
                        pushDataList = getPushDataList();
                        pushJsonData = toJson(pushDataList);
                        if (!pushDataList.isEmpty()) {
                            URL url = new URL(pushUrl);
                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                            urlConnection.setConnectTimeout(5000);
                            urlConnection.setReadTimeout(5000);
                            urlConnection.setRequestMethod("POST");
                            urlConnection.setDoOutput(true);

                            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                            wr.writeBytes(pushJsonData);
                            wr.flush();
                            wr.close();

                            int responseCode = urlConnection.getResponseCode();
                            if (responseCode != 200) {
                                logger.error("Push data to Falcon failed. Error response code: `{}`. Url: `{}`. Push json data: `{}`",
                                        responseCode, pushUrl, pushJsonData);
                            } else {
                                logger.debug("Push data success: `{}`.", pushJsonData);
                            }

                            urlConnection.disconnect();
                        } else {
                            logger.error("Empty push data list. No data report to Falcon. Url: `{}`. Json data: `{}`",
                                    pushUrl, pushJsonData);
                        }
                    } catch (Exception e) {
                        logger.error("Push data to falcon failed. Url :`" + pushUrl + "`. Push json data: `" + pushJsonData + "`.", e);
                    }
                }

            }, REPORT_INTERVAL_SECONDS, REPORT_INTERVAL_SECONDS, TimeUnit.SECONDS);
            state = BeanStatusEnum.NORMAL;
        }
    }

    @Override
    public synchronized void close() {
        if (state != BeanStatusEnum.CLOSED) {
            state = BeanStatusEnum.CLOSED;
            executorService.shutdown();
        }
    }

    /**
     * 获得监控数据列表
     *
     * @return 监控数据列表
     */
    protected abstract List<FalconData> getPushDataList();

    /**
     * 将监控数据列表转换为 Json 格式字符串后返回
     *
     * @param pushDataList 监控数据列表
     * @return 监控数据列表对应的 Json 格式字符串
     */
    private String toJson(List<FalconData> pushDataList) {
        StringBuilder buffer = new StringBuilder("[");
        if (!pushDataList.isEmpty()) {
            for (FalconData pushData : pushDataList) {
                buffer.append(pushData.toJson()).append(",");
            }
            buffer.deleteCharAt(buffer.length() - 1);
        }
        return buffer.append("]").toString();
    }

    /**
     * 创建一个默认的 Falcon 监控数据项并返回，endpoint、timestamp、step 值已进行设置
     *
     * @return 默认的 Falcon 监控数据项
     */
    protected FalconData create() {
        FalconData data = new FalconData();
        data.endpoint = this.endpoint;
        data.timestamp = System.currentTimeMillis() / 1000;
        data.step = REPORT_INTERVAL_SECONDS;
        return data;
    }

    /**
     * Falcon 监控数据项，字段含义请参考文档：
     * <p>
     *     <a href="https://book.open-falcon.org/zh/usage/data-push.html">https://book.open-falcon.org/zh/usage/data-push.html</a>
     * </p>
     */
    protected static class FalconData {

        /**
         * 标明 Metric 的主体(属主)，比如 metric 是 cpu_idle，那么 Endpoint 就表示这是哪台机器的 cpu_idle
         */
        protected String endpoint;

        /**
         * 最核心的字段，代表这个采集项具体度量的是什么, 比如是 cpu_idle 呢，还是 memory_free, 还是 qps
         */
        protected String metric;

        /**
         * 表示汇报该数据时的 unix 时间戳，注意是整数，代表的是秒
         */
        protected long timestamp;

        /**
         * 表示该数据采集项的汇报周期，这对于后续的配置监控策略很重要，必须明确指定
         */
        protected int step;

        /**
         * 代表该 metric 在当前时间点的值，float64
         */
        protected double value;

        /**
         * 只能是 COUNTER 或者 GAUGE 二选一，前者表示该数据采集项为计时器类型，后者表示其为原值 (注意大小写)
         * <ul>
         *     <li>GAUGE：即用户上传什么样的值，就原封不动的存储</li>
         *     <li>COUNTER：指标在存储和展现的时候，会被计算为 speed，即（当前值 - 上次值）/ 时间间隔</li>
         * </ul>
         */
        protected String counterType = "GAUGE";

        /**
         * 一组逗号分割的键值对, 对 metric 进一步描述和细化, 可以是空字符串. 比如 idc=lg，比如 service=xbox 等，多个 tag 之间用逗号分割
         */
        protected String tags = "module=naivecache";

        /**
         * 将当前 Falcon 监控数据项转换为 Json 格式字符串后返回
         *
         * @return 当前 Falcon 监控数据项对应的 Json 格式字符串
         */
        protected String toJson() {
            return "{\"endpoint\":\"" + endpoint +
                    "\",\"metric\":\"" + metric +
                    "\",\"timestamp\":" + timestamp +
                    ",\"step\":" + step +
                    ",\"value\":" + value +
                    ",\"counterType\":\"" + counterType +
                    "\",\"tags\":\"" + tags + "\"}";
        }

    }

}
