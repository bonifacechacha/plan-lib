package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.approval.service.ApprovalTrackerService;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.core.security.service.UserService;
import com.niafikra.dimension.core.util.DateUtils;
import com.niafikra.dimension.group.service.GroupService;
import com.niafikra.dimension.money.Money;
import com.niafikra.dimension.plan.domain.*;
import com.niafikra.dimension.plan.repository.ExpenseRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/26/17 9:41 AM
 */
@Service
public class ExpenseService {

    @Value("${dimension.expense.auto-reconcile-complete-retirement}")
    private boolean autoReconcileCompleteRetirement;

    @Value("${dimension.expense.pending-reconciliation-maximum-duration}")
    private int pendingReconciliationMaximumDurationDays;

    private AllocationService allocationService;
    private BudgetService budgetService;
    private PaymentService paymentService;
    private ExpenseRepository expenseRepository;
    private RoleService roleService;
    private ApprovalTrackerService trackerService;
    private CostCenterService costCenterService;
    private GroupService groupService;
    private UserService userService;

    public ExpenseService(AllocationService allocationService,
                          BudgetService budgetService,
                          PaymentService paymentService,
                          ExpenseRepository expenseRepository,
                          RoleService roleService,
                          ApprovalTrackerService trackerService,
                          CostCenterService costCenterService,
                          GroupService groupService,
                          UserService userService) {
        this.allocationService = allocationService;
        this.budgetService = budgetService;
        this.paymentService = paymentService;
        this.expenseRepository = expenseRepository;
        this.roleService = roleService;
        this.trackerService = trackerService;
        this.costCenterService = costCenterService;
        this.groupService = groupService;
        this.userService = userService;
    }

    public Money calculateTotalExpenditure(Budget budget) {
        return calculateTotalExpenditure(budget, null, null);
    }

    public Money calculateTotalRolesExpenditure(Budget budget, User user, Resource resource) {
        return roleService.findRoles(user).stream()
                .map(role -> calculateTotalExpenditure(budget, role, resource))
                .reduce(Money.getZERO(), (total, expense) -> total.plus(expense));
    }

    public Money calculateTotalExpenditure(Budget budget, Role role, Resource resource) {
        return calculateTotalExpenditure(ExpenseFilter.builder().budget(budget).role(role).resource(resource).build());
    }

    private Money calculateTotalExpenditure(ExpenseFilter filter) {

        List<Expense> expenses = getExpenses(filter);

        return expenses.stream()
                .map(expense -> expense.getActualAmount())
                .reduce(Money.getZERO(), (total, amount) -> total.plus(amount));

    }

    private List<Expense> getExpenses(ExpenseFilter filter) {
        return expenseRepository.findAll(createPredicate(filter));
    }


    public Money calculateTotalBalance(Budget budget, Role role, Resource resource) {
        return allocationService.calculateTotalAllocated(budget, role, resource)
                .minus(calculateTotalExpenditure(budget, role, resource));
    }

    public BigDecimal calculateBalancePercentage(Budget budget, Role role, Resource resource) {
        Money balance = calculateTotalBalance(budget, role, resource);
        Money allocated = allocationService.calculateTotalAllocated(budget, role, resource);
        return balance.percentageOf(allocated, 2, RoundingMode.HALF_EVEN);
    }

    public Money calculateTotalRolesBalance(Budget budget, User user, Resource resource) {
        return roleService.findRoles(user).stream()
                .map(role -> calculateTotalBalance(budget, role, resource))
                .reduce(Money.getZERO(), (total, bal) -> total.plus(bal));
    }

    public Money calculateTotalMonthlyBalance(User currentUser) {
        return allocationService.calculateTotalMonthlyAllocated(currentUser)
                .minus(calculateTotalMonthlyExpenses(currentUser));
    }

    public Money calculateTotalBalance(Budget budget) {
        return calculateTotalBalance(budget, null, null);
    }


    public List<Expense> findExpenses(ExpenseFilter filter) {
        return expenseRepository.findAll(createPredicate(filter));
    }

    public Page<Expense> getExpenses(Optional<ExpenseFilter> filter, Pageable pageable) {
        if (filter.isPresent()) return expenseRepository.findAll(createPredicate(filter.get()), pageable);
        else return expenseRepository.findAll(pageable);
    }

    public Long countExpenses(Optional<ExpenseFilter> filter) {
        if (filter.isPresent()) return expenseRepository.count(createPredicate(filter.get()));
        else return expenseRepository.count();
    }

    private Predicate createPredicate(ExpenseFilter filter) {

        QExpense expense = QExpense.expense;
        BooleanBuilder query = new BooleanBuilder();

        if (filter.costCenter != null)
            query.and(expense.budget.costCenter.eq(filter.costCenter));

        if (filter.budget != null)
            query.and(expense.budget.eq(filter.budget));

        if (filter.role != null)
            query.and(expense.role.eq(filter.role));
        if (filter.resource != null)
            query.and(expense.resource.eq(filter.resource));
        if (filter.startTime != null)
            query.and(expense.payment.time.goe(filter.startTime));
        if (filter.endTime != null)
            query.and(expense.payment.time.loe(filter.endTime));
        if (filter.associatedUser != null)
            query.and(expense.associatedUser.eq(filter.associatedUser));
        if (filter.creator != null)
            query.and(expense.payment.creator.eq(filter.creator));
        if (filter.consumer != null)
            query.and(expense.consumer.eq(filter.consumer));
        if (filter.paymentMethod != null)
            query.and(expense.payment.method.eq(filter.paymentMethod));
        if (filter.description != null)
            query.and(expense.payment.description.containsIgnoreCase(filter.description));
        if (filter.associate != null)
            query.and(expense.payment.associate.containsIgnoreCase(filter.associate));
        if (filter.reference != null)
            query.and(expense.payment.reference.containsIgnoreCase(filter.reference));
        if (filter.retired != null) {
            if (filter.retired) query.and(expense.retirementEntries.isNotEmpty());
            else query.and(expense.retirementEntries.isEmpty());
        }
        if (filter.settled != null) {
            if (filter.settled) query.and(expense.settlements.isNotEmpty());
            else query.and(expense.settlements.isEmpty());
        }
        if (filter.reconciled != null)
            query.and(expense.reconciled.eq(filter.reconciled));


        return query;
    }

    @Transactional
    public Expense create(Expense expense) {
        return create(expense, expense.getPayment());
    }

    @Transactional
    public Expense create(Expense expense, Payment payment) {
        if (!payment.isPaid())
            throw new IllegalArgumentException("Payment passed is not marked as of type paid");

        validateExpense(expense.getBudget(),
                expense.getRole(),
                expense.getResource(),
                expense.getAssociatedUser(),
                payment.getTime());


        if (payment.getAmount().isZero() || payment.getAmount().isLessThan(Money.getZERO()))
            throw new IllegalArgumentException("Payment amount shall be greater than zero");

        //if the budget does not allow expenses if there is no enuf allocation
        if (!expense.getBudget().isAllowExpensesWithLessBalance()) {
            Money balance = calculateTotalBalance(
                    expense.getBudget(),
                    expense.getRole(),
                    expense.getResource()
            );

            if (payment.getAmount().isGreaterThan(balance))
                throw new IllegalArgumentException("Budget has insufficient balance to accommodate this expense. Balance is: " + balance);
        }


        expense.setPayment(payment);
        payment.setDescription(expense.toString());

        payment = paymentService.create(payment);
        return expenseRepository.save(expense);
    }

    @Transactional
    public void validateExpense(Budget budget,
                                Role role,
                                Resource resource,
                                User associatedUser,
                                LocalDateTime expenseTime) {
        if (!budgetService.isApproved(budget))
            throw new IllegalArgumentException("Expense can only be created on an approved budget");
//
//        if (budgetService.isArchived(budget))
//            throw new IllegalArgumentException("Expense can only be created on an active budget");

        if (expenseTime.toLocalDate().isBefore(budget.getStartDate()))
            throw new IllegalArgumentException("Expense can only be created after the budget start date :" + budget.getStartDate());

        if (!groupService.isMember(role, associatedUser))
            throw new IllegalArgumentException(associatedUser + " is not a member of " + role);

        if (!roleService.isResourceAllowed(role, resource))
            throw new IllegalArgumentException(resource + " is not allowed in " + role);

        if (!costCenterService.allowResource(budget.getCostCenter(), resource))
            throw new IllegalArgumentException(resource + " is not allowed in " + budget.getCostCenter());

        CostCenter costCenter = budget.getCostCenter();
        if (!costCenterService.allowRole(costCenter, role))
            throw new IllegalArgumentException(role + " is not allowed in " + costCenter);

    }

    public Money calculateTotalMonthlyExpenses(Role role) {
        LocalDateTime start = DateUtils.getStartOfThisMonth();
        LocalDateTime end = DateUtils.getEndOfThisMonth();

        return calculateTotalExpenditure(ExpenseFilter.builder().role(role).startTime(start).endTime(end).build());
    }

    public Money calculateTotalMonthlyExpenses(User user) {

        LocalDateTime start = DateUtils.getStartOfThisMonth();
        LocalDateTime end = DateUtils.getEndOfThisMonth();


        return calculateTotalExpenditure(user, start, end);
    }


    public Money calculateTotalPrevMonthExpenses(User user) {
        LocalDateTime start = DateUtils.getStartOfPrevMonth();
        LocalDateTime end = DateUtils.getEndOfPrevMonth();


        return calculateTotalExpenditure(user, start, end);
    }

    public Money calculateTotalMonthlyExpenses() {
        LocalDateTime start = DateUtils.getStartOfThisMonth();
        LocalDateTime end = DateUtils.getEndOfThisMonth();

        return calculateTotalExpenditure(null, start, end);
    }

    public Money calculateTotalExpenditure(User associatedUser, LocalDateTime start, LocalDateTime end) {
        return calculateTotalExpenditure(ExpenseFilter.builder().associatedUser(associatedUser).startTime(start).endTime(end).build());
    }

    public Money calculateTotalMonthlyExpenses(CostCenter costCenter) {
        LocalDateTime start = DateUtils.getStartOfThisMonth();
        LocalDateTime end = DateUtils.getEndOfThisMonth();

        return calculateTotalExpenditure(ExpenseFilter.builder().costCenter(costCenter).startTime(start).endTime(end).build());
    }

    public Optional<Expense> findExpense(Payment payment) {
        return expenseRepository.findOne(QExpense.expense.payment.eq(payment));
    }

    public Expense getExpense(Long expenseId) {
        return expenseRepository.findById(expenseId).orElseThrow(() -> new NoSuchElementException("There is no expense with id " + expenseId));
    }

    @Transactional
    public Expense cancelRetirements(Retirement request) {
//        if (request.getBudget().isArchived())
//            throw new IllegalStateException("Associated budget is locked :" + request.getBudget());

        if (request.getExpense().hasSettlements())
            throw new IllegalStateException("You can not cancel retirement once there are settlements associated with the expense :" + request.getExpense().getTotalSettlement());

        Expense expense = request.getExpense();
        expense.cancelRetirements(request.getEntries());
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense retire(Retirement request) {
        if (!trackerService.isApproved(request))
            throw new IllegalArgumentException("Retirement request is not approved");

//        if (request.getBudget().isArchived())
//            throw new IllegalStateException("Associated budget is locked :" + request.getBudget());

        if (request.getExpense().isReconciled())
            throw new IllegalStateException("Associated expense is already reconciled");

        if (request.getExpense().hasSettlements())
            throw new IllegalStateException("You can not submit more retirement once there are settlements associated with the expense :" + request.getExpense().getTotalSettlement());

        Expense expense = request.getExpense();
        expense.addRetirements(request.getEntries().stream().filter(RetirementEntry::isAccepted).collect(Collectors.toList()));

        expense = expenseRepository.save(expense);

        if (expense.getRetiredDifference().isZero() && autoReconcileCompleteRetirement)
            expense = markReconciled(expense);

        return expense;
    }

    @Transactional
    public Payment settle(Expense expense, Payment payment) {
        expense = getExpense(expense.getId());

//        if (expense.getBudget().isArchived())
//            throw new IllegalStateException("Associated budget is locked");

        if (expense.isReconciled())
            throw new IllegalStateException("Expense is already reconciled");

        if (expense.isCompletelySettled())
            throw new IllegalStateException("Expense is already completely settled");

        if (!payment.getAmount().isGreaterThanZero())
            throw new IllegalArgumentException("Payment amount shall be greater than zero");

        if (payment.getAmount().isGreaterThan(expense.getPendingSettlement()))
            throw new IllegalArgumentException("Payment amount:" + payment.getAmount() + " is greater than the pending amount to be settled " + expense.getPendingSettlement());

        payment.setDescription("Settlement for the retirement difference of " + expense.getRetiredDifference() + " on the expense : " + expense.toString());
        payment = paymentService.create(payment);
        expense.settle(payment);

        expenseRepository.save(expense);

        //mark this retirement as complete to simplify fetching of not complete or completely paid retirements
        if (expense.isCompletelySettled()) {
            markReconciled(expense);
        }

        return payment;
    }

    @Transactional
    public Expense markReconciled(Expense expense) {
        if (expense.isReconciled()) throw new IllegalArgumentException("The expense is already reconciled");

        if (expense.hasRetirements() && !expense.getRetiredDifference().isZero() && !expense.isCompletelySettled())
            throw new IllegalStateException("The expense requires settlement first before marked reconciled");

        expense.setReconciled(true);
        return expenseRepository.save(expense);
    }

    public long countOverThresholdPendingReconciliations(User associatedUser) {
        LocalDateTime cutOffTime = LocalDateTime.now().minus(Period.ofDays(pendingReconciliationMaximumDurationDays));
        return countExpenses(Optional.of(
                ExpenseFilter.builder()
                        .associatedUser(associatedUser)
                        .endTime(cutOffTime)
                        .reconciled(false)
                        .build())
        );
    }


    public List<Expense> findExpensesPendingReconciliation(String userId) {
        User user = userService.findUser(userId).orElseThrow(() -> new IllegalArgumentException("There is no user with login id" + userId));
        return findExpensesPendingReconciliation(user);
    }

    public List<Expense> findExpensesPendingReconciliation(User user) {
        return findExpenses(
                ExpenseService.ExpenseFilter
                        .builder()
                        .associatedUser(user)
                        .reconciled(false)
                        .settled(false)
                        .build()
        );
    }

    public String getStatus(Expense expense) {
        if (expense.isReconciled()) return "Reconciled";
        else if (expense.hasRetirements()) return "Retired";
        else return "Waiting Retirement";
    }

    public Money calculateTotalPendingReconciliation(User user) {
        return Money.sum(findExpensesPendingReconciliation(user), Expense::getPendingSettlement);
    }

    @Setter
    @Getter
    @Builder
    public static class ExpenseFilter {
        private CostCenter costCenter;
        private Budget budget;
        private Role role;
        private Resource resource;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private User associatedUser;
        private User creator;
        private Consumer consumer;
        private PaymentMethod paymentMethod;
        private String description;
        private String associate;
        private String reference;
        private Boolean retired;
        private Boolean settled;
        private Boolean reconciled;
    }
}
