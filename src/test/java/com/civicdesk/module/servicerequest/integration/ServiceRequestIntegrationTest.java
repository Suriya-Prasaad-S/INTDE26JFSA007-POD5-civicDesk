package com.civicdesk.module.serviceRequest.integration;

import com.civicdesk.module.serviceRequest.repository.ServiceRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests: real HTTP -> controller -> service -> JPA -> H2, exercising
 * the data seeded by {@code DummyDataSeeder} at startup. {@code @Transactional} rolls back
 * each test's writes so the seeded baseline is left untouched between tests.
 *
 * <p>Known seeded data: citizen-0001 (Active) and citizen-0002 (Flagged); svc-0001 / svc-0002
 * (Active) and svc-0003 (Inactive); active officers in dept-0004 and dept-0002.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ServiceRequestIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ServiceRequestRepository requestRepository;

    @Test
    @DisplayName("getAllServices returns only the Active seeded services")
    void getAllServicesReturnsActiveOnly() throws Exception {
        mockMvc.perform(get("/serviceRequest/getAllServices"))
                .andExpect(status().isOk())
                // svc-0001 and svc-0002 are Active; svc-0003 is Inactive and must be excluded.
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("submitRequest persists a request and auto-assigns an officer (201)")
    void submitRequestSucceeds() throws Exception {
        long before = requestRepository.count();

        mockMvc.perform(post("/serviceRequest/submitRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citizenId\":\"citizen-0001\",\"serviceId\":\"svc-0001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        org.assertj.core.api.Assertions.assertThat(requestRepository.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("submitRequest by a Flagged citizen is forbidden (403)")
    void submitRequestFlaggedCitizenForbidden() throws Exception {
        mockMvc.perform(post("/serviceRequest/submitRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citizenId\":\"citizen-0002\",\"serviceId\":\"svc-0001\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("submitRequest against an Inactive service is unprocessable (422)")
    void submitRequestInactiveServiceUnprocessable() throws Exception {
        mockMvc.perform(post("/serviceRequest/submitRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citizenId\":\"citizen-0001\",\"serviceId\":\"svc-0003\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("submitRequest for an unknown service returns 404")
    void submitRequestUnknownServiceNotFound() throws Exception {
        mockMvc.perform(post("/serviceRequest/submitRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citizenId\":\"citizen-0001\",\"serviceId\":\"does-not-exist\"}"))
                .andExpect(status().isNotFound());
    }
}
