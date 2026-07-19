package com.sami.app.scheduling.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A reminder rule attached to an appointment type. */
@Entity @Table(name = "appointment_type_reminders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentTypeReminder extends BaseEntity {
    @Column(name = "appointment_type_id", nullable = false) private Long appointmentTypeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private ReminderChannel channel;

    /** Minutes before the appointment start. */
    @Column(name = "offset_minutes", nullable = false) private int offsetMinutes;
    /** Count the offset in business hours rather than wall-clock. */
    @Column(name = "use_business_time", nullable = false) private boolean useBusinessTime;
    @Column(name = "template_code", length = 64) private String templateCode;
    @Column(name = "is_active", nullable = false) private boolean isActive;
}
