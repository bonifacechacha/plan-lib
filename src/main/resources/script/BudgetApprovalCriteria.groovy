package script

import com.niafikra.dimension.approval.domain.Strategy
import com.niafikra.dimension.approval.service.StrategyService
import com.niafikra.dimension.plan.domain.Budget

// return [] as Set
//ApplicationContext applicationContext;

StrategyService strategyService = applicationContext.getBean(StrategyService)
Budget budget = approvable;

String costCenterName = budget.getCostCenter().getName()
costCenterName = costCenterName.trim().replaceAll(" ", "")
String strategyName = costCenterName + "Flow";

Strategy strategy = strategyService.getStrategy(strategyName)
if (strategy == null)
    strategy = strategyService.getStrategy("GeneralBudgetFlow")

return strategyService.getNextApprovers(tracker,strategy)