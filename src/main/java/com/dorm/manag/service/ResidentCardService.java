package com.dorm.manag.service;

import com.dorm.manag.dto.ResidentCardDto;
import com.dorm.manag.entity.ResidentCard;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.ResidentCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResidentCardService {

    private final ResidentCardRepository residentCardRepository;

    @Transactional
    public ResidentCardDto generateCard(User user) {
        log.info("Generating resident card for user: {}", user.getUsername());

        Optional<ResidentCard> existingCard = residentCardRepository.findByUserId(user.getId());

        if (existingCard.isPresent()) {
            ResidentCard card = existingCard.get();

            if (card.isActive() && !isExpired(card)) {
                log.info("User already has an active card, returning existing card");
                return convertToDto(card);
            }

            log.info("Reactivating existing card for user: {}", user.getUsername());
            String newQrCode = generateUniqueQrCode();
            card.setQrCode(newQrCode);
            card.setActive(true);
            card.setIssuedDate(LocalDateTime.now());
            card.setExpirationDate(LocalDateTime.now().plusYears(1));

            ResidentCard savedCard = residentCardRepository.save(card);
            return convertToDto(savedCard);
        }

        String qrCode = generateUniqueQrCode();

        ResidentCard residentCard = new ResidentCard();
        residentCard.setUser(user);
        residentCard.setQrCode(qrCode);
        residentCard.setIssuedDate(LocalDateTime.now());
        residentCard.setExpirationDate(LocalDateTime.now().plusYears(1));
        residentCard.setActive(true);
        residentCard.setUsageCount(0L);
        residentCard.setAccessLevel("BASIC");

        ResidentCard savedCard = residentCardRepository.save(residentCard);
        log.info("Successfully generated new card for user: {}", user.getUsername());

        return convertToDto(savedCard);
    }

    @Transactional(readOnly = true)
    public Optional<ResidentCardDto> getCardByUser(User user) {
        return residentCardRepository.findByUserId(user.getId())
                .filter(ResidentCard::isActive)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Optional<ResidentCardDto> getCardByUserId(Long userId) {
        return residentCardRepository.findByUserId(userId)
                .filter(ResidentCard::isActive)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Optional<ResidentCardDto> verifyQrCode(String qrCode) {
        log.info("Verifying QR code: {}", qrCode);

        return residentCardRepository.findByQrCode(qrCode)
                .filter(card -> card.isActive() && !isExpired(card))
                .map(card -> {
                    // Record usage
                    card.recordUsage();
                    residentCardRepository.save(card);
                    return convertToDto(card);
                });
    }

    @Transactional
    public void deactivateCard(Long userId) {
        residentCardRepository.findByUserId(userId)
                .ifPresent(card -> {
                    card.setActive(false);
                    residentCardRepository.save(card);
                    log.info("Deactivated card for user ID: {}", userId);
                });
    }

    @Transactional(readOnly = true)
    public List<ResidentCardDto> getExpiredCards() {
        return residentCardRepository.findExpiredCards(LocalDateTime.now())
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResidentCardDto> getActiveCards() {
        return residentCardRepository.findActiveCards()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countActiveCards() {
        return residentCardRepository.countActiveCards();
    }

    private String generateUniqueQrCode() {
        String qrCode;
        do {
            qrCode = "RC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (residentCardRepository.existsByQrCode(qrCode));

        return qrCode;
    }

    private boolean isExpired(ResidentCard card) {
        return card.getExpirationDate() != null &&
                card.getExpirationDate().isBefore(LocalDateTime.now());
    }

    private ResidentCardDto convertToDto(ResidentCard card) {
        ResidentCardDto dto = new ResidentCardDto();
        dto.setId(card.getId());
        dto.setUserId(card.getUser().getId());
        dto.setUserName(card.getUser().getFirstName() + " " + card.getUser().getLastName());
        dto.setUserEmail(card.getUser().getEmail());
        dto.setRoomNumber(card.getUser().getRoomNumber());
        dto.setQrCode(card.getQrCode());
        dto.setCardNumber(card.getCardNumber());
        dto.setAccessLevel(card.getAccessLevel());
        dto.setIssuedDate(card.getIssuedDate());
        dto.setExpirationDate(card.getExpirationDate());
        dto.setActive(card.isActive());
        dto.setLastUsed(card.getLastUsed());
        dto.setUsageCount(card.getUsageCount());
        dto.setCreatedAt(card.getCreatedAt());

        dto.calculateStatus();

        return dto;
    }
}