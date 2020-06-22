package script

import com.niafikra.dimension.core.security.domain.User
import Delegation
import com.niafikra.dimension.approval.domain.Strategy
import com.niafikra.dimension.approval.service.StrategyService
import com.niafikra.dimension.approval.service.ApprovalTrackerService

//ApplicationContext applicationContext;

ApprovalTrackerService trackerService = applicationContext.getBean(TrackerService)
StrategyService strategyService = applicationContext.getBean(StrategyService)
Strategy strategy = strategyService.getStrategy("DelegationApprovalFlow")
Set approvers = strategyService.getNextApprovers(tracker,strategy)

//if there is no any other pending approver then let the delegatee to approve
if (approvers.isEmpty()) {
    Delegation delegation = trackerService.getApprovable(tracker)
    User delegatee = delegation.getDelegatee()

    //ensure that the delegatee has not approved yet
    if (!trackerService.getApprovedUsers(tracker).contains(delegatee)) return [delegatee] as Set
    else return [] as Set;
} else return approvers;