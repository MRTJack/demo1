package uz.pdp.app_hr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.app_hr.models.Role;
import uz.pdp.app_hr.models.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByRoleName(RoleName roleName);
}
