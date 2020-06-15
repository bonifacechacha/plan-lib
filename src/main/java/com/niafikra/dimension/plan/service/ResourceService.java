package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.category.domain.Category;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.plan.domain.CostCenter;
import com.niafikra.dimension.plan.domain.QResource;
import com.niafikra.dimension.plan.domain.Resource;
import com.niafikra.dimension.plan.domain.Role;
import com.niafikra.dimension.plan.repository.ResourceRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 8/29/17 11:30 PM
 */

@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class ResourceService {

    private ResourceRepository resourceRepository;
    private RoleService roleService;
    private CostCenterService costCenterService;

    public ResourceService(ResourceRepository resourceRepository,
                           RoleService roleService,
                           CostCenterService costCenterService) {
        this.resourceRepository = resourceRepository;
        this.roleService = roleService;
        this.costCenterService = costCenterService;
    }

    public Resource save(Resource resource) {
        return resourceRepository.save(resource);
    }

    public void delete(Resource resource) {
        resourceRepository.delete(resource);
    }

    public Long countResources(Optional<ResourceFilter> filter) {
        if (filter.isPresent()) return resourceRepository.count(createPredicate(filter.get()));
        else return resourceRepository.count();
    }

    private Predicate createPredicate(ResourceFilter filter) {
        BooleanBuilder query = new BooleanBuilder();
        QResource resource = QResource.resource;

        if (filter.name != null) query.and(resource.name.containsIgnoreCase(filter.name));
        if (filter.description != null) query.and(resource.description.containsIgnoreCase(filter.description));
        if (filter.category != null) query.and(resource.category.eq(filter.category));

        return query;
    }

    public Page<Resource> findResources(Optional<ResourceFilter> filter, Pageable pageable) {
        if (filter.isPresent()) return resourceRepository.findAll(createPredicate(filter.get()), pageable);
        else return resourceRepository.findAll(pageable);
    }

    public List<Resource> getAll() {
        return resourceRepository.findAll(Sort.by(Sort.Order.asc("name")));
    }


    public Set<Resource> getResources(User user, CostCenter costCenter) {
        List<Role> roles = roleService.findRoles(user);
        return roles.stream()
                .flatMap(role -> roleService.getResources(role).stream())
                .filter(resource -> costCenterService.getResources(costCenter).contains(resource))
                .collect(Collectors.toSet());
    }

    /**
     * Get role compatible resources which are also compatible with the specified cost center
     *
     * @param costCenter
     * @return
     */
    public Set<Resource> getResources(Role role, CostCenter costCenter) {
        return costCenterService.getResources(costCenter)
                .stream()
                .filter(resource -> roleService.isResourceAllowed(role, resource))
                .collect(Collectors.toSet());
    }

    public Resource getResource(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("There is no resource with id " + resourceId));
    }


    @Getter
    @Setter
    @Builder
    public static class ResourceFilter {
        private String name;
        private Category category;
        private String description;
    }
}
