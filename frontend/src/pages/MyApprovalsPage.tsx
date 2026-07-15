import {
  Alert,
  Box,
  Button,
  Chip,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import CheckIcon from "@mui/icons-material/Check";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { approvalApi } from "../api/resources";

export default function MyApprovalsPage() {
  const qc = useQueryClient();
  const { data: tasks = [], isLoading } = useQuery({ queryKey: ["myApprovalTasks"], queryFn: approvalApi.myTasks });
  const approve = useMutation({
    mutationFn: (timesheetId: string) => approvalApi.approveTimesheet(timesheetId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["myApprovalTasks"] });
      qc.invalidateQueries({ queryKey: ["timesheets"] });
      qc.invalidateQueries({ queryKey: ["timesheet"] });
    },
  });

  return (
    <Box>
      <Typography variant="h5" mb={2}>My Approvals</Typography>
      {approve.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {(approve.error as any)?.response?.data?.message ?? "Could not approve this task."}
        </Alert>
      )}
      <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "hidden" }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Process</TableCell>
              <TableCell>Project</TableCell>
              <TableCell>Employee</TableCell>
              <TableCell>Step</TableCell>
              <TableCell>Submitted</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {tasks.map((task) => (
              <TableRow key={task.stepId} hover>
                <TableCell><Chip size="small" label={task.processCode} /></TableCell>
                <TableCell>{task.projectCode ?? "-"}</TableCell>
                <TableCell>{task.employeeNumber} - {task.employeeName}</TableCell>
                <TableCell>
                  <Stack spacing={0.25}>
                    <Typography variant="body2">{task.stepName}</Typography>
                    <Typography variant="caption" color="text.secondary">Step {task.stepOrder}</Typography>
                  </Stack>
                </TableCell>
                <TableCell>{task.submittedAt ? new Date(task.submittedAt).toLocaleString() : ""}</TableCell>
                <TableCell align="right">
                  {task.entityType === "TIMESHEET" && (
                    <Button
                      size="small"
                      color="success"
                      startIcon={<CheckIcon />}
                      disabled={approve.isPending}
                      onClick={() => approve.mutate(task.entityId)}
                    >
                      Approve
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {!isLoading && tasks.length === 0 && (
              <TableRow>
                <TableCell colSpan={6}>
                  <Typography variant="body2" color="text.secondary" p={1}>No pending approvals.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
}
