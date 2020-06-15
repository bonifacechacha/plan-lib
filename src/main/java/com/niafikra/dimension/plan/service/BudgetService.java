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
import com.niafikra.dimension.core.util.HasLogger;
import com.niafikra.dimension.money.Money;
import com.niafikra.dimension.plan.domain.*;
import com.niafikra.dimension.plan.repository.BudgetRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/12/17 7:44 PM
 */
@Service
public class BudgetService implements InitializingBean, HasLogger {

    @Value("${dimension.budget.funds-exceed-cost}")
    private boolean fundsExceedCost;

    @Value("${dimension.budget.archiving-outdated-threshold-months}")
    private int archivingOutdatedThresholdMonthCount;

    private BudgetRepository budgetRepository;
    private AllocationService allocationService;
    private UserService userService;
    private CriteriaService criteriaService;
    private ApprovalTrackerService trackerService;
    private RoleService roleService;
    private CostCenterService costCenterService;

    public BudgetService(BudgetRepository budgetRepository,
                         AllocationService allocationService,
                         UserService userService,
                         CriteriaService criteriaService,
                         ApprovalTrackerService trackerService,
                         RoleService roleService,
                         CostCenterService costCenterService) {
        this.budgetRepository = budgetRepository;
        this.allocationService = allocationService;
        this.userService = userService;
        this.criteriaService = criteriaService;
        this.trackerService = trackerService;
        this.roleService = roleService;
        this.costCenterService = costCenterService;
    }

    public Page<Budget> findBudgets(BudgetFilter filter, Pageable pageable) {
        return budgetRepository.findAll(createPredicate(filter), pageable);
    }

    public List<Budget> findBudgets(BudgetFilter filter) {
        return budgetRepository.findAll(createPredicate(filter));
    }

    private Predicate createPredicate(BudgetFilter filter) {
        BooleanBuilder query = new BooleanBuilder();
        QBudget budget = QBudget.budget;

        if (filter.archived != null)
            query.and(budget.archived.eq(filter.archived));
        if (filter.approved != null)
            query.and(budget.approved.eq(filter.approved));
        if (filter.startTime != null)
            query.and(budget.timeCreated.goe(filter.startTime));
        if (filter.endTime != null)
            query.and(budget.timeCreated.loe(filter.endTime));
        if (!StringUtils.isEmpty(filter.titleFilter))
            query.and(budget.title.containsIgnoreCase(filter.titleFilter));
        if (filter.costCenter != null)
            query.and(budget.costCenter.eq(filter.costCenter));
        if (filter.role != null)
            query.and(budget.allocations.any().role.eq(filter.role));
        if (filter.creator != null)
            query.and(budget.creator.eq(filter.creator));
        if (filter.planner != null)
            query.and(budget.costCenter.planners.contains(filter.planner));

        return query;
    }

    public Long countBudgets(BudgetFilter filter) {
        return budgetRepository.count(createPredicate(filter));
    }

    public Set<Budget> findActiveAllocatedBudgets(User user) {
        List<Role> roles = roleService.findRoles(user);
        return roles.stream()
                .flatMap(role -> findActiveAllocatedBudgets(role).stream())
                .collect(Collectors.toSet());
    }

    public Set<Budget> findActiveAllocatedBudgets(Role role) {
        BooleanBuilder query = new BooleanBuilder();
        query.and(QBudget.budget.allocations.any().role.eq(role));
        query.and(QBudget.budget.approved.eq(true));

        List<Budget> budgets = budgetRepository.findAll(query);
        return budgets.stream()
                .filter(budget -> budget.isActive())
                .collect(Collectors.toSet());
    }

    public Set<Budget> findActiveAssociatedBudgets(Role role) {
        BooleanBuilder query = new BooleanBuilder();
        query.and(QBudget.budget.costCenter.roles.contains(role));
        query.and(QBudget.budget.approved.eq(true));

        List<Budget> budgets = budgetRepository.findAll(query);
        return budgets.stream()
                .filter(budget -> budget.isActive())
                .collect(Collectors.toSet());
    }


    public Set<Budget> findActiveAssociatedBudgets(User user) {
        List<Role> roles = roleService.findRoles(user);
        return roles.stream()
                .flatMap(role -> findActiveAssociatedBudgets(role).stream())
                .collect(Collectors.toSet());
    }

    public Money calculateTotalAllocation(Budget budget) {
        return allocationService.calculateTotalAllocated(budget, (Role) null, null);
    }


    public Money calculateTotalProposed(Budget budget) {
        return allocationService.calculateTotalProposed(budget, null, null);
    }

    @Transactional
    public Budget save(Budget budget) {
        validateBudgetFunds(budget);
        return budgetRepository.save(budget);
    }

    public void validateBudgetFunds(Budget budget) {
        if (budget.getFund().isGreaterThan(budget.getCost()) && !fundsExceedCost)
            throw new IllegalStateException(String.format("Allocated funds:%s shall not exceed associated cost:%s", budget.getFund(), budget.getCost()));
    }

    @EventListener
    public void onFetchApprovable(FetchApprovableEvent event) {
        Tracker tracker = event.getTracker();
        if (tracker.getType().equals(Budget.APPROVAL_TYPE)) {
            event.setApprovable(getBudget(tracker));
        }
    }


    @EventListener
    @Transactional
    public void onBudgetApprovalComplete(ApprovalCompletionEvent<Budget> event) {
        Budget budget = event.getApprovable();
        budget = getBudget(budget.getId());
        budget.setApproved(event.getTracker().isApproved());
        save(budget);

        if (budget.isApproved()) {
            allocationService.allocate(budget);
        }
    }

    @EventListener
    @Transactional
    public void onBudgetApprovalCancelled(ApprovalCancellationEvent<Budget> event) {
        Budget budget = event.getApprovable();

        if (budget.isApproved()) {
            throw new IllegalStateException("Once budget is approved, the approval process can not be cancelled");
        } else {
            budget.setApproved(null);
            budgetRepository.save(budget);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Budget create(Budget budget) {
        Budget existingBudget = getBudget(budget.getTitle());
        if (existingBudget != null)
            throw new IllegalArgumentException("There is another budget with same title:" + budget.getTitle());

        User user = SecurityUtils.getCurrentUser(userService);

        //if current user is not one of the allowed planners
        if (!costCenterService.canPlan(budget.getCostCenter(), user))
            throw new IllegalArgumentException(user + " is not an authorised planner for the cost center " + budget.getCostCenter());


        budget.setCreator(user);
        budget = save(budget);
        return budget;
    }

    @Transactional
    public Tracker submitApproval(Budget budget) {
        User user = SecurityUtils.getCurrentUser(userService);
        //if current user is not one of the allowed planners
        if (!costCenterService.canPlan(budget.getCostCenter(), user))
            throw new IllegalArgumentException(user + " is not an authorised planner for the cost center " + budget.getCostCenter());

        if (budget.isArchived())
            throw new IllegalArgumentException("A locked budget can not be submitted for approval");


        return criteriaService.registerApproval(
                budget,
                Budget.APPROVAL_CRITERIA,
                SecurityUtils.getCurrentUser(userService),
                Collections.emptyMap());
    }

    @Transactional
    public Budget update(Budget budget) {
        if (budget.isArchived())
            throw new IllegalArgumentException("A locked budget can not be updated");

        //TODO PREVENT UPDATE OF COST CENTER AND BUDGET PERIOD (PERIOD CHANGE SHOULD HAVE APPROVAL PROCESS)
        return budgetRepository.save(budget);
    }

    private Budget getBudget(String title) {
        return budgetRepository.findByTitle(title);
    }


    public Budget getBudget(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("There is no role with id :" + budgetId));
    }


    @Transactional
    public void delete(Budget budget) {
        User user = SecurityUtils.getCurrentUser(userService);
        //if current user is not one of the allowed planners
        if (!costCenterService.canPlan(budget.getCostCenter(), user))
            throw new IllegalArgumentException(user + " is not an authorised planner for the cost center " + budget.getCostCenter());

        if (budget.isArchived())
            throw new IllegalArgumentException("A locked budget can not be deleted");

        if (budget.isApproved())
            throw new IllegalArgumentException("Once budget is approved it can not be deleted");

        if (isApprovalSubmitted(budget))
            trackerService.cancelApproval(budget);

        budgetRepository.delete(budget);
    }


    public boolean isArchived(Budget budget) {
        return budget.isArchived();
    }


    public boolean isApproved(Budget budget) {
        return trackerService.isApproved(budget);
    }


    public boolean isApprovalSubmitted(Budget budget) {
        return trackerService.isRegistered(budget);
    }


    public boolean canDelete(Budget budget) {
        return !isApproved(budget) && !isArchived(budget);
    }


    public boolean canArchive(Budget budget) {
        return !isArchived(budget);
    }


    public boolean canRetrieve(Budget budget) {
        return isArchived(budget);
    }


    public boolean canSubmit(Budget budget) {
        return !isApprovalSubmitted(budget);
    }

    public Budget getBudget(Tracker tracker) {
        if (!tracker.getType().equals(Budget.APPROVAL_TYPE))
            throw new IllegalArgumentException("Tracker type" + tracker.getType() + " is not of type :" + Budget.APPROVAL_TYPE);

        return getBudget(Budget.getId(tracker.getReference()));
    }

    public String getStatus(Budget budget) {

        String status = "";
        if (!isApprovalSubmitted(budget))
            status += "Draft";
        else {
            status = budget.isApproved() ? "Approved" : (budget.isDeclined() ? "Declined" : "Waiting approval");
        }

        if (budget.isArchived()) status += " , Archived";
        if (!budget.isUpToDate()) status += " , Out of date";

        return status;
    }

    @Transactional
    public Budget adjustEndDate(Budget budget, LocalDate proposedEndDate) {
        if (isArchived(budget))
            retrieve(budget);

        budget.setEndDate(proposedEndDate);
        return budgetRepository.save(budget);
    }

    @Transactional
    public Budget retrieve(Budget budget) {
        budget.setArchived(false);
        return budgetRepository.save(budget);
    }

    @Transactional
    public Budget archive(Budget budget) {
        budget.setArchived(true);
        return budgetRepository.save(budget);
    }

    @Transactional
    @Scheduled(cron = "0 1 0 * * *")
    public void archiveOutdatedBudgets() {
        List<Budget> outdatedBudgets = budgetRepository.findAll(
                QBudget.budget.archived.eq(false)
                        .and(QBudget.budget.endDate.before(LocalDate.now().minusMonths(archivingOutdatedThresholdMonthCount)))
        );

        outdatedBudgets.forEach(budget -> archive(budget));
    }


    @Override
    @Transactional()
    public void afterPropertiesSet() {
        //Archiving outdated budgets
        getLogger().info("Archiving outdated budget...");
        archiveOutdatedBudgets();
    }

    public Budget adjustFund(Budget budget, Money amount, User user) {
        if (!costCenterService.canPlan(budget.getCostCenter(), user))
            throw new IllegalArgumentException(user + " is not an authorised planner for the cost center " + budget.getCostCenter());

        if (budget.isArchived())
            throw new IllegalArgumentException("You can not adjust fund on an archived budget");

        if (!budget.isApproved())
            throw new IllegalArgumentException("You can only adjust fund on approved budget, if it still a draft you can direct edit it");

        budget.setFund(budget.getFund().plus(amount));
        return budgetRepository.save(budget);
    }

    @Getter
    @Setter
    @Builder
    public static class BudgetFilter {
        private CostCenter costCenter;
        private Role role;
        private Resource resource;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private User planner;
        private User creator;
        private String titleFilter;
        private Boolean approved;
        private Boolean archived;

        public void setPeriodRange(LocalDateTime start, LocalDateTime end) {
            this.startTime = start;
            this.endTime = end;
        }

    }
}
