import { Navigate, Route, Routes } from "react-router-dom";
import { Box, CircularProgress } from "@mui/material";
import AppLayout from "./components/AppLayout";
import EmployeesPage from "./pages/EmployeesPage";
import OrganizationPage from "./pages/OrganizationPage";
import OrgUnitTypesPage from "./pages/OrgUnitTypesPage";
import PayrollComponentsPage from "./pages/PayrollComponentsPage";
import CountriesPage from "./pages/CountriesPage";
import CurrenciesPage from "./pages/CurrenciesPage";
import UsersPage from "./pages/UsersPage";
import RolesPage from "./pages/RolesPage";
import LegacyImportPage from "./pages/LegacyImportPage";
import LoginPage from "./pages/LoginPage";
import { useAuth } from "./auth/AuthContext";

export default function App() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <Box sx={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <AppLayout>
      <Routes>
        <Route path="/" element={<Navigate to="/employees" replace />} />
        <Route path="/login" element={<Navigate to="/employees" replace />} />
        <Route path="/employees" element={<EmployeesPage />} />
        <Route path="/organization" element={<OrganizationPage />} />
        <Route path="/org-unit-types" element={<OrgUnitTypesPage />} />
        <Route path="/payroll-components" element={<PayrollComponentsPage />} />
        <Route path="/countries" element={<CountriesPage />} />
        <Route path="/currencies" element={<CurrenciesPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="/roles" element={<RolesPage />} />
        <Route path="/legacy-import" element={<LegacyImportPage />} />
        <Route path="*" element={<Navigate to="/employees" replace />} />
      </Routes>
    </AppLayout>
  );
}
