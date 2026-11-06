package com.oism.application.service;
import com.oism.domain.dto.DashboardSummaryDTO;
import com.oism.domain.repository.ReportRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportingService {

    private final ReportRepository reportRepository;

    public ReportingService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public DashboardSummaryDTO getDashboardSummary() {
        var totalRevenue = reportRepository.calculateTotalRevenue();
        var grossProfit = reportRepository.calculateGrossProfit();
        var lowStockCount = reportRepository.countLowStockItems();

        return new DashboardSummaryDTO(totalRevenue, grossProfit, lowStockCount);
    }
}