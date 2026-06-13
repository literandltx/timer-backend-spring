package com.literandltx.timer.dto.sync;

public record SyncMessage<T>(
        SyncAction action,
        T payload
) {
}