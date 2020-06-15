package com.niafikra.dimension.plan.repository;

import com.niafikra.dimension.core.util.repository.BaseRepository;
import com.niafikra.dimension.plan.domain.Expense;
import org.springframework.stereotype.Repository;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 10/1/17 2:34 PM
 */
@Repository
public interface ExpenseRepository extends BaseRepository<Expense, Long> {
}
