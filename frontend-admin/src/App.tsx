import { App as AntApp, ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { RequireAuth, RequireRole } from './auth/guards'
import AppLayout from './components/AppLayout'
import DashboardPage from './pages/dashboard/DashboardPage'
import BranchesPage from './pages/settings/BranchesPage'
import BrandsPage from './pages/catalog/BrandsPage'
import CategoriesPage from './pages/catalog/CategoriesPage'
import LoginPage from './pages/auth/LoginPage'
import OrdersPage from './pages/orders/OrdersPage'
import ProductDetailPage from './pages/catalog/ProductDetailPage'
import ProductsPage from './pages/catalog/ProductsPage'
import ProfitReportPage from './pages/reports/ProfitReportPage'
import ReceiptsPage from './pages/inventory/ReceiptsPage'
import RegisterPage from './pages/auth/RegisterPage'
import SettingsPage from './pages/settings/SettingsPage'
import SimulatorPage from './pages/orders/SimulatorPage'
import StockPage from './pages/inventory/StockPage'
import StockReportsPage from './pages/reports/StockReportsPage'
import StocktakesPage from './pages/inventory/StocktakesPage'
import SuppliersPage from './pages/inventory/SuppliersPage'
import TransfersPage from './pages/inventory/TransfersPage'
import UsersPage from './pages/settings/UsersPage'

export default function App() {
  return (
    <ConfigProvider locale={viVN}>
      <AntApp>
        <BrowserRouter>
          <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route element={<RequireAuth />}>
                <Route element={<AppLayout />}>
                  <Route index element={<Navigate to="/dashboard" replace />} />
                  <Route element={<RequireRole roles={['OWNER', 'STAFF']} />}>
                    <Route path="/dashboard" element={<DashboardPage />} />
                    <Route path="/orders" element={<OrdersPage />} />
                    <Route path="/products" element={<ProductsPage />} />
                    <Route path="/products/:id" element={<ProductDetailPage />} />
                    <Route path="/suppliers" element={<SuppliersPage />} />
                    <Route path="/receipts" element={<ReceiptsPage />} />
                    <Route path="/transfers" element={<TransfersPage />} />
                    <Route path="/stocktakes" element={<StocktakesPage />} />
                    <Route path="/stock" element={<StockPage />} />
                    <Route path="/stock-reports" element={<StockReportsPage />} />
                  </Route>
                  <Route element={<RequireRole roles={['OWNER']} />}>
                    <Route path="/profit-report" element={<ProfitReportPage />} />
                    <Route path="/branches" element={<BranchesPage />} />
                    <Route path="/users" element={<UsersPage />} />
                    <Route path="/settings" element={<SettingsPage />} />
                    <Route path="/categories" element={<CategoriesPage />} />
                    <Route path="/brands" element={<BrandsPage />} />
                    <Route path="/simulator" element={<SimulatorPage />} />
                  </Route>
                </Route>
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </AuthProvider>
        </BrowserRouter>
      </AntApp>
    </ConfigProvider>
  )
}
