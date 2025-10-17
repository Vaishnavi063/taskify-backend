package com.taskify.backend.models.auth;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    @Field("fullName")
    private String fullName;

    @Field("email")
    private String email;

    @Builder.Default
    private UserRoles role = UserRoles.USER;

    private String password;

    @Builder.Default
    private PricingModel pricingModel = PricingModel.FREE;

    private String avatar;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
