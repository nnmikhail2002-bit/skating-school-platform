package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.student.request.CreateStudentRequest;
import com.skating.platform.backend.dto.student.request.UpdateStudentRequest;
import com.skating.platform.backend.dto.student.response.StudentResponse;
import com.skating.platform.backend.entity.Student;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.StudentMapper;
import com.skating.platform.backend.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {
    private final StudentRepository repository;
    private final StudentMapper mapper;

    public StudentService(StudentRepository repository, StudentMapper mapper){
        this.repository = repository;
        this.mapper = mapper;
    }

    public Page<StudentResponse> getAllStudents(Pageable pageable){
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    public StudentResponse createStudent(CreateStudentRequest request){
        if (repository.existsByPhone(request.getPhone())){
            throw new ConflictException(
                    "Student with this phone already exists"
            );
        }
        Student student = mapper.toEntity(request);
        Student saved = repository.save(student);
        return mapper.toResponse(saved);
    }

    Student getEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student not found")
                );
    }

    public StudentResponse getStudentById(Long id){
        Student student = getEntityById(id);
        return mapper.toResponse(student);
    }

    public StudentResponse updateStudent(Long id, UpdateStudentRequest request){

        Student existing = getEntityById(id);

        if(repository.existsByPhone(request.getPhone())
                && !existing.getPhone().equals(request.getPhone())){
            throw new ConflictException(
                    "Student with this phone already exists"
            );
        }

        mapper.updateEntity(existing, request);

        Student saved = repository.save(existing);

        return mapper.toResponse(saved);
    }

    @Transactional
    public void deleteStudent(Long studentId){
        Student student = getEntityById(studentId);
        repository.delete(student);
    }

    public Page<StudentResponse> searchStudents(String query, Pageable pageable){
        return repository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query, pageable)
                .map(mapper::toResponse);
    }

}
