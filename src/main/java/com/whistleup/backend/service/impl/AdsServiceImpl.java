package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.Advertisement;
import com.whistleup.backend.entity.ComplaintImage;
import com.whistleup.backend.repository.AdsRepository;
import com.whistleup.backend.service.AdsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdsServiceImpl implements AdsService {

    private final AdsRepository adsRepository;

    @Override
    public String uploadAdWithCity(String city, MultipartFile[] files) throws IOException {
        try {
            if (files != null && files.length > 0) {
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }
                    String contentType = file.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        continue;
                    }
                    Advertisement advertisement = new Advertisement();
                    Advertisement image = Advertisement.builder()
                            .imageData(file.getBytes())
                            .fileName(file.getOriginalFilename())
                            .contentType(file.getContentType())
                            .build();
                    adsRepository.save(advertisement);
                }
            }
            return "Success";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
