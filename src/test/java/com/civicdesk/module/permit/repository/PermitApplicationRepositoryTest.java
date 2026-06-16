package com.civicdesk.module.permit.repository;

import com.civicdesk.module.permit.entity.PermitApplication;
import com.civicdesk.module.permit.enums.PermitStatus;
import com.civicdesk.module.permit.enums.PermitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl"
})
public class PermitApplicationRepositoryTest {

    @Autowired
    private PermitApplicationRepository permitRepo;

    private PermitApplication testPermit;

    @BeforeEach
    public void setUp() {
        permitRepo.deleteAll();

        testPermit = new PermitApplication();
        testPermit.setPermitId("perm-test-001");
        testPermit.setCitizenId("cit-test-001");
        testPermit.setPermitType(PermitType.BuildingPermit);
        testPermit.setApplicationDate(LocalDate.now());
        testPermit.setPropertyAddress("12 Anna Nagar, Chennai");
        testPermit.setWard("Ward 5");
        testPermit.setZone("North");
        testPermit.setValidityPeriod(24);
        testPermit.setFee(15000.00);
        testPermit.setStatus(PermitStatus.Applied);
        testPermit.setDeleted(false);
        testPermit.setCreatedAt(LocalDateTime.now());
        testPermit.setUpdatedAt(LocalDateTime.now());

        permitRepo.save(testPermit);
    }

    @Test
    public void findByPermitIdAndIsDeletedFalse_Success() {
        Optional<PermitApplication> result =
                permitRepo.findByPermitIdAndIsDeletedFalse("perm-test-001");

        assertTrue(result.isPresent());
        assertEquals("perm-test-001", result.get().getPermitId());
    }

    @Test
    public void findByPermitIdAndIsDeletedFalse_DeletedPermit_ReturnsEmpty() {
        testPermit.setDeleted(true);
        permitRepo.save(testPermit);

        Optional<PermitApplication> result =
                permitRepo.findByPermitIdAndIsDeletedFalse("perm-test-001");

        assertFalse(result.isPresent());
    }

    @Test
    public void findByCitizenIdAndIsDeletedFalse_Success() {
        List<PermitApplication> result =
                permitRepo.findByCitizenIdAndIsDeletedFalse("cit-test-001");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("cit-test-001", result.get(0).getCitizenId());
    }

    @Test
    public void findByStatusAndIsDeletedFalse_Success() {
        List<PermitApplication> result =
                permitRepo.findByStatusAndIsDeletedFalse(PermitStatus.Applied);

        assertFalse(result.isEmpty());
        assertEquals(PermitStatus.Applied, result.get(0).getStatus());
    }

    @Test
    public void findByPermitTypeAndIsDeletedFalse_Success() {
        List<PermitApplication> result =
                permitRepo.findByPermitTypeAndIsDeletedFalse(PermitType.BuildingPermit);

        assertFalse(result.isEmpty());
        assertEquals(PermitType.BuildingPermit, result.get(0).getPermitType());
    }

    @Test
    public void findByIsDeletedFalse_Success() {
        List<PermitApplication> result = permitRepo.findByIsDeletedFalse();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }
}