package project.illit.smedia.fandom.dto;

import project.illit.smedia.fandom.entity.Company;

public record CompanyResponse(
        Integer id,
        String name,
        String bio
) {
    public static CompanyResponse from(Company company) {
        if (company == null) {
            return null;
        }
        return new CompanyResponse(company.getId(), company.getName(), company.getBio());
    }
}
