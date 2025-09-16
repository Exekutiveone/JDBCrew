package de.jdbcrew.devicebridge.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsStatusForAllKnownTargets() throws Exception {
        mockMvc.perform(get("/api/devices/pi/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value("pi"))
                .andExpect(jsonPath("$.reachable").value(true));

        mockMvc.perform(get("/api/devices/server/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value("server"))
                .andExpect(jsonPath("$.reachable").value(true));

        mockMvc.perform(get("/api/devices/aws/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value("aws"))
                .andExpect(jsonPath("$.reachable").value(true));
    }

    @Test
    void runsCommandsForDifferentTargets() throws Exception {
        String payload = "{\"command\":\"echo hi\"}";

        mockMvc.perform(post("/api/devices/server/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value("server"))
                .andExpect(jsonPath("$.reachable").value(true))
                .andExpect(jsonPath("$.data.stdout").value("(mock server) ran: echo hi"));

        mockMvc.perform(post("/api/devices/aws/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value("aws"))
                .andExpect(jsonPath("$.reachable").value(true))
                .andExpect(jsonPath("$.data.stdout").value("(mock aws) ran: echo hi"));
    }

    @Test
    void rejectsUnknownTarget() throws Exception {
        mockMvc.perform(get("/api/devices/unknown/status"))
                .andExpect(status().isBadRequest());
    }
}

