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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

/**
 * Abstract notification for Jenkins.
 *
 * @author ogondza
 */
public abstract class MailWatcherAbstractNotification {

    private final static Logger LOGGER = Logger.getLogger(
            MailWatcherAbstractNotification.class.getName()
    );

    private static final String MAIL_WATCHER_PLUGIN = "mail-watcher-plugin: ";
    private final String jenkinsRootUrl;

    public MailWatcherAbstractNotification(final String jenkinsRootUrl) {

        this.jenkinsRootUrl = jenkinsRootUrl == null
                ? "/"
                : jenkinsRootUrl
        ;
    }

    protected abstract String getSubject();
    protected abstract String getBody();

    public abstract String getUrl();
    public abstract String getRecipients();

    private String getArtefactUrl() {

        return jenkinsRootUrl + this.getUrl();
    }

    protected boolean shouldNotify() {

        return true;
    }

    public final String getMailSubject() {

        return MAIL_WATCHER_PLUGIN + this.getSubject();
    }

    public final String getMailBody() {

        return this.getBody() + "\n\nUrl: " + this.getArtefactUrl();
    }

    public final void notify(final MailWatcherMailer mailer) {

        try {

            mailer.send(this);

        } catch ( AddressException ex ) {

            log(MAIL_WATCHER_PLUGIN + "unable to parse address", ex);
            return;
        } catch ( MessagingException ex ) {

            log(MAIL_WATCHER_PLUGIN + "unable to notify", ex);
            return;
        }

        log(MAIL_WATCHER_PLUGIN + "notified: " + this.getSubject());
    }

    private void log(String state) {

        LOGGER.log(Level.INFO, state);
    }

    private void log(String state, Throwable ex) {

        LOGGER.log(Level.INFO, state, ex);
    }
}
