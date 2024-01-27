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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Job;
import hudson.plugins.jobConfigHistory.ConfigInfo;
import hudson.plugins.jobConfigHistory.JobConfigHistory;
import hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction;

import java.util.List;

/**
 * @author ogondza
 */
public class ConfigHistory {

    private final JobConfigHistory plugin;

    public ConfigHistory(final JobConfigHistory plugin) {

        this.plugin = plugin;
    }

    public @CheckForNull String lastChangeDiffUrl(final @NonNull Job<?, ?> job) {

        if (plugin == null) return null;

        final List<ConfigInfo> configs = storedConfigurations(job);
        if (configs == null || configs.size() < 2) return null;

        return String.format(
                "%sjobConfigHistory/showDiffFiles?timestamp1=%s&timestamp2=%s",
                job.getShortUrl(), configs.get(1).getDate(), configs.get(0).getDate()
        );
    }

    private @CheckForNull List<ConfigInfo> storedConfigurations(final Job<?, ?> job) {

        final JobConfigHistoryProjectAction action = job.getAction(JobConfigHistoryProjectAction.class);

        if (action == null) return null;

        return action.getJobConfigs();
    }
}
