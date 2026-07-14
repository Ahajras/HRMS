import { Box, Stack, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "../api/resources";

const INK = "#151A21";
const PANEL = "#1B222B";
const PANEL_BORDER = "#2A333F";
const AMBER = "#F2A93B";
const TEAL = "#38D6C0";
const CORAL = "#FF6E5C";
const SLATE = "#8B96A5";
const PAPER = "#EDEFF2";

const mono = { fontFamily: "'IBM Plex Mono', monospace" };

function fmt(n: number) {
  return Math.round(n).toLocaleString();
}

function Eyebrow({ children, color = SLATE }: { children: React.ReactNode; color?: string }) {
  return (
    <Typography sx={{ ...mono, fontSize: 11, letterSpacing: "0.14em", textTransform: "uppercase", color, fontWeight: 600 }}>
      {children}
    </Typography>
  );
}

function BoardPanel({
  label, value, unit, accent = AMBER, big = false,
}: { label: string; value: string | number; unit?: string; accent?: string; big?: boolean }) {
  return (
    <Box
      sx={{
        flex: 1,
        minWidth: 150,
        bgcolor: PANEL,
        border: `1px solid ${PANEL_BORDER}`,
        borderTop: `3px solid ${accent}`,
        borderRadius: "4px",
        p: 2,
        position: "relative",
      }}
    >
      <Eyebrow>{label}</Eyebrow>
      <Stack direction="row" alignItems="baseline" spacing={0.75} mt={0.75}>
        <Typography sx={{ ...mono, fontSize: big ? 40 : 30, fontWeight: 700, color: PAPER, lineHeight: 1 }}>
          {value}
        </Typography>
        {unit && <Typography sx={{ ...mono, fontSize: 13, color: SLATE }}>{unit}</Typography>}
      </Stack>
    </Box>
  );
}

function LivePulse() {
  return (
    <Box sx={{ display: "inline-flex", alignItems: "center", gap: 0.75, ml: 1.5 }}>
      <Box
        sx={{
          width: 7, height: 7, borderRadius: "50%", bgcolor: TEAL,
          boxShadow: `0 0 0 0 ${TEAL}`,
          animation: "pulseDot 1.8s ease-out infinite",
          "@keyframes pulseDot": {
            "0%": { boxShadow: `0 0 0 0 ${TEAL}66` },
            "70%": { boxShadow: `0 0 0 8px ${TEAL}00` },
            "100%": { boxShadow: `0 0 0 0 ${TEAL}00` },
          },
        }}
      />
      <Typography sx={{ ...mono, fontSize: 11, letterSpacing: "0.1em", color: TEAL, textTransform: "uppercase" }}>Live</Typography>
    </Box>
  );
}

function RosterBar({ present, onLeave, absent, notMarked }: { present: number; onLeave: number; absent: number; notMarked: number }) {
  const total = Math.max(present + onLeave + absent + notMarked, 1);
  const seg = (n: number) => `${(n / total) * 100}%`;
  return (
    <Box>
      <Box sx={{ display: "flex", height: 14, borderRadius: "3px", overflow: "hidden", border: `1px solid ${PANEL_BORDER}` }}>
        <Box sx={{ width: seg(present), bgcolor: TEAL }} />
        <Box sx={{ width: seg(onLeave), bgcolor: AMBER }} />
        <Box sx={{ width: seg(absent), bgcolor: CORAL }} />
        <Box sx={{ width: seg(notMarked), bgcolor: "#3A4453" }} />
      </Box>
      <Stack direction="row" spacing={3} mt={1.5} flexWrap="wrap">
        {[
          { label: "Present", value: present, color: TEAL },
          { label: "On leave", value: onLeave, color: AMBER },
          { label: "Absent", value: absent, color: CORAL },
          { label: "Not marked", value: notMarked, color: "#3A4453" },
        ].map((s) => (
          <Stack key={s.label} direction="row" alignItems="center" spacing={0.75}>
            <Box sx={{ width: 9, height: 9, borderRadius: "2px", bgcolor: s.color }} />
            <Typography sx={{ color: SLATE, fontSize: 13 }}>{s.label}</Typography>
            <Typography sx={{ ...mono, color: PAPER, fontSize: 13, fontWeight: 600 }}>{s.value}</Typography>
          </Stack>
        ))}
      </Stack>
    </Box>
  );
}

export default function DashboardPage() {
  const { data, isLoading } = useQuery({ queryKey: ["dashboardSummary"], queryFn: dashboardApi.summary });

  const today = new Date();
  const dateStr = today.toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "long", year: "numeric" });

  if (isLoading || !data) {
    return (
      <Box sx={{ bgcolor: INK, m: -3, p: 3, minHeight: "100vh" }}>
        <Typography sx={{ color: SLATE }}>Loading site board…</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ bgcolor: INK, m: -3, p: { xs: 2, md: 3 }, minHeight: "100vh" }}>
      <Stack direction="row" alignItems="center" mb={0.5}>
        <Eyebrow color={AMBER}>Workforce Operations</Eyebrow>
        <LivePulse />
      </Stack>
      <Typography sx={{ color: PAPER, fontSize: 26, fontWeight: 700, mb: 3 }}>{dateStr}</Typography>

      {/* Vitals strip */}
      <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap mb={3}>
        <BoardPanel label="Active Projects" value={data.projectCount} accent={AMBER} big />
        <BoardPanel label="Active Workforce" value={fmt(data.activeEmployeeCount)} accent={AMBER} big />
        <BoardPanel label="Present Today" value={fmt(data.presentToday)} accent={TEAL} big />
        <BoardPanel label="On Leave Today" value={fmt(data.onLeaveToday)} accent={AMBER} big />
        <BoardPanel label="Absent Today" value={fmt(data.absentToday)} accent={CORAL} big />
      </Stack>

      <Stack direction={{ xs: "column", md: "row" }} spacing={1.5}>
        {/* Today's roster */}
        <Box sx={{ flex: 1, bgcolor: PANEL, border: `1px solid ${PANEL_BORDER}`, borderTop: `3px solid ${TEAL}`, borderRadius: "4px", p: 2.5 }}>
          <Eyebrow color={TEAL}>Today's Roster</Eyebrow>
          <Box mt={2}>
            <RosterBar present={data.presentToday} onLeave={data.onLeaveToday} absent={data.absentToday} notMarked={data.notMarkedToday} />
          </Box>
          <Box mt={3}>
            <Eyebrow>Month to date</Eyebrow>
            <Stack direction="row" spacing={3} mt={1}>
              <Box><Typography sx={{ ...mono, color: PAPER, fontSize: 20, fontWeight: 700 }}>{fmt(data.presentDaysMonth)}</Typography><Typography sx={{ color: SLATE, fontSize: 12 }}>present days</Typography></Box>
              <Box><Typography sx={{ ...mono, color: PAPER, fontSize: 20, fontWeight: 700 }}>{fmt(data.leaveDaysMonth)}</Typography><Typography sx={{ color: SLATE, fontSize: 12 }}>leave days</Typography></Box>
              <Box><Typography sx={{ ...mono, color: PAPER, fontSize: 20, fontWeight: 700 }}>{fmt(data.absentDaysMonth)}</Typography><Typography sx={{ color: SLATE, fontSize: 12 }}>absent days</Typography></Box>
            </Stack>
          </Box>
        </Box>

        {/* Payroll */}
        <Box sx={{ flex: 1, bgcolor: PANEL, border: `1px solid ${PANEL_BORDER}`, borderTop: `3px solid ${AMBER}`, borderRadius: "4px", p: 2.5 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Eyebrow color={AMBER}>
              {data.periodYear}-{String(data.periodMonth).padStart(2, "0")} Payroll {data.periodLocked ? "(Locked)" : "(Not yet locked)"}
            </Eyebrow>
          </Stack>
          <Typography sx={{ color: SLATE, fontSize: 12, mt: 0.5 }}>{data.payslipCount} payslips</Typography>
          <Stack mt={2}>
            <Eyebrow>Net disbursed</Eyebrow>
            <Typography sx={{ ...mono, color: PAPER, fontSize: 34, fontWeight: 700 }}>{fmt(data.netDisbursed)} <Box component="span" sx={{ fontSize: 16, color: SLATE }}>QAR</Box></Typography>
          </Stack>
          <Stack direction="row" spacing={4} mt={2.5}>
            <Box><Typography sx={{ ...mono, color: TEAL, fontSize: 18, fontWeight: 700 }}>{fmt(data.totalAllowances)}</Typography><Typography sx={{ color: SLATE, fontSize: 12 }}>allowances (QAR)</Typography></Box>
            <Box><Typography sx={{ ...mono, color: CORAL, fontSize: 18, fontWeight: 700 }}>{fmt(data.totalDeductions)}</Typography><Typography sx={{ color: SLATE, fontSize: 12 }}>deductions (QAR)</Typography></Box>
          </Stack>
        </Box>
      </Stack>
    </Box>
  );
}
