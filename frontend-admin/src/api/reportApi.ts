import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1/reports';

export interface DashboardData {
  totalRevenue: number;
  grossProfit: number;
  totalOrders: number;
  lowStockCount: number;
  chartData: { date: string; revenue: number; profit: number }[];
  lowStockItems: { id: string; skuCode: string; productName: string; availableStock: number; threshold: number }[];
}

export const fetchDashboardSummary = async (): Promise<DashboardData> => {
  const response = await axios.get<DashboardData>(`${API_BASE_URL}/dashboard-summary`);
  return response.data;
};