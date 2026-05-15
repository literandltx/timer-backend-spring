package com.example.timer_backend.mapper;

import com.example.timer_backend.config.MapperConfig;
import com.example.timer_backend.dto.timer.entry.CreateTimerEntryRequestDto;
import com.example.timer_backend.dto.timer.entry.CreateTimerEntryResponseDto;
import com.example.timer_backend.dto.timer.entry.TimerEntryRequestDto;
import com.example.timer_backend.dto.timer.entry.TimerEntryResponseDto;
import com.example.timer_backend.model.Label;
import com.example.timer_backend.model.TimerEntry;
import com.example.timer_backend.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(config = MapperConfig.class)
public interface TimerEntryMapper {
    @Mappings({
            @Mapping(target = "userId", source = "user.id"),
            @Mapping(target = "labelId", source = "label.id")
    })
    TimerEntryResponseDto toTimerEntryResponse(TimerEntry entity);

    @Mappings({
            @Mapping(target = "userId", source = "user.id"),
            @Mapping(target = "labelId", source = "label.id")
    })
    CreateTimerEntryResponseDto toCreateTimerEntryResponse(TimerEntry entity);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "user", source = "user"),
            @Mapping(target = "label", source = "label"),
            @Mapping(target = "durationSeconds", source = "dto.durationSeconds"),
            @Mapping(target = "startTime", source = "dto.startTime")
    })
    TimerEntry toTimerEntry(CreateTimerEntryRequestDto dto, User user, Label label);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "user", ignore = true),
            @Mapping(target = "label", source = "label"),
            @Mapping(target = "durationSeconds", source = "dto.durationSeconds"),
            @Mapping(target = "startTime", source = "dto.startTime")
    })
    void updateTimerEntryFromDto(TimerEntryRequestDto dto, Label label, @MappingTarget TimerEntry entity);
}
