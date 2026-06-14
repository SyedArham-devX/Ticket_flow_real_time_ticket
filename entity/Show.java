package com.shivam.bookMyShow.entity;

import com.shivam.bookMyShow.entity.enums.ShowStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "shows",
    uniqueConstraints = @UniqueConstraint(
        name = "unique_screen_date_time",
        columnNames = {"screen_id", "show_date", "start_time"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Show {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(nullable = false)
    private LocalDate showDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceRegular;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePremium;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceVip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowStatus status = ShowStatus.SCHEDULED;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
