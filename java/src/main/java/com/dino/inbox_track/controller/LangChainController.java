package com.dino.inbox_track.controller;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_5;

import com.dino.inbox_track.client.CustomLLMChatModel;
import com.dino.inbox_track.dto.EmailDTO;
import com.dino.inbox_track.prompt.SubjectClassifierTemplate;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lc")
@Slf4j
public class LangChainController {

    private final ChatModel chatModel;

    public LangChainController(CustomLLMChatModel customLLMChatModel) {
        this.chatModel = customLLMChatModel;
    }

    @GetMapping("/hw")
    public String helloWorld() {
        log.info(chatModel.provider().toString());
        log.info(chatModel.supportedCapabilities().toString());
        log.info(chatModel.defaultRequestParameters().modelName());
        return chatModel.chat("Say Hello World!");
    }

    @GetMapping("/llm")
    public String helloWorldLLLm() {
        return chatModel.chat("Say Hello World!");
    }


    @GetMapping("/subject")
    public String emailSubjects() {


        List<EmailDTO> subjects = List.of(
                EmailDTO.builder().emailId("13846").subject("This is a subhect").build(),
                EmailDTO.builder().emailId("13sada846").subject("Thisfdsfsdf is a subhect").build(),
                EmailDTO.builder().emailId("13843536").subject("This is a dfg").build()
        );
        SubjectClassifierTemplate.SubjectClassifierPrompt subjectClassifierTemplate =
                new SubjectClassifierTemplate.SubjectClassifierPrompt(subjects);
        Prompt prompt = StructuredPromptProcessor.toPrompt(subjectClassifierTemplate);
        String promptText = prompt.text();
        log.info("Number of tokens: {}", new OpenAiTokenCountEstimator(GPT_5).estimateTokenCountInText(promptText));
        return chatModel.chat(promptText);
    }

}
