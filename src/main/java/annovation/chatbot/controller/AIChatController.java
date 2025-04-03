package annovation.chatbot.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AIChatController {

    private final OpenAiChatModel chatClient;

    @GetMapping("/generate")
    public String generate(
            @RequestParam(
                    value = "message",
                    defaultValue = "Tell me a joke"
            )
            String message
    ) {
        return chatClient
                .call(message);
    }

    @GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateStream(
            @RequestParam(value = "message", defaultValue = "Tell me a joke") String message
    ) {
        // 프롬프트 생성 (Groq API에 보낼 메세지)
        Prompt prompt = new Prompt(List.of(new UserMessage(message)));

        // 스트리밍 처리 (각 chunk는 AI가 생성한 텍스트 일부)
        return chatClient.stream(prompt)
                .map(chunk -> { // 각 chunk를 SSE(Server-Sent Event)로 변환하는 과정
                    if (chunk.getResult() == null ||
                            chunk.getResult().getOutput() == null ||
                            chunk.getResult().getOutput().getText() == null) {
                        return ServerSentEvent.<String>builder()
                                .data("[DONE]")
                                .build();
                    }

                    String text = chunk.getResult().getOutput().getText();
                    return ServerSentEvent.<String>builder()
                            .data("\"" + text + "\"")
                            .build();
                });
    }
}