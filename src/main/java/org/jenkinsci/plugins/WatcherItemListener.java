package org.jenkinsci.plugins;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import jenkins.model.Jenkins;

@Extension
public class WatcherItemListener extends ItemListener {

    private final MailWatcherMailer mailer;
    private final String jenkinsRootUrl;

    public WatcherItemListener() {

        this(
                new MailWatcherMailer(),
                Jenkins.getInstance().getRootUrl()
        );
    }

    public WatcherItemListener(
            final MailWatcherMailer mailer,
            final String jenkinsRootUrl
    ) {

        if (mailer == null) throw new IllegalArgumentException(
                "No mailer provided"
        );

        this.mailer = mailer;
        this.jenkinsRootUrl = jenkinsRootUrl == null
                ? "/"
                : jenkinsRootUrl
        ;
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName) {

        if ( !(item instanceof Job<?, ?>) ) return;

        final Job<?, ?> job = (Job<?, ?>) item;

        assert newName.equals(item.getName()): "Renamed item is supposed to have new name set";

        notify(job, "renamed from " + oldName, "");
    }

    @Override
    public void onUpdated(Item item) {

        if ( !(item instanceof Job<?, ?>) ) return;

        notify((Job<?, ?>) item, "updated", "");
    }

    @Override
    public void onDeleted(Item item) {

        if ( !(item instanceof Job<?, ?>) ) return;

        notify((Job<?, ?>) item, "deleted", "");
    }

    private void notify(
            final Job<?, ?> job, final String subject, final String body
    ) {

        new Notifier(jenkinsRootUrl, job, subject, body).notify(mailer);
    }

    public static class Notifier extends MailWatcherAbstractNotifier {

        final Job<?, ?> job;
        final String subject;
        final String body;

        final WatcherJobProperty property;

        public Notifier(
                final String jenkinsRootUrl,
                final Job<?, ?> job,
                final String subject,
                final String body
        ) {

            super(jenkinsRootUrl);

            this.job = job;
            this.subject = subject;
            this.body = body;

            this.property = job.getProperty(WatcherJobProperty.class);
        }

        @Override
        public String getUrl() {

            return job.getShortUrl();
        }

        @Override
        protected String getSubject() {

            return String.format("Job %s %s", job.getName (), subject);
        }

        @Override
        protected String getBody() {

            return body;
        }

        @Override
        public String getRecipients() {

            return property.getWatcherAddresses();
        }

        @Override
        protected boolean shouldNotify() {

            return property != null;
        }
    }
}
