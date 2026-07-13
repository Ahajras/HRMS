import { useContext, useEffect, useState } from "react";
import { Link as RouterLink, useLocation } from "react-router-dom";
import {
  Alert,
  AppBar,
  Avatar,
  Box,
  Button,
  Collapse,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Snackbar,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from "@mui/material";
import ExpandLess from "@mui/icons-material/ExpandLess";
import ExpandMore from "@mui/icons-material/ExpandMore";
import PeopleIcon from "@mui/icons-material/People";
import AccountCircleIcon from "@mui/icons-material/AccountCircle";
import AccountTreeIcon from "@mui/icons-material/AccountTree";
import LayersIcon from "@mui/icons-material/Layers";
import PaymentsIcon from "@mui/icons-material/Payments";
import SavingsIcon from "@mui/icons-material/Savings";
import PublicIcon from "@mui/icons-material/Public";
import GavelIcon from "@mui/icons-material/Gavel";
import TimerIcon from "@mui/icons-material/Timer";
import WorkIcon from "@mui/icons-material/Work";
import BusinessIcon from "@mui/icons-material/Business";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import ScheduleIcon from "@mui/icons-material/Schedule";
import EventBusyIcon from "@mui/icons-material/EventBusy";
import BeachAccessIcon from "@mui/icons-material/BeachAccess";
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";
import GroupsIcon from "@mui/icons-material/Groups";
import AttachMoneyIcon from "@mui/icons-material/AttachMoney";
import ManageAccountsIcon from "@mui/icons-material/ManageAccounts";
import SecurityIcon from "@mui/icons-material/Security";
import BuildIcon from "@mui/icons-material/Build";
import ImportExportIcon from "@mui/icons-material/ImportExport";
import LogoutIcon from "@mui/icons-material/Logout";
import MenuIcon from "@mui/icons-material/Menu";
import KeyboardDoubleArrowLeftIcon from "@mui/icons-material/KeyboardDoubleArrowLeft";
import DarkModeIcon from "@mui/icons-material/DarkMode";
import LightModeIcon from "@mui/icons-material/LightMode";
import { useQuery } from "@tanstack/react-query";
import { COMPANY_STORAGE_KEY, getCompanyId, setCompanyId } from "../api/client";
import { companyProfileApi } from "../api/resources";
import { useAuth } from "../auth/AuthContext";
import { ThemeModeContext } from "../theme";

const DRAWER_WIDTH = 284;
const RAIL_WIDTH = 76;
const BRANDING_STORAGE_KEY = "hrms.branding";

interface NavItem {
  to: string;
  label: string;
  icon: React.ReactNode;
  authority?: string;
  authorities?: string[];
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

const NAV_GROUPS: NavGroup[] = [
  {
    label: "My Portal",
    items: [
      { to: "/my-portal", label: "My Payslips, Timesheet & Leave", icon: <AccountCircleIcon />, authority: "self.payslip.read" },
    ],
  },
  {
    label: "Workforce",
    items: [
      { to: "/employees", label: "Employees", icon: <PeopleIcon />, authority: "employee.read" },
      { to: "/organization", label: "Organization", icon: <AccountTreeIcon />, authority: "organization.read" },
      { to: "/crews", label: "Crews", icon: <GroupsIcon />, authority: "employee.read" },
      { to: "/timekeepers", label: "Timekeepers", icon: <ManageAccountsIcon />, authorities: ["employee.read", "timekeeper.attendance"] },
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
      { to: "/leave", label: "Leave", icon: <BeachAccessIcon />, authority: "employee.read" },
    ],
  },
  {
    label: "Payroll",
    items: [
      { to: "/payroll-runs", label: "Payroll Runs", icon: <PaymentsIcon />, authority: "payroll.config.read" },
      { to: "/payroll-reports", label: "Reports", icon: <PaymentsIcon />, authority: "payroll.config.read" },
      { to: "/sif-export", label: "WPS / SIF Export", icon: <PaymentsIcon />, authority: "payroll.sif.generate" },
      { to: "/provisions", label: "Provisions", icon: <SavingsIcon />, authority: "payroll.config.read" },
      { to: "/provision-rules", label: "Provision Rules", icon: <SavingsIcon />, authority: "payroll.config.read" },
      { to: "/tickets", label: "Tickets", icon: <SavingsIcon />, authority: "payroll.config.read" },
      { to: "/payroll-components", label: "Pay Components", icon: <PaymentsIcon />, authority: "payroll.config.read" },
      { to: "/payroll-rules", label: "Payroll Rules", icon: <PaymentsIcon />, authority: "payroll.config.read" },
      { to: "/day-zero", label: "Day Zero", icon: <PaymentsIcon />, authority: "payroll.config.read" },
    ],
  },
  {
    label: "Configuration",
    items: [
      { to: "/projects", label: "Projects", icon: <WorkIcon />, authority: "reference.read" },
      { to: "/company-profile", label: "Company Profile", icon: <BusinessIcon />, authority: "reference.read" },
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
      { to: "/audit-tools", label: "Audit Tools", icon: <BuildIcon />, authority: "audit.tools" },
    ],
  },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const theme = useTheme();
  const desktop = useMediaQuery(theme.breakpoints.up("lg"));
  const tabletUp = useMediaQuery(theme.breakpoints.up("md"));
  const { user, logout, hasAuthority } = useAuth();
  const { mode, toggleMode } = useContext(ThemeModeContext);
  const [company, setCompany] = useState(getCompanyId());
  const [mobileOpen, setMobileOpen] = useState(false);
  const [rail, setRail] = useState(false);

  // Do not carry the previous screen's scroll position into a new page. On
  // shorter/zoomed viewports that could leave the page title and action
  // buttons above the visible area.
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [location.pathname]);

  const isPlatformAdmin = !user?.companyId;
  const needsCompany = isPlatformAdmin && !company.trim();
  const companyQueryEnabled = !!user?.companyId || !!company.trim();
  const { data: profile } = useQuery({
    queryKey: ["companyProfile", company],
    queryFn: companyProfileApi.get,
    enabled: companyQueryEnabled,
  });

  const companyName = profile?.companyName || "HRMS";
  const logoUrl = profile?.logoUrl || "";

  useEffect(() => {
    if (profile?.companyName || profile?.logoUrl) {
      localStorage.setItem(BRANDING_STORAGE_KEY, JSON.stringify({
        companyName: profile.companyName,
        legalName: profile.legalName,
        logoUrl: profile.logoUrl,
      }));
    }
  }, [profile]);

  const onCompanyChange = (value: string) => {
    setCompany(value);
    setCompanyId(value);
  };

  const canSee = (item: NavItem) => {
    if (item.authorities?.length) return item.authorities.some(hasAuthority);
    return !item.authority || hasAuthority(item.authority);
  };
  const groups = NAV_GROUPS
    .map((g) => ({ ...g, items: g.items.filter(canSee) }))
    .filter((g) => g.items.length > 0);

  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});
  const toggle = (label: string) => setCollapsed((c) => ({ ...c, [label]: !c[label] }));

  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  useEffect(() => {
    const handler = (e: Event) => setErrorMsg((e as CustomEvent<string>).detail);
    window.addEventListener("api-error", handler);
    return () => window.removeEventListener("api-error", handler);
  }, []);

  const drawerWidth = desktop && rail ? RAIL_WIDTH : DRAWER_WIDTH;

  const drawerContent = (
    <Box sx={{ height: "100%", display: "flex", flexDirection: "column", bgcolor: "#10131a", color: "#e5e7eb" }}>
      <Stack direction="row" alignItems="center" spacing={1.25} sx={{ minHeight: 72, px: rail && desktop ? 1.5 : 2 }}>
        <Avatar
          src={logoUrl || undefined}
          variant="rounded"
          sx={{ width: 42, height: 42, bgcolor: "#2563eb", color: "common.white", fontWeight: 900 }}
        >
          {companyName.slice(0, 1).toUpperCase()}
        </Avatar>
        {!(rail && desktop) && (
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="subtitle1" noWrap fontWeight={900}>{companyName}</Typography>
            <Typography variant="caption" color="#94a3b8" noWrap>Workforce & Payroll</Typography>
          </Box>
        )}
      </Stack>
      <Divider sx={{ borderColor: "rgba(148,163,184,.18)" }} />
      <Box
        sx={{
          flex: 1,
          overflowY: "auto",
          py: 1.25,
          scrollbarWidth: "thin",
          scrollbarColor: "rgba(148,163,184,.34) transparent",
          "&::-webkit-scrollbar": { width: 8 },
          "&::-webkit-scrollbar-track": { bgcolor: "transparent" },
          "&::-webkit-scrollbar-thumb": { bgcolor: "rgba(148,163,184,.34)", borderRadius: 8 },
        }}
      >
        {groups.map((group) => {
          const hasActive = group.items.some((i) => location.pathname.startsWith(i.to));
          const isOpen = rail && desktop ? true : (!collapsed[group.label] || hasActive);
          return (
            <List key={group.label} disablePadding sx={{ px: 1 }}>
              {!(rail && desktop) && (
                <ListItemButton
                  onClick={() => toggle(group.label)}
                  sx={{ minHeight: 34, borderRadius: 1, color: "#94a3b8", px: 1.25 }}
                >
                  <ListItemText
                    primary={group.label}
                    primaryTypographyProps={{ fontSize: 11, fontWeight: 800, letterSpacing: 0, textTransform: "uppercase" }}
                  />
                  {isOpen ? <ExpandLess fontSize="small" /> : <ExpandMore fontSize="small" />}
                </ListItemButton>
              )}
              <Collapse in={isOpen} timeout="auto" unmountOnExit={false}>
                {group.items.map((item) => {
                  const selected = location.pathname.startsWith(item.to);
                  const row = (
                    <ListItemButton
                      key={item.to}
                      component={RouterLink}
                      to={item.to}
                      selected={selected}
                      onClick={() => setMobileOpen(false)}
                      sx={{
                        minHeight: 42,
                        justifyContent: rail && desktop ? "center" : "flex-start",
                        borderRadius: 1,
                        mb: 0.25,
                        px: rail && desktop ? 1 : 1.5,
                        color: selected ? "#ffffff" : "#cbd5e1",
                        "&.Mui-selected": { bgcolor: "#2563eb" },
                        "&.Mui-selected:hover": { bgcolor: "#1d4ed8" },
                        "&:hover": { bgcolor: "rgba(148,163,184,.12)" },
                      }}
                    >
                      <ListItemIcon sx={{ minWidth: rail && desktop ? 0 : 34, color: "inherit" }}>{item.icon}</ListItemIcon>
                      {!(rail && desktop) && (
                        <ListItemText primary={item.label} primaryTypographyProps={{ fontSize: 13, fontWeight: selected ? 800 : 600 }} />
                      )}
                    </ListItemButton>
                  );
                  return rail && desktop ? <Tooltip key={item.to} title={item.label} placement="right">{row}</Tooltip> : row;
                })}
              </Collapse>
            </List>
          );
        })}
      </Box>
      <Divider sx={{ borderColor: "rgba(148,163,184,.18)" }} />
      <Box sx={{ p: 1.25 }}>
        {desktop && (
          <Tooltip title={rail ? "Expand navigation" : "Collapse navigation"} placement="right">
            <IconButton onClick={() => setRail((v) => !v)} sx={{ color: "#cbd5e1" }}>
              <KeyboardDoubleArrowLeftIcon sx={{ transform: rail ? "rotate(180deg)" : "none" }} />
            </IconButton>
          </Tooltip>
        )}
      </Box>
    </Box>
  );

  return (
    <Box sx={{ minHeight: "100vh", display: "flex", bgcolor: "background.default" }}>
      <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{ display: { xs: "block", md: "none" }, "& .MuiDrawer-paper": { width: DRAWER_WIDTH, border: 0 } }}
        >
          {drawerContent}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: "none", md: "block" },
            "& .MuiDrawer-paper": {
              width: drawerWidth,
              boxSizing: "border-box",
              border: 0,
              transition: theme.transitions.create("width", { duration: theme.transitions.duration.shorter }),
              overflowX: "hidden",
            },
          }}
          open
        >
          {drawerContent}
        </Drawer>
      </Box>

      <Box sx={{ flexGrow: 1, minWidth: 0 }}>
      <AppBar
        position="sticky"
        elevation={0}
        sx={{
          bgcolor: mode === "dark" ? "rgba(17,24,39,.94)" : "rgba(255,255,255,.94)",
          color: "text.primary",
          borderBottom: "1px solid",
          borderColor: "divider",
          backdropFilter: "blur(10px)",
          width: "100%",
          top: 0,
        }}
      >
        <Toolbar sx={{ minHeight: { xs: 60, md: 64 }, gap: 1.5, px: { xs: 1.5, sm: 2.5 } }}>
          {!tabletUp && (
            <IconButton edge="start" onClick={() => setMobileOpen(true)} aria-label="Open navigation">
              <MenuIcon />
            </IconButton>
          )}
          <Avatar src={logoUrl || undefined} variant="rounded" sx={{ width: 34, height: 34, bgcolor: "primary.main" }}>
            {companyName.slice(0, 1).toUpperCase()}
          </Avatar>
          <Box sx={{ minWidth: 0, flexGrow: 1 }}>
            <Typography variant="subtitle1" noWrap fontWeight={900}>{companyName}</Typography>
            <Typography variant="caption" color="text.secondary" noWrap sx={{ display: { xs: "none", sm: "block" } }}>
              HRMS — Workforce & Payroll
            </Typography>
          </Box>
          {isPlatformAdmin && (
            <Tooltip title="Platform admin: company UUID sent as X-Company-Id">
              <TextField
                label="Company ID"
                value={company}
                onChange={(e) => onCompanyChange(e.target.value)}
                key={COMPANY_STORAGE_KEY}
                sx={{ width: { xs: 170, sm: 300 }, display: { xs: needsCompany ? "block" : "none", md: "block" } }}
              />
            </Tooltip>
          )}
          <Stack direction="row" spacing={1} alignItems="center" sx={{ display: { xs: "none", sm: "flex" }, maxWidth: 240 }}>
            <Avatar sx={{ width: 30, height: 30, bgcolor: mode === "dark" ? "#1f2937" : "#e2e8f0", color: "text.primary", fontSize: 13 }}>
              {(user?.fullName || user?.username || "U").slice(0, 1).toUpperCase()}
            </Avatar>
            <Typography variant="body2" noWrap color="text.secondary">{user?.fullName ?? user?.username}</Typography>
          </Stack>
          <Tooltip title={mode === "dark" ? "Morning theme" : "Evening theme"}>
            <IconButton onClick={toggleMode} color="primary" aria-label="Toggle theme">
              {mode === "dark" ? <LightModeIcon /> : <DarkModeIcon />}
            </IconButton>
          </Tooltip>
          <Button color="primary" startIcon={<LogoutIcon />} onClick={logout} sx={{ minWidth: { xs: 40, sm: 96 } }}>
            <Box component="span" sx={{ display: { xs: "none", sm: "inline" } }}>Logout</Box>
          </Button>
        </Toolbar>
      </AppBar>
      <Box component="main" sx={{ p: { xs: 1.25, sm: 2, lg: 2.5 } }}>
        {needsCompany ? (
          <Alert severity="info" sx={{ maxWidth: 760 }}>
            Enter a Company ID in the top bar to open company-scoped screens.
          </Alert>
        ) : children}
      </Box>
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
