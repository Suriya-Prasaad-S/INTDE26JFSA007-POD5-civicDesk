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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests: real HTTP -> Spring Security filter chain -> controller ->
 * service -> JPA -> H2, exercising the data seeded by {@code DummyDataSeeder} at startup.
 * {@code @Transactional} rolls back each test's writes so the seeded baseline is left
 * untouched between tests.
 *
 * <p>Every {@code /serviceRequest/**} endpoint now requires a valid JWT, so each request is
 * authenticated via {@code .with(user(...).roles(...))}. Role / ownership enforcement
 * (401 / 403) is asserted directly below.</p>
 *
 * <p>Known seeded data: citizen-0001 (Active) and citizen-0002 (Suspended); svc-0001 / svc-0002
 * (Active) and svc-0003 (Inactive); an active field officer in the demo department.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ServiceRequestIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ServiceRequestRepository requestRepository;

    // ---------------------------------------------------------------- Functional flow

    @Test
    @DisplayName("getAllServices returns only the Active seeded services")
    void getAllServicesReturnsActiveOnly() throws Exception {
        mockMvc.perform(get("/serviceRequest/getAllServices").with(user("cit").roles("CIT")))
                .andExpect(status().isOk())
                // svc-0001 and svc-0002 are Active; svc-0003 is Inactive and must be excluded.
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("submitRequest persists a request and auto-assigns an officer (201)")
    void submitRequestSucceeds() throws Exception {
        long before = requestRepository.count();

        mockMvc.perform(post("/serviceRequest/submitRequest").with(user("cit").roles("CIT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citizenId\":\"citizen-0001\",\"serviceId\":\"svc-0001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        org.assertj.core.api.Assertions.assertThat(requestRepository.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("submitRequest by a Suspended citizen is forbidden (403)")
    void submitRequestFlaggedCitizenForbidden() throws Exception {
        mockMvc.perform(post("/serviceRequest/submitRequest").with(user("cit").roles("CIT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citizenId\":\"citizen-0002\",\"serviceId\":\"svc-0001\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("submitRequest against an Inactive service is unprocessable (422)")
    void submitRequestInactiveServiceUnprocessable() throws Exception {
        mockMvc.perform(post("/serviceRequest/submitRequest").with(user("cit").roles("CIT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citizenId\":\"citizen-0001\",\"serviceId\":\"svc-0003\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("submitRequest for an unknown service returns 404")
    void submitRequestUnknownServiceNotFound() throws Exception {
        mockMvc.perform(post("/serviceRequest/submitRequest").with(user("cit").roles("CIT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citizenId\":\"citizen-0001\",\"serviceId\":\"does-not-exist\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- Role / auth enforcement

    @Test
    @DisplayName("a request with no JWT is rejected with 401")
    void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/serviceRequest/getAllServices"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized. JWT token is missing or invalid."));
    }

    @Test
    @DisplayName("a Citizen cannot access the request queue (403)")
    void citizenCannotViewRequestQueue() throws Exception {
        mockMvc.perform(get("/serviceRequest/getAllRequests").with(user("cit").roles("CIT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied. Citizens cannot access the request queue."));
    }

    @Test
    @DisplayName("a Field Officer can access the request queue (200)")
    void officerCanViewRequestQueue() throws Exception {
        mockMvc.perform(get("/serviceRequest/getAllRequests").with(user("fo").roles("FO")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a non-Admin cannot create a catalog service (403)")
    void nonAdminCannotCreateService() throws Exception {
        mockMvc.perform(post("/serviceRequest/createService").with(user("cit").roles("CIT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceName\":\"X\",\"departmentId\":\"DPT01\",\"category\":\"Certificate\","
                                + "\"processingDays\":7,\"requiredDocuments\":[\"NationalID\"],\"fee\":10.00}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied. Only Admin role can create services."));
    }

    @Test
    @DisplayName("a non-Citizen cannot submit a service request (403)")
    void nonCitizenCannotSubmitRequest() throws Exception {
        mockMvc.perform(post("/serviceRequest/submitRequest").with(user("fo").roles("FO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citizenId\":\"citizen-0001\",\"serviceId\":\"svc-0001\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied. Only citizens can submit service requests."));
    }
}
