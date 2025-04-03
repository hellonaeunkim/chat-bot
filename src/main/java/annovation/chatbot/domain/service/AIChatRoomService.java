package annovation.chatbot.domain.service;

import annovation.chatbot.domain.entity.AIChatRoom;
import annovation.chatbot.domain.repository.AIChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AIChatRoomService {

    private final AIChatRoomRepository aiChatRoomRepository;

    @Transactional
    public AIChatRoom createRoom() {
        AIChatRoom aiChatRoom = AIChatRoom.builder().build();
        return aiChatRoomRepository.save(aiChatRoom);
    }
}
