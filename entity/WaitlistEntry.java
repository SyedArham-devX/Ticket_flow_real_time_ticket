package com.shivam.bookMyShow.entity;

import com.shivam.bookMyShow.entity.enums.WaitlistStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_entries",
    uniqueConstraints = @UniqueConstraint(
        name = "unique_user_show_waitlist",
        columnNames = {"user_id", "show_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WaitlistEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    private LocalDateTime notifiedAt;

    @CreationTimestamp private LocalDateTime createdAt;
}
