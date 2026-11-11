package com.example.scalys.repositories;

import com.example.scalys.models.VnpayIpnEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VnpayIpnEventRepository extends JpaRepository<VnpayIpnEvent, Long> { }
