package com.shivam.bookMyShow.service;
import com.shivam.bookMyShow.dto.response.WaitlistDto;
import java.util.List;
public interface WaitlistService {
    WaitlistDto joinWaitlist(Long showId);
    void leaveWaitlist(Long showId);
    WaitlistDto getMyPosition(Long showId);
    List<WaitlistDto> getWaitlistForShow(Long showId);
    void notifyWaitlist(Long showId);
}
