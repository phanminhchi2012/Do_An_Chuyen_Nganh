package com.example.scalys.controllers;

import com.example.scalys.dtos.ContactResponse;
import com.example.scalys.dtos.ContactPageResponse;
import com.example.scalys.models.ContactMessage;
import com.example.scalys.services.ContactService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contacts")
public class AdminContactController {
    private final ContactService contacts;
    public AdminContactController(ContactService contacts) { this.contacts = contacts; }

    @GetMapping
    public ContactPageResponse list(@RequestParam(required = false) ContactMessage.Status status,
                                      @RequestParam(defaultValue = "0") int page) {
        return contacts.list(status, page);
    }

    @GetMapping("/new-count")
    public Map<String, Long> newCount() { return Map.of("count", contacts.newCount()); }

    @PatchMapping("/{id}/status")
    public ContactResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatus request,
                                        Authentication authentication) {
        return contacts.updateStatus(id, request.status(), authentication.getName());
    }

    public record UpdateStatus(@NotNull ContactMessage.Status status) { }
}
