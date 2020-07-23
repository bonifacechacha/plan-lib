package script

import com.niafikra.dimension.plan.domain.RetirementEntry
import com.niafikra.dimension.plan.service.RequisitionApprovalFlowService

// return [] as Set
// ApplicationContext applicationContext;

RetirementEntry retirement = approvable
RequisitionApprovalFlowService flowService = applicationContext.getBean(RequisitionApprovalFlowService)
return flowService.findNextApprovers(retirement)