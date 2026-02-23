package com.dino.inbox_track;

import com.dino.inbox_track.service.OllamaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
public class ServiceTest {

    @Mock
    RestClient restClient;
    OllamaService ollamaService = new OllamaService(restClient);

    @Test
    void contextLoads() {
        String response = ollamaService.classifyEmailTest("Body", "String");
        Assert.hasLength(response, "Length");

    }

}
