import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';

import Layout from './components/layout/Layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import FilesPage from './pages/FilesPage';
import FileDetailPage from './pages/FileDetailPage';
import MappingDesignerPage from './pages/MappingDesignerPage';
import ErrorsPage from './pages/ErrorsPage';
import PartnersPage from './pages/PartnersPage';
import MonitoringPage from './pages/MonitoringPage';
import ImportOrdersPage from './pages/ImportOrdersPage';
import ImportOrderDetailPage from './pages/ImportOrderDetailPage';
import ImportOrderFormPage from './pages/ImportOrderFormPage';
import TmsOrdersPage from './pages/TmsOrdersPage';
import CompanyInformationPage from './pages/CompanyInformationPage';
import RoutePlannerPage from './pages/RoutePlannerPage';
import OrderLinePlanningPage from './pages/OrderLinePlanningPage';
import AddressMasterPage from './pages/AddressMasterPage';
import MastersHubPage from './pages/MastersHubPage';
import ReferenceCategoryPage from './pages/ReferenceCategoryPage';
import PartiesMasterPage from './pages/PartiesMasterPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, err: any) => {
        if (err?.response?.status === 401) return false;
        return failureCount < 2;
      },
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Public */}
          <Route path="/login" element={<LoginPage />} />

          {/* Protected — wrapped in Layout */}
          <Route element={<Layout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard"  element={<DashboardPage />} />
            <Route path="/files"      element={<FilesPage />} />
            <Route path="/files/:entryNo" element={<FileDetailPage />} />
            <Route path="/mappings"   element={<MappingDesignerPage />} />
            <Route path="/errors"     element={<ErrorsPage />} />
            <Route path="/partners"   element={<PartnersPage />} />
            <Route path="/monitoring"        element={<MonitoringPage />} />
            <Route path="/import-orders"               element={<ImportOrdersPage />} />
            <Route path="/import-orders/new"           element={<ImportOrderFormPage />} />
            <Route path="/import-orders/:entryNo/edit" element={<ImportOrderFormPage />} />
            <Route path="/import-orders/:entryNo/partition/:partitionKey" element={<ImportOrderDetailPage />} />
            <Route path="/import-orders/:entryNo"      element={<ImportOrderDetailPage />} />
            <Route path="/tms-orders"        element={<TmsOrdersPage />} />
            <Route path="/company-information" element={<CompanyInformationPage />} />
            <Route path="/route-planner" element={<RoutePlannerPage />} />
            <Route path="/planning" element={<OrderLinePlanningPage />} />
            <Route path="/address-master" element={<AddressMasterPage />} />
            <Route path="/masters" element={<MastersHubPage />} />
            <Route path="/masters/ref/:category" element={<ReferenceCategoryPage />} />
            <Route path="/masters/parties" element={<PartiesMasterPage />} />
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
