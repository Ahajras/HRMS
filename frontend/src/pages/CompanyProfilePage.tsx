import { useEffect, useState } from "react";
import {
  Alert,
  Avatar,
  Box,
  Button,
  Divider,
  Grid,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import BusinessIcon from "@mui/icons-material/Business";
import SaveIcon from "@mui/icons-material/Save";
import UploadIcon from "@mui/icons-material/Upload";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
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

  const uploadLogo = (file?: File) => {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => set("logoUrl", String(reader.result || ""));
    reader.readAsDataURL(file);
  };

  const companyName = form.companyName || "Company name";
  const logoUrl = form.logoUrl || "";

  return (
    <Box>
      <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" alignItems={{ xs: "stretch", md: "center" }} spacing={1.5} mb={2}>
        <Box>
          <Typography variant="h5">Company Profile</Typography>
          <Typography variant="body2" color="text.secondary">Company identity used in the application header and documents.</Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<SaveIcon />}
          disabled={isLoading || save.isPending || !form.companyName}
          onClick={() => save.mutate(form)}
          sx={{ alignSelf: { xs: "stretch", md: "center" } }}
        >
          Save profile
        </Button>
      </Stack>

      <Grid container spacing={2}>
        <Grid item xs={12} lg={4}>
          <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "hidden" }}>
            <Box sx={{ bgcolor: "#10131a", color: "common.white", p: 2 }}>
              <Typography variant="subtitle2" fontWeight={900}>Header preview</Typography>
            </Box>
            <Box sx={{ p: 2 }}>
              <Stack direction="row" spacing={1.25} alignItems="center" sx={{ border: "1px solid", borderColor: "divider", borderRadius: 1, p: 1.25 }}>
                <Avatar src={logoUrl || undefined} variant="rounded" sx={{ width: 42, height: 42, bgcolor: "primary.main" }}>
                  {companyName.slice(0, 1).toUpperCase()}
                </Avatar>
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="subtitle1" fontWeight={900} noWrap>{companyName}</Typography>
                  <Typography variant="caption" color="text.secondary" noWrap>HRMS — Workforce & Payroll</Typography>
                </Box>
              </Stack>

              <Stack alignItems="center" spacing={1.5} sx={{ py: 3 }}>
                <Avatar src={logoUrl || undefined} sx={{ width: 112, height: 112, bgcolor: "primary.main", fontSize: 40 }}>
                  <BusinessIcon fontSize="large" />
                </Avatar>
                <Box textAlign="center">
                  <Typography variant="subtitle1" fontWeight={900}>{companyName}</Typography>
                  <Typography variant="body2" color="text.secondary">{form.legalName || "Legal name"}</Typography>
                </Box>
              </Stack>

              <Divider sx={{ my: 1.5 }} />
            <Typography variant="caption" color="text.secondary">
                Use a square or horizontal logo with a transparent background for the cleanest header result.
              </Typography>
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12} lg={8}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
            <Typography variant="subtitle1" fontWeight={900} mb={1.5}>Site branding</Typography>
            <Grid container spacing={1.5}>
              <Grid item xs={12} md={6}>
                <TextField fullWidth required label="Company name" value={form.companyName}
                  onChange={(e) => set("companyName", e.target.value)} />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField fullWidth label="Legal name" value={form.legalName ?? ""}
                  onChange={(e) => set("legalName", e.target.value)} />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth label="Logo URL" value={form.logoUrl ?? ""}
                  onChange={(e) => set("logoUrl", e.target.value)}
                  helperText="This logo appears in the application header and sidebar." />
              </Grid>
              <Grid item xs={12}>
                <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
                  <Button variant="outlined" component="label" startIcon={<UploadIcon />}>
                    Upload logo
                    <input
                      hidden
                      type="file"
                      accept="image/png,image/jpeg,image/webp,image/svg+xml"
                      onChange={(e) => uploadLogo(e.target.files?.[0])}
                    />
                  </Button>
                  <Button
                    variant="outlined"
                    color="error"
                    startIcon={<DeleteOutlineIcon />}
                    disabled={!form.logoUrl}
                    onClick={() => set("logoUrl", "")}
                  >
                    Remove logo
                  </Button>
                  <Button
                    variant="contained"
                    startIcon={<SaveIcon />}
                    disabled={isLoading || save.isPending || !form.companyName}
                    onClick={() => save.mutate(form)}
                  >
                    Save branding
                  </Button>
                </Stack>
              </Grid>
            </Grid>

            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle1" fontWeight={900} mb={1.5}>Company details</Typography>
            <Grid container spacing={1.5}>
              <Grid item xs={12} md={6}>
                <TextField fullWidth label="Tax number" value={form.taxNumber ?? ""}
                  onChange={(e) => set("taxNumber", e.target.value)} />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField fullWidth label="Registration no." value={form.registrationNo ?? ""}
                  onChange={(e) => set("registrationNo", e.target.value)} />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField fullWidth label="Email" value={form.email ?? ""}
                  onChange={(e) => set("email", e.target.value)} />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField fullWidth label="Phone" value={form.phone ?? ""}
                  onChange={(e) => set("phone", e.target.value)} />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField fullWidth label="Website" value={form.website ?? ""}
                  onChange={(e) => set("website", e.target.value)} />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth multiline minRows={3} label="Address" value={form.addressLine ?? ""}
                  onChange={(e) => set("addressLine", e.target.value)} />
              </Grid>
              <Grid item xs={12}>
                <Button
                  variant="contained"
                  startIcon={<SaveIcon />}
                  disabled={isLoading || save.isPending || !form.companyName}
                  onClick={() => save.mutate(form)}
                >
                  Save profile
                </Button>
              </Grid>
            </Grid>
          </Paper>

          {save.isSuccess && <Alert severity="success" sx={{ mt: 2 }}>Company profile saved.</Alert>}
          {save.isError && <Alert severity="error" sx={{ mt: 2 }}>{(save.error as any)?.response?.data?.message ?? "Could not save company profile."}</Alert>}
        </Grid>
      </Grid>
    </Box>
  );
}
