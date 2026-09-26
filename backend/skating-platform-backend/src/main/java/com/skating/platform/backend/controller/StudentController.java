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


@Tag(name = "Students", description = "Management of students")
@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @Operation(summary = "Get the list of students", description = "Returns students with pagination and sorting")
    @GetMapping
    public Page<StudentResponse> getAllStudents(Pageable pageable) {
        return service.getAllStudents(pageable);
    }

    @Operation(summary = "create student", description = "Creates Student")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Student created"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid student data"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Student with this phone number already exists"
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse createStudent(
            @Valid @RequestBody CreateStudentRequest request
    ) {
        return service.createStudent(request);
    }

    @Operation(summary = "Get student by ID", description = "Returns a student by identifier")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Student found"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student is not found"
            )
    })
    @GetMapping("/{studentId}")
    public StudentResponse getStudentById(@PathVariable Long studentId) {
        return service.getStudentById(studentId);
    }

    @Operation (summary = "Update data student by ID", description = "Update fields student by his identifier")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Student found and successfully updated"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid update data student"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student is not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "The specified phone number is already being used by another student"
            )
    })
    @PutMapping("/{studentId}")
    public StudentResponse updateStudent(@PathVariable Long studentId, @Valid @RequestBody UpdateStudentRequest request){
        return service.updateStudent(studentId, request);
    }

    @Operation (summary = "Delete student by Id", description =  "Deletes a student by identifier")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Student successfully deleted"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student is not found"
            )
    })
    @DeleteMapping("/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent (@PathVariable Long studentId){
        service.deleteStudent(studentId);
    }

    @Operation (summary = "Search students", description =  "Searches for students by first or last name, with support for pagination and sorting")
    @GetMapping("/search")
    public Page<StudentResponse> searchStudents(
            @RequestParam String query,
            Pageable pageable
    ){
        return service.searchStudents(query, pageable);
    }
}
