package project.illit.smedia.fandom.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @EntityGraph(attributePaths = {"company"})
    @Query("""
            SELECT f FROM OfficialFandom f
            LEFT JOIN f.company c
            WHERE UPPER(f.status) = 'ACTIVE'
              AND (:filterByCompany = false OR c.id = :companyId)
              AND (
                  LOWER(f.name) LIKE :containsPattern ESCAPE '\\'
                  OR LOWER(c.name) LIKE :containsPattern ESCAPE '\\'
              )
            ORDER BY CASE WHEN LOWER(f.name) LIKE :prefixPattern ESCAPE '\\' THEN 0 ELSE 1 END ASC,
                     f.name ASC
            """)
    Page<OfficialFandom> searchActiveFandoms(
            @Param("containsPattern") String containsPattern,
            @Param("prefixPattern") String prefixPattern,
            @Param("filterByCompany") boolean filterByCompany,
            @Param("companyId") int companyId,
            Pageable pageable
    );
}
