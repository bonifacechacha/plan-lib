package com.niafikra.dimension.plan.controller;

import com.niafikra.dimension.plan.domain.Payment;
import com.niafikra.dimension.plan.domain.Requisition;
import com.niafikra.dimension.plan.service.PaymentService;
import com.niafikra.dimension.plan.service.RequisitionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 10/3/17 1:02 PM
 */
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private PaymentService paymentService;
    private RequisitionService requisitionService;

    public PaymentController(PaymentService paymentService, RequisitionService requisitionService) {
        this.paymentService = paymentService;
        this.requisitionService = requisitionService;
    }

    @GetMapping("/{id}")
    public String show(@PathVariable("id") Long id, Model model) {
        Payment payment = paymentService.getPayment(id);
        model.addAttribute("payment", payment);
        Optional<Requisition> requisition = requisitionService.findRequisition(payment);
        if (requisition.isPresent()) model.addAttribute("requisition", requisition.get());

        return "plan/payment/show";
    }

    @GetMapping("/print/{id}")
    public String print(@PathVariable("id") Long id, Model model) {
        Payment payment = paymentService.getPayment(id);
        model.addAttribute("payment", payment);
        model.addAttribute("print", true);
        Optional<Requisition> requisition = requisitionService.findRequisition(payment);
        if (requisition.isPresent()) model.addAttribute("requisition", requisition.get());
        return "plan/payment/show";
    }
}
