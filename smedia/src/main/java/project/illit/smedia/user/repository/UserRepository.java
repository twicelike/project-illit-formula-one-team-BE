package project.illit.smedia.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.illit.smedia.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByRole_NameIgnoreCase(String roleName, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE User u
            SET u.totalFollower = COALESCE(u.totalFollower, 0) + 1
            WHERE u.id = :userId
            """)
    int incrementTotalFollower(@Param("userId") Integer userId);

    @Modifying
    @Query("""
            UPDATE User u
            SET u.totalFollower = CASE WHEN COALESCE(u.totalFollower, 0) > 0 THEN COALESCE(u.totalFollower, 0) - 1 ELSE 0 END
            WHERE u.id = :userId
            """)
    int decrementTotalFollower(@Param("userId") Integer userId);

    @Modifying
    @Query("""
            UPDATE User u
            SET u.totalFollowing = COALESCE(u.totalFollowing, 0) + 1
            WHERE u.id = :userId
            """)
    int incrementTotalFollowing(@Param("userId") Integer userId);

    @Modifying
    @Query("""
            UPDATE User u
            SET u.totalFollowing = CASE WHEN COALESCE(u.totalFollowing, 0) > 0 THEN COALESCE(u.totalFollowing, 0) - 1 ELSE 0 END
            WHERE u.id = :userId
            """)
    int decrementTotalFollowing(@Param("userId") Integer userId);
}
