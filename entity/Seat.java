package com.shivam.bookMyShow.entity;

import com.shivam.bookMyShow.entity.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seats",
    uniqueConstraints = @UniqueConstraint(
        name = "unique_screen_row_seat",
        columnNames = {"screen_id", "row_number", "seat_number"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Seat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(nullable = false)
    private String rowNumber;

    @Column(nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType type;

    @CreationTimestamp private LocalDateTime createdAt;
}
