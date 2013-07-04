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

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import jenkins.model.Jenkins;

/**
 * Notify whenever Job configuration changes.
 *
 * Sends email to the list of recipients on following events: onRenamed,
 * onUpdated and onDeleted.
 *
 * @author ogondza
 */
@Extension
public class WatcherItemListener extends ItemListener {

    private final MailWatcherMailer mailer;
    private final String jenkinsRootUrl;

    public WatcherItemListener() {

        this(
                new MailWatcherMailer(Jenkins.getInstance()),
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
        this.jenkinsRootUrl = jenkinsRootUrl;
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName) {

        if (!(item instanceof Job<?, ?>)) return;

        final Job<?, ?> job = (Job<?, ?>) item;

        getNotification().subject("renamed from " + oldName).send(job);
    }

    @Override
    public void onUpdated(Item item) {

        if (!(item instanceof Job<?, ?>)) return;

        getNotification().subject("updated").send(item);
    }

    @Override
    public void onDeleted(Item item) {

        if (!(item instanceof Job<?, ?>)) return;

        getNotification().subject("deleted").send(item);
    }

    private Notification.Builder getNotification() {

        return new Notification.Builder(mailer, jenkinsRootUrl);
    }

    private static class Notification extends MailWatcherNotification {

        public Notification(final Builder builder) {

            super(builder);
        }

        @Override
        protected String getSubject() {

            return String.format("Job %s %s", getName (), super.getSubject());
        }

        private static class Builder extends MailWatcherNotification.Builder {

            public Builder(final MailWatcherMailer mailer, final String jenkinsRootUrl) {

                super(mailer, jenkinsRootUrl);
            }

            @Override
            public void send(final Object o) {

                final Job<?, ?> job = (Job<?, ?>) o;

                final WatcherJobProperty property = job.getProperty(
                        WatcherJobProperty.class
                );

                if (property!=null) {

                    this.recipients(property.getWatcherAddresses());
                }

                url(job.getShortUrl());
                name(job.getName());

                new Notification(this).send();
            }
        }
    }
}
