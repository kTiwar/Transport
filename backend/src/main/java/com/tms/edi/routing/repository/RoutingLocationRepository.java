package com.tms.edi.routing.repository;

import com.tms.edi.routing.entity.RoutingLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoutingLocationRepository extends JpaRepository<RoutingLocation, Long> {
    Optional<RoutingLocation> findByAddressKey(String addressKey);
}
