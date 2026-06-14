package com.shivam.bookMyShow.entity;

import com.shivam.bookMyShow.entity.enums.ScreenType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "screens")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Screen {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScreenType type;

    @Column(nullable = false)
    private Integer totalSeats;

    @CreationTimestamp private LocalDateTime createdAt;
}
