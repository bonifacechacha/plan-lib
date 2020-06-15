package com.niafikra.dimension.plan.repository;

import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.core.util.repository.NamedRepository;
import com.niafikra.dimension.plan.domain.CostCenter;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 8/31/17 9:12 PM
 */
@Repository
public interface CostCenterRepository extends NamedRepository<CostCenter> {
    List<CostCenter> findCostCentersByPlanners(User user);
}