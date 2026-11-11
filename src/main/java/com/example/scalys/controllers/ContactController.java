package com.example.scalys.controllers;

import com.example.scalys.dtos.ContactRequest;
import com.example.scalys.services.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final ContactService contacts;
    public ContactController(ContactService contacts) { this.contacts = contacts; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Message submit(@Valid @RequestBody ContactRequest request, HttpServletRequest http) {
        contacts.submit(request, http.getRemoteAddr());
        return new Message("Đã nhận câu hỏi. Shop sẽ liên hệ với bạn sớm.");
    }

    public record Message(String message) { }
}
