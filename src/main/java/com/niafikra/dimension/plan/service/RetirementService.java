package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.approval.ApprovalPermission;
import com.niafikra.dimension.approval.domain.Tracker;
import com.niafikra.dimension.approval.event.ApprovalCancellationEvent;
import com.niafikra.dimension.approval.event.ApprovalCompletionEvent;
import com.niafikra.dimension.approval.event.ApprovalEvent;
import com.niafikra.dimension.approval.event.FetchApprovableEvent;
import com.niafikra.dimension.approval.service.ApprovalTrackerService;
import com.niafikra.dimension.approval.service.CriteriaService;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.core.security.service.UserService;
import com.niafikra.dimension.money.Money;
import com.niafikra.dimension.plan.domain.*;
import com.niafikra.dimension.plan.repository.RetirementEntryRepository;
import com.niafikra.dimension.plan.repository.RetirementRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.niafikra.dimension.core.security.SecurityUtils.getCurrentUser;
import static com.niafikra.dimension.core.security.SecurityUtils.hasAuthority;
import static java.lang.String.format;

@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class RetirementService {

    private RetirementRepository retirementRepository;
    private RetirementEntryRepository entryRepository;
    private ExpenseService expenseService;
    private ApprovalTrackerService trackerService;
    private CriteriaService criteriaService;
    private UserService userService;

    public RetirementService(RetirementRepository retirementRepository, RetirementEntryRepository entryRepository, ExpenseService expenseService, ApprovalTrackerService trackerService, CriteriaService criteriaService, UserService userService) {
        this.retirementRepository = retirementRepository;
        this.entryRepository = entryRepository;
        this.expenseService = expenseService;
        this.trackerService = trackerService;
        this.criteriaService = criteriaService;
        this.userService = userService;
    }

    public Page<Retirement> findRetirementRequests(
            CostCenter costCenter,
            Budget budget,
            Role role,
            Resource resource,
            LocalDateTime startTime,
            LocalDateTime endTime,
            User creator,
            Consumer consumer,
            String description,
            Pageable pageable) {
        return retirementRepository.findAll(
                createPredicate(
                        costCenter,
                        budget,
                        role,
                        resource,
                        startTime,
                        endTime,
                        creator,
                        consumer,
                        description
                ),
                pageable
        );
    }

    public Long countRetirementRequests(
            CostCenter costCenter,
            Budget budget,
            Role role,
            Resource resource,
            LocalDateTime startTime,
            LocalDateTime endTime,
            User creator,
            Consumer consumer,
            String description) {
        return retirementRepository.count(
                createPredicate(
                        costCenter,
                        budget,
                        role,
                        resource,
                        startTime,
                        endTime,
                        creator,
                        consumer,
                        description
                )
        );
    }

    private Predicate createPredicate(
            CostCenter costCenter,
            Budget budget,
            Role role,
            Resource resource,
            LocalDateTime startTime,
            LocalDateTime endTime,
            User creator,
            Consumer consumer,
            String description) {

        QRetirement retirement = QRetirement.retirement;
        BooleanBuilder query = new BooleanBuilder();

        if (costCenter != null)
            query.and(retirement.expense.budget.costCenter.eq(costCenter));

        if (budget != null)
            query.and(retirement.expense.budget.eq(budget));
        if (role != null)
            query.and(retirement.expense.role.eq(role));
        if (resource != null)
            query.and(retirement.expense.resource.eq(resource));
        if (startTime != null)
            query.and(retirement.timeCreated.goe(startTime));
        if (endTime != null)
            query.and(retirement.timeCreated.loe(endTime));
        if (creator != null)
            query.and(retirement.creator.eq(creator));
        if (consumer != null)
            query.and(retirement.expense.consumer.eq(consumer));
        if (description != null)
            query.and(retirement.expense.payment.description.containsIgnoreCase(description));

        return query;
    }

    @Transactional
    public Retirement submit(Retirement retirement, User user) {
        retirement = save(retirement);

        Optional<Retirement> pendingRetirement = findPendingApprovalRetirement(retirement.getExpense());
        if (pendingRetirement.isPresent())
            throw new IllegalArgumentException("There is a another retirement request pending approval: " + pendingRetirement.get().getId());

        criteriaService.registerApproval(retirement, Retirement.APPROVAL_CRITERIA, user, Collections.emptyMap());
        return retirement;
    }

    @Transactional
    public Retirement save(Retirement retirement) {
        validateRetirement(retirement);

        if (trackerService.isApprovalStarted(retirement))
            throw new IllegalStateException("You are not allowed to update retirement once the approval has started");

        retirement = retirementRepository.save(retirement);
        return retirement;
    }

    @Transactional
    public void retire(Retirement retirement) {
        Expense expense = retirement.getExpense();

        if (!isApproved(retirement))
            throw new IllegalArgumentException("Retirement request is not approved");

//        if (retirement.getBudget().isArchived())
//            throw new IllegalStateException("Associated budget is locked " + retirement.getBudget());

        if (expense.isReconciled())
            throw new IllegalStateException("Associated expense is already reconciled");

        expenseService.retire(retirement);
        retirementRepository.save(retirement);
    }

    private void validateRetirement(Retirement retirement) {
        if (CollectionUtils.isEmpty(retirement.getEntries()))
            throw new IllegalArgumentException("There are no retired entries submited");

        if (!retirement.getCreator().equals(retirement.getExpense().getAssociatedUser()))
            throw new IllegalArgumentException(format("The creator of retirement :%s must be the person who was associated with the retired expense :%s", retirement.getCreator(), retirement.getExpense().getAssociatedUser()));

        User currentUser = getCurrentUser(userService);
        if (!currentUser.equals(retirement.getCreator()))
            throw new IllegalArgumentException(format("The retiring user :%s must be the person who was associated with the retired expense :%s", currentUser, retirement.getExpense().getAssociatedUser()));

        Optional<RetirementEntry> negRetirement = retirement.getEntries().stream().filter(r -> r.getAmount().isLessThanZero()).findAny();
        if (negRetirement.isPresent())
            throw new IllegalArgumentException("Retired amount should not be less than zero: " + negRetirement.get().getAmount());

        if (retirement.getExpense().isReconciled())
            throw new IllegalStateException("Associated expense is already reconciled");

//        if (retirement.getExpense().getBudget().isArchived())
//            throw new IllegalStateException("Associated budget is locked " + retirement.getExpense().getBudget());

    }

    public Optional<Retirement> findPendingApprovalRetirement(Expense expense) {
        return findRetirementRequests(expense)
                .stream()
                .filter(retirement -> trackerService.isPending(retirement))
                .findAny();
    }

    public Money findRetiredAmountPendingApproval(Expense expense) {
        return findPendingApprovalRetirement(expense)
                .map(retirement -> retirement.getTotal())
                .orElse(Money.getZERO());
    }

    public List<Retirement> findRetirementRequests(Expense expense) {
        return retirementRepository.findAll(QRetirement.retirement.expense.eq(expense));
    }

    @EventListener
    @Transactional
    public void onApprovalStep(ApprovalEvent<Retirement> event) {
        Retirement retirement = event.getApprovable();
        if (event.isApproved() && retirement.getAcceptedEntries().isEmpty())
            throw new UnsupportedOperationException("There must be atlease one accepted entry in the retirement when approving");
    }

    @EventListener
    public void onFetchApprovable(FetchApprovableEvent event) {
        Tracker tracker = event.getTracker();
        if (tracker.getType().equals(Retirement.APPROVAL_TYPE)) {
            event.setApprovable(getRetirement(tracker));
        }
    }


    @EventListener
    @Transactional
    public void onRetirementApprovalComplete(ApprovalCompletionEvent<Retirement> event) {
        Retirement retirement = event.getApprovable();
        retirement = getRetirement(retirement.getId());
        retirementRepository.save(retirement);

        if (event.getTracker().isApproved())
            retire(retirement);
        else {
            retirement.getAcceptedEntries().forEach(entry -> entry.setAccepted(false));
            retirementRepository.save(retirement);
        }
    }

    public boolean isApproved(Retirement retirement) {
        return trackerService.isApproved(retirement);
    }

    @EventListener
    @Transactional
    public void onRetirementApprovalCancellation(ApprovalCancellationEvent<Retirement> event) {
        Retirement retirement = event.getApprovable();
        expenseService.cancelRetirements(retirement);
    }


    @Transactional
    public void delete(Retirement retirement) {
        if (trackerService.isRegistered(retirement))
            trackerService.cancelApproval(retirement);

        retirementRepository.delete(retirement);
    }

    public Retirement getRetirement(Tracker tracker) {
        if (!tracker.getType().equals(Retirement.APPROVAL_TYPE))
            throw new IllegalArgumentException("Tracker type" + tracker.getType() + " is not of type :" + Retirement.APPROVAL_TYPE);

        return getRetirement(Retirement.getId(tracker.getReference()));
    }


    public Retirement getRetirement(Long id) {
        return retirementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("There is no retirement request with id :" + id));
    }

    public String getStatus(Retirement retirement) {
        if (trackerService.isRegistered(retirement)) {
            return trackerService.getTracker(retirement).getStatus();
        } else return "Draft";
    }

    @Transactional
    public Retirement acceptRetirementEntries(Retirement retirement, Set<RetirementEntry> acceptedEntries) {

        if (!trackerService.isPending(retirement))
            throw new IllegalStateException("Retirement request is not currently pending approval");

        //can current user approve retirement request
        if (!trackerService.canApprove(retirement, getCurrentUser(userService)) && !hasAuthority(ApprovalPermission.OVERRIDE_APPROVE_REQUEST))
            throw new UnsupportedOperationException("Current user is not among the current approvers of the retirement request");

        //find entry which is not part of the retirement
        Optional<RetirementEntry> notBelongingEntry = acceptedEntries.stream().filter(entry -> !retirement.getEntries().contains(entry)).findAny();
        if (notBelongingEntry.isPresent())
            throw new IllegalArgumentException("There is an entry which is not part of the submited retirement request " + notBelongingEntry.get());

        acceptedEntries.stream().forEach(entry -> entry.setAccepted(true));
        retirement.getEntries().stream().filter(entry -> !acceptedEntries.contains(entry)).forEach(entry -> entry.setAccepted(false));

        entryRepository.saveAll(retirement.getEntries());
        return retirementRepository.findById(retirement.getId()).get();
    }

    public BigDecimal calculateEntryPercentage(RetirementEntry entry) {
        if (entry.getId() == null) return null;
        Optional<Retirement> retirement = findRetirement(entry);
        if (retirement.isPresent()) return retirement.get().calculateEntryPercentage(entry);
        else return null;
    }

    public Optional<Retirement> findRetirement(RetirementEntry entry) {
        return retirementRepository.findOne(QRetirement.retirement.entries.contains(entry));
    }
}
