import { App as AntApp, ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { RequireAuth, RequireRole } from './auth/guards'
import AppLayout from './components/AppLayout'
import BrandsPage from './pages/BrandsPage'
import CategoriesPage from './pages/CategoriesPage'
import LoginPage from './pages/LoginPage'
import OrdersPage from './pages/OrdersPage'
import ProductDetailPage from './pages/ProductDetailPage'
import ProductsPage from './pages/ProductsPage'
import ProfitReportPage from './pages/ProfitReportPage'
import ReceiptsPage from './pages/ReceiptsPage'
import RegisterPage from './pages/RegisterPage'
import SimulatorPage from './pages/SimulatorPage'
import StockPage from './pages/StockPage'
import StocktakesPage from './pages/StocktakesPage'
import SuppliersPage from './pages/SuppliersPage'
import TransfersPage from './pages/TransfersPage'

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
                  <Route index element={<Navigate to="/orders" replace />} />
                  <Route element={<RequireRole roles={['OWNER', 'STAFF']} />}>
                    <Route path="/orders" element={<OrdersPage />} />
                    <Route path="/products" element={<ProductsPage />} />
                    <Route path="/products/:id" element={<ProductDetailPage />} />
                    <Route path="/suppliers" element={<SuppliersPage />} />
                    <Route path="/receipts" element={<ReceiptsPage />} />
                    <Route path="/transfers" element={<TransfersPage />} />
                    <Route path="/stocktakes" element={<StocktakesPage />} />
                    <Route path="/stock" element={<StockPage />} />
                  </Route>
                  <Route element={<RequireRole roles={['OWNER']} />}>
                    <Route path="/categories" element={<CategoriesPage />} />
                    <Route path="/brands" element={<BrandsPage />} />
                    <Route path="/profit-report" element={<ProfitReportPage />} />
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
