package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.AlumniDirectoryResponse;
import com.teligati.backend.dto.PagedResponse;
import com.teligati.backend.dto.StudentDirectoryResponse;
import com.teligati.backend.dto.TeacherDirectoryResponse;
import com.teligati.backend.service.DirectoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    public DirectoryController(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<PagedResponse<StudentDirectoryResponse>>> getStudentDirectory(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "batch", required = false) String batch,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("id").ascending());
        Page<StudentDirectoryResponse> result = directoryService.getStudentDirectory(name, department, batch, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(result), "Student directory retrieved successfully"));
    }

    @GetMapping("/alumni")
    public ResponseEntity<ApiResponse<PagedResponse<AlumniDirectoryResponse>>> getAlumniDirectory(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "batch", required = false) String batch,
            @RequestParam(value = "company", required = false) String company,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "position", required = false) String position,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("id").ascending());
        Page<AlumniDirectoryResponse> result = directoryService.getAlumniDirectory(name, department, batch, company, country, position, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(result), "Alumni directory retrieved successfully"));
    }

    @GetMapping("/teachers")
    public ResponseEntity<ApiResponse<PagedResponse<TeacherDirectoryResponse>>> getTeacherDirectory(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "designation", required = false) String designation,
            @RequestParam(value = "researchFocus", required = false) String researchFocus,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("id").ascending());
        Page<TeacherDirectoryResponse> result = directoryService.getTeacherDirectory(name, department, designation, researchFocus, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(result), "Teacher directory retrieved successfully"));
    }
}
