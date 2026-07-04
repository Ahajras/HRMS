import { useEffect, useState } from "react";
import { Link as RouterLink, useLocation } from "react-router-dom";
import {
  Alert,
  AppBar,
  Box,
  Button,
  Collapse,
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Snackbar,
  Toolbar,
  Typography,
  TextField,
  Tooltip,
} from "@mui/material";
import ExpandLess from "@mui/icons-material/ExpandLess";
import ExpandMore from "@mui/icons-material/ExpandMore";
import PeopleIcon from "@mui/icons-material/People";
import AccountTreeIcon from "@mui/icons-material/AccountTree";
import LayersIcon from "@mui/icons-material/Layers";
import PaymentsIcon from "@mui/icons-material/Payments";
import PublicIcon from "@mui/icons-material/Public";
import GavelIcon from "@mui/icons-material/Gavel";
import TimerIcon from "@mui/icons-material/Timer";
import WorkIcon from "@mui/icons-material/Work";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import ScheduleIcon from "@mui/icons-material/Schedule";
import EventBusyIcon from "@mui/icons-material/EventBusy";
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";
import GroupsIcon from "@mui/icons-material/Groups";
import AttachMoneyIcon from "@mui/icons-material/AttachMoney";
import ManageAccountsIcon from "@mui/icons-material/ManageAccounts";
import SecurityIcon from "@mui/icons-material/Security";
import ImportExportIcon from "@mui/icons-material/ImportExport";
import LogoutIcon from "@mui/icons-material/Logout";
import { COMPANY_STORAGE_KEY, getCompanyId, setCompanyId } from "../api/client";
import { useAuth } from "../auth/AuthContext";

const DRAWER_WIDTH = 240;

interface NavItem {
  to: string;
  label: string;
  icon: React.ReactNode;
  authority?: string;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

const NAV_GROUPS: NavGroup[] = [
  {
    label: "Workforce",
    items: [
      { to: "/employees", label: "Employees", icon: <PeopleIcon />, authority: "employee.read" },
      { to: "/organization", label: "Organization", icon: <AccountTreeIcon />, authority: "organization.read" },
      { to: "/crews", label: "Crews", icon: <GroupsIcon />, authority: "employee.read" },
      { to: "/timekeepers", label: "Timekeepers", icon: <ManageAccountsIcon />, authority: "employee.read" },
    ],
  },
  {
    label: "Time & Attendance",
    items: [
      { to: "/payroll-calendar", label: "Payroll Calendar", icon: <CalendarMonthIcon />, authority: "employee.read" },
      { to: "/timesheets", label: "Timesheets", icon: <AccessTimeIcon />, authority: "employee.read" },
      { to: "/roster", label: "Shift Roster", icon: <GroupsIcon />, authority: "employee.read" },
      { to: "/shifts", label: "Shifts", icon: <ScheduleIcon />, authority: "employee.read" },
      { to: "/time-setup", label: "Time Setup", icon: <EventBusyIcon />, authority: "employee.read" },
    ],
  },
  {
    label: "Payroll",
    items: [
      { to: "/payroll-runs", label: "Payroll Runs", icon: <PaymentsIcon />, authority: "payroll.config.read" },
      { to: "/payroll-reports", label: "Reports", icon: <PaymentsIcon />, authority: "payroll.config.read" },
      { to: "/payroll-components", label: "Pay Components", icon: <PaymentsIcon />, authority: "payroll.config.read" },
      { to: "/payroll-rules", label: "Payroll Rules", icon: <PaymentsIcon />, authority: "payroll.config.read" },
    ],
  },
  {
    label: "Configuration",
    items: [
      { to: "/projects", label: "Projects", icon: <WorkIcon />, authority: "reference.read" },
      { to: "/org-unit-types", label: "Org Levels", icon: <LayersIcon />, authority: "organization.read" },
      { to: "/countries", label: "Countries", icon: <PublicIcon />, authority: "reference.read" },
      { to: "/country-law", label: "Country Law", icon: <GavelIcon />, authority: "reference.read" },
      { to: "/overtime-categories", label: "Overtime Categories", icon: <TimerIcon />, authority: "reference.read" },
      { to: "/currencies", label: "Currencies", icon: <AttachMoneyIcon />, authority: "reference.read" },
    ],
  },
  {
    label: "Administration",
    items: [
      { to: "/users", label: "Users", icon: <ManageAccountsIcon />, authority: "security.user.read" },
      { to: "/roles", label: "Roles", icon: <SecurityIcon />, authority: "security.role.read" },
      { to: "/legacy-import", label: "Legacy Import", icon: <ImportExportIcon />, authority: "employee.read" },
    ],
  },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const { user, logout, hasAuthority } = useAuth();
  const [company, setCompany] = useState(getCompanyId());

  const onCompanyChange = (value: string) => {
    setCompany(value);
    setCompanyId(value);
  };

  // Platform/super-admin accounts have no company in their token; let them target one.
  const isPlatformAdmin = !user?.companyId;

  // Keep only groups/items the user is allowed to see.
  const groups = NAV_GROUPS
    .map((g) => ({ ...g, items: g.items.filter((i) => !i.authority || hasAuthority(i.authority)) }))
    .filter((g) => g.items.length > 0);

  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});
  const toggle = (label: string) => setCollapsed((c) => ({ ...c, [label]: !c[label] }));

  // Global error toast: any failed API call dispatches an "api-error" event.
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  useEffect(() => {
    const handler = (e: Event) => setErrorMsg((e as CustomEvent<string>).detail);
    window.addEventListener("api-error", handler);
    return () => window.removeEventListener("api-error", handler);
  }, []);

  return (
    <Box sx={{ display: "flex" }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar sx={{ gap: 2 }}>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            HRMS — Workforce &amp; Payroll
          </Typography>
          {isPlatformAdmin && (
            <Tooltip title="Platform admin: company (tenant) UUID sent as X-Company-Id">
              <TextField
                size="small"
                label="Company ID"
                value={company}
                onChange={(e) => onCompanyChange(e.target.value)}
                key={COMPANY_STORAGE_KEY}
                sx={{ width: 320, bgcolor: "background.paper", borderRadius: 1 }}
              />
            </Tooltip>
          )}
          <Typography variant="body2">{user?.fullName ?? user?.username}</Typography>
          <Button color="inherit" startIcon={<LogoutIcon />} onClick={logout}>
            Logout
          </Button>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: "border-box" },
        }}
      >
        <Toolbar />
        <Box sx={{ overflow: "auto" }}>
          {groups.map((group) => {
            const hasActive = group.items.some((i) => location.pathname.startsWith(i.to));
            const isOpen = !collapsed[group.label] || hasActive;
            return (
              <List
                key={group.label}
                disablePadding
                subheader={
                  <ListSubheader
                    component="div"
                    onClick={() => toggle(group.label)}
                    sx={{ cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "space-between", lineHeight: "36px", userSelect: "none" }}
                  >
                    {group.label}
                    {isOpen ? <ExpandLess fontSize="small" /> : <ExpandMore fontSize="small" />}
                  </ListSubheader>
                }
              >
                <Collapse in={isOpen} timeout="auto" unmountOnExit>
                  {group.items.map((item) => (
                    <ListItemButton
                      key={item.to}
                      component={RouterLink}
                      to={item.to}
                      selected={location.pathname.startsWith(item.to)}
                      sx={{ pl: 3 }}
                    >
                      <ListItemIcon sx={{ minWidth: 36 }}>{item.icon}</ListItemIcon>
                      <ListItemText primary={item.label} />
                    </ListItemButton>
                  ))}
                </Collapse>
              </List>
            );
          })}
        </Box>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        {children}
      </Box>

      <Snackbar
        open={!!errorMsg}
        autoHideDuration={6000}
        onClose={() => setErrorMsg(null)}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert severity="error" variant="filled" onClose={() => setErrorMsg(null)} sx={{ maxWidth: 600 }}>
          {errorMsg}
        </Alert>
      </Snackbar>
    </Box>
  );
}
