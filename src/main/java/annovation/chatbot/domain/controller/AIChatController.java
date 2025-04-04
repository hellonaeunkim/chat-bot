package annovation.chatbot.domain.controller;

import static annovation.chatbot.domain.entity.AIChatRoom.PREVIEWS_MESSAGES_COUNT;

import annovation.chatbot.domain.dto.response.AIChatRoomMsgResponse;
import annovation.chatbot.domain.entity.AIChatRoom;
import annovation.chatbot.domain.entity.AIChatRoomMessage;
import annovation.chatbot.domain.service.AIChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AIChatController {

    private final OpenAiChatModel chatClient;
    private final AIChatRoomService aiChatRoomService;

    @ResponseStatus(HttpStatus.OK)
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

    @Operation(summary = "채팅방 대화 생성")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/generate-stream/{chatRoomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateStream(
            @PathVariable Long chatRoomId,
            @RequestParam(value = "message", defaultValue = "Tell me a joke") String message
    ) {
        // 채팅방 ID로 AIChatRoom 조회
        AIChatRoom aiChatRoom = aiChatRoomService.findById(chatRoomId);

        // aiChatRoom에 저장된 이전 메시지 전체를 가져온다
        List<AIChatRoomMessage> oldMessages = aiChatRoom.getMessages();
        // 전체 메시지 개수를 변수에 저장 (나중에 메시지의 길이 체크 등에 활용)
        int oldMessagesSize = oldMessages.size();
        // 이전 대화에서 가져올 메시지 수를 제한
        int previousMessagesSize = PREVIEWS_MESSAGES_COUNT;

        // 이전 대화 내용 가져오기 (최대 10개)
        // oldMessages 리스트에서 가장 최근의 10개 메시지를 뽑아옴
        List<Message> previousMessages = oldMessages
                // 전체 메시지 수가 10개 이상이면 최근 10개, 그렇지 않으면 처음부터 전부 사용
                .subList(Math.max(0, oldMessagesSize - previousMessagesSize), oldMessagesSize)
                .stream()
                .flatMap(msg ->
                        Stream.of(
                                new UserMessage(msg.getUserMessage()),
                                new AssistantMessage(msg.getBotMessage())
                        )
                )
                .collect(Collectors.toList()); // 최종적으로 List<Message> 형태로 수집

        // 시스템 메시지 추가 (한국인 컨텍스트)
        List<Message> messages = new ArrayList<>();
        // AI에게 대화 컨텍스트(지침)를 알려주는 시스템 메시지 추가
        messages.add(new SystemMessage("""
                당신은 한국인과 대화하고 있습니다.
                한국의 문화와 정서를 이해하고 있어야 합니다.
                최대한 한국어/영어만 사용해줘요.
                한자, 일본어 사용 자제해주세요.
                영어보다 한국어를 우선적으로 사용해줘요.
                """));

        // 가장 마지막 요약 메시지를 시스템 메시지 형태로 추가
        if (!aiChatRoom.getSummaryMessages().isEmpty()) {
            messages.add(
                    new SystemMessage(
                            "지난 대화 요약\n\n" + aiChatRoom.getSummaryMessages()
                                    .getLast().getMessage()
                    )
            );
        }

        messages.addAll(previousMessages);  // 이전 대화 메시지들을 시스템 메시지 뒤에 이어 붙임
        messages.add(new UserMessage(message)); // 사용자가 현재 입력한 메시지를 추가

        // 프롬프트 생성 (Groq API에 보낼 메세지)
        Prompt prompt = new Prompt(messages);

        // 프롬프트에 대한 응답을 받을 StringBuilder
        StringBuilder fullResponse = new StringBuilder();

        // 스트리밍 처리 (각 chunk는 AI가 생성한 텍스트 일부)
        return chatClient.stream(prompt)
                .map(chunk -> { // 각 chunk를 SSE(Server-Sent Event)로 변환하는 과정
                    if (chunk.getResult() == null ||
                            chunk.getResult().getOutput() == null ||
                            chunk.getResult().getOutput().getText() == null) {

                        aiChatRoom.addMessage(
                                message,
                                fullResponse.toString()
                        );

                        aiChatRoomService.save(aiChatRoom);

                        return ServerSentEvent.<String>builder()
                                .data("[DONE]")
                                .build();
                    }

                    String text = chunk.getResult().getOutput().getText();
                    fullResponse.append(text);
                    return ServerSentEvent.<String>builder()
                            .data("\"" + text + "\"")
                            .build();
                });
    }

    // todo : 로그인 사용자 인증 - @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "채팅방 생성")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping
    public AIChatRoom createRoom() {
        // AIChatRoom 생성
        AIChatRoom aiChatRoom = aiChatRoomService.createRoom();

        // 생성된 방의 ID를 반환
        return aiChatRoom;
    }

    @Operation(summary = "채팅방 조회")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{chatRoomId}")
    public AIChatRoom getChatRoom(@PathVariable Long chatRoomId) {
        AIChatRoom aiChatRoom = aiChatRoomService.findById(chatRoomId);

        return aiChatRoom;
    }

    @Operation(summary = "특정 사용자의 채팅방 메세지 기록 조회")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{chatRoomId}/messages")
    public List<AIChatRoomMsgResponse> getMessages(
            @PathVariable Long chatRoomId
    ) {
        AIChatRoom aiChatRoom = aiChatRoomService.findById(chatRoomId);

        return aiChatRoom.getMessages()
                .stream()
                .map(AIChatRoomMsgResponse::from)
                .toList();
    }
}