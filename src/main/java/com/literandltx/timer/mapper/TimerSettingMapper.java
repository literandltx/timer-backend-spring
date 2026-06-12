package com.literandltx.timer.mapper;

import com.literandltx.timer.config.MapperConfig;
import com.literandltx.timer.dto.settings.TimerSettingRequestDto;
import com.literandltx.timer.dto.settings.TimerSettingResponseDto;
import com.literandltx.timer.model.TimerSetting;
import com.literandltx.timer.model.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface TimerSettingMapper {

    @Mapping(source = "preference.uuid", target = "timerOptionId")
    TimerSettingResponseDto toResponseDto(TimerSetting timerSetting);

    @Mapping(target = "preference", ignore = true)
    @Mapping(target = "user", source = "authUser")
    TimerSetting toEntity(TimerSettingRequestDto request, User authUser);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "preference", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(TimerSettingRequestDto request, @MappingTarget TimerSetting timerSetting);
}
