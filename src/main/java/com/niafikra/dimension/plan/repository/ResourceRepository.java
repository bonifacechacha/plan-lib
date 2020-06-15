package com.niafikra.dimension.plan.repository;

import com.niafikra.dimension.category.repository.CategorisedRepository;
import com.niafikra.dimension.plan.domain.Resource;
import org.springframework.stereotype.Repository;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 8/30/17 11:34 AM
 */
@Repository
public interface ResourceRepository extends CategorisedRepository<Resource> {
}
