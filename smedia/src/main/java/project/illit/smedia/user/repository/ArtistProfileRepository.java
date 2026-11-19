package project.illit.smedia.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.illit.smedia.user.entity.ArtistProfile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Integer> {

    @EntityGraph(attributePaths = {"company", "user"})
    Optional<ArtistProfile> findWithDetailsByUserId(Integer userId);

    @EntityGraph(attributePaths = {"company", "user"})
    List<ArtistProfile> findWithDetailsByUserIdInOrderByStageNameAsc(Collection<Integer> userIds);

    @Override
    @EntityGraph(attributePaths = {"company", "user"})
    Page<ArtistProfile> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"company", "user"})
    Page<ArtistProfile> findByCompany_Id(Integer companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "user"})
    Page<ArtistProfile> findByStageNameContainingIgnoreCase(String stageName, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "user"})
    Page<ArtistProfile> findByCompany_IdAndStageNameContainingIgnoreCase(
            Integer companyId,
            String stageName,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE ArtistProfile a
            SET a.followersCount = COALESCE(a.followersCount, 0) + 1
            WHERE a.userId = :artistUserId
            """)
    int incrementFollowersCount(@Param("artistUserId") Integer artistUserId);

    @Modifying
    @Query("""
            UPDATE ArtistProfile a
            SET a.followersCount = CASE WHEN COALESCE(a.followersCount, 0) > 0 THEN COALESCE(a.followersCount, 0) - 1 ELSE 0 END
            WHERE a.userId = :artistUserId
            """)
    int decrementFollowersCount(@Param("artistUserId") Integer artistUserId);

    @EntityGraph(attributePaths = {"company", "user"})
    @Query("""
            SELECT a FROM ArtistProfile a
            JOIN a.user u
            LEFT JOIN a.company c
            WHERE u.isActive = true
              AND (:filterByCompany = false OR c.id = :companyId)
              AND (:filterByFandom = false OR EXISTS (
                    SELECT 1 FROM FandomMember fm
                    WHERE fm.fandom.id = :fandomId
                      AND fm.user.id = a.userId
                      AND UPPER(fm.role) = 'ARTIST'
              ))
              AND (
                  LOWER(a.stageName) LIKE :containsPattern ESCAPE '\\'
                  OR LOWER(u.name) LIKE :containsPattern ESCAPE '\\'
                  OR LOWER(c.name) LIKE :containsPattern ESCAPE '\\'
              )
            ORDER BY CASE WHEN LOWER(a.stageName) LIKE :prefixPattern ESCAPE '\\' THEN 0 ELSE 1 END ASC,
                     COALESCE(a.followersCount, 0) DESC,
                     a.stageName ASC
            """)
    Page<ArtistProfile> searchActiveArtists(
            @Param("containsPattern") String containsPattern,
            @Param("prefixPattern") String prefixPattern,
            @Param("filterByCompany") boolean filterByCompany,
            @Param("companyId") int companyId,
            @Param("filterByFandom") boolean filterByFandom,
            @Param("fandomId") int fandomId,
            Pageable pageable
    );
}
