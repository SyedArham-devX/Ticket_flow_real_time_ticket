package com.shivam.bookMyShow.service.impl;

import com.shivam.bookMyShow.dto.response.WaitlistDto;
import com.shivam.bookMyShow.entity.*;
import com.shivam.bookMyShow.entity.enums.WaitlistStatus;
import com.shivam.bookMyShow.exception.*;
import com.shivam.bookMyShow.repository.*;
import com.shivam.bookMyShow.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.shivam.bookMyShow.util.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistServiceImpl implements WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final ShowRepository showRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public WaitlistDto joinWaitlist(Long showId) {
        User user = getCurrentUser();
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        if (waitlistRepository.findByUserAndShow(user, show).isPresent()) {
            throw new BadRequestException("You are already on the waitlist for this show");
        }

        int nextPosition = waitlistRepository.findMaxPositionByShow(show).orElse(0) + 1;

        WaitlistEntry entry = WaitlistEntry.builder()
                .user(user).show(show).position(nextPosition)
                .status(WaitlistStatus.WAITING).build();

        return modelMapper.map(waitlistRepository.save(entry), WaitlistDto.class);
    }

    @Override
    @Transactional
    public void leaveWaitlist(Long showId) {
        User user = getCurrentUser();
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
        WaitlistEntry entry = waitlistRepository.findByUserAndShow(user, show)
                .orElseThrow(() -> new ResourceNotFoundException("You are not on the waitlist for this show"));
        waitlistRepository.delete(entry);
    }

    @Override
    public WaitlistDto getMyPosition(Long showId) {
        User user = getCurrentUser();
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
        WaitlistEntry entry = waitlistRepository.findByUserAndShow(user, show)
                .orElseThrow(() -> new ResourceNotFoundException("You are not on the waitlist for this show"));
        return modelMapper.map(entry, WaitlistDto.class);
    }

    @Override
    public List<WaitlistDto> getWaitlistForShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
        return waitlistRepository.findByShowAndStatusOrderByPositionAsc(show, WaitlistStatus.WAITING)
                .stream().map(e -> modelMapper.map(e, WaitlistDto.class)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void notifyWaitlist(Long showId) {
        Show show = showRepository.findById(showId).orElse(null);
        if (show == null) return;
        List<WaitlistEntry> waiting = waitlistRepository
                .findByShowAndStatusOrderByPositionAsc(show, WaitlistStatus.WAITING);
        if (waiting.isEmpty()) return;
        // Notify first person in queue
        WaitlistEntry first = waiting.get(0);
        first.setStatus(WaitlistStatus.NOTIFIED);
        first.setNotifiedAt(LocalDateTime.now());
        waitlistRepository.save(first);
        log.info("Notified waitlist user {} for show {}", first.getUser().getId(), showId);
    }
}
