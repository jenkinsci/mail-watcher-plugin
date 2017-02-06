/*
 * The MIT License
 *
 * Copyright (c) 2012 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.mailwatcher.jobConfigHistory;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.when;

import hudson.model.Job;
import hudson.plugins.jobConfigHistory.ConfigInfo;
import hudson.plugins.jobConfigHistory.JobConfigHistory;
import hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @since November 12, 2014
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(JobConfigHistoryProjectAction.class)
public class ConfigHistoryTest {
    static final String DATE_1 = "date1";
    static final String DATE_2 = "date2";
    static final String SHORT_URL = "http://shorturl.com/";
    @Mock ConfigInfo configInfo1;
    @Mock ConfigInfo configInfo2;
    @Mock Job job;
    @Mock JobConfigHistory jobConfigHistory;
    @Mock JobConfigHistoryProjectAction jobConfigHistoryProjectAction;

    @Before public void setup() {
        when(configInfo1.getDate()).thenReturn(DATE_1);
        when(configInfo2.getDate()).thenReturn(DATE_2);
    }

    private ConfigHistory make(JobConfigHistory plugin) {
        return new ConfigHistory(plugin);
    }

    private ConfigHistory make() {
        return make(jobConfigHistory);
    }

    @Test public void testLastChangeDiffUrlNull() {
        assertNull(make(null).lastChangeDiffUrl(job));
    }

    @Test public void testLastChangeDiffUrl() throws Exception {
        when(job.getAction(JobConfigHistoryProjectAction.class)).thenReturn(jobConfigHistoryProjectAction);
        doReturn(SHORT_URL).when(job).getShortUrl();
        doReturn(newArrayList(configInfo2, configInfo1)).when(jobConfigHistoryProjectAction).getJobConfigs();
        assertEquals(SHORT_URL + "jobConfigHistory/showDiffFiles?timestamp1=date1&timestamp2=date2",
                make().lastChangeDiffUrl(job));
    }
}
