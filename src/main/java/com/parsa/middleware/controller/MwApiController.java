package com.parsa.middleware.controller;


import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.service.ImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MwApiController {
    @Autowired
    private ImportService importService;
    @GetMapping("/import")
    public ResponseEntity<String> startImport() {
        importService.refreshToDoFolderAndImport();
        return ResponseEntity.ok("Import process triggered successfully");
    }

    @PostMapping("/cancelImport/{taskId}")
    public ResponseEntity<String> cancelImport(@PathVariable int taskId) {
        importService.cancelImport(taskId);
        return ResponseEntity.ok("Import cancellation requested for Task ID: " + taskId);
    }

    @PostMapping("/setFavorite/{taskId}")
    public ResponseEntity<String> setFavorite(@PathVariable int taskId) {
        importService.setFavorite(taskId);
        return ResponseEntity.ok("Import cancellation requested for Task ID: " + taskId);
    }

    @PutMapping("/{taskId}/status/{oldStatus}/{newStatus}")
    public ResponseEntity<String> updateQueueStatus(@PathVariable int taskId,
                                                    @PathVariable ImportStatus oldStatus,
                                                    @PathVariable ImportStatus newStatus) {
        boolean success = importService.updateQueueStatus(taskId, oldStatus, newStatus);
        if (success) {
            return ResponseEntity.ok("Queue status updated successfully to: " + newStatus);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to update queue status");
        }
    }

}
