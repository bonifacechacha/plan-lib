package com.niafikra.dimension.plan.repository;

import com.niafikra.dimension.core.util.repository.BaseRepository;
import com.niafikra.dimension.plan.domain.PeriodAdjustment;
import org.springframework.stereotype.Repository;

@Repository
public interface PeriodAdjustmentRepository extends BaseRepository<PeriodAdjustment, Long> {
}
