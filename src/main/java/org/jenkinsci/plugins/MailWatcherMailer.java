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

    public void send (
            final String recipients,
            final String subject,
            final String body
    ) throws MessagingException, AddressException {

        final MimeMessage msg = new MimeMessage(mailerDescriptor.createSession());
        msg.setFrom(new InternetAddress(mailerDescriptor.getAdminAddress()));
        msg.setSentDate(new Date());
        msg.setSubject(subject);
        msg.setText(body);
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));

        Transport.send(msg);
    }
}
