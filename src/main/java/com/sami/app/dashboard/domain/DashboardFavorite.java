package com.sami.app.dashboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** A user's favorite dashboard (composite key: user + dashboard). */
@Entity
@Table(name = "dashboard_favorites")
@IdClass(DashboardFavorite.Key.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFavorite {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "dashboard_id")
    private Long dashboardId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Composite primary key. */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private Long userId;
        private Long dashboardId;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(userId, key.userId) && Objects.equals(dashboardId, key.dashboardId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, dashboardId);
        }
    }
}
