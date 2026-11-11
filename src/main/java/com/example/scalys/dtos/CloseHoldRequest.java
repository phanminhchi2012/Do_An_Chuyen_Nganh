package com.example.scalys.dtos;

import com.example.scalys.models.BalanceMethod;

public record CloseHoldRequest(String code, String phone, BalanceMethod balanceMethod) { }
