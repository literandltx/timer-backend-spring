package com.literandltx.timer.mapper;

import com.literandltx.timer.config.MapperConfig;
import com.literandltx.timer.dto.label.LabelCreateRequestDto;
import com.literandltx.timer.dto.label.LabelResponseDto;
import com.literandltx.timer.model.Label;
import com.literandltx.timer.model.User;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface LabelMapper {

    Label toLabel(LabelCreateRequestDto request, User user);

    LabelResponseDto toResponseDto(Label label);

}
