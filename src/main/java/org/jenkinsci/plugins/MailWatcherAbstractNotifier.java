package org.jenkinsci.plugins;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

public abstract class MailWatcherAbstractNotifier {

    private final static Logger LOGGER = Logger.getLogger(
            MailWatcherAbstractNotifier.class.getName()
    );

    private static final String MAIL_WATCHER_PLUGIN = "mail-watcher-plugin: ";
    private final String jenkinsRootUrl;

    public MailWatcherAbstractNotifier(final String jenkinsRootUrl) {

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
