package com.tms.edi;

import com.tms.edi.entity.cfg.CommunicationPartner;
import com.tms.edi.entity.imp.ImportOrderHeader;
import com.tms.edi.enums.MappingType;
import com.tms.edi.repository.imp.ImportOrderCargoRepository;
import com.tms.edi.repository.imp.ImportOrderEquipmentRepository;
import com.tms.edi.repository.imp.ImportOrderLineRepository;
import com.tms.edi.repository.imp.ImportOrderReferenceRepository;
import com.tms.edi.repository.imp.ImportTransportCostRepository;
import com.tms.edi.repository.tms.TmsAddressRepository;
import com.tms.edi.service.imp.ImportMappingService;
import com.tms.edi.service.imp.ImportOrderValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class TmsEdiApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context boots without errors
    }
}

@ExtendWith(MockitoExtension.class)
class ImportOrderValidationServiceUnitTests {

    @Mock
    private ImportMappingService mappingService;
    @Mock
    private ImportOrderLineRepository lineRepo;
    @Mock
    private ImportOrderCargoRepository cargoRepo;
    @Mock
    private ImportOrderReferenceRepository refRepo;
    @Mock
    private ImportOrderEquipmentRepository equipRepo;
    @Mock
    private ImportTransportCostRepository costRepo;
    @Mock
    private TmsAddressRepository addressRepo;

    @Test
    void orderMapCheck_acceptsTransportTypeWithCaseAndSpaceVariationsWhenMapped() {
        ImportOrderValidationService service = new ImportOrderValidationService(
                mappingService, lineRepo, cargoRepo, refRepo, equipRepo, costRepo, addressRepo
        );

        CommunicationPartner partner = CommunicationPartner.builder()
                .code("P1")
                .transportTypeIsInternal(false)
                .build();
        ImportOrderHeader header = ImportOrderHeader.builder()
                .communicationPartner("P1")
                .transportType("  TeSt  ")
                .build();

        when(mappingService.getPartner("P1")).thenReturn(partner);
        when(mappingService.checkMapping("P1", MappingType.TRANSPORT_TYPE, "TeSt")).thenReturn(false);

        List<String> errors = service.orderMapCheck(header);

        assertTrue(errors.isEmpty());
        verify(mappingService).checkMapping(eq("P1"), eq(MappingType.TRANSPORT_TYPE), eq("TeSt"));
    }

    @Test
    void orderMapCheck_returnsConfiguredMessageWhenTransportTypeMappingMissing() {
        ImportOrderValidationService service = new ImportOrderValidationService(
                mappingService, lineRepo, cargoRepo, refRepo, equipRepo, costRepo, addressRepo
        );

        CommunicationPartner partner = CommunicationPartner.builder()
                .code("P1")
                .transportTypeIsInternal(false)
                .build();
        ImportOrderHeader header = ImportOrderHeader.builder()
                .communicationPartner("P1")
                .transportType("test")
                .build();

        when(mappingService.getPartner("P1")).thenReturn(partner);
        when(mappingService.checkMapping("P1", MappingType.TRANSPORT_TYPE, "test")).thenReturn(true);
        when(mappingService.getMappedForeignIds("P1", MappingType.TRANSPORT_TYPE))
                .thenReturn(List.of("TEST", "SEA"));

        List<String> errors = service.orderMapCheck(header);

        assertEquals(1, errors.size());
        assertEquals("Transport Type is not configured. Please map it in master data. Received: 'test'", errors.get(0));
    }
}
