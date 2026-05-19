package com.codeit.monew.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class BaseSoftDeleteEntity {

    public static final String IS_DELETED_FALSE = "is_deleted = false";

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
