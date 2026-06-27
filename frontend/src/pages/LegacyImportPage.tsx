import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import UploadFileIcon from "@mui/icons-material/UploadFile";
import VisibilityIcon from "@mui/icons-material/Visibility";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import { useMutation } from "@tanstack/react-query";
import { legacyImportApi } from "../api/resources";
import { getApiErrorMessage } from "../api/errors";
import type { ImportSummary } from "../api/types";

/** Pretty label for a raw count key, e.g. "employee_inserted" -> "Employees added". */
const COUNT_LABELS: Record<string, string> = {
  component_inserted: "Pay components added",
  component_updated: "Pay components updated",
  employee_inserted: "Employees added",
  employee_updated: "Employees updated",
  contract_inserted: "Contracts added",
  contract_updated: "Contracts updated",
  document_inserted: "Documents added",
  document_skipped: "Documents already present",
  pay_item_inserted: "Pay items added",
  pay_item_updated: "Pay items updated",
  dependent_inserted: "Dependents added",
  dependent_skipped: "Dependents already present",
};

export default function LegacyImportPage() {
  const [files, setFiles] = useState<File[]>([]);
  const [summary, setSummary] = useState<ImportSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  const preview = useMutation({
    mutationFn: () => legacyImportApi.preview(files),
    onSuccess: (s) => { setSummary(s); setError(null); },
    onError: (e) => setError(getApiErrorMessage(e)),
  });

  const commit = useMutation({
    mutationFn: () => legacyImportApi.commit(files),
    onSuccess: (s) => { setSummary(s); setError(null); },
    onError: (e) => setError(getApiErrorMessage(e)),
  });

  const busy = preview.isPending || commit.isPending;

  const onPick = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFiles(e.target.files ? Array.from(e.target.files) : []);
    setSummary(null);
    setError(null);
  };

  return (
    <Box>
      <Typography variant="h5" mb={1}>Import from Legacy System</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Upload the legacy FoxPro/DBF snapshot exported from the old HR system — either a
        single .zip of the dated folder, or the individual files
        (payresulth.dbf, payresultd.dbf, dependants.dbf). Preview first to see what will
        change, then import. Re-importing a fresh export updates existing employees and
        adds new ones, with no duplicates.
      </Typography>

      <Paper variant="outlined" sx={{ p: 3, mb: 3 }}>
        <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap" useFlexGap>
          <Button component="label" variant="outlined" startIcon={<UploadFileIcon />} disabled={busy}>
            Choose files
            <input hidden type="file" multiple accept=".zip,.dbf,.fpt,.dbt" onChange={onPick} />
          </Button>
          <Typography variant="body2" color="text.secondary">
            {files.length === 0 ? "No files selected" : files.map((f) => f.name).join(", ")}
          </Typography>
        </Stack>

        <Stack direction="row" spacing={2} mt={3}>
          <Button
            variant="outlined"
            startIcon={<VisibilityIcon />}
            onClick={() => preview.mutate()}
            disabled={busy || files.length === 0}
          >
            Preview
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<CloudUploadIcon />}
            onClick={() => commit.mutate()}
            disabled={busy || files.length === 0}
          >
            Import now
          </Button>
        </Stack>
      </Paper>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {summary && (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Stack direction="row" spacing={1} alignItems="center" mb={2}>
            <Typography variant="h6">
              {summary.committed ? "Import complete" : "Preview (nothing saved yet)"}
            </Typography>
            <Chip
              size="small"
              color={summary.committed ? "success" : "default"}
              label={summary.committed ? "Saved to database" : "Dry run"}
            />
          </Stack>

          <Typography variant="body2" color="text.secondary" mb={2}>
            Read from snapshot: {summary.sourceHeaderRows} employee rows,{" "}
            {summary.sourceDetailRows} pay rows, {summary.sourceDependentRows} dependent rows.
          </Typography>

          <Table size="small" sx={{ maxWidth: 480, mb: 3 }}>
            <TableBody>
              {Object.entries(summary.counts).map(([k, v]) => (
                <TableRow key={k}>
                  <TableCell>{COUNT_LABELS[k] ?? k}</TableCell>
                  <TableCell align="right"><b>{v}</b></TableCell>
                </TableRow>
              ))}
              {Object.keys(summary.counts).length === 0 && (
                <TableRow><TableCell colSpan={2}>No changes detected.</TableCell></TableRow>
              )}
            </TableBody>
          </Table>

          {summary.sample.length > 0 && (
            <>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="subtitle2" mb={1}>Sample (first {summary.sample.length})</Typography>
              <Table size="small" sx={{ mb: 3 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Number</TableCell>
                    <TableCell>Name</TableCell>
                    <TableCell>Nationality</TableCell>
                    <TableCell>Hire date</TableCell>
                    <TableCell>Pay</TableCell>
                    <TableCell>Action</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {summary.sample.map((s) => (
                    <TableRow key={s.employeeNumber}>
                      <TableCell>{s.employeeNumber}</TableCell>
                      <TableCell>{s.name}</TableCell>
                      <TableCell>{s.nationality ?? "—"}</TableCell>
                      <TableCell>{s.hireDate}</TableCell>
                      <TableCell>{s.pay ?? "—"}</TableCell>
                      <TableCell>
                        <Chip size="small" label={s.action}
                          color={s.action === "NEW" ? "primary" : "default"} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </>
          )}

          {summary.warnings.length > 0 && (
            <Alert severity="warning">
              <Typography variant="subtitle2" mb={0.5}>
                {summary.warnings.length} warning(s)
              </Typography>
              <Box component="ul" sx={{ m: 0, pl: 2 }}>
                {summary.warnings.map((w, i) => <li key={i}>{w}</li>)}
              </Box>
            </Alert>
          )}
        </Paper>
      )}
    </Box>
  );
}
