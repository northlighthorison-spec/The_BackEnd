package com.wha.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "site_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SiteConfig {

    @Id
    @Column(length = 60)
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(length = 200)
    private String label;

    @Column(length = 20)
    @Builder.Default
    private String type = "text";

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
