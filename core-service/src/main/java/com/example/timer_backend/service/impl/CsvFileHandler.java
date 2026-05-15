package com.example.timer_backend.service.impl;

import com.example.timer_backend.dto.export.TimerEntryCsvDto;
import com.example.timer_backend.exception.custom.FileProcessingException;
import com.example.timer_backend.exception.custom.FileStorageException;
import com.example.timer_backend.exception.custom.InvalidFileFormatException;
import com.example.timer_backend.model.Label;
import com.example.timer_backend.model.TimerEntry;
import com.example.timer_backend.provider.FileType;
import com.example.timer_backend.service.FileHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CsvFileHandler implements FileHandler {
    private final CsvMapper csvMapper = new CsvMapper();

    @Override
    public List<TimerEntry> importFile(MultipartFile file) {
        CsvSchema schema = csvMapper.schemaFor(TimerEntryCsvDto.class)
                .withHeader()
                .withColumnSeparator(',');

        try (
                InputStream is = file.getInputStream();
                MappingIterator<TimerEntryCsvDto> it = csvMapper
                        .readerFor(TimerEntryCsvDto.class)
                        .with(schema)
                        .readValues(is)
        ) {
            List<TimerEntry> entries = new ArrayList<>();
            while (it.hasNext()) {
                try {
                    TimerEntryCsvDto dto = it.next();
                    entries.add(TimerEntry.builder()
                            .label(Label.builder()
                                    .name(dto.getLabelName())
                                    .color(dto.getColor())
                                    .build())
                            .durationSeconds(dto.getDurationSeconds())
                            .startTime(dto.getStartTime())
                            .build());
                } catch (RuntimeException e) {
                    long rowNumber = it.getCurrentLocation().getLineNr();
                    throw new InvalidFileFormatException("Error parsing row in CSV file. Invalid data at row: " + rowNumber, e);
                }
            }
            return entries;

        } catch (IOException e) {
            throw new FileStorageException("Could not read the uploaded file: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public byte[] exportFile(List<TimerEntry> entries) {
        List<TimerEntryCsvDto> dtos = entries.stream()
                .map(e -> new TimerEntryCsvDto(
                        e.getLabel().getName(),
                        e.getLabel().getColor(),
                        e.getDurationSeconds(),
                        e.getStartTime()))
                .toList();

        CsvSchema schema = csvMapper.schemaFor(TimerEntryCsvDto.class).withHeader();
        try {
            return csvMapper.writer(schema).writeValueAsBytes(dtos);
        } catch (JsonProcessingException e) {
            throw new FileProcessingException("Internal error occurred while generating CSV export", e);
        }
    }

    @Override
    public FileType getSupportedType() {
        return FileType.CSV;
    }
}
