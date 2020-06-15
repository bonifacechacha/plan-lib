package com.niafikra.dimension.plan.controller;

import com.google.common.collect.ImmutableMap;
import com.niafikra.dimension.core.security.SecurityUtils;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.core.security.service.UserService;
import com.niafikra.dimension.plan.domain.Budget;
import com.niafikra.dimension.plan.service.AllocationService;
import com.niafikra.dimension.plan.service.BudgetService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/budget")
public class BudgetController {

    private AllocationService allocationService;
    private BudgetService budgetService;
    private UserService userService;

    public BudgetController(AllocationService allocationService, BudgetService budgetService, UserService userService) {
        this.allocationService = allocationService;
        this.budgetService = budgetService;
        this.userService = userService;
    }

    @GetMapping("/{id}/allocation/format")
    public ModelAndView downloadAllocationImportFormat(@PathVariable("id") Long id) {
        Budget budget = budgetService.getBudget(id);
        User currentUser = SecurityUtils.getCurrentUser(userService);

        return new ModelAndView(new XlsAllocationsImportView(), ImmutableMap.of("proposals", allocationService.prepareAllocationProposals(budget, currentUser)));
    }

}
