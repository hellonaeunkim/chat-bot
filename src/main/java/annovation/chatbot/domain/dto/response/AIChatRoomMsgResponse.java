package annovation.chatbot.domain.dto.response;

import annovation.chatbot.domain.entity.AIChatRoomMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AIChatRoomMsgResponse {

    private long id;
    private long chatRoomId;
    private String createDate;
    private String modifyDate;
    private String userMessage;
    private String botMessage;

    public static AIChatRoomMsgResponse from(AIChatRoomMessage message) {
        return AIChatRoomMsgResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .createDate(message.getCreateDate().toString())
                .modifyDate(message.getModifyDate().toString())
                .userMessage(message.getUserMessage())
                .botMessage(message.getBotMessage())
                .build();
    }

}
