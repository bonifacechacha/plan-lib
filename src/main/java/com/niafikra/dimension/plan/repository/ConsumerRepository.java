package com.niafikra.dimension.plan.repository;

import com.niafikra.dimension.category.repository.CategorisedRepository;
import com.niafikra.dimension.plan.domain.Consumer;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 8/31/17 12:09 PM
 */
@Repository
public interface ConsumerRepository extends CategorisedRepository<Consumer> {
    List<Consumer> findAllByActive(boolean active);
}
