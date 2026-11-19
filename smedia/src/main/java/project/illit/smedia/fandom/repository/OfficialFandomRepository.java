package project.illit.smedia.fandom.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import project.illit.smedia.fandom.entity.OfficialFandom;

import java.util.Optional;

public interface OfficialFandomRepository extends JpaRepository<OfficialFandom, Integer> {

    @EntityGraph(attributePaths = {"company"})
    Optional<OfficialFandom> findWithCompanyById(Integer id);

    boolean existsByNameIgnoreCase(String name);

    Optional<OfficialFandom> findByNameIgnoreCase(String name);

    @Override
    @EntityGraph(attributePaths = {"company"})
    Page<OfficialFandom> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    Page<OfficialFandom> findByStatusIgnoreCase(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    Page<OfficialFandom> findByCompany_Id(Integer companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    Page<OfficialFandom> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    Page<OfficialFandom> findByStatusIgnoreCaseAndCompany_Id(String status, Integer companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    Page<OfficialFandom> findByStatusIgnoreCaseAndNameContainingIgnoreCase(String status, String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    Page<OfficialFandom> findByCompany_IdAndNameContainingIgnoreCase(Integer companyId, String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    Page<OfficialFandom> findByStatusIgnoreCaseAndCompany_IdAndNameContainingIgnoreCase(
            String status,
            Integer companyId,
            String keyword,
            Pageable pageable
    );
}
