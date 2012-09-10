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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;

/**
 * Abstract notification for Jenkins.
 *
 * @author ogondza
 */
public abstract class MailWatcherNotification {

    private final static Logger LOGGER = Logger.getLogger(
            MailWatcherNotification.class.getName()
    );

    private static final String MAIL_WATCHER_PLUGIN = "mail-watcher-plugin: ";

    final private String subject;
    final private String body;
    final private String recipients;

    final private String url;
    final private String name;

    final private String jenkinsRootUrl;

    final private MailWatcherMailer mailer;

    public MailWatcherNotification(final Builder builder) {

        this.subject = builder.subject;
        this.body = builder.body;
        this.recipients = builder.recipients;

        this.url = builder.url;
        this.name = builder.name;

        this.jenkinsRootUrl = builder.jenkinsRootUrl;

        this.mailer = builder.mailer;
    }

    protected String getSubject() {

        return subject;
    }

    protected String getBody() {

        return body;
    }

    public String getRecipients() {

        return recipients;
    }

    public String getUrl() {

        return url;
    }

    public String getName() {

        return name;
    }

    private String getArtefactUrl() {

        return jenkinsRootUrl + this.getUrl();
    }

    protected boolean shouldNotify() {

        return recipients != null;
    }

    public final String getMailSubject() {

        return MAIL_WATCHER_PLUGIN + this.getSubject();
    }

    public final String getMailBody() {

        return this.getBody() + "\n\nUrl: " + this.getArtefactUrl();
    }

    public final void send() {

        try {

            final MimeMessage msg = mailer.send(this);
            if (msg != null) {

                log(MAIL_WATCHER_PLUGIN + "notified: " + this.getSubject());
            }
        } catch (AddressException ex) {

            log(MAIL_WATCHER_PLUGIN + "unable to parse address", ex);
        } catch (MessagingException ex) {

            log(MAIL_WATCHER_PLUGIN + "unable to notify", ex);
        }
    }

    private void log(String state) {

        LOGGER.log(Level.INFO, state);
    }

    private void log(String state, Throwable ex) {

        LOGGER.log(Level.INFO, state, ex);
    }

    public static abstract class Builder {

        final private MailWatcherMailer mailer;
        final private String jenkinsRootUrl;

        private String subject = "";
        private String body = "";
        private String recipients;

        private String url = "";
        private String name = "";

        public Builder(final MailWatcherMailer mailer, final String jenkinsRootUrl) {

            this.mailer = mailer;

            this.jenkinsRootUrl = jenkinsRootUrl == null
                    ? "/"
                    : jenkinsRootUrl
            ;
        }

        public Builder subject(final String subject) {

            this.subject = subject;
            return this;
        }

        public Builder body(final String body) {

            this.body = body;
            return this;
        }

        public Builder recipients(final String recipients) {

            this.recipients = recipients;
            return this;
        }

        protected Builder url(final String url) {

            this.url = url;
            return this;
        }

        protected Builder name(final String name) {

            this.name = name;
            return this;
        }

        abstract public void send(final Object object);
    }
}
