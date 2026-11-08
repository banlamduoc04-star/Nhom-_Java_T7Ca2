package com.oism.application.service;

import com.oism.domain.dto.DashboardSummaryDTO;
import com.oism.domain.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final ReportRepository reportRepository;

    public ReportingService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public DashboardSummaryDTO getDashboardSummary() {
        BigDecimal totalRevenue = reportRepository.calculateTotalRevenue();
        BigDecimal grossProfit = reportRepository.calculateGrossProfit();
        long lowStockCount = reportRepository.countLowStockItems();

        List<DashboardSummaryDTO.RevenueProfitPoint> chartData = List.of(
            new DashboardSummaryDTO.RevenueProfitPoint("10/10", new BigDecimal("12000000"), new BigDecimal("4500000")),
            new DashboardSummaryDTO.RevenueProfitPoint("11/10", new BigDecimal("19000000"), new BigDecimal("7200000")),
            new DashboardSummaryDTO.RevenueProfitPoint("12/10", new BigDecimal("15000000"), new BigDecimal("5100000")),
            new DashboardSummaryDTO.RevenueProfitPoint("13/10", new BigDecimal("22000000"), new BigDecimal("8900000"))
        );

        List<DashboardSummaryDTO.LowStockItemDTO> lowStockItems = List.of(
            new DashboardSummaryDTO.LowStockItemDTO("1", "POLO-BLK-M", "Áo Polo Nam Classic", 4, 10),
            new DashboardSummaryDTO.LowStockItemDTO("2", "JEAN-BLU-31", "Quần Jean Slimfit Blue", 2, 10)
        );

        return new DashboardSummaryDTO(
            totalRevenue, 
            grossProfit, 
            142L, 
            lowStockCount, 
            chartData, 
            lowStockItems
        );
    }
}
