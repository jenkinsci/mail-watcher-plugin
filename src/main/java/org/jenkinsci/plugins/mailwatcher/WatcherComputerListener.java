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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.ComputerListener;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.slaves.OfflineCause;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;

/**
 * Notify whenever Computer marked online/offline.
 *
 * Sends email do the list of recipients on following events: onOffline,
 * onOnline, onTemporarilyOffline and onTemporarilyOnline.
 *
 * @author ogondza
 */
@Extension
public class WatcherComputerListener extends ComputerListener {

    private final MailWatcherMailer mailer;
    private final String jenkinsRootUrl;

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public WatcherComputerListener() {

        this(
                new MailWatcherMailer(Jenkins.getInstance()),
                Jenkins.getInstance().getRootUrl()
        );
    }

    public WatcherComputerListener(
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
    public void onOffline(final Computer c) {

        getNotification().online(false)
                .subject("marked offline")
                .send(c)
        ;
    }

    // @Override
    public void onOffline(final Computer c, final OfflineCause cause) {

        if (cause == null) {
            onOffline(c);
            return;
        }

        getNotification().online(false)
                .subject("marked offline")
                .body(cause.toString())
                .send(c)
        ;
    }

    @Override
    public void onOnline(final Computer c, final TaskListener listener) {

        getNotification().online(true)
                .subject("marked online")
                .send(c)
        ;
    }

    @Override
    public void onTemporarilyOffline(final Computer c, final OfflineCause cause) {
        String causeString = "";
        if (cause!=null) {
            causeString = cause.toString();
        }
        getNotification().online(false)
                .subject("marked temporarily offline")
                .body(causeString)
                .send(c)
        ;
    }

    @Override
    public void onTemporarilyOnline(final Computer c) {

        getNotification().online(true)
                .subject("marked online (was temporarily offline)")
                .send(c)
        ;
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

            return String.format("Computer %s %s", getName(), super.getSubject());
        }

        private static class Builder extends MailWatcherNotification.Builder {

            private boolean online;

            public Builder(final MailWatcherMailer mailer, final String jenkinsRootUrl) {

                super(mailer, jenkinsRootUrl);
            }

            public Builder online(final boolean online) {

                this.online = online;
                return this;
            }

            @Override
            public void send(final Object o) {

                final Computer computer = (Computer) o;

                final WatcherNodeProperty property = getWatcherNodeProperty(computer);

                if (property!=null) {

                    final String recipients = this.online
                            ? property.getOnlineAddresses()
                            : property.getOfflineAddresses()
                    ;
                    this.recipients(recipients);
                }

                url(computer.getUrl());
                name(computer.getDisplayName());

                new Notification(this).send();
            }

            private static WatcherNodeProperty getWatcherNodeProperty(
                    final Computer computer
            ) {

                final Node node = computer.getNode();
                if (node==null) return null;

                final DescribableList<NodeProperty<?>, NodePropertyDescriptor> properties = (node instanceof Jenkins)
                        ? ((Jenkins) node).getGlobalNodeProperties()
                        : node.getNodeProperties()
                ;

                return properties.get(WatcherNodeProperty.class);
            }
        }
    }
}
