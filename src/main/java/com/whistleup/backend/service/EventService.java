package com.whistleup.backend.service;

import com.whistleup.backend.resource.EventCreateResource;
import com.whistleup.backend.resource.EventResponseResource;

import java.util.List;

public interface EventService {

    EventResponseResource createEvent(EventCreateResource request);

    EventResponseResource updateEvent(String eventId, EventCreateResource request);

    List<EventResponseResource> getEventsByProfileId(String profileId);

    List<EventResponseResource> getEventsByBuildingId(String buildingId);
}
