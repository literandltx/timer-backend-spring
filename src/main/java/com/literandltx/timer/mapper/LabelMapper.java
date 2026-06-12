package com.literandltx.timer.mapper;

import com.literandltx.timer.config.MapperConfig;
import com.literandltx.timer.dto.label.LabelCreateRequestDto;
import com.literandltx.timer.dto.label.LabelResponseDto;
import com.literandltx.timer.dto.label.LabelUpdateRequestDto;
import com.literandltx.timer.model.Label;
import com.literandltx.timer.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(config = MapperConfig.class)
public interface LabelMapper {

    @Mappings({
            @Mapping(target = "user", source = "user")
    })
    Label toLabel(LabelCreateRequestDto request, User user);

    LabelResponseDto toResponseDto(Label label);

    @Mappings({
            @Mapping(target = "uuid", ignore = true),
            @Mapping(target = "user", ignore = true),
            @Mapping(target = "deleted", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
    })
    void updateLabelFromDto(LabelUpdateRequestDto request, @MappingTarget Label label);
}
