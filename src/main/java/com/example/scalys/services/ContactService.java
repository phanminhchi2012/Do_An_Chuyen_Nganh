package com.example.scalys.services;

import com.example.scalys.dtos.ContactRequest;
import com.example.scalys.dtos.ContactPageResponse;
import com.example.scalys.dtos.ContactResponse;
import com.example.scalys.models.ContactMessage;
import com.example.scalys.repositories.ContactMessageRepository;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContactService {
    private final ContactMessageRepository messages;
    private final EmailNotificationService email;
    private final ContactSubmissionLimiter limiter;

    public ContactService(ContactMessageRepository messages, EmailNotificationService email,
                          ContactSubmissionLimiter limiter) {
        this.messages = messages;
        this.email = email;
        this.limiter = limiter;
    }

    @Transactional
    public void submit(ContactRequest request, String remoteAddress) {
        if (!email.hasContactRecipient()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Shop chưa cấu hình email nhận liên hệ. Vui lòng thử lại sau.");
        if (request.question().trim().length() < 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Câu hỏi cần ít nhất 10 ký tự.");
        limiter.checkAndRecord(remoteAddress);
        ContactMessage message = messages.saveAndFlush(new ContactMessage(request.name().trim(),
                request.phone(), request.email().trim().toLowerCase(Locale.ROOT), request.question().trim()));
        email.queueContact(message);
    }

    @Transactional(readOnly = true)
    public ContactPageResponse list(ContactMessage.Status status, int page) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang không hợp lệ.");
        var pageable = PageRequest.of(page, 10);
        Page<ContactMessage> found = status == null
                ? messages.findAllByOrderByCreatedAtDescIdDesc(pageable)
                : messages.findByStatusOrderByCreatedAtDescIdDesc(status, pageable);
        return new ContactPageResponse(found.getContent().stream().map(ContactResponse::from).toList(),
                found.getTotalElements(), found.getTotalPages(), page);
    }

    @Transactional(readOnly = true)
    public long newCount() { return messages.countByStatus(ContactMessage.Status.NEW); }

    @Transactional
    public ContactResponse updateStatus(Long id, ContactMessage.Status status, String actor) {
        ContactMessage message = messages.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lời nhắn."));
        message.setStatus(status, actor);
        return ContactResponse.from(message);
    }
}
