package org.jenkinsci.plugins;

import hudson.tasks.Mailer;
import hudson.util.FormValidation;

import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailWatcherMailer {

    final private Mailer.DescriptorImpl mailerDescriptor = Mailer.descriptor();

    public void send(final MailWatcherAbstractNotifier notifier) throws
            MessagingException, AddressException
    {

        if (!notifier.shouldNotify()) return;

        final MimeMessage msg = new MimeMessage(mailerDescriptor.createSession());
        msg.setFrom(new InternetAddress(mailerDescriptor.getAdminAddress()));
        msg.setSentDate(new Date());
        msg.setSubject(notifier.getMailSubject());
        msg.setText(notifier.getMailBody());
        msg.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(notifier.getRecipients())
        );

        Transport.send(msg);
    }

    public static FormValidation validateMailAddresses(final String value) {

        try {

            final InternetAddress[] addresses = InternetAddress.parse(value, false);

            if ( addresses.length == 0 ) {

                return FormValidation.error("Empty address list provided");
            }

            return validateAddresses(addresses);
        } catch ( AddressException ex ) {

            return FormValidation.error(
                    "Invalid address provided: " + ex.getMessage ()
            );
        }
    }

    private static FormValidation validateAddresses(final InternetAddress[] addresses) {

        for ( final InternetAddress address: addresses ) {

            final String rawAddress = address.toString();
            if ( rawAddress.indexOf("@") > 0 ) continue;

            return FormValidation.error(
                    rawAddress + " does not look like an email address"
            );
        }

        return FormValidation.ok();
    }
}
