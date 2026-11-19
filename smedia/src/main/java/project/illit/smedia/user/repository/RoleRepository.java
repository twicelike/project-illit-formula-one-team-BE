package project.illit.smedia.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.illit.smedia.user.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByNameIgnoreCase(String name);
}
