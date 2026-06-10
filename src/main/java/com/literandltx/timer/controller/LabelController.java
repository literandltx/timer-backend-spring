package com.literandltx.timer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labels")
public class LabelController {
    @GetMapping
    public ResponseEntity<Void> findAll() {
        return ResponseEntity.ok().build();
    }
}
