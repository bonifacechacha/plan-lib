package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.approval.domain.Approvable;
import com.niafikra.dimension.approval.domain.Strategy;
import com.niafikra.dimension.approval.domain.Tracker;
import com.niafikra.dimension.approval.service.ApprovalTrackerService;
import com.niafikra.dimension.approval.service.StrategyService;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.plan.domain.*;
import com.niafikra.dimension.plan.repository.RequisitionApprovalFlowRepository;
import com.querydsl.core.BooleanBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class RequisitionApprovalFlowService {
    @Value("${dimension.requisition.enforce-approval-flow}")
    private boolean enforceApprovalFlow;

    private RequisitionApprovalFlowRepository flowRepository;
    private ApprovalTrackerService trackerService;
    private StrategyService strategyService;

    public RequisitionApprovalFlowService(RequisitionApprovalFlowRepository flowRepository,
                                          ApprovalTrackerService trackerService,
                                          StrategyService strategyService) {
        this.flowRepository = flowRepository;
        this.trackerService = trackerService;
        this.strategyService = strategyService;
    }

    public List<RequisitionApprovalFlow> findAll() {
        return flowRepository.findAll();
    }


    @Transactional
    public void delete(RequisitionApprovalFlow flow) {
        flowRepository.delete(flow);
    }

    @Transactional
    public RequisitionApprovalFlow update(RequisitionApprovalFlow flow) {

        //check if there is a flow already created similar to the one we are updating
        //the id ensure that is different
        QRequisitionApprovalFlow QApprovalFlow = QRequisitionApprovalFlow.requisitionApprovalFlow;

        BooleanBuilder query = new BooleanBuilder();

        if (flow.getCostCenter() != null)
            query.and(QApprovalFlow.costCenter.eq(flow.getCostCenter()));
        else query.and(QApprovalFlow.costCenter.isNull());

        if (flow.getRole() != null)
            query.and(QApprovalFlow.role.eq(flow.getRole()));
        else query.and(QApprovalFlow.role.isNull());

        if (flow.getResource() != null)
            query.and(QApprovalFlow.resource.eq(flow.getResource()));
        else query.and(QApprovalFlow.resource.isNull());


        if (flow.getCategory() != null)
            query.and(QApprovalFlow.category.eq(flow.getCategory()));
        else query.and(QApprovalFlow.category.isNull());

        //the id check ensure that the one we are looking is not the one we are updating
        query.and(QApprovalFlow.id.ne(flow.getId()));

        Optional<RequisitionApprovalFlow> existingFlow = flowRepository.findOne(query);
        if (existingFlow.isPresent())
            throw new IllegalArgumentException("There is a similare approval flow already created with id : " + existingFlow.get().getId());
        return flowRepository.save(flow);
    }

    @Transactional
    public RequisitionApprovalFlow create(RequisitionApprovalFlow flow) {
        //check if there is a flow already created similar to the one we are creating
        QRequisitionApprovalFlow QApprovalFlow = QRequisitionApprovalFlow.requisitionApprovalFlow;
        BooleanBuilder query = new BooleanBuilder();

        if (flow.getCostCenter() != null)
            query.and(QApprovalFlow.costCenter.eq(flow.getCostCenter()));
        else query.and(QApprovalFlow.costCenter.isNull());

        if (flow.getRole() != null)
            query.and(QApprovalFlow.role.eq(flow.getRole()));
        else query.and(QApprovalFlow.role.isNull());

        if (flow.getResource() != null)
            query.and(QApprovalFlow.resource.eq(flow.getResource()));
        else query.and(QApprovalFlow.resource.isNull());


        if (flow.getCategory() != null)
            query.and(QApprovalFlow.category.eq(flow.getCategory()));
        else query.and(QApprovalFlow.category.isNull());


        Optional<RequisitionApprovalFlow> existingFlow = flowRepository.findOne(query);
        if (existingFlow.isPresent())
            throw new IllegalArgumentException("There is a similar approval flow already created with id : " + existingFlow.get().getId());

        return flowRepository.save(flow);
    }

    public Set<User> findNextApprovers(Requisition requisition) {
        return findNextApprovers(requisition.getCostCenter(),
                requisition.getRole(),
                requisition.getResource(),
                requisition);
    }

    public Set<User> findNextApprovers(Retirement retirement) {
        return findNextApprovers(retirement.getExpense().getBudget().getCostCenter(),
                retirement.getExpense().getRole(),
                retirement.getExpense().getResource(),
                retirement);
    }

    public Set<User> findNextApprovers(CostCenter costCenter, Role role, Resource resource, Approvable approvable) {
        RequisitionApprovalFlow approvalFlow = findRequisitionApprovalFlow(costCenter, role, resource);
        if (approvalFlow == null) {
            if (enforceApprovalFlow)
                throw new IllegalArgumentException("There is no approval flow for this requisition");
            else return Collections.emptySet();
        } else {
            Strategy strategy = createRuntimeStrategy(approvalFlow);
            Tracker tracker = trackerService.getTracker(approvable);
            return strategyService.getNextApprovers(tracker, strategy);
        }
    }

    private RequisitionApprovalFlow findRequisitionApprovalFlow(Requisition requisition) {
        return findRequisitionApprovalFlow(requisition.getCostCenter(), requisition.getRole(), requisition.getResource());
    }

    private RequisitionApprovalFlow findRequisitionApprovalFlow(CostCenter costCenter, Role role, Resource resource) {
        Assert.noNullElements(new Object[]{costCenter, role, resource}, "Both cost center role and resource must be specified ");

        QRequisitionApprovalFlow QApprovalFlow = QRequisitionApprovalFlow.requisitionApprovalFlow;
        BooleanBuilder query = new BooleanBuilder();

        query.and(QApprovalFlow.costCenter.eq(costCenter).or(QApprovalFlow.costCenter.isNull()));
        query.and(QApprovalFlow.role.eq(role).or(QApprovalFlow.role.isNull()));
        query.and(QApprovalFlow.resource.eq(resource).or(QApprovalFlow.resource.isNull()));
        query.and(QApprovalFlow.category.eq(resource.getCategory()).or(QApprovalFlow.category.isNull()));

        List<RequisitionApprovalFlow> flows = flowRepository.findAll(query);
        if (flows.isEmpty()) return null;
        return flows.stream().sorted().skip(flows.size() - 1).findFirst().orElse(null);
    }

    private Strategy createRuntimeStrategy(RequisitionApprovalFlow approvalFlow) {
        Strategy strategy = new Strategy();
        strategy.setLevels(approvalFlow.getLevels());
        return strategy;
    }

}
