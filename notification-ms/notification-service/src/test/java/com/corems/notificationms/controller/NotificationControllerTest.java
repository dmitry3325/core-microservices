package com.corems.notificationms.controller;

import com.corems.notificationms.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.OffsetDateTime;


@WebMvcTest(NotificationController.class)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    public void testGetNotificationStatusListWhenAllParametersEmptyThenReturnOk() throws Exception {
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("[]"));
    }

    @Test
    public void testGetNotificationStatusListWhenAllParametersPresentThenReturnOk() throws Exception {
        // Arrange
        OffsetDateTime fromDate = OffsetDateTime.now().minusDays(1);
        OffsetDateTime toDate = OffsetDateTime.now();;

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications?type=sms&userId=user1&fromDate=" + fromDate + "&toDate=" + toDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}