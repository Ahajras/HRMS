import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider, CssBaseline } from "@mui/material";
import { useMemo, useState } from "react";
import { createAppTheme, ThemeModeContext, THEME_STORAGE_KEY, type AppThemeMode } from "./theme";
import App from "./App";
import { AuthProvider } from "./auth/AuthContext";

const queryClient = new QueryClient({
  defaultOptions: { queries: { refetchOnWindowFocus: false, retry: 1 } },
});

function Root() {
  const [mode, setMode] = useState<AppThemeMode>(() =>
    localStorage.getItem(THEME_STORAGE_KEY) === "dark" ? "dark" : "light"
  );
  const theme = useMemo(() => createAppTheme(mode), [mode]);
  const themeMode = useMemo(() => ({
    mode,
    toggleMode: () => setMode((current) => {
      const next = current === "dark" ? "light" : "dark";
      localStorage.setItem(THEME_STORAGE_KEY, next);
      return next;
    }),
  }), [mode]);

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeModeContext.Provider value={themeMode}>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <BrowserRouter>
            <AuthProvider>
              <App />
            </AuthProvider>
          </BrowserRouter>
        </ThemeProvider>
      </ThemeModeContext.Provider>
    </QueryClientProvider>
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <Root />
  </React.StrictMode>
);
