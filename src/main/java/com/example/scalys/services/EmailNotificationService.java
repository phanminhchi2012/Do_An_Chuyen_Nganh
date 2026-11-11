package com.example.scalys.services;

import com.example.scalys.models.Order;
import com.example.scalys.models.ContactMessage;
import com.example.scalys.repositories.EmailOutboxRepository;
import com.example.scalys.models.EmailOutbox;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailNotificationService {
    private static final DateTimeFormatter VIETNAM_TIME = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final EmailOutboxRepository outbox;
    private final ObjectProvider<JavaMailSender> sender;
    private final boolean enabled;
    private final String from;
    private final String publicUrl;
    private final String contactRecipient;

    public EmailNotificationService(EmailOutboxRepository outbox, ObjectProvider<JavaMailSender> sender,
                                    @Value("${shop.mail.enabled:false}") boolean enabled,
                                    @Value("${shop.mail.from:}") String from,
                                    @Value("${shop.public-url:http://localhost:2003}") String publicUrl,
                                    @Value("${shop.contact.recipient:}") String contactRecipient) {
        this.outbox = outbox;
        this.sender = sender;
        this.enabled = enabled;
        this.from = from;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
        this.contactRecipient = contactRecipient.trim();
    }

    public boolean hasContactRecipient() { return !contactRecipient.isBlank(); }

    public void queueContact(ContactMessage contact) {
        String body = "Khách hàng gửi câu hỏi mới trên Scalys.\n\n"
                + "Mã liên hệ: #" + contact.getId() + "\n"
                + "Họ tên: " + contact.getCustomerName() + "\n"
                + "Số điện thoại: " + contact.getPhone() + "\n"
                + "Email: " + contact.getEmail() + "\n\n"
                + "Câu hỏi:\n" + contact.getQuestion() + "\n\n"
                + "Quản lý tại: " + publicUrl + "/admin/contacts";
        outbox.save(new EmailOutbox(contact, "CONTACT_NEW", contactRecipient,
                "SCALYS - Liên hệ mới #" + contact.getId(), body));
    }

    public void queuePaid(Order order) {
        if (order.getOrderType() == com.example.scalys.models.OrderType.HOLD_DEPOSIT)
            queue(order, "DEPOSIT_PAID", "SCALYS - Xác nhận giữ pet",
                    "Shop đã nhận cọc cho " + order.getQuantity() + " " + order.getProduct().getName()
                    + ". Mã hóa đơn: " + order.getPublicCode() + ".\nTiền cọc: " + order.getPaymentAmount()
                    + " VND. Hạn giữ: " + VIETNAM_TIME.format(order.getHoldExpiresAt())
                    + " (giờ Việt Nam).\nChốt mua tại: " + publicUrl + "/order-lookup"
                    + "\nNếu hủy hoặc quá hạn, tiền cọc không được hoàn.");
        else queue(order, "PURCHASE_PAID", "SCALYS - Xác nhận thanh toán",
                "Shop đã nhận thanh toán cho " + order.getProduct().getName() + ".\nMã hóa đơn: "
                + order.getPublicCode() + ".\nTra cứu: " + publicUrl + "/order-lookup"
                + "\nNhập mã hóa đơn trong email này và số điện thoại đã dùng khi đặt hàng.");
    }

    public void queueOrderReceived(Order order) {
        queue(order, "ORDER_RECEIVED", "SCALYS - Mã hóa đơn " + order.getPublicCode(),
                "Shop đã nhận đơn " + order.getPublicCode() + " cho " + order.getQuantity() + " "
                + order.getProduct().getName() + ".\nĐơn đang chờ shop xác nhận."
                + "\nTra cứu tại: " + publicUrl + "/order-lookup"
                + "\nNhập mã đơn trong email này và số điện thoại đã dùng khi đặt hàng.");
    }

    public void queueDepositExpired(Order order) {
        queue(order, "DEPOSIT_EXPIRED", "SCALYS - Lượt giữ pet đã hết hạn",
                "Lượt giữ pet của đơn " + order.getPublicCode() + " đã hết hạn lúc "
                + VIETNAM_TIME.format(order.getHoldExpiresAt())
                + " (giờ Việt Nam). Pet đã được trả lại kho; tiền cọc không được hoàn.");
    }

    public void queueBalancePaid(Order order) {
        queue(order, "BALANCE_PAID", "SCALYS - Đơn giữ pet đã thanh toán đủ",
                "Đơn " + order.getPublicCode() + " đã thanh toán đủ cho "
                + order.getProduct().getName() + ".\nTra cứu: " + publicUrl + "/order-lookup");
    }

    private void queue(Order order, String kind, String subject, String body) {
        if (order.getEmail() == null || order.getEmail().isBlank()) return;
        if (!outbox.existsByOrderIdAndKind(order.getId(), kind))
            outbox.save(new EmailOutbox(order, kind, order.getEmail(), subject, body));
    }

    @Transactional(readOnly = true)
    public List<Long> pendingIds() {
        if (!enabled || from.isBlank() || sender.getIfAvailable() == null) return List.of();
        return outbox.findTop20BySentAtIsNullOrderByIdAsc().stream().map(EmailOutbox::getId).toList();
    }

    @Transactional
    public void sendOne(Long id) {
        if (!enabled || from.isBlank()) return;
        EmailOutbox item = outbox.findByIdForUpdate(id).orElse(null);
        if (item == null || item.getSentAt() != null) return;
        JavaMailSender mail = sender.getIfAvailable();
        if (mail == null) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(item.getRecipient());
        message.setSubject(item.getSubject());
        message.setText(item.getBody());
        mail.send(message);
        item.markSent();
    }
}
