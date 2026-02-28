package com.whistleup.backend.mapper;

import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.entity.ServiceOrder;
import com.whistleup.backend.entity.ServicePerson;
import com.whistleup.backend.exception.ServiceOrderNotFoundException;
import com.whistleup.backend.repository.ServicePersonRepository;
import com.whistleup.backend.resource.ServiceOrderResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceOrderMapper {

    private final ServicePersonRepository servicePersonRepository;

    public ServiceOrder toEntity(ServiceOrderResource resource) {
        return ServiceOrder.builder()
                .orderType(resource.getOrderType())
                .profileId(resource.getProfileId())
                .buildingId(resource.getBuildingId())
                .date(resource.getDate())
                .orderStatus(OrderStatus.CREATED)  // service layer will override if assigned
                .build();
    }

    public ServiceOrderResource toResource(ServiceOrder entity) {
        return ServiceOrderResource.builder()
                .orderId(entity.getOrderId())
                .orderType(entity.getOrderType())
                .profileId(entity.getProfileId())
                .buildingId(entity.getBuildingId())
                .date(entity.getDate())
                .orderCreationDate(entity.getOrderCreationDate())
                .servicePersonId(entity.getServicePerson() != null
                        ? entity.getServicePerson().getServicePersonId()
                        : null)
                .orderStatus(entity.getOrderStatus())
                .build();
    }
}