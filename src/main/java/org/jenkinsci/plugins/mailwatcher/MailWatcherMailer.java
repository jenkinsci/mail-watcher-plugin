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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.User;
import hudson.plugins.jobConfigHistory.JobConfigHistory;
import hudson.tasks.Mailer;
import hudson.util.FormValidation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.mailwatcher.jobConfigHistory.ConfigHistory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Send email notification.
 *
 * @author ogondza
 */
public class MailWatcherMailer {

    private final @NonNull Mailer.DescriptorImpl mailerDescriptor;
    private final @NonNull Jenkins jenkins;
    private final @NonNull ConfigHistory configHistory;

    /*package*/ MailWatcherMailer(final @NonNull Jenkins jenkins) {

        this.jenkins = jenkins;
        this.mailerDescriptor = jenkins.getDescriptorByType(Mailer.DescriptorImpl.class);
        this.configHistory = new ConfigHistory(plugin(JobConfigHistory.class));
    }

    /*package*/ @NonNull User getDefaultInitiator() {

        final User current = User.current();
        return current != null
                ? current
                : User.getUnknown()
        ;
    }

    /*package*/ @CheckForNull
    <GC extends GlobalConfiguration> GC plugin(final @NonNull Class<GC> clazz) {

        return GlobalConfiguration.all().get(clazz);
    }

    /*package*/ @NonNull URL absoluteUrl(final @NonNull String url) {

        try {

            return new URL(jenkins.getRootUrl() + url);
        } catch (MalformedURLException ex) {

            throw new AssertionError(ex);
        }
    }

    /*package*/ @NonNull ConfigHistory configHistory() {

        return configHistory;
    }

    /**
     * Send the notification
     *
     * @return sent MimeMessage or null if notification was not sent
     */
    public MimeMessage send(final MailWatcherNotification notification) throws
            MessagingException
    {

        if (!notification.shouldNotify()) return null;

        final InternetAddress[] recipients = InternetAddress.parse(
                notification.getRecipients()
        );

        if (recipients.length == 0) return null;

        final MimeMessage msg = new MimeMessage(mailerDescriptor.createSession());
        msg.setFrom(new InternetAddress(mailerDescriptor.getAdminAddress()));
        final String replyToAddress = mailerDescriptor.getReplyToAddress();
        if (replyToAddress != null) {
            msg.setReplyTo(InternetAddress.parse(replyToAddress));
        }

        msg.setSentDate(new Date());
        msg.setSubject(notification.getMailSubject());
        msg.setText(notification.getMailBody());
        msg.setRecipients(Message.RecipientType.TO, recipients);

        send(msg);

        return msg;
    }

    @Restricted(NoExternalUse.class)
    /*package*/ void send(final MimeMessage msg) throws MessagingException {
        Transport.send(msg);
    }

    /**
     * Validate list of email addresses.
     *
     * @param addressesCandidate String representing list of addresses
     * @return FormValidation representing state of validation
     */
    public static FormValidation validateMailAddresses(
            final String addressesCandidate
    ) {

        try {

            final InternetAddress[] addresses = InternetAddress.parse(
                    addressesCandidate, false
             );

            if (addresses.length == 0) {

                return FormValidation.warning("Empty address list provided");
            }

            return validateAddresses(addresses);
        } catch (AddressException ex) {

            return FormValidation.error(
                    "Invalid address provided: " + ex.getMessage ()
            );
        }
    }

    private static FormValidation validateAddresses(
            final InternetAddress[] addresses
    ) {

        for (final InternetAddress address: addresses) {

            final String rawAddress = address.toString();
            if (rawAddress.indexOf("@") > 0) continue;

            return FormValidation.error(
                    rawAddress + " does not look like an email address"
            );
        }

        return FormValidation.ok();
    }
}
