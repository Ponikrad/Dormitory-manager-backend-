package com.dorm.manag.service;

import com.dorm.manag.entity.ProfileImage;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.ProfileImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final ProfileImageRepository profileImageRepository;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Transactional
    public ProfileImage uploadProfileImage(User user, MultipartFile file) throws IOException {
        log.info("Uploading profile image for user: {}", user.getUsername());

        validateFile(file);

        Optional<ProfileImage> existingImage = profileImageRepository.findByUserId(user.getId());

        ProfileImage profileImage;
        if (existingImage.isPresent()) {
            profileImage = existingImage.get();
            profileImage.setFileName(file.getOriginalFilename());
            profileImage.setFileType(file.getContentType());
            profileImage.setImageData(file.getBytes());
            profileImage.setFileSize(file.getSize());
            log.info("Updating existing profile image for user: {}", user.getUsername());
        } else {
            profileImage = new ProfileImage(
                    user,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes());
            log.info("Creating new profile image for user: {}", user.getUsername());
        }

        return profileImageRepository.save(profileImage);
    }

    @Transactional(readOnly = true)
    public Optional<ProfileImage> getProfileImage(User user) {
        return profileImageRepository.findByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public Optional<ProfileImage> getProfileImageByUserId(Long userId) {
        return profileImageRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteProfileImage(User user) {
        profileImageRepository.findByUserId(user.getId())
                .ifPresent(image -> {
                    profileImageRepository.delete(image);
                    log.info("Deleted profile image for user: {}", user.getUsername());
                });
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds maximum limit of 5MB");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        boolean isValidType = false;

        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase();
            if (lowerContentType.equals("image/jpeg") ||
                    lowerContentType.equals("image/jpg") ||
                    lowerContentType.equals("image/png") ||
                    lowerContentType.contains("jpeg") ||
                    lowerContentType.contains("jpg") ||
                    lowerContentType.contains("png")) {
                isValidType = true;
            }
        }

        if (!isValidType && originalFilename != null) {
            String lowerFilename = originalFilename.toLowerCase();
            if (lowerFilename.endsWith(".jpg") ||
                    lowerFilename.endsWith(".jpeg") ||
                    lowerFilename.endsWith(".png")) {
                isValidType = true;
                log.info("File type validated by extension: {}", originalFilename);
            }
        }

        if (!isValidType) {
            log.error("Invalid file type. ContentType: {}, Filename: {}", contentType, originalFilename);
            throw new RuntimeException("Invalid file type. Only JPEG and PNG images are allowed");
        }

        log.info("File validated successfully. Type: {}, Size: {} bytes", contentType, file.getSize());
    }
}