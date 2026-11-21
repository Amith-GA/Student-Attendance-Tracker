package com.attendance.tracker.controller;

import com.attendance.tracker.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * AT-42: admin downloads all attendance as a CSV file.
 * Only ADMIN users can open /admin/** (see SecurityConfig).
 */
@Controller
public class AdminExportController {

    private final ExportService exportService;

    public AdminExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /** The page with the download button. */
    @GetMapping("/admin/export")
    public String page(Model model) {
        model.addAttribute("recordCount", exportService.recordCount());
        return "admin/export";
    }

    /** The download itself. "attachment" makes the browser save the file instead of showing it. */
    @GetMapping("/admin/export/attendance.csv")
    public ResponseEntity<byte[]> download() {
        // "﻿" is a byte-order mark: it makes Excel read names with accents correctly
        String csv = "﻿" + exportService.attendanceCsv();
        String fileName = "attendance-" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
