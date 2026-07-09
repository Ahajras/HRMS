import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Alert,
  Avatar,
  Box,
  Button,
  Checkbox,
  Divider,
  FormControlLabel,
  IconButton,
  Link,
  Paper,
  Stack,
  TextField,
  Typography,
  useTheme,
} from "@mui/material";
import BusinessCenterIcon from "@mui/icons-material/BusinessCenter";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import PersonOutlineIcon from "@mui/icons-material/PersonOutline";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import { useQuery } from "@tanstack/react-query";
import { companyProfileApi } from "../api/resources";
import { useAuth } from "../auth/AuthContext";

const REMEMBER_LOGIN_KEY = "hrms.rememberLogin";
const SAVED_USERNAME_KEY = "hrms.savedUsername";
const BRANDING_STORAGE_KEY = "hrms.branding";

function cachedBranding() {
  try {
    return JSON.parse(localStorage.getItem(BRANDING_STORAGE_KEY) || "{}") as {
      companyName?: string;
      legalName?: string;
      logoUrl?: string;
    };
  } catch {
    return {};
  }
}

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const theme = useTheme();
  const [remember, setRemember] = useState(() => localStorage.getItem(REMEMBER_LOGIN_KEY) === "true");
  const [username, setUsername] = useState(() => localStorage.getItem(SAVED_USERNAME_KEY) || "");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [forgotMsg, setForgotMsg] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [brandCache] = useState(cachedBranding);
  const { data: publicBrand } = useQuery({
    queryKey: ["publicCompanyProfile"],
    queryFn: companyProfileApi.getPublic,
    retry: false,
  });

  const brand = publicBrand?.companyName ? publicBrand : brandCache;
  const companyName = brand.companyName || "HRMS";
  const legalName = brand.legalName || "Workforce & Payroll";
  const logoUrl = brand.logoUrl || "";

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username.trim(), password);
      if (remember) {
        localStorage.setItem(REMEMBER_LOGIN_KEY, "true");
        localStorage.setItem(SAVED_USERNAME_KEY, username.trim());
      } else {
        localStorage.removeItem(REMEMBER_LOGIN_KEY);
        localStorage.removeItem(SAVED_USERNAME_KEY);
      }
      navigate("/", { replace: true });
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? "Login failed. Check your credentials.";
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "grid",
        placeItems: "center",
        bgcolor: theme.palette.mode === "dark" ? "#0b1120" : "#eef3f8",
        p: { xs: 1.5, sm: 3 },
        position: "relative",
        overflow: "hidden",
        "&::before": {
          content: '""',
          position: "absolute",
          inset: 0,
          background:
            "linear-gradient(135deg, rgba(37,99,235,.12), transparent 34%), linear-gradient(315deg, rgba(15,118,110,.13), transparent 38%)",
        },
      }}
    >
      <Paper
        elevation={0}
        sx={{
          width: "min(1080px, 100%)",
          minHeight: { xs: "auto", md: 610 },
          display: "grid",
          gridTemplateColumns: { xs: "1fr", md: "430px 1fr" },
          borderRadius: 2,
          overflow: "hidden",
          border: "1px solid",
          borderColor: "rgba(148,163,184,.35)",
          boxShadow: "0 24px 80px rgba(15,23,42,.16)",
          position: "relative",
          zIndex: 1,
        }}
      >
        <Box sx={{ p: { xs: 3, sm: 4.5 }, bgcolor: "background.paper" }}>
          <Stack spacing={3} sx={{ height: "100%" }}>
            <Stack direction="row" spacing={1.25} alignItems="center">
              <Avatar variant="rounded" sx={{ bgcolor: "primary.main", width: 44, height: 44 }}>
                {logoUrl ? <Box component="img" src={logoUrl} alt="" sx={{ width: "100%", height: "100%", objectFit: "cover" }} /> : <BusinessCenterIcon />}
              </Avatar>
              <Box>
                <Typography variant="h6" fontWeight={900}>{companyName}</Typography>
                <Typography variant="caption" color="text.secondary">{legalName}</Typography>
              </Box>
            </Stack>

            <Box>
              <Typography variant="h4" fontWeight={900} sx={{ fontSize: { xs: 28, sm: 34 } }}>
                Sign in
              </Typography>
              <Typography variant="body2" color="text.secondary" mt={0.75}>
                Access your company workspace.
              </Typography>
            </Box>

            <form onSubmit={onSubmit}>
              <Stack spacing={2.25}>
                {error && <Alert severity="error">{error}</Alert>}
                <TextField
                  label="Username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoFocus
                  required
                  fullWidth
                  InputProps={{ startAdornment: <PersonOutlineIcon sx={{ color: "text.secondary", mr: 1 }} /> }}
                />
                <TextField
                  label="Password"
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  fullWidth
                  InputProps={{
                    startAdornment: <LockOutlinedIcon sx={{ color: "text.secondary", mr: 1 }} />,
                    endAdornment: (
                      <IconButton edge="end" onClick={() => setShowPassword((v) => !v)} aria-label={showPassword ? "Hide password" : "Show password"}>
                        {showPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                      </IconButton>
                    ),
                  }}
                />
                <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1}>
                  <FormControlLabel
                    control={<Checkbox checked={remember} onChange={(e) => setRemember(e.target.checked)} />}
                    label="Save login"
                  />
                  <Link component="button" type="button" variant="body2" onClick={() => setForgotMsg(true)}>
                    Forget password?
                  </Link>
                </Stack>
                {forgotMsg && <Alert severity="info">Please contact your HRMS administrator to reset your password.</Alert>}
                <Button type="submit" variant="contained" disabled={submitting} fullWidth sx={{ minHeight: 44 }}>
                  {submitting ? "Signing in..." : "Sign in"}
                </Button>
              </Stack>
            </form>

            <Box sx={{ flex: 1 }} />
            <Divider />
            <Typography variant="caption" color="text.secondary">
              Secure HRMS access for authorized users.
            </Typography>
          </Stack>
        </Box>

        <Box
          sx={{
            display: { xs: "none", md: "block" },
            bgcolor: "#10131a",
            color: "common.white",
            p: 4,
            position: "relative",
            overflow: "hidden",
          }}
        >
          <Stack spacing={2.5} sx={{ position: "relative", zIndex: 1 }}>
            <Stack direction="row" spacing={1.5} alignItems="center">
              <Avatar src={logoUrl || undefined} variant="rounded" sx={{ width: 54, height: 54, bgcolor: "primary.main" }}>
                {companyName.slice(0, 1).toUpperCase()}
              </Avatar>
              <Box>
                <Typography variant="h5" fontWeight={900}>{companyName}</Typography>
                <Typography variant="body2" sx={{ color: "#cbd5e1" }}>{legalName}</Typography>
              </Box>
            </Stack>
            <Typography variant="body2" sx={{ color: "#cbd5e1", maxWidth: 460 }}>
              Payroll, attendance, leave, and project cost control in one working surface.
            </Typography>

            <Box sx={{ mt: 2, border: "1px solid rgba(148,163,184,.22)", borderRadius: 2, bgcolor: "rgba(255,255,255,.06)", overflow: "hidden" }}>
              <Stack direction="row" alignItems="center" spacing={1} sx={{ p: 1.5, borderBottom: "1px solid rgba(148,163,184,.18)" }}>
                <Box sx={{ width: 10, height: 10, borderRadius: "50%", bgcolor: "#22c55e" }} />
                <Box sx={{ width: 10, height: 10, borderRadius: "50%", bgcolor: "#f59e0b" }} />
                <Box sx={{ width: 10, height: 10, borderRadius: "50%", bgcolor: "#ef4444" }} />
                <Box sx={{ flex: 1 }} />
                <Box sx={{ width: 120, height: 10, borderRadius: 1, bgcolor: "rgba(255,255,255,.18)" }} />
              </Stack>
              <Box sx={{ p: 2 }}>
                <Box sx={{ display: "grid", gridTemplateColumns: "1.15fr .85fr", gap: 1.5 }}>
                  <MockPanel title="Payroll" value="98%" color="#38bdf8" />
                  <MockPanel title="Attendance" value="42k" color="#34d399" />
                  <Box sx={{ gridColumn: "1 / -1", height: 170, borderRadius: 1, bgcolor: "rgba(15,23,42,.88)", p: 2 }}>
                    <Stack direction="row" spacing={1.2} alignItems="end" sx={{ height: "100%" }}>
                      {[42, 70, 52, 88, 64, 110, 76, 128, 95, 145, 116, 156].map((h, idx) => (
                        <Box
                          key={idx}
                          sx={{
                            flex: 1,
                            height: h,
                            borderRadius: "5px 5px 0 0",
                            bgcolor: idx % 3 === 0 ? "#38bdf8" : idx % 3 === 1 ? "#34d399" : "#f59e0b",
                            opacity: 0.9,
                          }}
                        />
                      ))}
                    </Stack>
                  </Box>
                  <MockTable />
                </Box>
              </Box>
            </Box>
          </Stack>
        </Box>
      </Paper>
    </Box>
  );
}

function MockPanel({ title, value, color }: { title: string; value: string; color: string }) {
  return (
    <Box sx={{ borderRadius: 1, bgcolor: "rgba(255,255,255,.08)", p: 1.5, minHeight: 92 }}>
      <Typography variant="caption" sx={{ color: "#cbd5e1" }}>{title}</Typography>
      <Stack direction="row" alignItems="end" justifyContent="space-between" mt={1}>
        <Typography variant="h5" fontWeight={900}>{value}</Typography>
        <Box sx={{ width: 42, height: 42, borderRadius: 1, bgcolor: color }} />
      </Stack>
    </Box>
  );
}

function MockTable() {
  return (
    <Box sx={{ gridColumn: "1 / -1", borderRadius: 1, bgcolor: "rgba(255,255,255,.08)", p: 1.5 }}>
      {[0, 1, 2].map((row) => (
        <Stack key={row} direction="row" spacing={1.5} alignItems="center" sx={{ py: 0.75 }}>
          <Box sx={{ width: 30, height: 30, borderRadius: 1, bgcolor: row === 1 ? "#2563eb" : "rgba(255,255,255,.16)" }} />
          <Box sx={{ flex: 1, height: 9, borderRadius: 1, bgcolor: "rgba(255,255,255,.18)" }} />
          <Box sx={{ width: 70, height: 9, borderRadius: 1, bgcolor: "rgba(255,255,255,.24)" }} />
        </Stack>
      ))}
    </Box>
  );
}
