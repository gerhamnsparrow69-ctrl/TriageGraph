package in.ac.bitspilani.triagegraph.notification.infrastructure;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.notification.application.NotifyProperties;
import in.ac.bitspilani.triagegraph.notification.domain.AbstractNotificationChannel;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.NotificationMessage;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;

/** Plain-text email over SMTP; Mailpit catches it in dev (http://localhost:8025). */
@Component
class EmailChannel extends AbstractNotificationChannel {

    private final JavaMailSender mail;
    private final NotifyProperties props;

    EmailChannel(JavaMailSender mail, NotifyProperties props) {
        this.mail = mail;
        this.props = props;
    }

    @Override
    public ChannelType type() {
        return ChannelType.EMAIL;
    }

    @Override
    public boolean canReach(UserRef recipient) {
        return recipient.email() != null && !recipient.email().isBlank();
    }

    @Override
    protected void deliver(UserRef recipient, NotificationMessage message) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(props.mailFrom());
        m.setTo(recipient.email());
        m.setSubject("[TriageGraph] " + message.subject());
        m.setText("Hi " + recipient.name() + ",\n\n" + message.body() + "\n\n— TriageGraph service desk");
        mail.send(m);
    }
}
