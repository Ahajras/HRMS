package com.hrms.migration.web;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.migration.dto.ImportSummary;
import com.hrms.migration.dto.LegacyRawDto;
import com.hrms.migration.service.LegacyImportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Admin "Import from legacy system" endpoints. Accepts the legacy FoxPro/DBF
 * snapshot (uploaded as a single .zip of the dated folder, or the individual
 * .dbf files) and loads it into the new HRMS — the web equivalent of the CLI
 * cutover tool. The import is idempotent, so it is safe to re-run after each
 * fresh export from the still-running legacy system.
 *
 * <p>Two steps mirror the CLI {@code --dry-run} then real load:
 * {@code /preview} reports what would change without writing; {@code /} commits.
 * Both require a valid token and a company scope (X-Company-Id / JWT cid).
 */
@RestController
@RequestMapping("/api/v1/legacy-import")
public class LegacyImportController {

    private final LegacyImportService service;

    public LegacyImportController(LegacyImportService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    public ImportSummary preview(@RequestParam("files") MultipartFile[] files) {
        return process(files, false);
    }

    @PostMapping
    public ImportSummary commit(@RequestParam("files") MultipartFile[] files) {
        return process(files, true);
    }

    /** Full legacy snapshot (header + all detail lines) for one employee. */
    @GetMapping("/raw/{employeeId}")
    public LegacyRawDto raw(@PathVariable UUID employeeId) {
        return service.getRaw(employeeId);
    }

    private ImportSummary process(MultipartFile[] files, boolean commit) {
        if (files == null || files.length == 0) {
            throw new BusinessRuleException("import.no.files", "No files were uploaded.");
        }
        Path tmp;
        try {
            tmp = Files.createTempDirectory("legacy-import-");
        } catch (IOException e) {
            throw new IllegalStateException("Could not create a temp working directory", e);
        }
        try {
            for (MultipartFile file : files) {
                String name = sanitize(file.getOriginalFilename());
                if (name == null) {
                    continue;
                }
                if (name.toLowerCase().endsWith(".zip")) {
                    unzip(file, tmp);
                } else {
                    file.transferTo(tmp.resolve(name).toFile());
                }
            }
            return service.run(tmp.toFile(), commit);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read the uploaded files: " + e.getMessage(), e);
        } finally {
            deleteRecursively(tmp);
        }
    }

    private static void unzip(MultipartFile zip, Path target) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zip.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = sanitize(entry.getName());
                if (name == null) {
                    continue;
                }
                Path out = target.resolve(name);
                Files.copy(zis, out);
            }
        }
    }

    /** Strip any directory part to defend against zip-slip; keep the base name. */
    private static String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String base = raw.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.trim();
        return base.isEmpty() ? null : base;
    }

    private static void deleteRecursively(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            });
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
