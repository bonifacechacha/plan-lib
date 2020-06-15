package com.niafikra.dimension.plan.repository;

import com.niafikra.dimension.core.util.repository.BaseRepository;
import com.niafikra.dimension.plan.domain.Allocation;
import org.springframework.stereotype.Repository;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/15/17 7:56 PM
 */
@Repository
public interface AllocationRepository extends BaseRepository<Allocation, Long> {
}
