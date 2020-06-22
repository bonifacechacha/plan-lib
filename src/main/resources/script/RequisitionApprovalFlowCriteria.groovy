package script

import com.niafikra.dimension.plan.domain.Requisition
import com.niafikra.dimension.plan.service.RequisitionApprovalFlowService

// return [] as Set
// ApplicationContext applicationContext;

Requisition requisition = approvable
RequisitionApprovalFlowService flowService = applicationContext.getBean(RequisitionApprovalFlowService)
return flowService.findNextApprovers(requisition)