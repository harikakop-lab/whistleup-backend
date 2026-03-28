package com.whistleup.backend.service;

import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.constants.ServiceIssueStatus;
import com.whistleup.backend.entity.ServiceOrder;
import com.whistleup.backend.entity.ServicePerson;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.exception.ServiceOrderNotFoundException;
import com.whistleup.backend.mapper.ServiceOrderMapper;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.repository.ServiceOrderRepository;
import com.whistleup.backend.repository.ServicePersonRepository;
import com.whistleup.backend.resource.ServiceOrderRescheduleRequest;
import com.whistleup.backend.resource.ServiceOrderResource;
import com.whistleup.backend.resource.VhsWebhookUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final ServicePersonRepository servicePersonRepository;
    private final ServiceOrderMapper serviceOrderMapper;
    private final ProfileRepository profileRepository;
    private final BuildingDetailsRepository buildingDetailsRepository;
    private final VhsBookingClient vhsBookingClient;
    private final NotificationSendService notificationSendService;

    public List<ServiceOrderResource> getAllOrdersForProfile(String profileId) {
        return getAllOrdersForProfile(profileId, null, null);
    }

    public List<ServiceOrderResource> getAllOrdersForProfile(
            String profileId,
            OrderStatus orderStatus,
            ServiceIssueStatus issueStatus) {
        log.info("Fetching orders for profileId: {} with orderStatus: {} and issueStatus: {}",
                profileId, orderStatus, issueStatus);

        List<ServiceOrder> orders;
        if (Objects.nonNull(orderStatus)) {
            orders = serviceOrderRepository.findAllByProfileIdAndOrderStatus(profileId, orderStatus);
        } else if (Objects.nonNull(issueStatus)) {
            orders = serviceOrderRepository.findAllByProfileIdAndIssueStatus(profileId, issueStatus);
        } else {
            orders = serviceOrderRepository.findAllByProfileId(profileId);
        }

        return orders.stream()
                .sorted(Comparator.comparing(ServiceOrder::getOrderCreationDate).reversed())
                .map(serviceOrderMapper::toResource)
                .collect(Collectors.toList());
    }

    public List<ServiceOrderResource> getAllOrdersForBuilding(String buildingId) {
        log.info("Fetching all orders for buildingId: {}", buildingId);
        return serviceOrderRepository.findAllByBuildingId(buildingId)
                .stream()
                .sorted(Comparator.comparing(ServiceOrder::getOrderCreationDate).reversed())
                .map(serviceOrderMapper::toResource)
                .collect(Collectors.toList());
    }

    public ServiceOrderResource getOrderByProfileAndOrderId(String profileId, String orderId) {
        log.info("Fetching orderId: {} for profileId: {}", orderId, profileId);
        ServiceOrder order = serviceOrderRepository.findById(parseOrderId(orderId, "order"))
                .filter(o -> o.getProfileId().equals(profileId))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId + " for profileId: " + profileId));
        return serviceOrderMapper.toResource(order);
    }

    @Transactional
    public ServiceOrderResource createServiceOrder(ServiceOrderResource createResource) {
        log.info("Creating service order for profileId: {}", createResource.getProfileId());
        if (createResource.getOrderType() == null) {
            createResource.setOrderType(com.whistleup.backend.constants.ServiceOrderType.CLEANER);
        }

        Profile profile = profileRepository.findByPhone(createResource.getProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for phone: " + createResource.getProfileId()));
        BuildingDetails building = buildingDetailsRepository.findById(Long.valueOf(createResource.getBuildingId()))
                .orElseThrow(() -> new IllegalArgumentException("Building not found for id: " + createResource.getBuildingId()));

        String city = building.getBuildingAddress() != null && building.getBuildingAddress().getCity() != null
                ? building.getBuildingAddress().getCity() : "Bengaluru";
        String address = building.getBuildingAddress() != null && building.getBuildingAddress().getStreetName() != null
                ? building.getBuildingAddress().getStreetName()
                : building.getBuildingName();
        String flatNo = profile.getFlatNo() == null ? "" : profile.getFlatNo();
        String externalReference = String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
        String vhsBookingId = vhsBookingClient.createBooking(
                createResource.getOptionTitle(),
                createResource.getDate(),
                createResource.getTimeSlot(),
                createResource.getAmount(),
                profile.getName(),
                city,
                address,
                flatNo,
                externalReference
        );
        var vhsBooking = vhsBookingClient.getBooking(vhsBookingId);

        ServiceOrder entity = serviceOrderMapper.toEntity(createResource);
        entity.setServicePerson(null);
        entity.setOrderStatus(OrderStatus.CREATED);
        entity.setVhsBookingId(vhsBookingId);
        entity.setVhsStatus(firstNonBlank(
                jsonText(vhsBooking, "status"),
                jsonText(vhsBooking, "booking_status"),
                "BOOKED"
        ));
        entity.setVhsServicePersonName(firstNonBlank(
                jsonText(vhsBooking, "servicePersonName"),
                jsonText(vhsBooking, "assignedTechnicianName"),
                jsonText(vhsBooking, "technician_name")
        ));
        entity.setVhsServicePersonPhone(firstNonBlank(
                jsonText(vhsBooking, "servicePersonPhone"),
                jsonText(vhsBooking, "assignedTechnicianPhone"),
                jsonText(vhsBooking, "technician_phone")
        ));
        if (entity.getVhsServicePersonName() != null) {
            entity.setOrderStatus(OrderStatus.ASSIGNED);
        }
        return serviceOrderMapper.toResource(serviceOrderRepository.save(entity));
    }

    @Transactional
    public ServiceOrderResource rescheduleOrder(String profileId, String orderId, ServiceOrderRescheduleRequest request) {
        ServiceOrder order = serviceOrderRepository.findById(parseOrderId(orderId, "order"))
                .filter(o -> o.getProfileId().equals(profileId))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId + " for profileId: " + profileId));

        if (order.getVhsBookingId() == null || order.getVhsBookingId().isBlank()) {
            throw new IllegalStateException("VHS booking id is missing for this order.");
        }
        boolean changed = false;
        if (request.getDate() != null && !request.getDate().equals(order.getDate())) {
            vhsBookingClient.changeDate(order.getVhsBookingId(), request.getDate().format(DateTimeFormatter.ISO_DATE));
            order.setDate(request.getDate());
            changed = true;
        }
        if (request.getTimeSlot() != null && !request.getTimeSlot().isBlank() && !request.getTimeSlot().equals(order.getTimeSlot())) {
            vhsBookingClient.changeSlot(order.getVhsBookingId(), request.getTimeSlot());
            order.setTimeSlot(request.getTimeSlot());
            changed = true;
        }
        if (!changed) {
            throw new IllegalArgumentException("No slot/date change provided.");
        }
        order.setVhsStatus("RESCHEDULED");
        return serviceOrderMapper.toResource(serviceOrderRepository.save(order));
    }

    @Transactional
    public ServiceOrderResource cancelOrder(String profileId, String orderId, String cancelReason) {
        ServiceOrder order = serviceOrderRepository.findById(parseOrderId(orderId, "order"))
                .filter(o -> o.getProfileId().equals(profileId))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId + " for profileId: " + profileId));

        if (order.getVhsBookingId() != null && !order.getVhsBookingId().isBlank()) {
            vhsBookingClient.cancelBooking(order.getVhsBookingId(),
                    (cancelReason == null || cancelReason.isBlank())
                            ? "Customer requested cancellation"
                            : cancelReason.trim());
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setVhsStatus("CANCELLED");
        return serviceOrderMapper.toResource(serviceOrderRepository.save(order));
    }

    @Transactional
    public ServiceOrderResource applyVhsWebhook(VhsWebhookUpdateRequest request) {
        if (request.getVhsBookingId() == null || request.getVhsBookingId().isBlank()) {
            throw new IllegalArgumentException("vhsBookingId is required");
        }
        ServiceOrder order = serviceOrderRepository.findByVhsBookingId(request.getVhsBookingId().trim())
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found for VHS booking id: " + request.getVhsBookingId()));

        OrderStatus previousOrderStatus = order.getOrderStatus();
        String previousVhs = order.getVhsStatus();
        String previousName = order.getVhsServicePersonName();
        String previousPhone = order.getVhsServicePersonPhone();

        String incomingStatus = request.getStatus() == null ? "" : request.getStatus().trim();
        if (!incomingStatus.isEmpty()) {
            order.setVhsStatus(incomingStatus);
            order.setOrderStatus(mapVhsStatusToOrderStatus(incomingStatus));
        }
        if (request.getServicePersonName() != null && !request.getServicePersonName().isBlank()) {
            order.setVhsServicePersonName(request.getServicePersonName().trim());
            order.setOrderStatus(OrderStatus.ASSIGNED);
        }
        if (request.getServicePersonPhone() != null && !request.getServicePersonPhone().isBlank()) {
            order.setVhsServicePersonPhone(request.getServicePersonPhone().trim());
        }

        ServiceOrder saved = serviceOrderRepository.save(order);

        boolean meaningfulChange =
                !Objects.equals(previousOrderStatus, saved.getOrderStatus())
                        || !Objects.equals(previousVhs, saved.getVhsStatus())
                        || !Objects.equals(previousName, saved.getVhsServicePersonName())
                        || !Objects.equals(previousPhone, saved.getVhsServicePersonPhone());
        if (meaningfulChange) {
            sendVhsStatusNotification(saved);
        }

        return serviceOrderMapper.toResource(saved);
    }

    private void sendVhsStatusNotification(ServiceOrder order) {
        String serviceLabel =
                order.getOptionTitle() != null && !order.getOptionTitle().isBlank()
                        ? order.getOptionTitle()
                        : "your home service";
        String title = "Booking update";
        String body = buildVhsNotificationBody(order, serviceLabel);
        try {
            notificationSendService.notifyUserByProfilePhone(
                    order.getProfileId(), title, body, "SERVICE_ORDER_VHS");
        } catch (Exception e) {
            log.warn(
                    "[vhs][webhook] push failed for orderId={} profileId={}: {}",
                    order.getOrderId(),
                    order.getProfileId(),
                    e.getMessage());
        }
    }

    private static String buildVhsNotificationBody(ServiceOrder order, String serviceLabel) {
        OrderStatus st = order.getOrderStatus();
        String vhs = order.getVhsStatus() != null ? order.getVhsStatus() : "";
        String tech = order.getVhsServicePersonName();
        return switch (st) {
            case ASSIGNED -> tech != null && !tech.isBlank()
                    ? String.format("%s — professional assigned: %s.", serviceLabel, tech)
                    : String.format("%s — a professional has been assigned.", serviceLabel);
            case IN_PROGRESS -> String.format("%s is in progress.", serviceLabel);
            case COMPLETED -> String.format("%s is completed. Thank you for using Nestiti.", serviceLabel);
            case CANCELLED -> String.format("%s was cancelled.", serviceLabel);
            case CONFIRMED -> String.format("%s is confirmed.", serviceLabel);
            case CREATED -> !vhs.isBlank()
                    ? String.format("%s — status: %s.", serviceLabel, vhs)
                    : String.format("%s booking was updated.", serviceLabel);
        };
    }

    @Transactional
    public ServiceOrderResource assignServicePerson(String orderId, String servicePersonId) {
        log.info("Assigning servicePersonId: {} to orderId: {}", servicePersonId, orderId);

        ServiceOrder order = serviceOrderRepository.findById(parseOrderId(orderId, "order"))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId));

        // Guard: don't re-assign if already completed or cancelled
        switch (order.getOrderStatus()) {
            case COMPLETED -> throw new IllegalStateException(
                    "Cannot assign a service person to a completed order.");
            case CANCELLED -> throw new IllegalStateException(
                    "Cannot assign a service person to a cancelled order.");
            default -> { }
        }

        ServicePerson servicePerson = servicePersonRepository.findById(parseServicePersonUuid(servicePersonId, "service person"))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Service person not found with id: " + servicePersonId));

        // Guard: service person must be active
        if (!servicePerson.getIsActive()) {
            throw new IllegalStateException(
                    "Service person with id: " + servicePersonId + " is not active.");
        }

        // Guard: service person must handle this order type
        if (!servicePerson.getServiceTypes().contains(order.getOrderType())) {
            throw new IllegalStateException(
                    "Service person does not handle order type: " + order.getOrderType());
        }

        order.setServicePerson(servicePerson);
        order.setOrderStatus(OrderStatus.ASSIGNED);

        return serviceOrderMapper.toResource(serviceOrderRepository.save(order));
    }

    @Transactional
    public ServiceOrderResource updateOrderStatus(String orderId, OrderStatus orderStatus) {
        ServiceOrder order = serviceOrderRepository.findById(parseOrderId(orderId, "order"))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId));
        order.setOrderStatus(orderStatus);
        return serviceOrderMapper.toResource(serviceOrderRepository.save(order));
    }

    @Transactional
    public ServiceOrderResource raiseIssue(String orderId, String issueText) {
        ServiceOrder order = serviceOrderRepository.findById(parseOrderId(orderId, "order"))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId));

        if (Objects.isNull(issueText) || issueText.trim().isEmpty()) {
            throw new IllegalArgumentException("Issue text cannot be empty");
        }

        order.setIssueStatus(ServiceIssueStatus.OPEN);
        order.setIssueText(issueText.trim());
        order.setIssueRaisedAt(LocalDateTime.now());
        return serviceOrderMapper.toResource(serviceOrderRepository.save(order));
    }

    @Transactional
    public ServiceOrderResource updateIssueStatus(String orderId, ServiceIssueStatus issueStatus) {
        ServiceOrder order = serviceOrderRepository.findById(parseOrderId(orderId, "order"))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId));
        order.setIssueStatus(issueStatus);
        return serviceOrderMapper.toResource(serviceOrderRepository.save(order));
    }

    @Transactional
    public void deleteServiceOrder(String orderId) {
        log.info("Deleting orderId: {}", orderId);
        ServiceOrder order = serviceOrderRepository.findById(parseOrderId(orderId, "order"))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId));
        serviceOrderRepository.delete(order);
    }

    private Long parseOrderId(String value, String fieldName) {
        try {
            return Long.valueOf(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " id: " + value);
        }
    }

    private UUID parseServicePersonUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " id: " + value);
        }
    }

    private OrderStatus mapVhsStatusToOrderStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ENGLISH);
        return switch (normalized) {
            case "BOOKED", "CREATED", "ASSIGNING_SERVICE_PERSON", "ASSIGNING" -> OrderStatus.CREATED;
            case "ASSIGNED" -> OrderStatus.ASSIGNED;
            case "CONFIRMED" -> OrderStatus.CONFIRMED;
            case "IN_PROGRESS" -> OrderStatus.IN_PROGRESS;
            case "COMPLETED", "DONE", "SUCCESS" -> OrderStatus.COMPLETED;
            case "CANCELLED", "CANCELED" -> OrderStatus.CANCELLED;
            default -> OrderStatus.CREATED;
        };
    }

    private String jsonText(com.fasterxml.jackson.databind.JsonNode node, String field) {
        if (node == null || field == null) return null;
        var value = node.get(field);
        if (value == null || value.isNull()) return null;
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}