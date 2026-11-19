package project.illit.smedia.interaction.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.illit.smedia.interaction.entity.Reaction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Integer> {

    Optional<Reaction> findByTargetIdAndTargetTypeAndUser_Id(
            Integer targetId,
            String targetType,
            Integer userId
    );

    @EntityGraph(attributePaths = {"user", "user.role"})
    Page<Reaction> findByTargetIdAndTargetTypeOrderByIdDesc(
            Integer targetId,
            String targetType,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT r.targetId, r.user.id
            FROM Reaction r
            JOIN r.user u
            JOIN u.role role
            WHERE r.targetType = :targetType
              AND r.targetId IN :targetIds
              AND UPPER(role.name) = 'ARTIST'
            """)
    List<Object[]> findDistinctArtistReactorsByTargetIds(
            @Param("targetType") String targetType,
            @Param("targetIds") Collection<Integer> targetIds
    );

    @Query("""
            SELECT r.targetId
            FROM Reaction r
            WHERE r.user.id = :userId
              AND r.targetType = :targetType
              AND r.targetId IN :targetIds
            """)
    List<Integer> findReactedTargetIdsByUser(
            @Param("userId") Integer userId,
            @Param("targetType") String targetType,
            @Param("targetIds") Collection<Integer> targetIds
    );

    @Modifying
    @Query("""
            DELETE FROM Reaction r
            WHERE r.targetType = :targetType
              AND r.targetId IN :targetIds
            """)
    int deleteByTargetTypeAndTargetIdIn(
            @Param("targetType") String targetType,
            @Param("targetIds") Collection<Integer> targetIds
    );
}
