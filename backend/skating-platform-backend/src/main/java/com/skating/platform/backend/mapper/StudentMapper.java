package com.skating.platform.backend.mapper;

import com.skating.platform.backend.dto.student.request.CreateStudentRequest;
import com.skating.platform.backend.dto.student.request.UpdateStudentRequest;
import com.skating.platform.backend.dto.student.response.StudentResponse;
import com.skating.platform.backend.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {
    public StudentResponse toResponse(Student student){
        return new StudentResponse(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getPhone(),
                student.getEmail(),
                student.getActive()
        );
    }

    public Student toEntity(CreateStudentRequest request){
        Student student = new Student();
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhone(request.getPhone());
        student.setEmail(request.getEmail());
        return student;
    }

    public void updateEntity(Student student, UpdateStudentRequest request){
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhone(request.getPhone());
        student.setEmail(request.getEmail());
    }
}