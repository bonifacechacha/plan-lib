package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.approval.domain.Tracker;
import com.niafikra.dimension.approval.event.ApprovalCancellationEvent;
import com.niafikra.dimension.approval.event.ApprovalCompletionEvent;
import com.niafikra.dimension.approval.event.FetchApprovableEvent;
import com.niafikra.dimension.approval.service.ApprovalTrackerService;
import com.niafikra.dimension.approval.service.CriteriaService;
import com.niafikra.dimension.core.security.SecurityUtils;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.core.security.service.UserService;
import com.niafikra.dimension.core.util.DateUtils;
import com.niafikra.dimension.money.Money;
import com.niafikra.dimension.plan.domain.*;
import com.niafikra.dimension.plan.repository.RequisitionRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/26/17 9:55 AM
 */

@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class RequisitionService {

    @Value("${dimension.requisition.allow-over-threshold-pending-reconciliation}")
    private boolean allowRequisitionPendingReconciliation;

    private ExpenseService expenseService;
    private ApprovalTrackerService trackerService;
    private CriteriaService criteriaService;
    private RoleService roleService;
    private UserService userService;
    private RequisitionRepository requisitionRepository;

    public RequisitionService(ExpenseService expenseService,
                              ApprovalTrackerService trackerService,
                              CriteriaService criteriaService,
                              RoleService roleService,
                              UserService userService,
                              RequisitionRepository requisitionRepository) {
        this.expenseService = expenseService;
        this.trackerService = trackerService;
        this.criteriaService = criteriaService;
        this.roleService = roleService;
        this.userService = userService;
        this.requisitionRepository = requisitionRepository;
    }

    public Money calculateTotalGrossBalance(Budget budget, Role role, Resource resource) {
        return expenseService
                .calculateTotalBalance(budget, role, resource)
                .minus(calculateTotalPendingPayments(budget, role, resource));
    }

    public Money calculateTotalGrossBalance(Budget budget) {
        return calculateTotalGrossBalance(budget, null, null);
    }

    public Money calculateTotalGrossRolesBalance(Budget budget, User user, Resource resource) {
        return roleService.findRoles(user).stream()
                .map(role -> calculateTotalGrossBalance(budget, role, resource))
                .reduce(Money.getZERO(), (total, bal) -> total.plus(bal));
    }

    public Money calculateTotalPendingPayments(User user) {
        return getRequisitions(null, null, null, null, null, null, user, null, null, true, false)
                .stream()
                .map(req -> req.getPendingAmount())
                .reduce(Money.getZERO(), (amt1, amt2) -> amt1.plus(amt2));
    }

    public Money calculateTotalPendingPayments(Budget budget, Role role, Resource resource) {
        return getRequisitions(null, budget, role, resource, null, null, null, null, null, true, false)
                .stream()
                .map(req -> req.getPendingAmount())
                .reduce(Money.getZERO(), (amt1, amt2) -> amt1.plus(amt2));
    }


    public Money calculateTotalPendingApprovalRequisitions(User user) {
        BooleanBuilder query = new BooleanBuilder()
                .and(QRequisition.requisition.approved.isNull())
                .and(QRequisition.requisition.creator.eq(user));

        List<Requisition> pendingRequests = requisitionRepository.findAll(query);
        return pendingRequests
                .stream()
                .map(req -> req.getPendingAmount())
                .reduce(Money.getZERO(), (amt1, amt2) -> amt1.plus(amt2));
    }
//
//    public List<Requisition> getPendingPaymentRequisitions(
//            CostCenter costCenter,
//            Budget budget,
//            Role role,
//            Resource resource,
//            LocalDateTime startTime,
//            LocalDateTime endTime,
//            User creator,
//            Consumer consumer) {
//        BooleanBuilder query = createPredicate(
//                costCenter,
//                budget,
//                role,
//                resource,
//                startTime,
//                endTime,
//                creator,
//                consumer,
//                null);
//
//        query.and(QRequisition.requisition.approved.eq(Boolean.TRUE));
//
//        List<Requisition> approvedRequisitions = requisitionRepository.findAll(query);
//        return approvedRequisitions
//                .stream()
//                .filter(req -> !req.isCompletelyPaid())
//                .collect(Collectors.toList());
//    }


    public Money calculateTotalMonthlyRequisitions() {
        LocalDateTime start = DateUtils.getStartOfThisMonth();
        LocalDateTime end = DateUtils.getEndOfThisMonth();
        return calculateTotalRequisitions(null, start, end);
    }

    private Money calculateTotalRequisitions(
            CostCenter costCenter,
            Budget budget,
            Role role,
            Resource resource,
            LocalDateTime start,
            LocalDateTime end,
            User creator,
            Consumer consumer,
            String description) {
        return getRequisitions(costCenter, budget, role, resource, start, end, creator, consumer, description, null, null)
                .stream()
                .map(requisition -> requisition.getApprovedAmount())
                .reduce(Money.getZERO(), (total, amount) -> total.plus(amount));
    }


    public Money calculateTotalRequisitions(User creator, LocalDateTime start, LocalDateTime end) {
        return calculateTotalRequisitions(null, null, null, null, start, end, creator, null, null);
    }

    /**
     * @return
     * @deprecated in favour of similar method which accept requisition filter instead
     */
    @Deprecated
    public List<Requisition> getRequisitions(
            CostCenter costCenter,
            Budget budget,
            Role role,
            Resource resource,
            LocalDateTime startTime,
            LocalDateTime endTime,
            User creator,
            Consumer consumer,
            String description,
            Boolean approved,
            Boolean fullFilled) {
        return findRequisitions(RequisitionFilter
                .builder()
                .costCenter(costCenter)
                .budget(budget)
                .role(role)
                .resource(resource)
                .startTime(startTime)
                .endTime(endTime)
                .creator(creator)
                .consumer(consumer)
                .description(description)
                .approved(approved)
                .fullFilled(fullFilled)
                .build()
        );
    }

    public List<Requisition> findRequisitions(RequisitionFilter filter) {
        return requisitionRepository.findAll(createPredicate(filter));
    }

    /**
     * @return
     * @deprecated in favour of similar method which accept requisition filter instead
     */
    @Deprecated
    public Page<Requisition> getRequisitions(
            CostCenter costCenter,
            Budget budget,
            Role role,
            Resource resource,
            LocalDateTime startTime,
            LocalDateTime endTime,
            User creator,
            Consumer consumer,
            String description,
            Boolean approved,
            Boolean fullFilled,
            Pageable pageable) {
        return findRequisitions(RequisitionFilter
                        .builder()
                        .costCenter(costCenter)
                        .budget(budget)
                        .role(role)
                        .resource(resource)
                        .startTime(startTime)
                        .endTime(endTime)
                        .creator(creator)
                        .consumer(consumer)
                        .description(description)
                        .approved(approved)
                        .fullFilled(fullFilled)
                        .build(),
                pageable
        );
    }

    public Page<Requisition> findRequisitions(RequisitionFilter filter, Pageable pageable) {
        return requisitionRepository.findAll(createPredicate(filter), pageable);
    }

    /**
     * @return
     * @deprecated in favour of similar method which accept requisition filter instead
     */
    @Deprecated
    public Long countRequisitions(
            CostCenter costCenter,
            Budget budget,
            Role role,
            Resource resource,
            LocalDateTime startTime,
            LocalDateTime endTime,
            User creator,
            Consumer consumer,
            String description,
            Boolean approved,
            Boolean fullFilled) {
        return countRequisitions(
                RequisitionFilter
                        .builder()
                        .costCenter(costCenter)
                        .budget(budget)
                        .role(role)
                        .resource(resource)
                        .startTime(startTime)
                        .endTime(endTime)
                        .creator(creator)
                        .consumer(consumer)
                        .description(description)
                        .approved(approved)
                        .fullFilled(fullFilled)
                        .build()
        );
    }

    public Long countRequisitions(RequisitionFilter filter) {
        return requisitionRepository.count(createPredicate(filter));
    }

    private BooleanBuilder createPredicate(RequisitionFilter filter) {

        QRequisition requisition = QRequisition.requisition;
        BooleanBuilder query = new BooleanBuilder();

        if (filter.id != null)
            query.and(requisition.id.eq(filter.id));

        if (filter.costCenter != null)
            query.and(requisition.budget.costCenter.eq(filter.costCenter));

        if (filter.budget != null)
            query.and(requisition.budget.eq(filter.budget));
        if (filter.role != null)
            query.and(requisition.role.eq(filter.role));
        if (filter.resource != null)
            query.and(requisition.resource.eq(filter.resource));
        if (filter.startTime != null)
            query.and(requisition.timeCreated.goe(filter.startTime));
        if (filter.endTime != null)
            query.and(requisition.timeCreated.loe(filter.endTime));
        if (filter.creator != null)
            query.and(requisition.creator.eq(filter.creator));
        if (filter.consumer != null)
            query.and(requisition.consumer.eq(filter.consumer));
        if (filter.description != null)
            query.and(requisition.description.containsIgnoreCase(filter.description));

        if (filter.approved != null)
            query.and(requisition.approved.eq(filter.approved));

        if (filter.fullFilled != null)
            query.and(requisition.fulfilled.eq(filter.fullFilled));

        return query;
    }

    @Transactional
    public Requisition create(Requisition requisition) {

        User creator = SecurityUtils.getCurrentUser(userService);

        requisition.setCreator(creator);
        requisition.setTimeCreated(LocalDateTime.now());

        //ensure that the requisitions pass the expenses checks
        validateRequisition(requisition);
        return requisitionRepository.save(requisition);
    }

    private void validateRequisition(Requisition requisition) {
        //ensure that the requisitions pass the expense checks
        expenseService.validateExpense(
                requisition.getBudget(),
                requisition.getRole(),
                requisition.getResource(),
                requisition.getCreator(),
                requisition.getTimeCreated()
        );

        Budget budget = requisition.getBudget();

        if (!budget.isUpToDate(requisition.getTimeCreated().toLocalDate()))
            throw new IllegalArgumentException("Requisitions can only be created on an up to date budget");

        if (!budget.isAllowRequestWithLessBalance()) {
            Money balance = expenseService.calculateTotalBalance(
                    requisition.getBudget(),
                    requisition.getRole(),
                    requisition.getResource()
            );

            if (balance.isLessThan(requisition.getRequestedAmount()))
                throw new IllegalStateException("Balance :" + balance + " is less than requested amount :" + requisition.getRequestedAmount());
        }

        if (!budget.isAllowRequestWithLessGrossBalance()) {
            Money balance = calculateTotalGrossBalance(
                    requisition.getBudget(),
                    requisition.getRole(),
                    requisition.getResource()
            );

            if (balance.isLessThan(requisition.getRequestedAmount()))
                throw new IllegalStateException("Gross balance :" + balance + " is less than requested amount :" + requisition.getRequestedAmount());
        }

        if (!budget.isAllowRequestWithSimilarPending()) {
            List<Requisition> pendingRequests = findPendingApprovalRequisitions(
                    requisition.getBudget(),
                    requisition.getRole(),
                    requisition.getResource()
            );

            //find other pending request apart from the one validated
            //if requisition is already created validation during submit will need to exclude it
            pendingRequests = pendingRequests
                    .stream()
                    .filter(req -> !req.getId().equals(requisition.getId()))
                    .collect(Collectors.toList());

            if (!pendingRequests.isEmpty()) {
                Requisition existingReq = pendingRequests.stream().findFirst().get();
                throw new IllegalArgumentException(String.format("A similar requisition by %s is pending approval :%s", existingReq.getCreator(), requisition));
            }

        }
    }

    public List<Requisition> findPendingApprovalRequisitions(Budget budget, Role role, Resource resource) {
        QRequisition requisition = QRequisition.requisition;
        BooleanBuilder query = new BooleanBuilder()
                .and(requisition.approved.isNull())
                .and(requisition.budget.eq(budget))
                .and(requisition.role.eq(role))
                .and(requisition.resource.eq(resource));

        return requisitionRepository.findAll(query);
    }

    @Transactional
    public Requisition update(Requisition requisition) {
        User currentUser = SecurityUtils.getCurrentUser(userService);
        if (!isEditable(requisition, currentUser))
            throw new IllegalArgumentException("You are not allowed to update tHis requisition.");

        validateRequisition(requisition);

        requisition = requisitionRepository.save(requisition);
        if (isSubmitted(requisition))
            trackerService.updateTrackerDescription(requisition);

        return requisition;
    }


    @Transactional
    public Requisition submit(Requisition requisition) {
        if (isSubmitted(requisition))
            throw new IllegalArgumentException("Requisition is already submitted for approval");

        User currentUser = SecurityUtils.getCurrentUser(userService);
        if (!requisition.getCreator().equals(currentUser))
            throw new IllegalArgumentException("Only the creator of requisition is allowed to submit it for approval");
//
        validateRequisition(requisition);
        checkOverThresholdPendingReconciliations(requisition.getCreator());

        requisition.setApprovedAmount(requisition.getRequestedAmount());
        requisition = update(requisition);
        criteriaService.registerApproval(requisition, Requisition.APPROVAL_CRITERIA, currentUser, Collections.emptyMap());

        return requisition;
    }

    private void checkOverThresholdPendingReconciliations(User user) {
        long overThresholdPendingRecon = expenseService.countOverThresholdPendingReconciliations(user);
        if (overThresholdPendingRecon > 0 && !allowRequisitionPendingReconciliation)
            throw new IllegalArgumentException(String.format("There is/are %d pending reconciliation(s) for %s that must be reconciled first!", overThresholdPendingRecon, user));
    }


    @Transactional
    public Requisition createSubmit(Requisition requisition) {
        requisition = create(requisition);
        return submit(requisition);
    }

    @Transactional
    public Requisition updateSubmit(Requisition requisition) {
        requisition = update(requisition);
        return submit(requisition);
    }

    public String getStatus(Requisition requisition) {
        if (!isSubmitted(requisition)) return "Draft";
        else {
            if (isApproved(requisition)) {
                if (requisition.isCompletelyPaid()) return "Completely paid";
                    //paid but not completely
                else if (requisition.isPaymentProcessed()) return "Partially paid";
                else return "Pending Payment";
            } else if (isDeclined(requisition)) return "Declined";
            else return "Waiting for approval";
        }
    }


    public Requisition getRequisition(Tracker tracker) {
        if (!tracker.getType().equals(Requisition.APPROVAL_TYPE))
            throw new IllegalArgumentException("Tracker type" + tracker.getType() + " is not of type :" + Requisition.APPROVAL_TYPE);

        return getRequisition(Requisition.getId(tracker.getReference()));
    }


    public Requisition getRequisition(Long id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("There is no requisition with id :" + id));
    }

    @EventListener
    public void onFetchApprovable(FetchApprovableEvent event) {
        Tracker tracker = event.getTracker();
        if (tracker.getType().equals(Requisition.APPROVAL_TYPE)) {
            event.setApprovable(getRequisition(tracker));
        }
    }


    @EventListener
    @Transactional
    public void onRequisitionApprovalComplete(ApprovalCompletionEvent<Requisition> event) {
        Requisition requisition = event.getApprovable();
        requisition = getRequisition(requisition.getId());
        requisition.setApproved(event.getTracker().isApproved());
        requisitionRepository.save(requisition);
    }

    @EventListener
    @Transactional
    public void onRequisitionApprovalCancellation(ApprovalCancellationEvent<Requisition> event) {
        Requisition requisition = event.getApprovable();
        if (requisition.isPaymentProcessed()) {
            throw new IllegalStateException("You can not cancel approval once payments for requisition are processed");
        }

        requisition.setApproved(null);
        requisition.setApprovedAmount(Money.getZERO());
        requisitionRepository.save(requisition);
    }

    public boolean isDeclined(Requisition requisition) {
        return trackerService.isDeclined(requisition);
    }

    public boolean isApproved(Requisition requisition) {
        return trackerService.isApproved(requisition);
    }

    public boolean isSubmitted(Requisition requisition) {
        return trackerService.isRegistered(requisition);
    }

    public boolean isEditable(Requisition requisition, User user) {
        if (trackerService.isRegistered(requisition)) {
            //if approval is started only allow users who are currently approving to edit requisition
            //if current user is not one of approvers then throw exception
            if (trackerService.canApproveOrOverride(requisition, user)) return true;
        } else {
            //if the approval process has not started then only the creator can update requisition
            if (user.equals(requisition.getCreator())) return true;
        }

        //otherwise anybody else should not be able to update except creator or approver as above
        return false;
    }

    public Expense pay(Requisition requisition, Payment payment) {
        requisition = getRequisition(requisition.getId());

        if (!requisition.isApproved())
            throw new IllegalStateException("Requisition is not approved");

        if (payment.getAmount().isZero() || payment.getAmount().isLessThan(Money.getZERO()))
            throw new IllegalArgumentException("Payment amount shall be greater than zero");

        if (payment.getAmount().isGreaterThan(requisition.getPendingAmount()))
            throw new IllegalArgumentException("Payment amount:" + payment.getAmount() + " is greater than the pending amount of the requisition: " + requisition.getPendingAmount());

        if (requisition.isCompletelyPaid())
            throw new IllegalStateException("Requisition is already paid completely");

        if (requisition.isLocked())
            throw new IllegalStateException("Requisition is locked");

//        checkOverThresholdPendingReconciliations(requisition.getCreator());

        Expense expense = new Expense(
                requisition.getBudget(),
                requisition.getRole(),
                requisition.getResource(),
                requisition.getCreator(),
                requisition.getConsumer()
        );
        expense = expenseService.create(expense, payment);
        requisition.addExpense(expense);

        //marj this requisition as complete to simplify fetching of not complete or completely paid requisitions
        if (requisition.isCompletelyPaid())
            requisition.setFulfilled(true);

        requisitionRepository.save(requisition);
        return expense;
    }

    public boolean isPayable(Requisition requisition) {
        return isApproved(requisition) && !requisition.isCompletelyPaid();
    }

    @Transactional
    public void delete(Requisition requisition) {
        if (requisition.isPaymentProcessed())
            throw new IllegalStateException("Requisition is already processed for payment");

        if (requisition.isLocked())
            throw new IllegalStateException("Requisition is locked");

        if (isSubmitted(requisition))
            trackerService.cancelApproval(requisition);

        requisition = requisitionRepository.findById(requisition.getId()).get();
        requisitionRepository.delete(requisition);
    }


    public Optional<Requisition> findRequisition(Expense expense) {
        return requisitionRepository.findOne(QRequisition.requisition.expenses.contains(expense));
    }


    public Optional<Requisition> findExpenseRequisition(Long expenseId) {
        return requisitionRepository.findOne(QRequisition.requisition.expenses.any().id.eq(expenseId));
    }

    public Optional<Requisition> findRequisition(Payment payment) {
        Optional<Expense> expense = expenseService.findExpense(payment);
        return expense.flatMap(this::findRequisition);
    }

    @Getter
    @Setter
    @Builder
    public static class RequisitionFilter {
        private Long id;
        private CostCenter costCenter;
        private Budget budget;
        private Role role;
        private Resource resource;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private User creator;
        private Consumer consumer;
        private String description;
        private Boolean approved;
        private Boolean fullFilled;

        public void setPeriodRange(LocalDateTime start, LocalDateTime end) {
            this.startTime = start;
            this.endTime = end;
        }

    }
}
