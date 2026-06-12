package com.literandltx.timer.mapper;

import com.literandltx.timer.config.MapperConfig;
import com.literandltx.timer.dto.entry.TimerEntryCreateRequestDto;
import com.literandltx.timer.dto.entry.TimerEntryResponseDto;
import com.literandltx.timer.model.TimerEntry;
import com.literandltx.timer.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface TimerEntryMapper {

    TimerEntry toTimerEntry(TimerEntryCreateRequestDto request, User user);

    @Mapping(source = "label.uuid", target = "labelId")
    TimerEntryResponseDto toResponseDto(TimerEntry timerEntry);

}
