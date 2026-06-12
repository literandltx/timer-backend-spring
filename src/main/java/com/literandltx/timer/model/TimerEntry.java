package com.literandltx.timer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "timer_entries", uniqueConstraints = {
        @UniqueConstraint(
                name = "uc_timerentry_user_starttime",
                columnNames = {"user_id", "start_time"}
        )
})
public class TimerEntry extends SyncEntity implements UserOwned {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Label label;

    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds;

    @Column(name = "start_time", nullable = false)
    private Long startTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
