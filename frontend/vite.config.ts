import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// Vite dev server proxies /api to the Spring Boot backend on :8080.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
