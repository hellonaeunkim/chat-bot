package annovation.chatbot.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "AICHAT_ROOM")
public class AIChatRoom {

    public static final int PREVIEWS_MESSAGES_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @CreatedDate
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime modifyDate;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AIChatRoomSummaryMessage> summaryMessages = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AIChatRoomMessage> messages = new ArrayList<>();

    public AIChatRoomMessage addMessage(String userMessage, String botMessage) {
        AIChatRoomMessage message = AIChatRoomMessage
                .builder()
                .chatRoom(this)
                .userMessage(userMessage)
                .botMessage(botMessage)
                .build();
        messages.add(message);

        addSummaryMessageIfNeeded();

        return message;
    }

    // 이 메서드는 일정 수 이상의 메시지가 쌓이면, 해당 범위를 기준으로 요약 메시지를 자동 생성하는 기능입니다.
    // 주로 대화 내용이 너무 많아질 때 LLM이 전체 맥락을 이해하기 어렵기 때문에, 이전 대화를 압축하여 기억시키는 용도로 사용됩니다.
    private void addSummaryMessageIfNeeded() {

        // 요약이 필요 없으면 바로 리턴
        // 조건 1: 전체 메시지 수가 PREVIEWS_MESSAGES_COUNT 이하일 경우 (요약할 만큼 대화가 많지 않음)
        // 조건 2: 이미 요약 메시지가 하나도 없는 경우 (처음 대화인데 요약이 필요 없는 경우)
        if (messages.size() <= PREVIEWS_MESSAGES_COUNT && summaryMessages.isEmpty()) {
            return;
        }

        // 아무 요약도 없으면, -1 넣고 시작점이 0이 되도록 함
        int lastSummaryMessageIndex =
                summaryMessages.isEmpty() ? -1 : summaryMessages.getLast().getEndMessageIndex();

        int lastSummaryMessageNo = lastSummaryMessageIndex + 1;

        // 마지막 요약 이후 추가된 메시지가 PREVIEWS_MESSAGES_COUNT 개 이하라면, 요약 안 함
        if (messages.size() - PREVIEWS_MESSAGES_COUNT <= lastSummaryMessageNo) {
            return;
        }

        // 새로운 요약 메시지에 포함될 시작 인덱스와 끝 인덱스 설정
        int startMessageIndex = lastSummaryMessageIndex + 1;
        int endMessageIndex = startMessageIndex + PREVIEWS_MESSAGES_COUNT;

        StringBuilder messageBuilder = new StringBuilder();

        // 가장 마지막 요약 내용이 있을 경우, 그것을 먼저 추가한다
        if (!summaryMessages.isEmpty()) {
            // 마지막 요약 메시지 내용 추가
            messageBuilder.append(summaryMessages.getLast().getMessage());
            messageBuilder.append("\n"); // 줄바꿈
            messageBuilder.append("\n"); // 한 줄 더 띄움 (가독성용)
        }

        // 새로운 요약 구간 시작 안내 텍스트 추가 (예: "== 10번 ~ 20번 내용 요약 ==")
        messageBuilder.append(
                "== %d번 ~ %d번 내용 요약 ==".formatted(startMessageIndex, endMessageIndex));
        messageBuilder.append("\n");

        // startMessageIndex부터 endMessageIndex 전까지 반복하면서 Q&A 형식으로 메시지 정리
        for (int i = startMessageIndex; i < endMessageIndex; i++) {
            AIChatRoomMessage message = messages.get(i); // 각 메시지 가져오기
            messageBuilder.append("Q: ").append(message.getUserMessage()).append("\n"); // 사용자 질문 추가
            messageBuilder.append("A: ").append(message.getBotMessage()).append("\n"); // 챗봇 응답 추가
            messageBuilder.append("\n"); // 각 QA 쌍 사이에 한 줄 띄움
        }

        // 요약 메시지 생성
        AIChatRoomSummaryMessage summaryMessage = AIChatRoomSummaryMessage
                .builder()
                .chatRoom(this) // 현재 채팅방과 연결
                .message(messageBuilder.toString()) // 요약 메세지
                .startMessageIndex(startMessageIndex)
                .endMessageIndex(endMessageIndex)
                .build();

        summaryMessages.add(summaryMessage);
    }
}
