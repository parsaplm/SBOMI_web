package com.parsa.middleware.service;

import com.parsa.middleware.model.StatusColor;
import com.parsa.middleware.repository.StatusColorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class StatusColorService {

    @Autowired
    private StatusColorRepository repository;

    public List<StatusColor> getAllStatusColors() {
        return repository.findAll();
    }

    public Optional<StatusColor> getStatusColor(String status) {
        return repository.findById(status);
    }



    public StatusColor updateStatusColor(String status, String color) {
        StatusColor statusColor = repository.findById(status).orElseThrow(() -> new RuntimeException("Status not found"));
        statusColor.setColor(color);
        return repository.save(statusColor);
    }

    public List<StatusColor> addStatusColors(List<StatusColor> statusColors) {
        return repository.saveAll(statusColors);
    }
}
