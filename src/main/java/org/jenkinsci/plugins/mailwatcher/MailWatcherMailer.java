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

import hudson.tasks.Mailer;
import hudson.util.FormValidation;

import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Send email notification.
 *
 * @author ogondza
 */
public class MailWatcherMailer {

    final private Mailer.DescriptorImpl mailerDescriptor = Mailer.descriptor();

    /**
     * Send the notification
     *
     * @return sent MimeMessage or null if notification was not sent
     */
    public MimeMessage send(final MailWatcherNotification notification) throws
            MessagingException, AddressException
    {

        if (!notification.shouldNotify()) return null;

        final InternetAddress[] recipients = InternetAddress.parse(
                notification.getRecipients()
        );

        if (recipients.length == 0) return null;

        final MimeMessage msg = new MimeMessage(mailerDescriptor.createSession());
        msg.setFrom(new InternetAddress(mailerDescriptor.getAdminAddress()));
        msg.setSentDate(new Date());
        msg.setSubject(notification.getMailSubject());
        msg.setText(notification.getMailBody());
        msg.setRecipients(Message.RecipientType.TO, recipients);

        Transport.send(msg);

        return msg;
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
