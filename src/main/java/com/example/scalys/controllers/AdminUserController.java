package com.example.scalys.controllers;

import com.example.scalys.dtos.AdminUserRequests;
import com.example.scalys.dtos.AdminUsersResponse;
import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.services.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService users;
    public AdminUserController(AdminUserService users) { this.users = users; }

    @GetMapping
    public AdminUsersResponse list() { return users.list(); }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUsersResponse.Staff createStaff(@Valid @RequestBody AdminUserRequests.CreateStaff request) {
        return users.createStaff(request);
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUsersResponse.Customer createCustomer(@Valid @RequestBody CustomerAccountRequests.Register request) {
        return users.createCustomer(request);
    }

    @PutMapping("/staff/{id}")
    public AdminUsersResponse.Staff updateStaff(@PathVariable Long id,
                                                 @Valid @RequestBody AdminUserRequests.UpdateStaff request) {
        return users.updateStaff(id, request);
    }

    @PutMapping("/customers/{id}")
    public AdminUsersResponse.Customer updateCustomer(@PathVariable Long id,
            @Valid @RequestBody AdminUserRequests.UpdateCustomer request) {
        return users.updateCustomer(id, request);
    }

    @DeleteMapping("/customers/{id}")
    public AdminUsersResponse.Customer disableCustomer(@PathVariable Long id) {
        return users.disableCustomer(id);
    }
}
