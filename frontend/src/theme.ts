import { createTheme } from "@mui/material/styles";

// Enterprise-neutral theme; refine with brand tokens later.
export const theme = createTheme({
  palette: {
    mode: "light",
    primary: { main: "#2563eb", dark: "#1d4ed8" },
    secondary: { main: "#0f766e" },
    background: { default: "#f3f6fb", paper: "#ffffff" },
    text: { primary: "#111827", secondary: "#64748b" },
  },
  shape: { borderRadius: 8 },
  typography: {
    fontFamily: ["Inter", "Segoe UI", "Roboto", "Helvetica", "Arial", "sans-serif"].join(","),
    h5: { fontWeight: 700 },
    h6: { fontWeight: 700 },
    button: { textTransform: "none", fontWeight: 700 },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: { boxShadow: "none" },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { backgroundImage: "none" },
      },
    },
    MuiTextField: {
      defaultProps: { size: "small" },
    },
  },
});
