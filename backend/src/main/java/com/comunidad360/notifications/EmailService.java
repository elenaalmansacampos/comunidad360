package com.comunidad360.notifications;

import com.comunidad360.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MessageSource messageSource;
    private final AppProperties properties;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        MessageSource messageSource,
                        AppProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.messageSource = messageSource;
        this.properties = properties;
    }

    public EmailResult send(String to, String subjectKey, String bodyKey,
                            Object[] args, Locale locale) {
        if (!properties.getMail().isEnabled()) {
            return new EmailResult(false, "MAIL_DISABLED");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return new EmailResult(false, "MAIL_NOT_CONFIGURED");
        }
        try {
            String subject = messageSource.getMessage(subjectKey, args, subjectKey, locale);
            String body = messageSource.getMessage(bodyKey, args, bodyKey, locale);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(properties.getMail().getFrom(), properties.getMail().getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            return new EmailResult(true, null);
        } catch (Exception exception) {
            return new EmailResult(false, "MAIL_SEND_FAILED");
        }
    }

    public record EmailResult(boolean sent, String errorCode) {
    }

    public EmailResult sendWithAttachment(String to, String subjectKey, String bodyKey, Object[] args,
                                          Locale locale, String filename, byte[] content, String contentType) {
        if (!properties.getMail().isEnabled()) {
            return new EmailResult(false, "MAIL_DISABLED");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return new EmailResult(false, "MAIL_NOT_CONFIGURED");
        }
        try {
            String subject = messageSource.getMessage(subjectKey, args, subjectKey, locale);
            String body = messageSource.getMessage(bodyKey, args, bodyKey, locale);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.getMail().getFrom(), properties.getMail().getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.addAttachment(filename, new ByteArrayResource(content), contentType);
            mailSender.send(message);
            return new EmailResult(true, null);
        } catch (Exception exception) {
            return new EmailResult(false, "MAIL_SEND_FAILED");
        }
    }
}
