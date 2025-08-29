package org.bits.diamabankwalletf.service;

import org.bits.diamabankwalletf.dto.CommentRequest;
import org.bits.diamabankwalletf.dto.ComplaintRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.to}")
    private String feedbackEmail;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendCustomerComment(CommentRequest request) throws MessagingException {
        Context context = new Context();
        context.setVariable("userName", request.getUserName());
        context.setVariable("userPhone", request.getUserPhone());
        context.setVariable("category", request.getCategory());
        context.setVariable("subject", request.getSubject());
        context.setVariable("comment", request.getComment());
        context.setVariable("email", request.getEmail());

        String htmlContent = templateEngine.process("comment-email", context);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        helper.setTo(feedbackEmail);
        helper.setSubject("Nouveau commentaire: " + request.getSubject());
        helper.setText(htmlContent, true); // HTML email

        mailSender.send(mimeMessage);
    }

    public void sendComplaintEmail(ComplaintRequest request) throws MessagingException {
        Context context = new Context();
        context.setVariable("userName", request.getUserName());
        context.setVariable("userPhone", request.getUserPhone());
        context.setVariable("complaintType", request.getComplaintType());
        context.setVariable("priority", request.getPriority());
        context.setVariable("title", request.getTitle());
        context.setVariable("description", request.getDescription());
        context.setVariable("incidentDate", request.getIncidentDate());
        context.setVariable("transactionRef", request.getTransactionRef());
        context.setVariable("contactMethod", request.getContactMethod());
        context.setVariable("alternateContact", request.getAlternateContact());

        String htmlContent = templateEngine.process("complaint-template", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(feedbackEmail);
        helper.setSubject("📌 Nouvelle Plainte Client");
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

}
