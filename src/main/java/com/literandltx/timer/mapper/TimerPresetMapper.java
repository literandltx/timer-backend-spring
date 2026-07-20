package com.literandltx.timer.mapper;

import com.literandltx.timer.config.MapperConfig;
import com.literandltx.timer.dto.preset.TimerPresetRequestDto;
import com.literandltx.timer.dto.preset.TimerPresetResponseDto;
import com.literandltx.timer.model.TimerPreset;
import com.literandltx.timer.model.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface TimerPresetMapper {

    @Mapping(source = "timerOption.uuid", target = "timerOptionUuid")
    @Mapping(source = "label.uuid", target = "labelUuid")
    TimerPresetResponseDto toResponseDto(TimerPreset timerPreset);

    @Mapping(target = "timerOption", ignore = true)
    @Mapping(target = "label", ignore = true)
    @Mapping(target = "user", source = "authUser")
    TimerPreset toEntity(TimerPresetRequestDto request, User authUser);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "timerOption", ignore = true)
    @Mapping(target = "label", ignore = true)
    void updateEntityFromDto(TimerPresetRequestDto request, @MappingTarget TimerPreset timerPreset);

}
