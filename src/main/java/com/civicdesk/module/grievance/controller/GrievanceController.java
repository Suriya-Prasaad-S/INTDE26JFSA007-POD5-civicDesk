package com.civicdesk.module.grievance.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// import com.civicdesk.module.grievance.dto.request.GrievanceCreateReq;
import com.civicdesk.module.grievance.dto.response.GrievanceDetailResponse;
import com.civicdesk.module.grievance.entity.Grievance;
import com.civicdesk.module.grievance.entity.GrievanceAction;
import com.civicdesk.module.grievance.service.GrievanceService;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("civicDesk/grievance/")
public class GrievanceController {

    @Autowired
    private GrievanceService grievanceService;

    @PostMapping("createGrievance")
    public ResponseEntity<?> createGrievance(@RequestBody Grievance grievance) {

        grievanceService.createGrievance(grievance);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Grievance created successfully"));
    }

    @PostMapping("createGrievanceAction")
    public ResponseEntity<?> createGrievanceAction(@RequestBody GrievanceAction grievanceAction) {

        grievanceService.createGrievanceAction(grievanceAction);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Grievance action created successfully"));
    }

    /**
     * Lists grievances scoped to the requesting user's role.
     * <p>
     * Examples:
     * <pre>
     *   GET civicDesk/grievance/getAllGrievances?role=CITIZEN&amp;userId=...
     *   GET civicDesk/grievance/getAllGrievances?role=FIELD_OFFICER&amp;userId=...
     *   GET civicDesk/grievance/getAllGrievances?role=ADMIN
     * </pre>
     * {@code userId} is required for CITIZEN and FIELD_OFFICER, and ignored for
     * DEPARTMENT_SUPERVISOR and ADMIN.
     */
    @GetMapping("getAllGrievances")
    public ResponseEntity<?> getAllGrievances(
            @RequestParam String role,
            @RequestParam(required = false) String userId) {

        List<Grievance> grievances = grievanceService.getGrievancesByRole(role, userId);

        return ResponseEntity.ok(grievances);
    }

    /**
     * Returns a single grievance with all of its related actions.
     * <p>Example: {@code GET civicDesk/grievance/getGrievanceDetails/{grievanceId}}
     */
    @GetMapping("getGrievanceDetails/{grievanceId}")
    public ResponseEntity<?> getGrievanceDetails(@PathVariable String grievanceId) {

        GrievanceDetailResponse details = grievanceService.getGrievanceDetails(grievanceId);

        return ResponseEntity.ok(details);
    }

}


































        //TODO: process POST request

        // I need to create the DTO's for the grievnace and grievance Action
        //To use the same DTO for both create and update i need to use the oncreate and onUpdate grouping
        //I need to create the mapper which is going to map DTO to entity and entity to DTO
        // i need to makes sure everything was cool in the case of the validation
        // I need to make sure to create the custom validations
        // I need to make sure to create the Global exception handlier and route every error request to the user
        // I need to finalize one response style to the user both for success and failure and map that with the below one
        // Instead of creating the things manually we can use the bulder pattern