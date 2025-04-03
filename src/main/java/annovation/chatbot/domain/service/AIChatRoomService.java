package annovation.chatbot.domain.service;

import annovation.chatbot.domain.entity.AIChatRoom;
import annovation.chatbot.domain.repository.AIChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class AIChatRoomService {

    private final AIChatRoomRepository aiChatRoomRepository;

    @Transactional
    public AIChatRoom createRoom() {
        AIChatRoom aiChatRoom = AIChatRoom.builder().build();
        return aiChatRoomRepository.save(aiChatRoom);
    }

    public AIChatRoom findById(Long id) {
        return aiChatRoomRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방이 존재하지 않습니다."));

    }

    public void save(AIChatRoom aiChatRoom) {
        aiChatRoomRepository.save(aiChatRoom);
    }
}
