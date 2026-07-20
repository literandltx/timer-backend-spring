package com.literandltx.timer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
@Table(name = "timer_presets")
public class TimerPreset extends SyncEntity implements UserOwned {

    @ManyToOne
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;

    @ManyToOne
    @JoinColumn(name = "timer_option_id", nullable = false)
    private TimerOption timerOption;

    @Column(name = "last_updated", nullable = false)
    private Long lastUpdated;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
