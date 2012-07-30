package org.jenkinsci.plugins;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import jenkins.model.Jenkins;

@Extension
public class MailWatcherListener extends ItemListener {

    private final static Logger LOGGER = Logger.getLogger(
            MailWatcherListener.class.getName()
    );

    private final MailWatcherMailer mailer;
    private final String jenkinsRootUrl;

    public MailWatcherListener() {

        this(
                new MailWatcherMailer(),
                Jenkins.getInstance().getRootUrl()
        );
    }

    public MailWatcherListener(
            final MailWatcherMailer mailer,
            final String jenkinsRootUrl
    ) {

        this.mailer = mailer;
        this.jenkinsRootUrl = jenkinsRootUrl;
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName) {

        if ( !(item instanceof Job<?, ?>) ) return;

        notify(
                (Job<?, ?>) item,
                getSubject(oldName + " renamed to " + newName),
                getMessage(item)
        );

        logNotified("renamed");
    }

    @Override
    public void onUpdated(Item item) {

        if ( !(item instanceof Job<?, ?>) ) return;

        notify(
                (Job<?, ?>) item,
                getSubject(item.getName () + " updated"),
                getMessage(item)
        );

        logNotified("updated");
    }

    @Override
    public void onDeleted(Item item) {

        if ( !(item instanceof Job<?, ?>) ) return;

        notify(
                (Job<?, ?>) item,
                getSubject(item.getName () + " deleted"),
                getMessage(item)
        );

        logNotified("deleted");
    }

    private void notify (Job<?, ?> job, String subject, String body) {

        final MailWatcherProperty property = job
                .getProperty(MailWatcherProperty.class)
        ;

        // no recipients - do not notify
        if (property == null) return;

        try {

            mailer.send(property.getWatcherAddresses(), subject, body);
        } catch ( AddressException ex ) {

            log("Unable to parse address", ex);
        } catch ( MessagingException ex ) {

            log("Unable to notify", ex);
        }
    }


    private String getSubject(final String text) {

        return String.format("mail-watcher-plugin: Job %s", text );
    }


    private String getMessage(final Item item) {

        final String format = "%s%s";

        return String.format(
                format,
                jenkinsRootUrl,
                item.getUrl()
        );
    }

    private void logNotified(String state) {

        log("Mail sent for job " + state);
    }


    private void log(String state) {

        LOGGER.log(Level.INFO, state);
    }

    private void log(String state, Throwable ex) {

        LOGGER.log(Level.INFO, state, ex);
    }
}
