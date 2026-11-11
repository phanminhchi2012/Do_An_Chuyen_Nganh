package com.example.scalys;

import com.example.scalys.dtos.AdminUserRequests;
import com.example.scalys.models.ContactMessage;
import com.example.scalys.repositories.ContactMessageRepository;
import com.example.scalys.repositories.StaffAccountRepository;
import com.example.scalys.services.AdminUserService;
import com.example.scalys.services.StaffPrincipal;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"shop.mail.enabled=false", "shop.contact.recipient=shop@example.invalid"})
@AutoConfigureMockMvc
@Transactional
class ContactHttpTests {
    @Autowired MockMvc http;
    @Autowired ContactMessageRepository contacts;
    @Autowired JdbcTemplate jdbc;
    @Autowired AdminUserService users;
    @Autowired StaffAccountRepository staffAccounts;

    @Test
    void guestContactQueuesEmailAndStaffCanHandleIt() throws Exception {
        long before = contacts.countByStatus(ContactMessage.Status.NEW);
        http.perform(get("/api/admin/contacts/new-count")).andExpect(status().isUnauthorized());
        String request = "{\"name\":\"Khách thử\",\"phone\":\"0912345678\","
                + "\"email\":\"guest@example.test\",\"question\":\"Shop có thể tư vấn loại pet phù hợp không?\"}";
        http.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());

        var csrfResponse = http.perform(get("/api/admin/auth/csrf")).andExpect(status().isOk()).andReturn();
        String csrf = token(csrfResponse.getResponse().getContentAsString());
        var guestSession = (MockHttpSession) csrfResponse.getRequest().getSession(false);
        http.perform(post("/api/contact").session(guestSession).header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.replace("0912345678", "123")))
                .andExpect(status().isBadRequest());
        http.perform(post("/api/contact").session(guestSession).header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());
        assertThat(contacts.countByStatus(ContactMessage.Status.NEW)).isEqualTo(before + 1);
        ContactMessage saved = contacts.findAll().stream().filter(item ->
                item.getEmail().equals("guest@example.test")).reduce((first, second) -> second).orElseThrow();
        Long queued = jdbc.queryForObject("SELECT COUNT(*) FROM email_outbox WHERE contact_id = ? "
                + "AND recipient = 'shop@example.invalid'", Long.class, saved.getId());
        assertThat(queued).isEqualTo(1);

        String staffEmail = "contact-staff-" + UUID.randomUUID() + "@example.test";
        var created = users.createStaff(new AdminUserRequests.CreateStaff(staffEmail, "Nhân viên", "password-123"));
        StaffPrincipal principal = StaffPrincipal.from(staffAccounts.findById(created.id()).orElseThrow());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
        var staffSession = new MockHttpSession();
        staffSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        try {
            http.perform(get("/api/admin/contacts/new-count").session(staffSession))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(before + 1));
            http.perform(get("/api/admin/contacts").param("status", "NEW").session(staffSession))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].email").value("guest@example.test"));
            var staffCsrfResponse = http.perform(get("/api/admin/auth/csrf").session(staffSession))
                    .andExpect(status().isOk()).andReturn();
            String staffCsrf = token(staffCsrfResponse.getResponse().getContentAsString());
            http.perform(patch("/api/admin/contacts/{id}/status", saved.getId())
                            .session(staffSession).header("X-CSRF-TOKEN", staffCsrf)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HANDLED\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("HANDLED"));
            assertThat(contacts.countByStatus(ContactMessage.Status.NEW)).isEqualTo(before);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String token(String response) {
        var matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(response);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
