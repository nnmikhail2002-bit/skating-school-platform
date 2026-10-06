package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.student.request.CreateStudentRequest;
import com.skating.platform.backend.dto.student.request.UpdateStudentRequest;
import com.skating.platform.backend.dto.student.response.StudentResponse;
import com.skating.platform.backend.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;


@Tag(name = "Students", description = "Management of students")
@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @Operation(
            summary = "Get all students",
            description = "Returns students with pagination and sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Students successfully returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Trainer or administrator role required")
    })
    @GetMapping
    public Page<StudentResponse> getAllStudents(Pageable pageable) {
        return service.getAllStudents(pageable);
    }

    @Operation(
            summary = "Create student",
            description = "Creates a new student profile"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Student successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid student data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "409", description = "Student with this phone number already exists")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse createStudent(
            @Valid @RequestBody CreateStudentRequest request
    ) {
        return service.createStudent(request);
    }

    @Operation(
            summary = "Get student by ID",
            description = "Returns a student by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student found"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Trainer or administrator role required"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/{studentId}")
    public StudentResponse getStudentById(@PathVariable Long studentId) {
        return service.getStudentById(studentId);
    }

    @Operation(
            summary = "Get current student profile",
            description = "Returns the student profile linked to the authenticated account"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current student profile returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Student role required"),
            @ApiResponse(responseCode = "404", description = "Student profile not found")
    })
    @GetMapping("/me")
    public StudentResponse getCurrentStudent(@AuthenticationPrincipal Jwt jwt) {
        return service.getCurrentStudent(jwt.getSubject());
    }


    @Operation(
            summary = "Update student",
            description = "Updates an existing student by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid student data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "409", description = "Phone number is already used by another student")
    })
    @PutMapping("/{studentId}")
    public StudentResponse updateStudent(@PathVariable Long studentId, @Valid @RequestBody UpdateStudentRequest request){
        return service.updateStudent(studentId, request);
    }

    @Operation(
            summary = "Delete student",
            description = "Deletes a student by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Student successfully deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @DeleteMapping("/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent (@PathVariable Long studentId){
        service.deleteStudent(studentId);
    }

    @Operation(
            summary = "Search students",
            description = "Searches students by first or last name with pagination and sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Trainer or administrator role required")
    })
    @GetMapping("/search")
    public Page<StudentResponse> searchStudents(
            @RequestParam String query,
            Pageable pageable
    ){
        return service.searchStudents(query, pageable);
    }
}
