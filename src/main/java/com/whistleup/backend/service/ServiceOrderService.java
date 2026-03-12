package com.whistleup.backend.service;

import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.constants.ServiceIssueStatus;
import com.whistleup.backend.entity.ServiceOrder;
import com.whistleup.backend.entity.ServicePerson;
import com.whistleup.backend.exception.ServiceOrderNotFoundException;
import com.whistleup.backend.mapper.ServiceOrderMapper;
import com.whistleup.backend.repository.ServiceOrderRepository;
import com.whistleup.backend.repository.ServicePersonRepository;
import com.whistleup.backend.resource.ServiceOrderResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final ServicePersonRepository servicePersonRepository;
    private final ServiceOrderMapper serviceOrderMapper;

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
        ServiceOrder order = serviceOrderRepository.findById(parseUuid(orderId, "order"))
                .filter(o -> o.getProfileId().equals(profileId))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId + " for profileId: " + profileId));
        return serviceOrderMapper.toResource(order);
    }

    @Transactional
    public ServiceOrderResource createServiceOrder(ServiceOrderResource createResource) {
        log.info("Creating service order for profileId: {}", createResource.getProfileId());

        // Auto-assign an active service person based on order type
        List<ServicePerson> availablePersons = servicePersonRepository
                .findActiveByServiceType(createResource.getOrderType());

        ServicePerson assignedPerson = null;

        if (!availablePersons.isEmpty()) {
            // Pick the one with the most experience
            assignedPerson = availablePersons.stream()
                    .max(Comparator.comparingInt(ServicePerson::getExperienceYears))
                    .orElse(null);

            log.info("Auto-assigned servicePersonId: {} for orderType: {}",
                    assignedPerson.getServicePersonId(), createResource.getOrderType());
        } else {
            log.warn("No active service person available for orderType: {}", createResource.getOrderType());
        }

        ServiceOrder entity = serviceOrderMapper.toEntity(createResource);
        entity.setServicePerson(assignedPerson);
        entity.setOrderStatus(assignedPerson != null ? OrderStatus.CONFIRMED : OrderStatus.CREATED);

        return serviceOrderMapper.toResource(serviceOrderRepository.save(entity));
    }

    @Transactional
    public ServiceOrderResource assignServicePerson(String orderId, String servicePersonId) {
        log.info("Assigning servicePersonId: {} to orderId: {}", servicePersonId, orderId);

        ServiceOrder order = serviceOrderRepository.findById(parseUuid(orderId, "order"))
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

        ServicePerson servicePerson = servicePersonRepository.findById(parseUuid(servicePersonId, "service person"))
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
        ServiceOrder order = serviceOrderRepository.findById(parseUuid(orderId, "order"))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId));
        order.setOrderStatus(orderStatus);
        return serviceOrderMapper.toResource(serviceOrderRepository.save(order));
    }

    @Transactional
    public ServiceOrderResource raiseIssue(String orderId, String issueText) {
        ServiceOrder order = serviceOrderRepository.findById(parseUuid(orderId, "order"))
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
        ServiceOrder order = serviceOrderRepository.findById(parseUuid(orderId, "order"))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId));
        order.setIssueStatus(issueStatus);
        return serviceOrderMapper.toResource(serviceOrderRepository.save(order));
    }

    @Transactional
    public void deleteServiceOrder(String orderId) {
        log.info("Deleting orderId: {}", orderId);
        ServiceOrder order = serviceOrderRepository.findById(parseUuid(orderId, "order"))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Order not found with id: " + orderId));
        serviceOrderRepository.delete(order);
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " id: " + value);
        }
    }
}