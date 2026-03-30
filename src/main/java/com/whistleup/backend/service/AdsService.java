package com.whistleup.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AdsService {
    String uploadAdWithCity(String city, MultipartFile[] files) throws IOException;
}
