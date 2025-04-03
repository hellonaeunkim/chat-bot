package annovation.chatbot.domain.dto.response;

import annovation.chatbot.domain.entity.AIChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AIChatRoomResponse {

    private long id;
    private String createDate;
    private String modifyDate;

    public static AIChatRoomResponse from(AIChatRoom chatRoom) {
        return AIChatRoomResponse.builder()
                .id(chatRoom.getId())
                .createDate(chatRoom.getCreateDate().toString())
                .modifyDate(chatRoom.getModifyDate().toString())
                .build();
    }

}
