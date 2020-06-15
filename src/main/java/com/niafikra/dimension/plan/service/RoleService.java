package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.plan.domain.CostCenter;
import com.niafikra.dimension.plan.domain.QRole;
import com.niafikra.dimension.plan.domain.Resource;
import com.niafikra.dimension.plan.domain.Role;
import com.niafikra.dimension.plan.repository.CostCenterRepository;
import com.niafikra.dimension.plan.repository.RoleRepository;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 8/30/17 10:42 PM
 */
@Service("ProjectRoleService")//TODO THIS SERVICE NAME IS TO PREVENT COLLISION WITH THE ROLE SERIVE OF THE CORE
//@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class RoleService {
    private RoleRepository roleRepository;
    private CostCenterRepository costCenterRepository;

    public RoleService(RoleRepository roleRepository,
                       CostCenterRepository costCenterRepository) {
        this.roleRepository = roleRepository;
        this.costCenterRepository = costCenterRepository;
    }

    @Transactional
    public Role save(Role role) {
        return roleRepository.save(role);
    }

    @Transactional
    public void delete(Role role) {
        roleRepository.delete(role);
    }

    public Page<Role> findRoles(String nameFilter, Pageable pageable) {
        return roleRepository.findAllByNameContainingIgnoreCase(nameFilter, pageable);
    }

    public Long countRoles(String nameFilter) {
        return roleRepository.countAllByNameContainingIgnoreCase(nameFilter);
    }

    public List<Role> getAll() {
        return roleRepository.findAll(Sort.by(Sort.Order.asc("name")));
    }

    public Role getRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("There is no role with id " + id));
    }

    @Transactional
    public Role updateRoleUsers(Role role, Set<User> users) {
        role.setUsers(users);
        return save(role);
    }

    @Transactional
    public Role updateRoleResources(Role role, Set<Resource> resources) {
        role.setResources(resources);
        return save(role);
    }

    public List<Role> findRoles(User currentUser) {
        return roleRepository.findAll(QRole.role.users.contains(currentUser));
    }

    public Set<Role> findAllowedRoles(User user, Resource resource) {
        List<Role> roles = findRoles(user);
        return roles
                .stream()
                .filter(role -> isResourceAllowed(role, resource))
                .collect(Collectors.toSet());
    }

    @Transactional
    public Set<Role> findAllowedRoles(CostCenter costCenter, User user) {
        CostCenter center = costCenterRepository.findById(costCenter.getId()).get();
//        Set<Role> roles = costCenter.getRoles();
        Hibernate.initialize(center.getRoles());
        return findRoles(user)
                .stream()
                .filter(role -> center.getRoles().contains(role))
                .collect(Collectors.toSet());
    }

    @Transactional
    public Set<Role> findAllowedRoles(CostCenter costCenter, Resource resource, User user) {
        Set<Role> userResourceRoles = findAllowedRoles(user, resource);
        return findAllowedRoles(costCenter, user)
                .stream()
                .filter(role -> userResourceRoles.contains(role))
                .collect(Collectors.toSet());
    }

    @Transactional
    public Set<Resource> getResources(Role role) {
        role = getRole(role.getId());
        Hibernate.initialize(role.getResources());
        return role.getResources();
    }

    @Transactional
    public boolean isResourceAllowed(Role role, Resource resource) {
        role = getRole(role.getId());
        return role.getResources().contains(resource);
    }

    @Transactional
    public Integer countResources(Role role) {
        role = getRole(role.getId());
        return role.getResources().size();
    }
}
