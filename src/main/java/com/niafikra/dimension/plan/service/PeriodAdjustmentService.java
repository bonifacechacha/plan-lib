package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.approval.domain.Approvable;
import com.niafikra.dimension.approval.domain.Tracker;
import com.niafikra.dimension.approval.event.ApprovalCancellationEvent;
import com.niafikra.dimension.approval.event.ApprovalCompletionEvent;
import com.niafikra.dimension.approval.event.FetchApprovableEvent;
import com.niafikra.dimension.approval.service.ApprovalTrackerService;
import com.niafikra.dimension.approval.service.CriteriaService;
import com.niafikra.dimension.core.security.SecurityUtils;
import com.niafikra.dimension.core.security.service.UserService;
import com.niafikra.dimension.plan.domain.Budget;
import com.niafikra.dimension.plan.domain.PeriodAdjustment;
import com.niafikra.dimension.plan.domain.QPeriodAdjustment;
import com.niafikra.dimension.plan.repository.PeriodAdjustmentRepository;
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

@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class PeriodAdjustmentService {

    private UserService userService;
    private ApprovalTrackerService trackerService;
    private CriteriaService criteriaService;
    private PeriodAdjustmentRepository adjustmentRepository;
    private BudgetService budgetService;


    public PeriodAdjustmentService(UserService userService, ApprovalTrackerService trackerService, CriteriaService criteriaService, PeriodAdjustmentRepository adjustmentRepository, BudgetService budgetService) {
        this.userService = userService;
        this.trackerService = trackerService;
        this.criteriaService = criteriaService;
        this.adjustmentRepository = adjustmentRepository;
        this.budgetService = budgetService;
    }

    public PeriodAdjustment create(PeriodAdjustment adjustment) {

        if (!adjustment.getBudget().isApproved()) {
            throw new IllegalStateException("Period adjustment can be created only on approved budget");
        }

        if (!adjustment.getProposedEndDate().isAfter(adjustment.getBudget().getEndDate())) {
            throw new IllegalArgumentException("Period adjustment proposed date (" + adjustment.getProposedEndDate() + ") should be greater than the current end date of the budget " + adjustment.getBudget().getEndDate());
        }

        Optional<PeriodAdjustment> pendingAdjustment = findPendingPeriodAdjustment(adjustment.getBudget());
        if (pendingAdjustment.isPresent()) {
            throw new IllegalArgumentException("There is an existing pending period adjustment waiting for approval");
        }

        adjustment.setCreator(SecurityUtils.getCurrentUser(userService));
        return adjustmentRepository.save(adjustment);
    }

    public Optional<PeriodAdjustment> findPendingPeriodAdjustment(Budget budget) {
        QPeriodAdjustment adjustment = QPeriodAdjustment.periodAdjustment;
        return adjustmentRepository.findOne(
                new BooleanBuilder()
                        .and(adjustment.approved.isNull())
                        .and(adjustment.budget.eq(budget))
        );
    }

    public String generateStatus(PeriodAdjustment periodAdjustment) {
        if (!isApprovalSubmitted(periodAdjustment))
            return "Draft";
        else {
            return periodAdjustment.isApproved() ? "Approved" : (periodAdjustment.isDeclined() ? "Declined" : "Waiting approval");
        }
    }

    private boolean isApprovalSubmitted(PeriodAdjustment periodAdjustment) {
        return trackerService.isRegistered(periodAdjustment);
    }

    public Page<PeriodAdjustment> findAll(Budget budget, Pageable pageable) {
        return adjustmentRepository.findAll(QPeriodAdjustment.periodAdjustment.budget.eq(budget), pageable);
    }

    public Long countAll(Budget budget) {
        return adjustmentRepository.count(QPeriodAdjustment.periodAdjustment.budget.eq(budget));
    }


    @Transactional
    public PeriodAdjustment createSubmit(PeriodAdjustment adjustment) {
        create(adjustment);
        return submit(adjustment);
    }


    @Transactional
    public PeriodAdjustment submit(PeriodAdjustment adjustment) {
        if (isApprovalSubmitted(adjustment))
            throw new IllegalArgumentException("Period adjustment is already submitted for approval");

        criteriaService.registerApproval(adjustment, PeriodAdjustment.APPROVAL_CRITERIA, SecurityUtils.getCurrentUser(userService), Collections.emptyMap());
        return adjustment;
    }

    @EventListener
    @Transactional
    public void onPeriodAdjustmentApprovalCancellation(ApprovalCancellationEvent<PeriodAdjustment> event) {
        PeriodAdjustment adjustment = event.getApprovable();
        if (adjustment.isApproved()) {
            throw new IllegalStateException("You can not cancel approval once period adjustment is approved");
        }

        //when cancelled delete the request so that they have to submit a new one
        adjustmentRepository.delete(adjustment);
    }


    @EventListener
    public void onFetchPeriodAdjustment(FetchApprovableEvent event) {
        Tracker tracker = event.getTracker();
        if (tracker.getType().equals(PeriodAdjustment.APPROVAL_TYPE)) {
            event.setApprovable(getPeriodAdjustment(tracker));
        }
    }

    private Approvable getPeriodAdjustment(Tracker tracker) {
        if (!tracker.getType().equals(PeriodAdjustment.APPROVAL_TYPE))
            throw new IllegalArgumentException("Tracker type" + tracker.getType() + " is not of type :" + PeriodAdjustment.APPROVAL_TYPE);

        return findAdjustment(PeriodAdjustment.getId(tracker.getReference()));
    }


    @EventListener
    @Transactional
    public void onPeriodAdjustmentApprovalComplete(ApprovalCompletionEvent<PeriodAdjustment> event) {
        PeriodAdjustment periodAdjustment = event.getApprovable();
        periodAdjustment = findAdjustment(periodAdjustment.getId());
        periodAdjustment.setApproved(event.getTracker().isApproved());
        adjustmentRepository.save(periodAdjustment);

        if (periodAdjustment.isApproved()) {
            budgetService.adjustEndDate(periodAdjustment.getBudget(), periodAdjustment.getProposedEndDate());
        }
    }

    public PeriodAdjustment findAdjustment(Tracker tracker) {
        return findAdjustment(PeriodAdjustment.getId(tracker.getReference()));
    }

    public PeriodAdjustment findAdjustment(Long id) {
        return adjustmentRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("There is no period adjustment with id " + id)
        );
    }


    @Transactional
    public PeriodAdjustment updateProposedDate(PeriodAdjustment adjustment, LocalDate proposedEndDate) {
        if (adjustment.isApproved()) {
            throw new IllegalStateException("You can not change the date of an already approved period adjustment");
        }

        if (!proposedEndDate.isAfter(adjustment.getBudget().getEndDate())) {
            throw new IllegalArgumentException("Period adjustment proposed date (" + adjustment.getProposedEndDate() + ") should be greater than the current end date of the budget " + adjustment.getBudget().getEndDate());
        }

        adjustment = findAdjustment(adjustment.getId());
        adjustment.setProposedEndDate(proposedEndDate);

        return adjustmentRepository.save(adjustment);
    }
}
