package org.bits.diamabankwalletf.controller;

import org.bits.diamabankwalletf.dto.CommentRequest;
import org.bits.diamabankwalletf.dto.ComplaintRequest;
import org.bits.diamabankwalletf.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.*;

import jakarta.mail.MessagingException;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final EmailService emailService;

    public CommentController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/comments")
    public ResponseEntity<String> sendComment(@RequestBody CommentRequest request) {
        try {
            emailService.sendCustomerComment(request);
            return ResponseEntity.ok("Comment sent successfully.");
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending email: " + e.getMessage());
        }
    }

    @PostMapping("/complaints")
    public ResponseEntity<String> submitComplaint(@RequestBody ComplaintRequest request) {
        try {
            emailService.sendComplaintEmail(request);
            return ResponseEntity.ok("Complaint submitted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to submit complaint: " + e.getMessage());
        }
    }
}
