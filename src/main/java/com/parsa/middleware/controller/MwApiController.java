package com.parsa.middleware.controller;


import com.parsa.middleware.service.ImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MwApiController {
    @Autowired
    private ImportService importService;
    @GetMapping("/import")
    public ResponseEntity<String> startImport() {
        importService.importData();
        return ResponseEntity.ok("Import process triggered successfully");
    }

}
