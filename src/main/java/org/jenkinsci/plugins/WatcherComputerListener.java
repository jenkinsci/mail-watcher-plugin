package org.jenkinsci.plugins;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.ComputerListener;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.slaves.OfflineCause;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;

@Extension
public class WatcherComputerListener extends ComputerListener {

    private final MailWatcherMailer mailer;
    private final String jenkinsRootUrl;

    public WatcherComputerListener() {

        this(
                new MailWatcherMailer(),
                Jenkins.getInstance().getRootUrl()
        );
    }

    public WatcherComputerListener(
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

    public void onOffline(final Computer c) {

        notify(c, "marked offline", "");
    }

    public void onOnline(final Computer c, final TaskListener listener) {

        notify(c, "marked online", "");
    }

    public void onTemporarilyOffline(final Computer c, final OfflineCause cause) {

        notify(c, "marked temporarily offline", cause.toString());
    }

    public void onTemporarilyOnline(final Computer c) {

        notify(c, "marked temporarily online", "");
    }

    private void notify (
            final Computer computer, final String subject, final String body
    ) {

        new Notifier(jenkinsRootUrl, computer, subject, body).notify(mailer);
    }

    public static class Notifier extends MailWatcherAbstractNotifier {

        final Computer computer;
        final String subject;
        final String body;

        final WatcherNodeProperty property;

        public Notifier(
                final String jenkinsRootUrl,
                final Computer computer,
                final String subject,
                final String body
        ) {

            super(jenkinsRootUrl);

            this.computer = computer;
            this.subject = subject;
            this.body = body;

            this.property = getWatcherNodeProperty(computer);
        }

        private WatcherNodeProperty getWatcherNodeProperty(final Computer computer) {

            final Node node = computer.getNode();
            if (node==null) return null;

            final DescribableList<NodeProperty<?>, NodePropertyDescriptor> properties =
                    node.getNodeProperties()
            ;

            if (properties == null) return null;

            for(NodeProperty<?> property: properties) {

                if (property instanceof WatcherNodeProperty) {

                    return (WatcherNodeProperty) property;
                }
            }

            return null;
        }

        @Override
        public String getUrl() {

            return computer.getUrl();
        }

        @Override
        protected String getSubject() {

            return String.format("Computer %s %s", computer.getName(), subject);
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
