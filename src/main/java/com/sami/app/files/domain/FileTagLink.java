package com.sami.app.files.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/** Join row between a file and a tag. */
@Entity
@Table(name = "file_tag_links")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileTagLink {

    @EmbeddedId
    private Key id;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Embeddable
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Column(name = "file_id", nullable = false)
        private Long fileId;

        @Column(name = "tag_id", nullable = false)
        private Long tagId;
    }
}
