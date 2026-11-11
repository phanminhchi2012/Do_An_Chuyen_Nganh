package com.example.scalys.services;

import com.example.scalys.dtos.CustomerOrderPage;
import com.example.scalys.dtos.CustomerOrderResponse;
import com.example.scalys.repositories.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerOrderHistoryService {
    private final OrderRepository orders;

    public CustomerOrderHistoryService(OrderRepository orders) { this.orders = orders; }

    @Transactional(readOnly = true)
    public CustomerOrderPage list(Long accountId, int page) {
        if (page < 0 || page > 100_000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Trang không hợp lệ.");
        var result = orders.findByCustomerAccountIdOrderByCreatedAtDescIdDesc(accountId,
                PageRequest.of(page, 10));
        return new CustomerOrderPage(result.getContent().stream().map(CustomerOrderResponse::from).toList(),
                page, result.getTotalPages(), result.getTotalElements());
    }
}
