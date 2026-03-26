package com.rental.controller;

import com.rental.entity.Vehicle;
import com.rental.repository.VehicleTypeRepository;
import com.rental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleTypeRepository vehicleTypeRepository;

    @GetMapping("/")
    public String home(Model model, @RequestParam(required = false) Integer typeId) {
        model.addAttribute("vehicleTypes", vehicleTypeRepository.findAll());
        model.addAttribute("vehicles", vehicleService.getAvailableByType(typeId));
        model.addAttribute("selectedType", typeId);
        return "index";
    }

    @GetMapping("/vehicles/{id}")
    public String vehicleDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("vehicle", vehicleService.getById(id));
        return "vehicle-detail";
    }
}
