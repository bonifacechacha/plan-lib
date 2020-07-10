package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.approval.domain.Tracker;
import com.niafikra.dimension.approval.service.ApprovalTrackerService;
import com.niafikra.dimension.core.security.SecurityUtils;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.core.security.service.UserService;
import com.niafikra.dimension.core.util.DateUtils;
import com.niafikra.dimension.group.service.GroupService;
import com.niafikra.dimension.money.Money;
import com.niafikra.dimension.plan.domain.*;
import com.niafikra.dimension.plan.repository.AllocationRepository;
import com.querydsl.core.BooleanBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/15/17 7:57 PM
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class AllocationService {

    private AllocationRepository allocationRepository;
    private UserService userService;
    private ApprovalTrackerService trackerService;
    private ResourceService resourceService;
    private RoleService roleService;
    private CostCenterService costCenterService;
    private GroupService groupService;
    private BudgetService budgetService;

    @Value("${dimension.budget.auto-increase-fund-during-adjustment}")
    private boolean autoIncreaseFundDuringAdjustment;

    public AllocationService(AllocationRepository allocationRepository,
                             UserService userService,
                             ApprovalTrackerService trackerService,
                             ResourceService resourceService,
                             RoleService roleService,
                             @Lazy BudgetService budgetService,
                             CostCenterService costCenterService,
                             GroupService groupService) {
        this.allocationRepository = allocationRepository;
        this.userService = userService;
        this.trackerService = trackerService;
        this.resourceService = resourceService;
        this.budgetService = budgetService;
        this.roleService = roleService;
        this.costCenterService = costCenterService;
        this.groupService = groupService;
    }


    public List<Allocation> getAllocations(
            Budget budget,
            Role role,
            Resource resource) {
        return getAllocations(null, budget, role, resource, null, null);
    }

    public List<Allocation> getAllocations(
            CostCenter costCenter,
            Budget budget,
            Role role,
            Resource resource,
            LocalDate startDate,
            LocalDate endEnd) {

        BooleanBuilder query = createPredicate(costCenter,
                budget, role, resource, startDate, endEnd);
        return allocationRepository.findAll(query);
    }


    public BooleanBuilder createPredicate(
            CostCenter costCenter,
            Budget budget,
            Role role,
            Resource resource,
            LocalDate startDate,
            LocalDate endDate) {


        BooleanBuilder query = new BooleanBuilder();
        if (costCenter != null)
            query.and(QAllocation.allocation.budget.costCenter.eq(costCenter));
        if (budget != null)
            query.and(QAllocation.allocation.budget.eq(budget));
        if (role != null) query.and(QAllocation.allocation.role.eq(role));
        if (resource != null)
            query.and(QAllocation.allocation.resource.eq(resource));


        //i did draw this whole logic on paper and thought of it thorough
        // the logic include those budgets whose allocations intersect and those that merge the range
        //if you want to change be very careful because i think this is ok
        BooleanBuilder rangeQuery = new BooleanBuilder();
        if (startDate != null)
            rangeQuery.or(new BooleanBuilder()
                    .and(QAllocation.allocation.budget.endDate.goe(startDate))
                    .and(QAllocation.allocation.budget.startDate.loe(startDate)));

        if (endDate != null)
            rangeQuery.or(new BooleanBuilder()
                    .and(QAllocation.allocation.budget.endDate.goe(endDate))
                    .and(QAllocation.allocation.budget.startDate.loe(endDate)));

        if (startDate != null && endDate != null) {
            rangeQuery.or(new BooleanBuilder()
                    .and(QAllocation.allocation.budget.startDate.goe(startDate))
                    .and(QAllocation.allocation.budget.endDate.loe(endDate)));
        }

        query.and(rangeQuery);

        return query;
    }

    public Money calculateTotalProposed(Budget budget, Role role, Resource resource) {
        return getAllocations(budget, role, resource)
                .stream()
                .map(allocation -> allocation.getProposedAmount())
                .reduce(Money.getZERO(), (total, proposed) -> total.plus(proposed));
    }

    public Money calculateTotalAllocated(
            CostCenter costCenter,
            Budget budget,
            Role role,
            Resource resource,
            LocalDate startDate,
            LocalDate endDate) {
        return getAllocations(costCenter, budget, role, resource, startDate, endDate)
                .stream()
                .map(allocation -> allocation.getAllocatedAmount())
                .reduce(Money.getZERO(), (total, allocated) -> total.plus(allocated));
    }


    public Money calculateTotalAllocated(Budget budget, Role role, Resource resource) {
        return calculateTotalAllocated(null, budget, role, resource, null, null);
    }

    public Money calculateTotalRolesAllocated(Budget budget, User user, Resource resource) {
        return roleService.findRoles(user).stream()
                .map(role -> calculateTotalAllocated(budget, role, resource))
                .reduce(Money.getZERO(), (total, allocated) -> total.plus(allocated));
    }

    public Money calculateTotalAllocated(Role role, LocalDate start, LocalDate end) {
        return calculateTotalAllocated(null, null, role, null, start, end);
    }

    public Money calculateTotalAllocated(CostCenter costCenter, LocalDate start, LocalDate end) {
        return calculateTotalAllocated(costCenter, null, null, null, start, end);
    }

    public Money calculateTotalAllocated(User currentUser, LocalDate start, LocalDate end) {
        return roleService.findRoles(currentUser)
                .stream()
                .map(role -> calculateTotalAllocated(role, start, end))
                .reduce(Money.getZERO(), (totalAllocated, allocated) -> totalAllocated.plus(allocated));
    }

    public Money calculateTotalMonthlyAllocated(CostCenter costCenter) {
        LocalDate start = DateUtils.getStartOfThisMonth().toLocalDate();
        LocalDate end = DateUtils.getStartOfNextMonth().toLocalDate().minusDays(1);
        return calculateTotalAllocated(costCenter, start, end);
    }

    public Money calculateTotalMonthlyAllocated(Role role) {
        LocalDate start = DateUtils.getStartOfThisMonth().toLocalDate();
        LocalDate end = DateUtils.getStartOfNextMonth().toLocalDate().minusDays(1);
        return calculateTotalAllocated(role, start, end);
    }

    public Money calculateTotalMonthlyAllocated(User currentUser) {
        LocalDate start = DateUtils.getStartOfThisMonth().toLocalDate();
        LocalDate end = DateUtils.getEndOfThisMonth().toLocalDate();
        return calculateTotalAllocated(currentUser, start, end);
    }


    public Money calculateTotalPrevMonthAllocated(User user) {
        LocalDate start = DateUtils.getStartOfPrevMonth().toLocalDate();
        LocalDate end = DateUtils.getEndOfPrevMonth().toLocalDate();
        return calculateTotalAllocated(user, start, end);
    }


    public Money calculateTotalMonthlyAllocated() {
        LocalDate start = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        return calculateTotalAllocated(null, null, null, null, start, end);
    }


    public Set<Role> getAllocatedRoles(Budget budget, Resource resource) {
        return getAllocations(budget, null, resource)
                .stream()
                .map(allocation -> allocation.getRole())
                .collect(Collectors.toSet());
    }


    public Set<Role> getAllocatedRoles(Budget budget, Resource resource, User associatedUser) {
        return getAllocatedRoles(budget, resource)
                .stream()
                .filter(role -> groupService.isMember(role, associatedUser))
                .collect(Collectors.toSet());
    }

    public Set<Resource> getAllocatedResources(Budget budget, Role role) {
        return getAllocations(budget, role, null)
                .stream()
                .map(allocation -> allocation.getResource())
                .collect(Collectors.toSet());
    }


    public Set<Resource> getNotAllocatedResources(@NotNull Budget budget, @NotNull Role role) {
        Set<Resource> allocated = getAllocatedResources(budget, role);
        return resourceService.getResources(role, budget.getCostCenter())
                .stream()
                .filter(resource -> !allocated.contains(resource)).collect(Collectors.toSet());

    }


    @Transactional
    public Allocation propose(Budget budget, Role role, Resource resource, Money proposedAmount, String description, String allocationChangeReason) {
        if (budget.isApproved())
            throw new IllegalArgumentException("Budget must not be approved yet to allocate new resource");


        if (budget.isArchived())
            throw new IllegalArgumentException("You can not propose on a locked budget");


        //the amount proposed should be greater than zero
        if (proposedAmount.isZero() || proposedAmount.isLessThan(Money.getZERO()))
            throw new IllegalArgumentException("Amount proposed should be greater than zero");

        User creator = SecurityUtils.getCurrentUser(userService);
        ensureUserCanPropose(budget, role, creator);

        Allocation allocation;
        if (isAllocated(budget, role, resource)) {
            allocation = getAllocation(budget, role, resource);

        } else {
            allocation = new Allocation(budget, role, resource);
            allocation.setCreator(creator);
        }

        //check if there is amount change before changing it in the allocation
        //if there no change in the amount currently proposed then do not record amount change
        boolean amountChanged = !allocation.getProposedAmount().equals(proposedAmount);

        allocation.setProposedAmount(proposedAmount);
        allocation.setDescription(description);

        allocation = allocationRepository.save(allocation);

        //ensure proposed amount does not exceed allocated funds
        Money allocated = calculateTotalProposed(budget, null, null);
        if (allocated.isGreaterThan(budget.getFund()))
            throw new IllegalStateException("Total allocations are greater than allocated funds for the budget");

        if (amountChanged)
            recordAllocationChange(allocation, creator, allocationChangeReason);
        return allocation;
    }

    @Transactional
    public Allocation adjustAllocation(Budget budget,
                                       Role role,
                                       Resource resource,
                                       Money amount,
                                       String description,
                                       String allocationChangeReason,
                                       User creator) {
        if (!budget.isApproved())
            throw new IllegalArgumentException("Budget must be approved yet to update allocation on resource");


        if (budget.isArchived())
            throw new IllegalArgumentException("You can not propose on a locked budget");


        //the amount proposed should be greater than zero
        if (amount.isZero() || amount.isLessThan(Money.getZERO()))
            throw new IllegalArgumentException("Amount to adjust should be greater than zero");

        ensureUserCanAdjustAllocation(budget, role, creator);

        Allocation allocation;
        if (isAllocated(budget, role, resource)) {
            allocation = getAllocation(budget, role, resource);

        } else {
            allocation = new Allocation(budget, role, resource);
            allocation.setProposedAmount(new Money());
            allocation.setAllocatedAmount(new Money());
            allocation.setCreator(creator);
        }

        //check if there is amount change before changing it in the allocation
        //if there no change in the amount currently proposed then do not record amount change
        boolean amountChanged = !allocation.getProposedAmount().equals(amount);

        allocation.setAllocatedAmount(amount.plus(allocation.getAllocatedAmount()));

        if (StringUtils.isEmpty(allocation.getDescription())) {
            allocation.setDescription(description);
        }

        allocation = allocationRepository.save(allocation);

        //ensure proposed amount does not exceed allocated funds
        Money allocated = calculateTotalAllocated(budget, null, null);
        if (allocated.isGreaterThan(budget.getFund())) {

            if (autoIncreaseFundDuringAdjustment) {
                budgetService.adjustFund(budget, allocated.minus(budget.getFund()), creator);
            } else
                throw new IllegalStateException("Total allocations " + allocated + " are greater than allocated funds " + budget.getFund() + " for the budget");
        }

        if (amountChanged)
            recordAllocationChange(allocation, creator, allocationChangeReason);
        return allocation;
    }

    private Allocation recordAllocationChange(Allocation allocation, User creator, String allocationChangeReason) {
        Money amountChange;
        if (allocation.getBudget().isApproved())
            amountChange = allocation.getAllocatedAmount();
        else amountChange = allocation.getProposedAmount();

        allocation.getChanges().add(new AllocationChange(creator, amountChange, allocationChangeReason));
        return allocationRepository.save(allocation);
    }

    private void ensureUserCanPropose(Budget budget, Role role, User user) {
        if (trackerService.isRegistered(budget)) {
            Tracker tracker = trackerService.getTracker(budget);
            if (!trackerService.canApprove(tracker, user))
                throw new IllegalArgumentException("Approval process for the budget has already started, You are not allowed to change allocations");
        } else {
            if (!costCenterService.canPlan(budget.getCostCenter(), user))
                throw new IllegalArgumentException(String.format("%s is not allowed to plan budget for %s", user, budget.getCostCenter()));

            if (!groupService.isMember(role, user))
                throw new IllegalArgumentException(String.format("%s is not a member of role %s", user, role));
        }
    }

    private void ensureUserCanAdjustAllocation(Budget budget, Role role, User user) {
        if (!costCenterService.canPlan(budget.getCostCenter(), user))
            throw new IllegalArgumentException(String.format("%s is not allowed to plan budget for %s", user, budget.getCostCenter()));

        if (!groupService.isMember(role, user))
            throw new IllegalArgumentException(String.format("%s is not a member of role %s", user, role));

    }

    public Allocation getAllocation(Budget budget, Role role, Resource resource) {
        BooleanBuilder query = createPredicate(null, budget, role, resource, null, null);
        return allocationRepository.findOne(query)
                .orElseThrow(() -> new IllegalArgumentException("There is no allocation for budget :" + budget + " role :" + role + " and resource :" + resource));
    }

    public Optional<Allocation> findAllocation(Budget budget, Role role, Resource resource) {
        BooleanBuilder query = createPredicate(null, budget, role, resource, null, null);
        return allocationRepository.findOne(query);
    }

    public boolean isAllocated(Budget budget, Role role, Resource resource) {
        return findAllocation(budget, role, resource).isPresent();
    }

    @Transactional
    public void deleteAllocations(Budget budget, Role role, Resource resource) {
        List<Allocation> allocations = getAllocations(budget, role, resource);
        if (allocations != null)
            delete(allocations);
    }


    @Transactional
    public void delete(Allocation allocation) {
        Budget budget = allocation.getBudget();

        if (budget.isApproved())
            throw new IllegalArgumentException("You can not delete an already approved budget");

        if (budget.isArchived())
            throw new IllegalArgumentException("You can not delete a locked budget");

        User user = SecurityUtils.getCurrentUser(userService);
        Role role = allocation.getRole();
        ensureUserCanPropose(budget, role, user);

        allocationRepository.delete(allocation);
    }


    @Transactional
    public void delete(Collection<Allocation> allocations) {
        allocations.forEach(allocation -> delete(allocation));
    }

    @Transactional
    public void allocate(Budget budget) {
        if (!budget.isApproved())
            throw new IllegalArgumentException("You can not allocate resources on a not approved budget");

        List<Allocation> allocations = getAllocations(budget, null, null);
        allocations.forEach(allocation -> {
            allocation.setAllocatedAmount(allocation.getProposedAmount());
            allocationRepository.save(allocation);
        });

        //ensure allocated amount does not exceed allocated funds
        Money allocated = calculateTotalAllocated(budget, null, null);
        if (allocated.isGreaterThan(budget.getFund()))
            throw new IllegalStateException("Total allocations are greater than allocated funds for the budget");
    }

    public Allocation getAllocation(Long allocationId) {
        return allocationRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("There is no allocation with id " + allocationId));
    }

    @Transactional
    public void importAllocations(Budget budget, InputStream importStream) throws IOException {

        Workbook workbook = new HSSFWorkbook(importStream);

        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        //pass info headers
        rowIterator.next();
        rowIterator.next();

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Long roleId = new Double(row.getCell(0).getNumericCellValue()).longValue();
            Role role = roleService.getRole(roleId);
            Long resourceId = new Double(row.getCell(1).getNumericCellValue()).longValue();
            Resource resource = resourceService.getResource(resourceId);
            BigDecimal proposedValue = new BigDecimal(row.getCell(4).getNumericCellValue());
            Money proposedAmount = new Money(proposedValue);

            Cell descriptionCell = row.getCell(5);
            String description = descriptionCell != null ? descriptionCell.getStringCellValue() : "";

            Cell reasonCell = row.getCell(6);
            String reason = reasonCell != null ? reasonCell.getStringCellValue() : "";

            if (proposedAmount.isZero()) {
                deleteAllocations(budget, role, resource);
            } else {
                propose(budget,
                        role,
                        resource,
                        proposedAmount,
                        description,
                        reason
                );
            }
        }
    }

    public List<AllocationProposal> prepareAllocationProposals(Budget budget, User user) {

        if (trackerService.canApprove(budget, user)) {
            //if user is currently required to approve the associated budget then he/she can change or propose allocations
            //from any role/resource combination allowed
            return costCenterService.getRoles(budget.getCostCenter())
                    .stream()
                    .flatMap(role -> resourceService.getResources(role, budget.getCostCenter()).stream().map(resource -> createAllocationProposal(budget, role, resource)))
                    .collect(Collectors.toList());
        } else {
            return roleService
                    .findRoles(user)
                    .stream()
                    .filter(role -> costCenterService.allowRole(budget.getCostCenter(), role))
                    .flatMap(role -> resourceService.getResources(role, budget.getCostCenter()).stream().map(resource -> createAllocationProposal(budget, role, resource)))
                    .collect(Collectors.toList());
        }
    }

    private AllocationProposal createAllocationProposal(Budget budget, Role role, Resource resource) {
        Optional<Allocation> allocation = findAllocation(budget, role, resource);
        if (allocation.isPresent())
            return new AllocationProposal(allocation.get());
        else return new AllocationProposal(budget, role, resource, Money.getZERO());
    }

    @Transactional
    public void propose(List<AllocationProposal> proposals) {
        proposals.forEach(proposal -> {
            propose(proposal);
        });
    }

    @Transactional
    public void propose(AllocationProposal proposal) {
        if (proposal.getProposedAmount().isZero()) {
            deleteAllocations(
                    proposal.getBudget(),
                    proposal.getRole(),
                    proposal.getResource()
            );
        } else {
            propose(
                    proposal.getBudget(),
                    proposal.getRole(),
                    proposal.getResource(),
                    proposal.getProposedAmount(),
                    proposal.getDescription(),
                    proposal.getReason()
            );

        }
    }

    public Set<Role> findRoles(Budget budget, User user, Resource resource) {
        if (budget.isAllowRequestWithLessBalance())
            //if allows requisition even when balance is less then use all associated resources with current user for the cost center
            return roleService.findAllowedRoles(budget.getCostCenter(), resource, user);
        else
            //if only can request for resources with balance then only show the ones with allocations
            return getAllocatedRoles(budget, resource, user);

    }

    public Set<Resource> findResources(Budget budget, User currentUser) {
        if (budget.isAllowRequestWithLessBalance())
            //if allows requisition even when balance is less then use all associated resources with current user for the cost center
            return resourceService.getResources(currentUser, budget.getCostCenter());
            //if only can request for resources with balance then only show the ones with allocations
        else return getAllocatedResources(budget, currentUser);
    }

    public Set<Resource> getAllocatedResources(Budget budget, User associatedUser) {
        Set<Resource> resources = new HashSet<>();

        List<Role> roles = roleService.findRoles(associatedUser);

        roles.forEach(role -> {
            Set<Resource> allocated = getAllocatedResources(budget, role);
            resources.addAll(allocated);
        });

        return resources;
    }

    @Transactional
    public List<AllocationChange> getChanges(Allocation allocation) {
        allocation = getAllocation(allocation.getId());

        Hibernate.initialize(allocation.getChanges());
        return allocation.getChanges()
                .stream()
                .sorted(Comparator.comparing(AllocationChange::getTimeCreated).reversed())
                .collect(Collectors.toList());
    }

}
