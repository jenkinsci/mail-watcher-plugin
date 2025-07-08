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
package org.jenkinsci.plugins.mailwatcher;

import hudson.plugins.jobConfigHistory.ConfigInfo;
import hudson.plugins.jobConfigHistory.JobConfigHistory;
import hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction;
import org.jenkinsci.plugins.mailwatcher.jobConfigHistory.ConfigHistory;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WatcherItemListenerWithJobConfigHistoryPluginTest extends WatcherItemListenerTest {

    private static final String RHS_TIMESTAMP = "1999-04-04_18:00:00";
    private static final String LHS_TIMESTAMP = "1999-03-28_18:00:00";

    private static final String CONFIG_HISTORY_URL = String.format(
            "jobConfigHistory/showDiffFiles?timestamp1=%s&timestamp2=%s",
            RHS_TIMESTAMP, LHS_TIMESTAMP);

    @Override
    protected void checkBody() {
        super.checkBody();
        String change = notification.pairs().get("Change");
        assertThat(change, startsWith(INSTANCE_URL));
        assertThat(change, endsWith(CONFIG_HISTORY_URL));
    }

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();

        givenInstanceUrl(INSTANCE_URL);
        givenJobConfigHistoryPlugin();
        givenSomeHistory();
    }

    private void givenInstanceUrl(final String url) {
        when(mailer.absoluteUrl(Mockito.anyString())).thenAnswer(
                (Answer<URL>) invocation -> new URL(url + invocation.getArguments()[0]));
    }

    private void givenJobConfigHistoryPlugin() {
        final JobConfigHistory plugin = mock(JobConfigHistory.class);
        when(mailer.plugin(JobConfigHistory.class)).thenReturn(plugin);

        when(mailer.configHistory()).thenReturn(new ConfigHistory(plugin));
    }

    private void givenSomeHistory() {
        final JobConfigHistoryProjectAction action = Mockito.mock(JobConfigHistoryProjectAction.class);
        when(jobStub.getAction(JobConfigHistoryProjectAction.class)).thenReturn(action);

        final List<ConfigInfo> configs = Arrays.asList(
                config(LHS_TIMESTAMP), config(RHS_TIMESTAMP));
        when(action.getJobConfigs()).thenReturn(configs);
    }

    private ConfigInfo config(final String date) {
        final ConfigInfo config = mock(ConfigInfo.class);
        when(config.getDate()).thenReturn(date);
        return config;
    }
}
