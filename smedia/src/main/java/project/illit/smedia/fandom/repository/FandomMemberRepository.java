package project.illit.smedia.fandom.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.illit.smedia.fandom.entity.FandomMember;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FandomMemberRepository extends JpaRepository<FandomMember, Long> {

    boolean existsByFandom_IdAndUser_Id(Integer fandomId, Integer userId);

    Optional<FandomMember> findByFandom_IdAndUser_Id(Integer fandomId, Integer userId);

    long countByFandom_Id(Integer fandomId);

    long countByFandom_IdAndRoleIgnoreCase(Integer fandomId, String role);

    @Query("""
            SELECT fm.fandom.id, COUNT(fm)
            FROM FandomMember fm
            WHERE fm.fandom.id IN :fandomIds
            GROUP BY fm.fandom.id
            """)
    List<Object[]> countMembersGroupedByFandomId(@Param("fandomIds") Collection<Integer> fandomIds);

    @Query("""
            SELECT fm.fandom.id, COUNT(fm)
            FROM FandomMember fm
            WHERE fm.fandom.id IN :fandomIds AND UPPER(fm.role) = 'ARTIST'
            GROUP BY fm.fandom.id
            """)
    List<Object[]> countArtistsGroupedByFandomId(@Param("fandomIds") Collection<Integer> fandomIds);

    @Query("""
            SELECT fm
            FROM FandomMember fm
            WHERE fm.user.id = :userId AND fm.fandom.id IN :fandomIds
            """)
    List<FandomMember> findByUserIdAndFandomIdIn(
            @Param("userId") Integer userId,
            @Param("fandomIds") Collection<Integer> fandomIds
    );

    @Query("""
            SELECT fm.user.id
            FROM FandomMember fm
            WHERE fm.fandom.id = :fandomId AND UPPER(fm.role) = 'ARTIST'
            """)
    List<Integer> findArtistUserIdsByFandomId(@Param("fandomId") Integer fandomId);

    @EntityGraph(attributePaths = {"fandom", "fandom.company"})
    Page<FandomMember> findByUser_IdAndFandom_StatusIgnoreCaseOrderByJoinedAtDesc(
            Integer userId,
            String fandomStatus,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role"})
    Page<FandomMember> findByFandom_Id(Integer fandomId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.role"})
    Page<FandomMember> findByFandom_IdAndRoleIgnoreCase(Integer fandomId, String role, Pageable pageable);
}
