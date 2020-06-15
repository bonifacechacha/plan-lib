package com.niafikra.dimension.plan.repository;

import com.niafikra.dimension.core.util.repository.BaseRepository;
import com.niafikra.dimension.plan.domain.Retirement;
import org.springframework.stereotype.Repository;

@Repository
public interface RetirementRepository extends BaseRepository<Retirement, Long> {
}
