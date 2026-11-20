package project.illit.smedia.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import project.illit.smedia.user.entity.Role;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.repository.RoleRepository;

@Slf4j
@Component
@Order(1)
@ConditionalOnProperty(name = "app.seed-roles", havingValue = "true", matchIfMissing = true)
public class RoleDataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    public RoleDataSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByNameIgnoreCase(roleName.name()).orElseGet(() -> {
                log.info("Seeding default role: {}", roleName.name());
                return roleRepository.save(Role.builder().name(roleName.name()).build());
            });
        }
    }
}
