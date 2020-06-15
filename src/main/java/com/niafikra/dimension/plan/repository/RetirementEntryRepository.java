package com.niafikra.dimension.plan.repository;

import com.niafikra.dimension.core.util.repository.BaseRepository;
import com.niafikra.dimension.plan.domain.RetirementEntry;
import org.springframework.stereotype.Repository;

@Repository
public interface RetirementEntryRepository extends BaseRepository<RetirementEntry, Long> {
}
