package com.niafikra.dimension.plan.repository;

import com.niafikra.dimension.core.util.repository.NamedRepository;
import com.niafikra.dimension.plan.domain.Role;
import org.springframework.stereotype.Repository;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 8/30/17 10:46 PM
 */
@Repository("ProjectRoleRepository")
public interface RoleRepository extends NamedRepository<Role> {
}
