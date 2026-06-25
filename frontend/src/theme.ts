import { createTheme } from "@mui/material/styles";

// Enterprise-neutral theme; refine with brand tokens later.
export const theme = createTheme({
  palette: {
    mode: "light",
    primary: { main: "#1f6feb" },
    secondary: { main: "#5a3fc0" },
    background: { default: "#f5f6f8" },
  },
  shape: { borderRadius: 8 },
  typography: {
    fontFamily: ["Inter", "Segoe UI", "Roboto", "Helvetica", "Arial", "sans-serif"].join(","),
    h6: { fontWeight: 600 },
  },
});
