package com.example.timer_backend.mapper;

import com.example.timer_backend.config.MapperConfig;
import com.example.timer_backend.dto.timer.setting.CreateTimerSettingResponseDto;
import com.example.timer_backend.dto.timer.setting.TimerSettingResponseDto;
import com.example.timer_backend.model.TimerSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(config = MapperConfig.class)
public interface TimerSettingMapper {
    @Mappings({
            @Mapping(target = "userId", source = "user.id"),
            @Mapping(target = "timerOptionId", source = "preference.id"),
            @Mapping(target = "value", source = "preference.value")
    })
    CreateTimerSettingResponseDto toCreateTimerSettingResponse(TimerSetting entity);

    @Mappings({
            @Mapping(target = "userId", source = "user.id"),
            @Mapping(target = "timerOptionId", source = "preference.id"),
            @Mapping(target = "value", source = "preference.value")
    })
    TimerSettingResponseDto toTimerSettingResponse(TimerSetting entity);
}
