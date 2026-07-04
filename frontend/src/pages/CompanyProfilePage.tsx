import { useEffect, useState } from "react";
import {
  Alert,
  Avatar,
  Box,
  Button,
  Grid,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import BusinessIcon from "@mui/icons-material/Business";
import SaveIcon from "@mui/icons-material/Save";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { companyProfileApi } from "../api/resources";
import type { CompanyProfile } from "../api/types";

const EMPTY: CompanyProfile = {
  companyName: "",
  legalName: "",
  taxNumber: "",
  registrationNo: "",
  email: "",
  phone: "",
  website: "",
  addressLine: "",
  logoUrl: "",
};

export default function CompanyProfilePage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["companyProfile"], queryFn: companyProfileApi.get });
  const [form, setForm] = useState<CompanyProfile>(EMPTY);

  useEffect(() => {
    if (data) setForm({ ...EMPTY, ...data });
  }, [data]);

  const save = useMutation({
    mutationFn: (payload: CompanyProfile) => companyProfileApi.save(payload),
    onSuccess: (saved) => {
      setForm(saved);
      qc.invalidateQueries({ queryKey: ["companyProfile"] });
    },
  });

  const set = <K extends keyof CompanyProfile>(key: K, value: CompanyProfile[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  return (
    <Box>
      <Typography variant="h5" mb={2}>Company Profile</Typography>

      <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "hidden", mb: 2 }}>
        <Box sx={{ px: 2, py: 2, bgcolor: "primary.main", color: "primary.contrastText" }}>
          <Stack direction="row" spacing={2} alignItems="center">
            <Avatar src={form.logoUrl || undefined} sx={{ width: 64, height: 64, bgcolor: "background.paper", color: "primary.main" }}>
              <BusinessIcon />
            </Avatar>
            <Box>
              <Typography variant="h5" fontWeight={800}>{form.companyName || "Company name"}</Typography>
              <Typography variant="body2">{form.legalName || "Legal name"}</Typography>
              <Typography variant="caption">{[form.phone, form.email, form.website].filter(Boolean).join(" · ")}</Typography>
            </Box>
          </Stack>
        </Box>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
        <Grid container spacing={1.5}>
          <Grid item xs={12} md={6}>
            <TextField fullWidth required size="small" label="Company name" value={form.companyName}
              onChange={(e) => set("companyName", e.target.value)} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField fullWidth size="small" label="Legal name" value={form.legalName ?? ""}
              onChange={(e) => set("legalName", e.target.value)} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField fullWidth size="small" label="Logo URL" value={form.logoUrl ?? ""}
              onChange={(e) => set("logoUrl", e.target.value)} />
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField fullWidth size="small" label="Tax number" value={form.taxNumber ?? ""}
              onChange={(e) => set("taxNumber", e.target.value)} />
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField fullWidth size="small" label="Registration no." value={form.registrationNo ?? ""}
              onChange={(e) => set("registrationNo", e.target.value)} />
          </Grid>
          <Grid item xs={12} md={4}>
            <TextField fullWidth size="small" label="Email" value={form.email ?? ""}
              onChange={(e) => set("email", e.target.value)} />
          </Grid>
          <Grid item xs={12} md={4}>
            <TextField fullWidth size="small" label="Phone" value={form.phone ?? ""}
              onChange={(e) => set("phone", e.target.value)} />
          </Grid>
          <Grid item xs={12} md={4}>
            <TextField fullWidth size="small" label="Website" value={form.website ?? ""}
              onChange={(e) => set("website", e.target.value)} />
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth multiline minRows={3} size="small" label="Address" value={form.addressLine ?? ""}
              onChange={(e) => set("addressLine", e.target.value)} />
          </Grid>
          <Grid item xs={12}>
            <Button
              variant="contained"
              startIcon={<SaveIcon />}
              disabled={isLoading || save.isPending || !form.companyName}
              onClick={() => save.mutate(form)}
            >
              Save
            </Button>
          </Grid>
        </Grid>
      </Paper>

      {save.isSuccess && <Alert severity="success" sx={{ mt: 2 }}>Company profile saved.</Alert>}
      {save.isError && <Alert severity="error" sx={{ mt: 2 }}>{(save.error as any)?.response?.data?.message ?? "Could not save company profile."}</Alert>}
    </Box>
  );
}
