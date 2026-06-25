import { useState } from "react";
import {
  Box,
  Button,
  Card,
  Collapse,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import AddIcon from "@mui/icons-material/Add";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { organizationUnitApi, orgUnitTypeApi } from "../api/resources";
import type { OrgUnitTreeNode, OrganizationUnit } from "../api/types";

const EMPTY: OrganizationUnit = {
  typeId: "",
  code: "",
  name: "",
  effectiveFrom: new Date().toISOString().slice(0, 10),
  status: "ACTIVE",
};

function TreeNode({ node, depth }: { node: OrgUnitTreeNode; depth: number }) {
  const [open, setOpen] = useState(true);
  const hasChildren = node.children && node.children.length > 0;
  return (
    <Box>
      <Stack direction="row" alignItems="center" sx={{ pl: depth * 3, py: 0.5 }}>
        {hasChildren ? (
          <IconButton size="small" onClick={() => setOpen(!open)}>
            {open ? <ExpandMoreIcon /> : <ChevronRightIcon />}
          </IconButton>
        ) : (
          <Box sx={{ width: 34 }} />
        )}
        <Typography variant="body2" fontWeight={600} sx={{ mr: 1 }}>{node.code}</Typography>
        <Typography variant="body2" color="text.secondary">{node.name}</Typography>
      </Stack>
      {hasChildren && (
        <Collapse in={open} timeout="auto" unmountOnExit>
          {node.children.map((c) => <TreeNode key={c.id} node={c} depth={depth + 1} />)}
        </Collapse>
      )}
    </Box>
  );
}

export default function OrganizationPage() {
  const qc = useQueryClient();
  const { data: tree = [], isLoading } = useQuery({ queryKey: ["orgTree"], queryFn: organizationUnitApi.tree });
  const { data: types = [] } = useQuery({ queryKey: ["orgUnitTypes"], queryFn: orgUnitTypeApi.list });
  const { data: units = [] } = useQuery({ queryKey: ["orgUnits"], queryFn: organizationUnitApi.list });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<OrganizationUnit>(EMPTY);

  const save = useMutation({
    mutationFn: (u: OrganizationUnit) => organizationUnitApi.create(u),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["orgTree"] });
      qc.invalidateQueries({ queryKey: ["orgUnits"] });
      setOpen(false);
    },
  });

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Organization Tree</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={() => { setForm(EMPTY); setOpen(true); }}>
          New Unit
        </Button>
      </Stack>

      <Card sx={{ p: 2, minHeight: 400 }}>
        {isLoading && <Typography color="text.secondary">Loading…</Typography>}
        {!isLoading && tree.length === 0 && (
          <Typography color="text.secondary">No organisation units yet. Create the root (Company) unit to begin.</Typography>
        )}
        {tree.map((n) => <TreeNode key={n.id} node={n} depth={0} />)}
      </Card>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>New Organisation Unit</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField select label="Level (Type)" value={form.typeId}
              onChange={(e) => setForm({ ...form, typeId: e.target.value })}>
              {types.map((t) => <MenuItem key={t.id} value={t.id}>{t.levelOrder} — {t.name}</MenuItem>)}
            </TextField>
            <TextField select label="Parent Unit" value={form.parentId ?? ""}
              onChange={(e) => setForm({ ...form, parentId: e.target.value || undefined })}>
              <MenuItem value="">(none — root)</MenuItem>
              {units.map((u) => <MenuItem key={u.id} value={u.id}>{u.code} — {u.name}</MenuItem>)}
            </TextField>
            <TextField label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} />
            <TextField label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <TextField label="Effective From" type="date" InputLabelProps={{ shrink: true }}
              value={form.effectiveFrom} onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => save.mutate(form)} disabled={save.isPending || !form.typeId}>Save</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
