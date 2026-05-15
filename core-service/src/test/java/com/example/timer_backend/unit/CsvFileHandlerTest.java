package com.example.timer_backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.timer_backend.exception.custom.FileProcessingException;
import com.example.timer_backend.exception.custom.FileStorageException;
import com.example.timer_backend.exception.custom.InvalidFileFormatException;
import com.example.timer_backend.model.Label;
import com.example.timer_backend.model.TimerEntry;
import com.example.timer_backend.provider.FileType;
import com.example.timer_backend.service.impl.CsvFileHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class CsvFileHandlerTest {

    @InjectMocks
    private CsvFileHandler csvFileHandler;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getSupportedType_ReturnsCsv() {
        FileType result = csvFileHandler.getSupportedType();
        assertEquals(FileType.CSV, result);
    }

    @Test
    void exportFile_ValidEntries_ReturnsCsvBytes() {
        // 1. Arrange
        Label mockLabel = mock(Label.class);
        when(mockLabel.getName()).thenReturn("Work");
        when(mockLabel.getColor()).thenReturn("#FF0000");

        TimerEntry mockEntry = mock(TimerEntry.class);
        when(mockEntry.getLabel()).thenReturn(mockLabel);
        when(mockEntry.getDurationSeconds()).thenReturn(3600L);
        when(mockEntry.getStartTime()).thenReturn(1672531200000L);

        List<TimerEntry> entries = List.of(mockEntry);

        // 2. Act
        byte[] result = csvFileHandler.exportFile(entries);
        String csvOutput = new String(result);

        // 3. Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        assertTrue(csvOutput.contains("labelName,color,durationSeconds,startTime"));
        assertTrue(csvOutput.contains("Work,\"#FF0000\",3600,1672531200000"));
    }

    @Test
    void exportFile_EmptyEntries_ReturnsOnlyHeaders() {
        // 1. Arrange
        List<TimerEntry> emptyEntries = Collections.emptyList();

        // 2. Act
        byte[] result = csvFileHandler.exportFile(emptyEntries);
        String csvOutput = new String(result);

        // 3. Assert
        assertNotNull(result);
        assertTrue(csvOutput.contains("labelName,color,durationSeconds,startTime"));
        assertEquals(1, csvOutput.trim().split("\n").length);
    }

    @Test
    void importFile_ValidCsvContent_ReturnsTimerEntries() throws IOException {
        // 1. Arrange
        String csvContent = "labelName,color,durationSeconds,startTime\n"
                + "Work,#FF0000,3600,1672531200000\n"
                + "Rest,#00FF00,600,1672534800000";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getInputStream()).thenReturn(inputStream);

        // 2. Act
        List<TimerEntry> result = csvFileHandler.importFile(mockFile);

        // 3. Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        TimerEntry firstEntry = result.get(0);
        assertEquals("Work", firstEntry.getLabel().getName());
        assertEquals("#FF0000", firstEntry.getLabel().getColor());
        assertEquals(3600L, firstEntry.getDurationSeconds());
        assertEquals(1672531200000L, firstEntry.getStartTime());

        TimerEntry secondEntry = result.get(1);
        assertEquals("Rest", secondEntry.getLabel().getName());
        assertEquals("#00FF00", secondEntry.getLabel().getColor());
        assertEquals(600L, secondEntry.getDurationSeconds());
        assertEquals(1672534800000L, secondEntry.getStartTime());
    }

    @Test
    void importFile_EmptyCsvWithHeaders_ReturnsEmptyList() throws IOException {
        // 1. Arrange
        String csvContent = "labelName,color,durationSeconds,startTime\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getInputStream()).thenReturn(inputStream);

        // 2. Act
        List<TimerEntry> result = csvFileHandler.importFile(mockFile);

        // 3. Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void importFile_InvalidDataFormat_ThrowsInvalidFileFormatException() throws IOException {
        // 1. Arrange
        String csvContent = "labelName,color,durationSeconds,startTime\n"
                + "Work,#FF0000,NOT_A_NUMBER,1672531200000\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getInputStream()).thenReturn(inputStream);

        // 2. Act & 3. Assert
        InvalidFileFormatException exception = assertThrows(
                InvalidFileFormatException.class,
                () -> csvFileHandler.importFile(mockFile)
        );

        assertTrue(exception.getMessage().contains("Error parsing row in CSV file"));
    }

    @Test
    void importFile_IoExceptionOnRead_ThrowsFileStorageException() throws IOException {
        // 1. Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("broken_file.csv");
        when(mockFile.getInputStream()).thenThrow(new IOException("Simulated IO failure"));

        // 2. Act & 3. Assert
        FileStorageException exception = assertThrows(
                FileStorageException.class,
                () -> csvFileHandler.importFile(mockFile)
        );

        assertEquals("Could not read the uploaded file: broken_file.csv", exception.getMessage());
    }

    @Test
    void exportFile_ThrowsJsonProcessingException_WrapsInFileProcessingException() throws Exception {
        // 1. Arrange
        Label mockLabel = mock(Label.class);
        TimerEntry mockEntry = mock(TimerEntry.class);
        when(mockEntry.getLabel()).thenReturn(mockLabel);
        List<TimerEntry> entries = List.of(mockEntry);
        CsvMapper mockMapper = mock(CsvMapper.class, RETURNS_DEEP_STUBS);
        JsonProcessingException mockException = mock(JsonProcessingException.class);
        when(mockMapper.writer(any(CsvSchema.class)).writeValueAsBytes(any())).thenThrow(mockException);
        ReflectionTestUtils.setField(csvFileHandler, "csvMapper", mockMapper);

        // 4. Act & Assert
        FileProcessingException exception = assertThrows(
                FileProcessingException.class,
                () -> csvFileHandler.exportFile(entries)
        );

        assertEquals("Internal error occurred while generating CSV export", exception.getMessage());
        assertEquals(mockException, exception.getCause());
    }
}
