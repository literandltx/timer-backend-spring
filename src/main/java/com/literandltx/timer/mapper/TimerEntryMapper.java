package com.literandltx.timer.mapper;

import com.literandltx.timer.config.MapperConfig;
import com.literandltx.timer.dto.entry.TimerEntryCreateRequestDto;
import com.literandltx.timer.dto.entry.TimerEntryResponseDto;
import com.literandltx.timer.dto.entry.TimerEntryUpdateRequestDto;
import com.literandltx.timer.model.TimerEntry;
import com.literandltx.timer.model.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface TimerEntryMapper {

    @Mapping(target = "label", ignore = true)
    TimerEntry toTimerEntry(TimerEntryCreateRequestDto request, User user);

    @Mapping(source = "label.uuid", target = "labelId")
    TimerEntryResponseDto toResponseDto(TimerEntry timerEntry);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "label", ignore = true)
    void updateEntryFromDto(TimerEntryUpdateRequestDto request, @MappingTarget TimerEntry timerEntry);
}
