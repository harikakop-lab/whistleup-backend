package com.whistleup.backend.service;

import com.whistleup.backend.resource.PollRequestResource;
import com.whistleup.backend.resource.PollResponseResource;

import java.util.List;

public interface PollService {
    List<PollResponseResource> getAllPollsByBuilding(String buildingId);

    PollResponseResource createPoll(PollRequestResource pollRequestResource);

    PollResponseResource updatePoll(Long pollId, PollRequestResource pollRequestResource);

    void deletePoll(Long pollId);

    void closePoll(Long pollId, boolean isClosed);
}
