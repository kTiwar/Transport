package com.tms.edi.repository.tms;

import com.tms.edi.entity.tms.TmsAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TmsAddressRepository extends JpaRepository<TmsAddress, String> {

    Optional<TmsAddress> findByNumberAndStreetAndNameAndPostalCodeAndCityAndCountryCode(
            String number, String street, String name,
            String postalCode, String city, String countryCode);
}
