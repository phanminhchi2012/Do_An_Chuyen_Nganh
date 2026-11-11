package com.example.scalys.services;

import com.example.scalys.models.StaffAccount;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record StaffPrincipal(Long id, String email, int sessionVersion) implements UserDetails, Serializable {
    public static StaffPrincipal from(StaffAccount staff) {
        return new StaffPrincipal(staff.getId(), staff.getEmail(), staff.getSessionVersion());
    }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_STAFF"));
    }
    @Override public String getPassword() { return ""; }
    @Override public String getUsername() { return email; }
}
