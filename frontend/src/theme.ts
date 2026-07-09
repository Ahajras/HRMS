import { createContext } from "react";
import { createTheme } from "@mui/material/styles";

export type AppThemeMode = "light" | "dark";

export const THEME_STORAGE_KEY = "hrms.themeMode";

export const ThemeModeContext = createContext({
  mode: "light" as AppThemeMode,
  toggleMode: () => {},
});

export function createAppTheme(mode: AppThemeMode) {
  const dark = mode === "dark";
  return createTheme({
    palette: {
      mode,
      primary: { main: dark ? "#60a5fa" : "#2563eb", dark: dark ? "#3b82f6" : "#1d4ed8" },
      secondary: { main: dark ? "#2dd4bf" : "#0f766e" },
      success: { main: "#16a34a" },
      warning: { main: "#d97706" },
      error: { main: "#dc2626" },
      background: {
        default: dark ? "#0b1120" : "#f3f6fb",
        paper: dark ? "#111827" : "#ffffff",
      },
      text: {
        primary: dark ? "#e5e7eb" : "#111827",
        secondary: dark ? "#94a3b8" : "#64748b",
      },
      divider: dark ? "rgba(148,163,184,.22)" : "rgba(148,163,184,.35)",
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
          root: { boxShadow: "none", borderRadius: 8 },
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
}
