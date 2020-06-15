package com.niafikra.dimension.plan.controller;

import com.niafikra.dimension.plan.domain.AllocationProposal;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public class XlsAllocationsImportView extends AbstractXlsView {

    public static final int HEADERS_INDEX = 1;
    public static final int TOTAL_INDEX = 0;
    public static final int ALLOCATIONS_START_INDEX = 2;

    @Override
    protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<AllocationProposal> proposals = (List<AllocationProposal>) model.get("proposals");

        response.setHeader("Content-Disposition", "attachment; filename=\"allocations.xls\"");

        Sheet sheet = workbook.createSheet("Allocation");
        sheet.setDefaultColumnWidth(35);


        int counter = 0;
        //create headers

        String proposedAmountCol = CellReference.convertNumToColString(4);
        String totalFormular = "SUM(" + proposedAmountCol + (ALLOCATIONS_START_INDEX + 1) + ":" + proposedAmountCol + (proposals.size() + ALLOCATIONS_START_INDEX) + ")";
        Row totalRow = sheet.createRow(counter++);
        totalRow.createCell(0).setCellValue("Total");
        totalRow.createCell(1, Cell.CELL_TYPE_FORMULA).setCellFormula(totalFormular);


        Row header = sheet.createRow(counter++);
        header.createCell(0).setCellValue("RoleId");
        header.createCell(1).setCellValue("ResourceId");
        header.createCell(2).setCellValue("Role");
        header.createCell(3).setCellValue("Resource");
        header.createCell(4).setCellValue("ProposedAmount");
        header.createCell(5).setCellValue("Description");
        header.createCell(6).setCellValue("Reason");

        for (AllocationProposal proposal : proposals) {
            Row row = sheet.createRow(counter++);
            row.createCell(0).setCellValue(proposal.getRole().getId());
            row.createCell(1).setCellValue(proposal.getResource().getId());
            row.createCell(2).setCellValue(proposal.getRole().getName());
            row.createCell(3).setCellValue(proposal.getResource().getName());
            row.createCell(4).setCellValue(proposal.getProposedAmount().toBaseCurrency().getAmount().doubleValue());
            row.createCell(5).setCellValue(proposal.getDescription());
            row.createCell(6).setCellValue(proposal.getReason());
        }

    }
}
