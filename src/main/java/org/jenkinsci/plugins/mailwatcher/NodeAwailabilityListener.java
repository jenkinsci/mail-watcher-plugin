/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
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
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Run;
import hudson.model.User;
import hudson.model.listeners.RunListener;
import hudson.slaves.OfflineCause;
import hudson.tasks.Mailer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jenkins.model.Jenkins;

@Extension
public class NodeAwailabilityListener extends RunListener<Run<?, ?>> {

    private final MailWatcherMailer mailer;
    private final String jenkinsRootUrl;

    public NodeAwailabilityListener() {

        this(
                new MailWatcherMailer(Jenkins.getInstance()),
                Jenkins.getInstance().getRootUrl()
        );
    }

    public NodeAwailabilityListener(
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
    public void onFinalized(Run<?, ?> r) {

        Computer computer = computer();

        if (!computer.isTemporarilyOffline()) return;

        User user = user(computer);
        if (user == null) return;

        if (!isIdle(computer)) return;

        String address = user.getProperty(Mailer.UserProperty.class).getAddress();

        getNotification().subject("Jenkins slave you have put offline is no longer occupied")
                .recipients(address)
                .send(r)
        ;
    }

    // TODO get rid of the once we bump Jenkins core dependency to 1.551+
    private User user(Computer computer) {
        Class<?> causeClass;
        try {
            causeClass = Class.forName("hudson.slaves.OfflineCause$UserCause");
        } catch (ClassNotFoundException ex) {
            // UserCause was introduced in 1.551. For earlier versions it would
            // be necessary to parse the message and create the user instance.
            // The notification is gracefully skipped instead.
            return null;
        }
        OfflineCause cause = computer.getOfflineCause();

        if (!causeClass.isAssignableFrom(cause.getClass())) return null;

        try {
            Method m = causeClass.getMethod("getUser");
            Object user = m.invoke(computer.getOfflineCause());
            return (User) user;
        } catch (SecurityException ex) {
            throw new AssertionError(ex);
        } catch (IllegalArgumentException ex) {
            throw new AssertionError(ex);
        } catch (NoSuchMethodException ex) {
            throw new AssertionError(ex);
        } catch (IllegalAccessException ex) {
            throw new AssertionError(ex);
        } catch (InvocationTargetException ex) {
            throw new AssertionError(ex);
        }
    }

    private Computer computer() {
        Thread t = Thread.currentThread();
        if (t instanceof Executor) {
            return ((Executor) t).getOwner();
        }

        throw new AssertionError("Whoa?!");
    }

    private boolean isIdle(Computer computer) {
        Thread current = Thread.currentThread();
        for (Executor e: computer.getExecutors()) {
            if (!e.isIdle() && e != current) return false;
        }

        for (Executor e: computer.getOneOffExecutors()) {
            if (!e.isIdle() && e != current) return false;
        }

        return true;
    }

    private Notification.Builder getNotification() {

        return new Notification.Builder(mailer, jenkinsRootUrl);
    }

    private static class Notification extends MailWatcherNotification {

        public Notification(Builder builder) {
            super(builder);
        }

        private static class Builder extends MailWatcherNotification.Builder {

            public Builder(final MailWatcherMailer mailer, final String jenkinsRootUrl) {
                super(mailer, jenkinsRootUrl);
            }

            @Override
            public void send(final Object o) {
                new Notification(this).send();
            }
        }
    }
}
