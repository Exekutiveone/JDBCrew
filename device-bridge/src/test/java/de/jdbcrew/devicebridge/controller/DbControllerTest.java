package de.jdbcrew.devicebridge.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jdbcrew.devicebridge.service.DbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/test.db")
@AutoConfigureMockMvc
class DbControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DbService dbService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        for (String db : dbService.supportedDatabases()) {
            dbService.deleteItems(db);
        }
    }

    @Test
    void uploadStoresDataAndDataEndpointReturnsRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "items.csv",
                MediaType.TEXT_PLAIN_VALUE,
                "Alpha\nBeta\n".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/db/{db}/upload", "db1").file(file))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/db/{db}/data", "db1"))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> rows = readRows(result);
        assertThat(rows).hasSize(2);
        assertThat(rows).allMatch(row -> row.containsKey("id") && row.containsKey("name"));
        assertThat(rows).extracting(row -> row.get("name")).containsExactly("Alpha", "Beta");
    }

    @Test
    void dataEndpointAppliesFilter() throws Exception {
        dbService.replaceItems("db3", List.of("Alpha", "Beta", "Gamma"));

        MvcResult result = mockMvc.perform(get("/api/db/{db}/data", "db3").param("filter", "name=Beta"))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> rows = readRows(result);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("name", "Beta");
    }

    @Test
    void downloadReturnsCsvWithData() throws Exception {
        dbService.replaceItems("db2", List.of("ItemA", "ItemB"));

        MvcResult result = mockMvc.perform(get("/api/db/{db}/download", "db2"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("db2")))
                .andReturn();

        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(content).contains("ItemA");
        assertThat(content).contains("ItemB");
        assertThat(result.getResponse().getContentType()).startsWith("text/csv");
    }

    @Test
    void uploadRejectsEmptyFiles() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/db/{db}/upload", "db1").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownDatabaseReturns404() throws Exception {
        mockMvc.perform(get("/api/db/{db}/data", "unknown"))
                .andExpect(status().isNotFound());
    }

    private List<Map<String, Object>> readRows(MvcResult result) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<List<Map<String, Object>>>() {}
        );
    }
}
