package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.core.util.event.SaveEvent;
import com.niafikra.dimension.plan.domain.CostCenter;
import com.niafikra.dimension.plan.domain.QCostCenter;
import com.niafikra.dimension.plan.domain.Resource;
import com.niafikra.dimension.plan.domain.Role;
import com.niafikra.dimension.plan.repository.CostCenterRepository;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 8/30/17 10:42 PM
 */
@Service
//@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class CostCenterService {
    private CostCenterRepository costCenterRepository;
    private ApplicationEventPublisher publisher;
    private RoleService roleService;

    public CostCenterService(CostCenterRepository costCenterRepository, ApplicationEventPublisher publisher, RoleService roleService) {
        this.costCenterRepository = costCenterRepository;
        this.publisher = publisher;
        this.roleService = roleService;
    }

    @Transactional
    public CostCenter save(CostCenter costCenter) {
        costCenter = costCenterRepository.save(costCenter);
        publisher.publishEvent(new SaveEvent<CostCenter>(costCenter));
        return costCenter;
    }

    @Transactional
    public void delete(CostCenter costCenter) {
        costCenterRepository.delete(costCenter);
    }

    public Page<CostCenter> findCostCenters(String nameFilter, Pageable pageable) {
        return costCenterRepository.findAllByNameContainingIgnoreCase(nameFilter, pageable);
    }

    public Long countCostCenters(String nameFilter) {
        return costCenterRepository.countAllByNameContainingIgnoreCase(nameFilter);
    }

    public List<CostCenter> getAll() {
        return costCenterRepository.findAll(Sort.by(Sort.Order.asc("name")));
    }

    @Transactional
    public CostCenter updateCostCenterRoles(CostCenter costCenter, Set<Role> roles) {
        costCenter.setRoles(roles);
        return save(costCenter);
    }

    @Transactional
    public CostCenter updateCostCenterResources(CostCenter costCenter, Set<Resource> resources) {
        costCenter.setResources(resources);
        return save(costCenter);
    }


    public CostCenter updateCostCenterPlanners(CostCenter costCenter, Set<User> planners) {
        costCenter.setPlanners(planners);
        return save(costCenter);
    }

    public CostCenter getCostCenter(Long id) {
        return costCenterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("There is no cost center with id :" + id));
    }

    public List<CostCenter> getPlannerCostCenters(User user) {
        return costCenterRepository.findCostCentersByPlanners(user);
    }

    public Collection<CostCenter> findCostCenters(User user) {
        return roleService
                .findRoles(user)
                .stream()
                .flatMap(role -> findCostCenters(role).stream())
                .collect(Collectors.toSet());
    }

    private List<CostCenter> findCostCenters(Role role) {
        return costCenterRepository.findAll(QCostCenter.costCenter.roles.contains(role));
    }

    //transactional to ensure presence of hibernate transaction for loading the lazy collection
    @Transactional
    public Set<Role> getRoles(CostCenter costCenter) {
        CostCenter center = getCostCenter(costCenter.getId());
        Hibernate.initialize(center.getRoles());
        return center.getRoles();
    }

    @Transactional
    public Set<Resource> getResources(CostCenter costCenter) {
        CostCenter center = getCostCenter(costCenter.getId());
        Hibernate.initialize(center.getResources());
        return center.getResources();
    }

    @Transactional
    public Set<User> getPlanners(CostCenter costCenter) {
        CostCenter center = getCostCenter(costCenter.getId());
        Hibernate.initialize(center.getPlanners());
        return center.getPlanners();
    }

    @Transactional
    public boolean canPlan(CostCenter costCenter, User user) {
        CostCenter center = getCostCenter(costCenter.getId());
        return center.getPlanners().contains(user);
    }

    @Transactional
    public boolean allowResource(CostCenter costCenter, Resource resource) {
        CostCenter center = getCostCenter(costCenter.getId());
        return center.getResources().contains(resource);
    }

    @Transactional
    public boolean allowRole(CostCenter costCenter, Role role) {
        CostCenter center = getCostCenter(costCenter.getId());
        return center.getRoles().contains(role);
    }
}
