package project.illit.smedia.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.illit.smedia.user.entity.Follow;
import project.illit.smedia.user.entity.FollowId;

import java.util.Collection;
import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    @EntityGraph(attributePaths = {"follower", "follower.role"})
    Page<Follow> findById_FollowingIdOrderByCreatedAtDesc(Integer followingId, Pageable pageable);

    @EntityGraph(attributePaths = {"following", "following.role"})
    Page<Follow> findById_FollowerIdOrderByCreatedAtDesc(Integer followerId, Pageable pageable);

    @Query("""
            SELECT f.id.followingId
            FROM Follow f
            WHERE f.id.followerId = :followerId
              AND f.id.followingId IN :targetIds
            """)
    List<Integer> findFollowedUserIdsIn(
            @Param("followerId") Integer followerId,
            @Param("targetIds") Collection<Integer> targetIds
    );
}
