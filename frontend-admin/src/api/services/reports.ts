import { api, data } from '../client'
import type { DailyReport, OrderChannel, ProfitReportView, SlowMovingItem, TopSellingItem, ValuationSummary } from '../types'

export const reportApi = {
  profit: (params: { from?: string; to?: string; branchId?: string; channel?: OrderChannel; skuId?: string }) =>
    data(api.get<ProfitReportView>('/reports/profit', { params })),
  exportCsv: (params: { from?: string; to?: string; branchId?: string; channel?: OrderChannel; skuId?: string }) =>
    api.get('/reports/profit/export', { params, responseType: 'blob' }),
}

export const stockReportApi = {
  valuation: (branchId?: string) =>
    data(api.get<ValuationSummary>('/reports/inventory-value', { params: { branchId } })),
  topSelling: (days = 30, limit = 10) =>
    data(api.get<TopSellingItem[]>('/reports/top-selling', { params: { days, limit } })),
  slowMoving: (days = 30) =>
    data(api.get<SlowMovingItem[]>('/reports/slow-moving', { params: { days } })),
  dailyReports: () => data(api.get<DailyReport[]>('/reports/daily')),
  triggerDaily: (date?: string) => data(api.post<DailyReport[]>('/reports/daily/run', null, { params: { date } })),
}
