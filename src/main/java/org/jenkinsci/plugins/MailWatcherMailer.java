package org.jenkinsci.plugins;

import hudson.tasks.Mailer;

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
}
