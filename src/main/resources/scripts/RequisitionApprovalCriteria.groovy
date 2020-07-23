package script

import com.niafikra.dimension.approval.domain.Strategy
import com.niafikra.dimension.approval.service.StrategyService

// return [] as Set
// ApplicationContext applicationContext;

StrategyService strategyService = applicationContext.getBean(StrategyService)
Strategy strategy = strategyService.getStrategy("RequisitionSampleFlow")
return strategyService.getNextApprovers(tracker,strategy)