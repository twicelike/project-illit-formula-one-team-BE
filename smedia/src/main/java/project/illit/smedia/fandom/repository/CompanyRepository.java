package project.illit.smedia.fandom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.illit.smedia.fandom.entity.Company;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Integer> {

    Optional<Company> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
