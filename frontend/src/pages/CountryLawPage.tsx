import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ruleApi, rulePackageApi } from "../api/resources";
import type { Rule } from "../api/types";

const CATEGORY_LABEL: Record<string, string> = {
  WORKING_TIME: "Working time",
  LEAVE: "Leave",
  OVERTIME: "Overtime",
  EOS: "End of service",
  RATE_BASE: "Rate bases",
};

const today = new Date().toISOString().slice(0, 10);
const isCurrent = (r: Rule) => r.status === "ACTIVE" && (!r.effectiveTo || r.effectiveTo >= today);
const fmtValue = (r: Rule) =>
  (r.valueNumber !== undefined && r.valueNumber !== null ? String(r.valueNumber) : r.valueText ?? "—") +
  (r.unit ? ` ${r.unit}` : "");

function RuleRow({ rule, history }: { rule: Rule; history: Rule[] }) {
  const qc = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [value, setValue] = useState<string>(rule.valueNumber != null ? String(rule.valueNumber) : "");
  const [effectiveFrom, setEffectiveFrom] = useState(today);

  const save = useMutation({
    mutationFn: () =>
      ruleApi.create({
        ...rule,
        id: undefined,
        valueNumber: Number(value),
        effectiveFrom,
        effectiveTo: undefined,
        status: "ACTIVE",
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rules"] });
      setEditing(false);
    },
  });

  return (
    <Box sx={{ py: 1, borderTop: 1, borderColor: "divider" }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between">
        <Box>
          <Typography variant="body2" fontWeight={600}>{rule.name}</Typography>
          <Typography variant="caption" color="text.secondary">
            {rule.description ?? rule.code} · since {rule.effectiveFrom}
          </Typography>
        </Box>
        <Stack direction="row" alignItems="center" spacing={1}>
          <Chip size="small" color="primary" variant="outlined" label={fmtValue(rule)} sx={{ fontWeight: 600 }} />
          <Button size="small" onClick={() => setEditing((e) => !e)}>{editing ? "Cancel" : "Edit"}</Button>
        </Stack>
      </Stack>

      {editing && (
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mt: 1 }}>
          <TextField size="small" label={`New value${rule.unit ? ` (${rule.unit})` : ""}`} type="number"
            value={value} onChange={(e) => setValue(e.target.value)} sx={{ width: 180 }} />
          <TextField size="small" label="Effective From" type="date" InputLabelProps={{ shrink: true }}
            value={effectiveFrom} onChange={(e) => setEffectiveFrom(e.target.value)} />
          <Button variant="contained" size="small" disabled={!value || save.isPending} onClick={() => save.mutate()}>
            Apply
          </Button>
          {save.isError && <Typography variant="caption" color="error">Effective date must be after {rule.effectiveFrom}.</Typography>}
        </Stack>
      )}

      {history.length > 0 && (
        <Box sx={{ mt: 0.5 }}>
          <Button size="small" onClick={() => setShowHistory((s) => !s)}>
            {showHistory ? "Hide history" : `History (${history.length})`}
          </Button>
          {showHistory && history.map((h) => (
            <Typography key={h.id} variant="caption" color="text.secondary" sx={{ display: "block", pl: 1, opacity: 0.7 }}>
              {fmtValue(h)} · {h.effectiveFrom}{h.effectiveTo ? ` → ${h.effectiveTo}` : ""}
            </Typography>
          ))}
        </Box>
      )}
    </Box>
  );
}

export default function CountryLawPage() {
  const { data: packages = [] } = useQuery({ queryKey: ["rulePackages"], queryFn: rulePackageApi.list });
  const { data: active } = useQuery({ queryKey: ["activePackage"], queryFn: rulePackageApi.getActive });
  const [pkgCode, setPkgCode] = useState<string>("");

  useEffect(() => {
    if (active?.packageCode && !pkgCode) setPkgCode(active.packageCode);
  }, [active, pkgCode]);

  const qc = useQueryClient();
  const setActive = useMutation({
    mutationFn: (code: string) => rulePackageApi.setActive(code),
    onSuccess: (r) => { qc.invalidateQueries({ queryKey: ["activePackage"] }); setPkgCode(r.packageCode); },
  });

  const { data: rules = [] } = useQuery({
    queryKey: ["rules", pkgCode],
    queryFn: () => ruleApi.byPackage(pkgCode),
    enabled: Boolean(pkgCode),
  });

  // Group: current rule per code + its history, organised by category.
  const grouped = useMemo(() => {
    const byCode: Record<string, Rule[]> = {};
    for (const r of rules) (byCode[r.code] ??= []).push(r);
    const cats: Record<string, { current: Rule; history: Rule[] }[]> = {};
    for (const code of Object.keys(byCode)) {
      const rows = byCode[code].slice().sort((a, b) => (a.effectiveFrom < b.effectiveFrom ? 1 : -1));
      const current = rows.find(isCurrent) ?? rows[0];
      const history = rows.filter((r) => r.id !== current.id);
      (cats[current.category] ??= []).push({ current, history });
    }
    return cats;
  }, [rules]);

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2} flexWrap="wrap" gap={2}>
        <Box>
          <Typography variant="h5">Country Law</Typography>
          <Typography variant="body2" color="text.secondary">
            Default legal values for the selected country. Edits apply forward and keep history.
          </Typography>
        </Box>
        <TextField select size="small" label="Operating country" value={pkgCode}
          onChange={(e) => setActive.mutate(e.target.value)} sx={{ minWidth: 240 }}>
          {packages.map((p) => (
            <MenuItem key={p.code} value={p.code}>{p.name}</MenuItem>
          ))}
        </TextField>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>
        These are configurable defaults. Confirm each value against current law before running payroll.
      </Alert>

      {Object.keys(grouped).sort().map((cat) => (
        <Paper key={cat} variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>{CATEGORY_LABEL[cat] ?? cat}</Typography>
          <Divider />
          {grouped[cat]
            .sort((a, b) => a.current.name.localeCompare(b.current.name))
            .map(({ current, history }) => (
              <RuleRow key={current.code} rule={current} history={history} />
            ))}
        </Paper>
      ))}

      {pkgCode && rules.length === 0 && (
        <Typography variant="body2" color="text.secondary">No rules defined for this package yet.</Typography>
      )}
    </Box>
  );
}
