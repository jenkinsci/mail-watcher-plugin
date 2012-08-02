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
package org.jenkinsci.plugins;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import jenkins.model.Jenkins;

/**
 * Notify whenever Job configuration changes.
 *
 * Sends email do the list of recipients on following events: onRenamed,
 * onUpdated and onDeleted.
 *
 *
 * @author ogondza
 */
@Extension
public class WatcherItemListener extends ItemListener {

    private final MailWatcherMailer mailer;
    private final String jenkinsRootUrl;

    public WatcherItemListener() {

        this(
                new MailWatcherMailer(),
                Jenkins.getInstance().getRootUrl()
        );
    }

    public WatcherItemListener(
            final MailWatcherMailer mailer,
            final String jenkinsRootUrl
    ) {

        if (mailer == null) throw new IllegalArgumentException(
                "No mailer provided"
        );

        this.mailer = mailer;
        this.jenkinsRootUrl = jenkinsRootUrl == null
                ? "/"
                : jenkinsRootUrl
        ;
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName) {

        if ( !(item instanceof Job<?, ?>) ) return;

        final Job<?, ?> job = (Job<?, ?>) item;

        assert newName.equals(item.getName()): "Renamed item is supposed to have new name set";

        notify(job, "renamed from " + oldName, "");
    }

    @Override
    public void onUpdated(Item item) {

        if ( !(item instanceof Job<?, ?>) ) return;

        notify((Job<?, ?>) item, "updated", "");
    }

    @Override
    public void onDeleted(Item item) {

        if ( !(item instanceof Job<?, ?>) ) return;

        notify((Job<?, ?>) item, "deleted", "");
    }

    private void notify(
            final Job<?, ?> job, final String subject, final String body
    ) {

        new Notification(jenkinsRootUrl, job, subject, body).notify(mailer);
    }

    public static class Notification extends MailWatcherAbstractNotification {

        final Job<?, ?> job;
        final String subject;
        final String body;

        final WatcherJobProperty property;

        public Notification(
                final String jenkinsRootUrl,
                final Job<?, ?> job,
                final String subject,
                final String body
        ) {

            super(jenkinsRootUrl);

            this.job = job;
            this.subject = subject;
            this.body = body;

            this.property = job.getProperty(WatcherJobProperty.class);
        }

        @Override
        public String getUrl() {

            return job.getShortUrl();
        }

        @Override
        protected String getSubject() {

            return String.format("Job %s %s", job.getName (), subject);
        }

        @Override
        protected String getBody() {

            return body;
        }

        @Override
        public String getRecipients() {

            return property.getWatcherAddresses();
        }

        @Override
        protected boolean shouldNotify() {

            return property != null;
        }
    }
}
