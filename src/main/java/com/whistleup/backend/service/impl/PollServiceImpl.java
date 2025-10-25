package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.PollEntity;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.PollRepository;
import com.whistleup.backend.resource.PollRequestResource;
import com.whistleup.backend.resource.PollResponseResource;
import com.whistleup.backend.service.PollService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class PollServiceImpl implements PollService {

    private final PollRepository pollRepository;

    public PollServiceImpl(PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }

    @Override
    public List<PollResponseResource> getAllPollsByBuilding(String buildingId) {
        List<PollEntity> polls = pollRepository.findAllByBuildingId(buildingId).orElse(Collections.emptyList());
        if (CollectionUtils.isEmpty(polls)) {
            return Collections.emptyList();
        }

        return polls.stream().map(pollEntity -> {
            PollResponseResource pollResponseResource = PollResponseResource.builder().build();
            BeanUtils.copyProperties(pollEntity, pollResponseResource);
            pollResponseResource.setTimestamp(pollEntity.getTimestamp().toString());
            return pollResponseResource;
        }).toList();
    }

    @Override
    public PollResponseResource createPoll(PollRequestResource pollRequestResource) {
        PollEntity pollEntity = PollEntity.builder().build();
        BeanUtils.copyProperties(pollRequestResource, pollEntity);
        pollEntity.setTimestamp(ZonedDateTime.now());
        PollEntity savedPollEntity = pollRepository.save(pollEntity);
        return getPollResponseResourceFromEntity(savedPollEntity);
    }

    @Override
    public PollResponseResource updatePoll(Long pollId, PollRequestResource pollRequestResource) {
        PollEntity pollEntity = pollRepository.findById(pollId).orElseThrow(() -> new NotFoundException("No poll found"));
        BeanUtils.copyProperties(pollRequestResource, pollEntity);
        PollEntity updatedPollEntity = pollRepository.saveAndFlush(pollEntity);
        return getPollResponseResourceFromEntity(updatedPollEntity);
    }

    @Override
    public void deletePoll(Long pollId) {
        pollRepository.deleteById(pollId);
    }

    @Override
    public void closePoll(Long pollId, boolean isClosed) {
        PollEntity pollEntity = pollRepository.findById(pollId).orElseThrow(() -> new NotFoundException("No poll found"));
        pollEntity.setClosed(Boolean.TRUE);
        pollRepository.saveAndFlush(pollEntity);
    }

    private PollResponseResource getPollResponseResourceFromEntity(PollEntity savedPollEntity) {
        PollResponseResource pollResponseResource = new PollResponseResource();
        BeanUtils.copyProperties(savedPollEntity, pollResponseResource);
        pollResponseResource.setTimestamp(savedPollEntity.getTimestamp().toString());
        return pollResponseResource;
    }
}
