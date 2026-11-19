package project.illit.smedia.fandom.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.illit.smedia.common.dto.ApiResponse;
import project.illit.smedia.common.ratelimit.RateLimit;
import project.illit.smedia.fandom.dto.CompanyRequest;
import project.illit.smedia.fandom.dto.CompanyResponse;
import project.illit.smedia.fandom.service.CompanyService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class EntertainmentCompanyController {

    private final CompanyService service;

    public EntertainmentCompanyController(CompanyService service) {
        this.service = service;
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/companies")
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> browseAll() {
        return ResponseEntity.ok(ApiResponse.ok("Loaded companies", service.listAll()));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/companies/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> viewOne(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(service.fetchDetail(id)));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/companies")
    public ResponseEntity<ApiResponse<CompanyResponse>> addCompany(@Valid @RequestBody CompanyRequest body) {
        CompanyResponse created = service.registerCompany(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Company created", created));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/companies/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> modifyCompany(
            @PathVariable Integer id,
            @Valid @RequestBody CompanyRequest body
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Company updated", service.editCompany(id, body)));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/companies/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable Integer id) {
        service.removeCompany(id);
        return ResponseEntity.ok(ApiResponse.ok("Company deleted"));
    }
}
