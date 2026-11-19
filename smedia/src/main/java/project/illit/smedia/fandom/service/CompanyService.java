package project.illit.smedia.fandom.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.common.exception.ConflictException;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.fandom.dto.CompanyRequest;
import project.illit.smedia.fandom.dto.CompanyResponse;
import project.illit.smedia.fandom.entity.Company;
import project.illit.smedia.fandom.repository.CompanyRepository;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companies;

    public CompanyService(CompanyRepository companies) {
        this.companies = companies;
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> listAll() {
        return companies.findAll().stream()
                .map(CompanyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse fetchDetail(Integer companyId) {
        return CompanyResponse.from(requireCompany(companyId));
    }

    @Transactional
    public CompanyResponse registerCompany(CompanyRequest payload) {
        String normalizedName = payload.name().trim();
        if (companies.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Entertainment company already exists: " + normalizedName);
        }
        Company entity = Company.builder()
                .name(normalizedName)
                .bio(payload.bio())
                .build();
        return CompanyResponse.from(companies.save(entity));
    }

    @Transactional
    public CompanyResponse editCompany(Integer companyId, CompanyRequest payload) {
        Company target = requireCompany(companyId);
        String normalizedName = payload.name().trim();
        companies.findByNameIgnoreCase(normalizedName)
                .filter(found -> !found.getId().equals(companyId))
                .ifPresent(duplicate -> {
                    throw new ConflictException("Entertainment company already exists: " + normalizedName);
                });
        target.setName(normalizedName);
        target.setBio(payload.bio());
        return CompanyResponse.from(companies.save(target));
    }

    @Transactional
    public void removeCompany(Integer companyId) {
        companies.delete(requireCompany(companyId));
    }

    public Company requireCompany(Integer companyId) {
        return companies.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Entertainment company not found with id " + companyId));
    }
}
