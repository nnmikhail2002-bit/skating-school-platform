package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.student.request.CreateStudentRequest;
import com.skating.platform.backend.dto.student.request.UpdateStudentRequest;
import com.skating.platform.backend.dto.student.response.StudentResponse;
import com.skating.platform.backend.entity.AppUser;
import com.skating.platform.backend.entity.Student;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.StudentMapper;
import com.skating.platform.backend.repository.AppUserRepository;
import com.skating.platform.backend.repository.StudentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository repository;

    @Mock
    private StudentMapper mapper;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private StudentService studentService;


    private Student student() {
        Student student = new Student();
        student.setId(1L);
        student.setFirstName("Mikhail");
        student.setLastName("Test");
        student.setPhone("89999999999");
        student.setActive(true);

        return student;
    }


    private AppUser user(String email, Student student) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setStudent(student);

        return user;
    }


    // CURRENT STUDENT //

    @Test
    void shouldGetCurrentStudentSuccessfully() {

        String email = "student@test.com";

        Student student = student();
        AppUser user = user(email, student);

        StudentResponse response = mock(StudentResponse.class);

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(mapper.toResponse(student))
                .thenReturn(response);


        StudentResponse result = studentService.getCurrentStudent(email);


        assertSame(response, result);

        verify(appUserRepository).findByEmail(email);
        verify(mapper).toResponse(student);
    }


    @Test
    void shouldRejectCurrentStudentWhenUserNotFound() {

        String email = "missing@test.com";

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> studentService.getCurrentStudent(email));


        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verifyNoInteractions(mapper);
    }


    @Test
    void shouldRejectCurrentStudentWhenProfileNotFound() {

        String email = "trainer@test.com";

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setStudent(null);

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> studentService.getCurrentStudent(email));


        assertEquals(
                "Student profile not found",
                exception.getMessage()
        );

        verifyNoInteractions(mapper);
    }


    // CREATE //

    @Test
    void shouldCreateStudentSuccessfully() {

        CreateStudentRequest request = mock(CreateStudentRequest.class);

        Student student = student();

        StudentResponse response = mock(StudentResponse.class);


        when(request.getPhone())
                .thenReturn("89999999999");

        when(repository.existsByPhone("89999999999"))
                .thenReturn(false);

        when(mapper.toEntity(request))
                .thenReturn(student);

        when(repository.save(student))
                .thenReturn(student);

        when(mapper.toResponse(student))
                .thenReturn(response);


        StudentResponse result = studentService.createStudent(request);


        assertSame(response, result);

        verify(repository).existsByPhone("89999999999");

        verify(mapper).toEntity(request);

        verify(repository).save(student);
    }


    @Test
    void shouldRejectCreateWhenPhoneAlreadyExists() {

        CreateStudentRequest request = mock(CreateStudentRequest.class);

        when(request.getPhone())
                .thenReturn("89999999999");

        when(repository.existsByPhone("89999999999"))
                .thenReturn(true);


        ConflictException exception =
                assertThrows(ConflictException.class, () -> studentService.createStudent(request));


        assertEquals(
                "Student with this phone already exists",
                exception.getMessage()
        );

        verify(repository, never()).save(any(Student.class));

        verifyNoInteractions(mapper);
    }


    // GET BY ID //

    @Test
    void shouldGetStudentByIdSuccessfully() {

        Student student = student();

        StudentResponse response = mock(StudentResponse.class);

        when(repository.findById(1L))
                .thenReturn(Optional.of(student));

        when(mapper.toResponse(student))
                .thenReturn(response);


        StudentResponse result = studentService.getStudentById(1L);


        assertSame(response, result);

        verify(repository).findById(1L);
        verify(mapper).toResponse(student);
    }


    @Test
    void shouldRejectGetStudentByIdWhenNotFound() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> studentService.getStudentById(1L));


        assertEquals(
                "Student not found",
                exception.getMessage()
        );

        verifyNoInteractions(mapper);
    }


    // GET ALL //

    @Test
    void shouldGetAllStudentsSuccessfully() {

        Pageable pageable = PageRequest.of(0, 10);

        Student student = student();

        StudentResponse response = mock(StudentResponse.class);

        Page<Student> students = new PageImpl<>(List.of(student), pageable, 1);

        when(repository.findAll(pageable))
                .thenReturn(students);

        when(mapper.toResponse(student))
                .thenReturn(response);


        Page<StudentResponse> result = studentService.getAllStudents(pageable);


        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().get(0));

        verify(repository).findAll(pageable);
        verify(mapper).toResponse(student);
    }


    // UPDATE //

    @Test
    void shouldUpdateStudentSuccessfully() {

        Student existing = student();

        UpdateStudentRequest request = mock(UpdateStudentRequest.class);

        StudentResponse response = mock(StudentResponse.class);


        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(request.getPhone())
                .thenReturn("88888888888");

        when(repository.existsByPhone("88888888888"))
                .thenReturn(false);

        when(repository.save(existing))
                .thenReturn(existing);

        when(mapper.toResponse(existing))
                .thenReturn(response);


        StudentResponse result = studentService.updateStudent(1L, request);


        assertSame(response, result);

        verify(mapper).updateEntity(existing, request);

        verify(repository).save(existing);
    }


    @Test
    void shouldRejectUpdateWhenPhoneAlreadyExists() {

        Student existing = student();

        UpdateStudentRequest request = mock(UpdateStudentRequest.class);


        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(request.getPhone())
                .thenReturn("88888888888");

        when(repository.existsByPhone("88888888888"))
                .thenReturn(true);


        ConflictException exception =
                assertThrows(ConflictException.class, () -> studentService.updateStudent(1L, request));


        assertEquals(
                "Student with this phone already exists",
                exception.getMessage()
        );

        verify(mapper, never())
                .updateEntity(any(), any());

        verify(repository, never())
                .save(any(Student.class));
    }


    @Test
    void shouldRejectUpdateWhenStudentNotFound() {

        UpdateStudentRequest request = mock(UpdateStudentRequest.class);

        when(repository.findById(1L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> studentService.updateStudent(1L, request));


        assertEquals(
                "Student not found",
                exception.getMessage()
        );

        verify(repository, never()).save(any(Student.class));
    }


    // DELETE //

    @Test
    void shouldDeleteStudentSuccessfully() {

        Student student = student();

        when(repository.findById(1L))
                .thenReturn(Optional.of(student));


        studentService.deleteStudent(1L);


        verify(repository).findById(1L);
        verify(repository).delete(student);
    }


    @Test
    void shouldRejectDeleteWhenStudentNotFound() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> studentService.deleteStudent(1L));


        assertEquals(
                "Student not found",
                exception.getMessage()
        );

        verify(repository, never()).delete(any(Student.class));
    }


    // SEARCH //

    @Test
    void shouldSearchStudentsSuccessfully() {

        String query = "Mik";

        Pageable pageable = PageRequest.of(0, 10);

        Student student = student();

        StudentResponse response = mock(StudentResponse.class);

        Page<Student> students =
                new PageImpl<>(
                        List.of(student),
                        pageable,
                        1
                );


        when(repository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                        query,
                        query,
                        pageable
                ))
                .thenReturn(students);

        when(mapper.toResponse(student))
                .thenReturn(response);


        Page<StudentResponse> result = studentService.searchStudents(query, pageable);


        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().get(0));

        verify(repository)
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                        query,
                        query,
                        pageable
                );
    }
}