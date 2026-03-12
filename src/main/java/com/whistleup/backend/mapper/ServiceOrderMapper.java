package com.whistleup.backend.mapper;

import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.entity.ServiceOrder;
import com.whistleup.backend.resource.ServiceOrderResource;
import org.springframework.stereotype.Component;

@Component
public class ServiceOrderMapper {

    public ServiceOrder toEntity(ServiceOrderResource resource) {
        return ServiceOrder.builder()
                .orderType(resource.getOrderType())
                .profileId(resource.getProfileId())
                .buildingId(resource.getBuildingId())
                .date(resource.getDate())
                .timeSlot(resource.getTimeSlot())
                .optionId(resource.getOptionId())
                .optionTitle(resource.getOptionTitle())
                .notes(resource.getNotes())
                .amount(resource.getAmount())
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
                .timeSlot(entity.getTimeSlot())
                .optionId(entity.getOptionId())
                .optionTitle(entity.getOptionTitle())
                .notes(entity.getNotes())
                .amount(entity.getAmount())
                .orderCreationDate(entity.getOrderCreationDate())
                .servicePersonId(entity.getServicePerson() != null
                        ? entity.getServicePerson().getServicePersonId()
                        : null)
                .servicePersonName(entity.getServicePerson() != null
                        ? entity.getServicePerson().getName()
                        : null)
                .servicePersonPhone(entity.getServicePerson() != null
                        ? entity.getServicePerson().getPhoneNumber()
                        : null)
                .servicePersonRating(entity.getServicePerson() != null
                        ? entity.getServicePerson().getRating()
                        : null)
                .orderStatus(entity.getOrderStatus())
                .issueStatus(entity.getIssueStatus())
                .issueText(entity.getIssueText())
                .issueRaisedAt(entity.getIssueRaisedAt())
                .build();
    }
}