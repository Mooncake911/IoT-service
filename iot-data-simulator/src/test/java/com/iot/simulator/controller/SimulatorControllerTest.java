package com.iot.simulator.controller;

import com.iot.simulator.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulatorController.class)
@DisplayName("Simulator Controller Tests")
public class SimulatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SimulationService simulationService;

    @Test
    @DisplayName("Should configure simulation with provided parameters")
    public void config_shouldCallConfigure() throws Exception {
        int deviceCount = 20;
        int messagesPerSecond = 5;

        Map<String, Integer> config = Map.of(
                "deviceCount", deviceCount,
                "messagesPerSecond", messagesPerSecond);

        mockMvc.perform(post("/api/simulator/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk());

        verify(simulationService).configure(deviceCount, messagesPerSecond);
    }

    @Test
    @DisplayName("Should start simulation")
    public void start_shouldCallStart() throws Exception {
        mockMvc.perform(post("/api/simulator/start"))
                .andExpect(status().isOk());

        verify(simulationService).start();
    }

    @Test
    @DisplayName("Should stop simulation")
    public void stop_shouldCallStop() throws Exception {
        mockMvc.perform(post("/api/simulator/stop"))
                .andExpect(status().isOk());

        verify(simulationService).stop();
    }
}
