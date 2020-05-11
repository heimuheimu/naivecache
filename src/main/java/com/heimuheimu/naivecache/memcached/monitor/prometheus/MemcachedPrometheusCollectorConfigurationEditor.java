/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 heimuheimu
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

package com.heimuheimu.naivecache.memcached.monitor.prometheus;

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * MemcachedPrometheusCollectorConfiguration 类型转换器，支持在 Spring 配置文件中通过字符串形式配置 MemcachedPrometheusCollectorConfiguration，
 * 字符串格式为：name, host1, host2, host3...
 *
 * @author heimuheimu
 * @since 1.2
 */
public class MemcachedPrometheusCollectorConfigurationEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Fails to parse `MemcachedPrometheusCollectorConfiguration`: `text could not be null or empty`.");
        }
        String[] parts = text.split(",");
        if (parts.length > 1) {
            String name = parts[0].trim();
            List<String> hostList = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                hostList.add(parts[i].trim());
            }
            setValue(new MemcachedPrometheusCollectorConfiguration(name, hostList));
        } else {
            throw new IllegalArgumentException("Fails to parse `MemcachedPrometheusCollectorConfiguration`: `invalid text`. `text`:`"
                    + text + "`.");
        }
    }

    @Override
    public String getAsText() {
        MemcachedPrometheusCollectorConfiguration configuration = (MemcachedPrometheusCollectorConfiguration) getValue();
        if (configuration != null) {
            StringBuilder buffer = new StringBuilder(configuration.getName());
            for (String host : configuration.getHostList()) {
                buffer.append(", ").append(host);
            }
            return buffer.toString();
        } else {
            return "";
        }
    }
}
