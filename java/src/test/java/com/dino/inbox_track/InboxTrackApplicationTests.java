package com.dino.inbox_track;

import com.dino.inbox_track.service.OllamaService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

@SpringBootTest
class InboxTrackApplicationTests {


	OllamaService ollamaService;

	@Test
	void contextLoads() {
//		String response = ollamaService.classifyEmailTest("Body", "String");
//		Assert.hasLength(response, "Length");
		System.out.println("Test Methods");

	}

}
