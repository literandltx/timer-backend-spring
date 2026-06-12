package com.literandltx.timer.mapper;

import com.literandltx.timer.config.MapperConfig;
import com.literandltx.timer.dto.option.TimerOptionCreateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionResponseDto;
import com.literandltx.timer.dto.option.TimerOptionUpdateRequestDto;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface TimerOptionMapper {

    TimerOption toTimerOption(TimerOptionCreateRequestDto request, User user);

    TimerOptionResponseDto toResponseDto(TimerOption model);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateOptionFromDto(TimerOptionUpdateRequestDto request, @MappingTarget TimerOption timerOption);
}
