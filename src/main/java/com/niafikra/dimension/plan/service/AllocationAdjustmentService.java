package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.approval.domain.Approvable;
import com.niafikra.dimension.approval.domain.Tracker;
import com.niafikra.dimension.approval.event.ApprovalCancellationEvent;
import com.niafikra.dimension.approval.event.ApprovalCompletionEvent;
import com.niafikra.dimension.approval.event.FetchApprovableEvent;
import com.niafikra.dimension.approval.service.ApprovalTrackerService;
import com.niafikra.dimension.approval.service.CriteriaService;
import com.niafikra.dimension.core.security.SecurityUtils;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.core.security.service.UserService;
import com.niafikra.dimension.money.Money;
import com.niafikra.dimension.plan.domain.*;
import com.niafikra.dimension.plan.repository.AllocationAdjustmentRepository;
import com.querydsl.core.BooleanBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * @Author Juma mketto
 * @Date 1/2/19.
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class AllocationAdjustmentService {
    private AllocationAdjustmentRepository allocationAdjustmentRepository;
    private UserService userService;
    private CriteriaService criteriaService;
    private ApprovalTrackerService trackerService;
    private AllocationService allocationService;

    public AllocationAdjustmentService(AllocationAdjustmentRepository allocationAdjustmentRepository,
                                       UserService userService,
                                       CriteriaService criteriaService,
                                       ApprovalTrackerService trackerService,
                                       AllocationService allocationService) {
        this.allocationAdjustmentRepository = allocationAdjustmentRepository;
        this.userService = userService;
        this.criteriaService = criteriaService;
        this.trackerService = trackerService;
        this.allocationService = allocationService;

    }

    @Transactional
    public AllocationAdjustment createSubmit(AllocationAdjustment adjustment, User creator) {
        create(adjustment, creator);
        return submit(adjustment);
    }

    private AllocationAdjustment submit(AllocationAdjustment adjustment) {
        if (isApprovalSubmitted(adjustment))
            throw new IllegalArgumentException("Allocation adjustment is already submitted for approval");

        adjustment.setAllocatedAmount(adjustment.getProposedAmount());
        allocationAdjustmentRepository.save(adjustment);

        criteriaService.registerApproval(adjustment, AllocationAdjustment.APPROVAL_CRITERIA, SecurityUtils.getCurrentUser(userService), Collections.emptyMap());
        return adjustment;
    }

    public AllocationAdjustment create(AllocationAdjustment adjustment, User creator) {
        if (!adjustment.getBudget().isApproved()) {
            throw new IllegalStateException("Allocation adjustment can be created only on approved budget");
        }
        if (LocalDate.now().isAfter(adjustment.getBudget().getEndDate())) {
            throw new IllegalArgumentException("Allocation adjustment can not be requested to the Expired budget. Budget Expired on " + adjustment.getBudget().getEndDate());
        }
        if (adjustment.getProposedAmount().isLessThanOrEqual(Money.getZERO())) {
            throw new IllegalArgumentException("Allocation adjustment can not be requested with Zero Amount.");
        }

        Optional<AllocationAdjustment> pendingAdjustment = findPendingAllocationAdjustment(
                adjustment.getBudget(),
                adjustment.getResource(),
                adjustment.getRole());

        if (pendingAdjustment.isPresent()) {
            throw new IllegalArgumentException("There is an existing pending allocation adjustment waiting for approval");
        }

        adjustment.setCreator(creator);

        return allocationAdjustmentRepository.save(adjustment);
    }

    private Optional<AllocationAdjustment> findPendingAllocationAdjustment(Budget budget, Resource resource, Role role) {
        QAllocationAdjustment adjustment = QAllocationAdjustment.allocationAdjustment;
        return allocationAdjustmentRepository.findOne(
                new BooleanBuilder()
                        .and(adjustment.approved.isNull())
                        .and(adjustment.budget.eq(budget))
                        .and(adjustment.resource.eq(resource))
                        .and(adjustment.role.eq(role))
        );

    }

    public Page<AllocationAdjustment> findAll(Budget budget, Pageable pageable) {
        return allocationAdjustmentRepository.findAll(QAllocationAdjustment.allocationAdjustment.budget.eq(budget), pageable);
    }

    public Long countAll(Budget budget) {
        return allocationAdjustmentRepository.count(QAllocationAdjustment.allocationAdjustment.budget.eq(budget));
    }

    public String generateStatus(AllocationAdjustment adjustment) {
        if (!isApprovalSubmitted(adjustment))
            return "Draft";
        else {
            return adjustment.isApproved() ? "Approved" : (adjustment.isDeclined() ? "Declined" : "Waiting approval");
        }
    }

    private boolean isApprovalSubmitted(AllocationAdjustment adjustment) {
        return trackerService.isRegistered(adjustment);
    }


    @EventListener
    @Transactional
    public void onAllocationAdjustmentApprovalCancellation(ApprovalCancellationEvent<AllocationAdjustment> event) {
        AllocationAdjustment adjustment = event.getApprovable();
        if (adjustment.isApproved()) {
            throw new IllegalStateException("You can not cancel approval once allocation adjustment is approved");
        }

        allocationAdjustmentRepository.delete(adjustment);
    }

    @EventListener
    public void onFetchAllocationAdjustment(FetchApprovableEvent event) {
        Tracker tracker = event.getTracker();
        if (tracker.getType().equals(AllocationAdjustment.APPROVAL_TYPE)) {
            event.setApprovable(getAllocationAdjustment(tracker));
        }
    }

    private Approvable getAllocationAdjustment(Tracker tracker) {
        if (!tracker.getType().equals(AllocationAdjustment.APPROVAL_TYPE))
            throw new IllegalArgumentException("Tracker type" + tracker.getType() + " is not of type :" + AllocationAdjustment.APPROVAL_TYPE);

        return findAdjustment(AllocationAdjustment.getId(tracker.getReference()));
    }

    private AllocationAdjustment findAdjustment(Long id) {
        return allocationAdjustmentRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("There is no allocation adjustment with id " + id)
        );
    }

    public AllocationAdjustment findAdjustment(Tracker tracker) {
        return findAdjustment(AllocationAdjustment.getId(tracker.getReference()));
    }

    @EventListener
    @Transactional
    public void onAllocationAdjustmentApprovalComplete(ApprovalCompletionEvent<AllocationAdjustment> event) {
        AllocationAdjustment adjustment = event.getApprovable();
        adjustment = findAdjustment(adjustment.getId());
        adjustment.setApproved(event.getTracker().isApproved());
        allocationAdjustmentRepository.save(adjustment);

        if (adjustment.isApproved()) {
            //  get Allocation and Update it.
            allocationService.adjustAllocation(
                    adjustment.getBudget(),
                    adjustment.getRole(),
                    adjustment.getResource(),
                    adjustment.getAllocatedAmount(),
                    adjustment.toString(),
                    adjustment.getReason(),
                    adjustment.getCreator()
            );
        }
    }

    public Set<Resource> getAllocatedResource(Budget budget) {
        return allocationService.getAllocatedResources(budget, SecurityUtils.getCurrentUser(userService));
    }

    @Transactional
    public AllocationAdjustment changeAllocatedAmount(AllocationAdjustment adjustment, Money amount, User user) {
        if (adjustment.isApproved()) {
            throw new IllegalStateException("You can not change the amount of an already approved Allocation adjustment");
        }

        if (!trackerService.canApproveOrOverride(adjustment, user))
            throw new SecurityException("Only the user who are set to approve the adjustment can change the allocated amount!");

        adjustment = findAdjustment(adjustment.getId());
        adjustment.setAllocatedAmount(amount);

        return allocationAdjustmentRepository.save(adjustment);
    }
}
