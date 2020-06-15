package com.niafikra.dimension.plan.repository;

import com.niafikra.dimension.core.util.repository.BaseRepository;
import com.niafikra.dimension.plan.domain.Budget;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/12/17 7:44 PM
 */
@Repository
@Transactional
public interface BudgetRepository extends BaseRepository<Budget, Long> {
    Budget findByTitle(String title);
}
