package com.whistleup.backend.service;

import com.whistleup.backend.entity.Event;
import com.whistleup.backend.repository.EventRepository;
import com.whistleup.backend.resource.EventCreateResource;
import com.whistleup.backend.resource.EventResponseResource;
import com.whistleup.backend.service.EventService;
import com.whistleup.backend.repository.ProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final ProfileRepository profileRepository;

    public EventServiceImpl(EventRepository eventRepository, ProfileRepository profileRepository) {
        this.eventRepository = eventRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    public EventResponseResource createEvent(EventCreateResource request) {
        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .profileId(request.getProfileId())
                .buildingId(resolveBuildingId(request))
                .createdAt(LocalDateTime.now())
                .build();

        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    @Override
    public EventResponseResource updateEvent(String eventId, EventCreateResource request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setBuildingId(resolveBuildingId(request));
        event.setUpdatedAt(LocalDateTime.now());

        Event updated = eventRepository.save(event);
        return mapToResponse(updated);
    }

    @Override
    public List<EventResponseResource> getEventsByProfileId(String profileId) {
        return eventRepository.findByProfileIdOrderByCreatedAtDesc(profileId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<EventResponseResource> getEventsByBuildingId(String buildingId) {
        return eventRepository.findByBuildingIdOrderByCreatedAtDesc(buildingId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private EventResponseResource mapToResponse(Event event) {
        return EventResponseResource.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .description(event.getDescription())
                .profileId(event.getProfileId())
                .buildingId(event.getBuildingId())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private String resolveBuildingId(EventCreateResource request) {
        if (request.getBuildingId() != null && !request.getBuildingId().isBlank()) {
            return request.getBuildingId();
        }
        if (request.getProfileId() != null && !request.getProfileId().isBlank()) {
            return profileRepository.findByPhone(request.getProfileId())
                    .map(p -> p.getBuildingId())
                    .orElse(null);
        }
        return null;
    }
}
