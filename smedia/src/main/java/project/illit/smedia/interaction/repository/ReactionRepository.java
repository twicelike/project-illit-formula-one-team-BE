package project.illit.smedia.interaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.illit.smedia.interaction.entity.Reaction;

import java.util.Collection;
import java.util.List;

public interface ReactionRepository extends JpaRepository<Reaction, Integer> {

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
}
