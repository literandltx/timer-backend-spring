package com.literandltx.timer.mapper;

import com.literandltx.timer.config.MapperConfig;
import com.literandltx.timer.dto.option.TimerOptionCreateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionResponseDto;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.User;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface TimerOptionMapper {

    TimerOption toTimerEntry(TimerOptionCreateRequestDto request, User user);

    TimerOptionResponseDto toResponseDto(TimerOption model);

}
