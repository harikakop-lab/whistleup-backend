//package com.whistleup.backend.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@Table(name = "complaint_images")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ComplaintImage {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "complaint_id", nullable = false)
//    private Complaints complaint;
//
//    @Lob
//    @Column(name = "image_data", nullable = false)
//    private byte[] imageData;
//
//    @Column(name = "file_name")
//    private String fileName;
//
//    @Column(name = "content_type")
//    private String contentType;
//}
