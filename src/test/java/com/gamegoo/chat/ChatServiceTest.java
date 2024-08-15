package com.gamegoo.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamegoo.apiPayload.code.status.ErrorStatus;
import com.gamegoo.apiPayload.exception.GeneralException;
import com.gamegoo.domain.board.Board;
import com.gamegoo.domain.chat.Chatroom;
import com.gamegoo.domain.chat.MemberChatroom;
import com.gamegoo.domain.member.LoginType;
import com.gamegoo.domain.member.Member;
import com.gamegoo.dto.chat.ChatResponse.ChatroomEnterDTO;
import com.gamegoo.repository.board.BoardRepository;
import com.gamegoo.repository.chat.ChatroomRepository;
import com.gamegoo.repository.chat.MemberChatroomRepository;
import com.gamegoo.repository.member.MemberRepository;
import com.gamegoo.service.chat.ChatCommandService;
import com.gamegoo.service.member.BlockService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
@Transactional
public class ChatServiceTest {

    @Autowired
    private ChatCommandService chatCommandService;

    @Autowired
    private BlockService blockService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatroomRepository chatroomRepository;

    @Autowired
    private MemberChatroomRepository memberChatroomRepository;

    @Autowired
    private BoardRepository boardRepository;

    private Member member1;

    private Member member2;

    private Member blindMember;

    private Board member2Board;

    @BeforeEach
    public void setMember() {
        // 기본 테스트에 사용할 멤버 객체를 미리 생성
        int randomProfileImage = ThreadLocalRandom.current().nextInt(1, 9);
        member1 = Member.builder()
            .id(1L)
            .email("test@mail.com")
            .password("12345678")
            .loginType(LoginType.GENERAL)
            .profileImage(randomProfileImage)
            .blind(false)
            .mike(false)
            .mannerLevel(1)
            .isAgree(true)
            .blockList(new ArrayList<>())
            .memberChatroomList(new ArrayList<>())
            .boardList(new ArrayList<>())
            .build();

        member2 = Member.builder()
            .id(2L)
            .email("test2@mail.com")
            .password("12345678")
            .loginType(LoginType.GENERAL)
            .profileImage(randomProfileImage)
            .blind(false)
            .mike(false)
            .mannerLevel(1)
            .isAgree(true)
            .blockList(new ArrayList<>())
            .memberChatroomList(new ArrayList<>())
            .boardList(new ArrayList<>())
            .build();

        blindMember = Member.builder()
            .id(1000L)
            .email("blind@mail.com")
            .password("12345678")
            .loginType(LoginType.GENERAL)
            .profileImage(randomProfileImage)
            .blind(true)
            .mike(false)
            .mannerLevel(1)
            .isAgree(true)
            .blockList(new ArrayList<>())
            .memberChatroomList(new ArrayList<>())
            .build();

        member1 = memberRepository.save(member1);
        member2 = memberRepository.save(member2);
        blindMember = memberRepository.save(blindMember);

        member2Board = Board.builder()
            .mode(1)
            .mainPosition(1)
            .subPosition(1)
            .wantPosition(1)
            .mike(true)
            .boardGameStyles(new ArrayList<>())
            .content("content")
            .boardProfileImage(1)
            .build();
        member2Board.setMember(member2);
        member2Board = boardRepository.save(member2Board);
    }

    @Test
    @Order(1)
    @DisplayName("1.특정 회원과 채팅 시작 - 성공 :: 기존 채팅방 없음")
    public void startChatroomByMemberIdSucceedsWhenNoExistingChatroom() throws Exception {
        // when
        ChatroomEnterDTO chatroomEnterDTO = chatCommandService.startChatroomByMemberId(
            member1.getId(), member2.getId());

        // then
        // 1. ChatroomEnterDTO의 값 검증
        assertNotNull(chatroomEnterDTO);
        assertEquals(member2.getId(), chatroomEnterDTO.getMemberId());

        // 2. 데이터베이스에서 채팅방이 실제로 생성되었는지 검증
        Optional<Chatroom> createdChatroom = chatroomRepository.findByUuid(
            chatroomEnterDTO.getUuid());
        assertTrue(createdChatroom.isPresent());

        // 3. MemberChatroom 엔티티가 각 회원에 대해 잘 생성되었는지 검증
        Optional<MemberChatroom> member1Chatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
            member1.getId(), createdChatroom.get().getId());
        Optional<MemberChatroom> member2Chatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
            member2.getId(), createdChatroom.get().getId());

        assertTrue(member1Chatroom.isPresent());
        assertTrue(member2Chatroom.isPresent());
    }

    @Test
    @Order(2)
    @DisplayName("2.특정 회원과 채팅 시작 - 성공 :: 기존 채팅방 있음")
    public void startChatroomByMemberIdSucceedsWhenExistingChatroom() throws Exception {
        //given
        ChatroomEnterDTO beforeChatroomEnterDTO = chatCommandService.startChatroomByMemberId(
            member1.getId(), member2.getId());

        // when
        ChatroomEnterDTO afterChatroomEnterDTO = chatCommandService.startChatroomByMemberId(
            member1.getId(), member2.getId());

        // then
        // 1. ChatroomEnterDTO의 값 검증
        assertNotNull(afterChatroomEnterDTO);
        assertEquals(member2.getId(), afterChatroomEnterDTO.getMemberId());

        // 2. 기존에 존재하던 채팅방에 입장된 것인지 검증
        assertTrue(beforeChatroomEnterDTO.getUuid().equals(afterChatroomEnterDTO.getUuid()));

        // 3. chatroomRepository에 1개의 데이터만 있는지 검증
        assertTrue(chatroomRepository.count() == 1);

    }

    @Test
    @Order(3)
    @DisplayName("3.특정 회원과 채팅 시작 - 성공 :: 기존 채팅방 있음 && 상대방이 나를 차단 && 이미 입장한 채팅방")
    public void startChatroomByMemberIdSucceedsWhenExistChatroomAndBlockedAndEntered()
        throws Exception {
        // given
        // 기존 채팅방 생성
        ChatroomEnterDTO oldChatroomEnterDTO = chatCommandService.startChatroomByMemberId(
            member1.getId(), member2.getId());

        Chatroom beforeChatroom = chatroomRepository.findByUuid(
            oldChatroomEnterDTO.getUuid()).get();

        MemberChatroom beforeMemberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
            member1.getId(), beforeChatroom.getId()).get();

        // 해당 채팅방 입장 처리
        beforeMemberChatroom.updateLastJoinDate(LocalDateTime.now());
        beforeMemberChatroom.updateLastViewDate(LocalDateTime.now());

        LocalDateTime beforeLastViewDate = beforeMemberChatroom.getLastViewDate();

        // 상대방이 나를 차단
        Member blockerMember = blockService.blockMember(member2.getId(), member1.getId());

        // when
        ChatroomEnterDTO newChatroomEnterDTO = chatCommandService.startChatroomByMemberId(
            member1.getId(), member2.getId());

        // then
        // 1. ChatroomEnterDTO의 값 검증
        assertNotNull(newChatroomEnterDTO);
        assertEquals(member2.getId(), newChatroomEnterDTO.getMemberId());
        assertEquals(newChatroomEnterDTO.getBlocked(), true);

        // 2. MemberChatroom 엔티티가 잘 업데이트 되었는지 확인
        MemberChatroom afterMemberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
                member1.getId(), beforeChatroom.getId())
            .orElseThrow(() -> new IllegalStateException("MemberChatroom not found"));

        // 3. MemberChatroom의 lastViewDate가 업데이트 되었는지 검증
        assertTrue(beforeLastViewDate.isBefore(afterMemberChatroom.getLastViewDate()));
    }

    @Test
    @Order(4)
    @DisplayName("4.특정 회원과 채팅 시작 - 실패 :: 동일한 회원 id로 요청")
    public void startChatroomByMemberIdFailsWhenMemberIdsAreSame() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.CHAT_TARGET_MEMBER_ID_INVALID;

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByMemberId(member1.getId(), member1.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());
    }

    @Test
    @Order(5)
    @DisplayName("5.특정 회원과 채팅 시작 - 실패 :: 채팅 대상이 존재하지 않음")
    public void startChatroomByMemberIdFailsWhenMemberNotExists() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.CHAT_TARGET_NOT_FOUND;

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByMemberId(member1.getId(), 10L);
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());
    }

    @Test
    @Order(6)
    @DisplayName("6.특정 회원과 채팅 시작 - 실패 :: 채팅 대상 회원이 탈퇴")
    public void startChatroomByMemberIdFailsWhenTargetMemberIsBlind() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.USER_DEACTIVATED;

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByMemberId(member1.getId(), blindMember.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());
    }

    @Test
    @Order(7)
    @DisplayName("7.특정 회원과 채팅 시작 - 실패 :: 기존 채팅방 없음 && 내가 상대방을 차단")
    public void startChatroomByMemberIdFailsWhenNoExistingChatroomAndBlock() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.CHAT_TARGET_IS_BLOCKED_CHAT_START_FAILED;

        Member member = blockService.blockMember(member1.getId(), member2.getId());

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByMemberId(member.getId(), member2.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());

    }

    @Test
    @Order(8)
    @DisplayName("8.특정 회원과 채팅 시작 - 실패 :: 기존 채팅방 없음 && 상대방이 나를 차단")
    public void startChatroomByMemberIdFailsWhenNoExistingChatroomAndBlocked() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.BLOCKED_BY_CHAT_TARGET_CHAT_START_FAILED;

        Member member = blockService.blockMember(member2.getId(), member1.getId());

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByMemberId(member1.getId(), member.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());

    }

    @Test
    @Order(9)
    @DisplayName("9.특정 회원과 채팅 시작 - 실패 :: 기존 채팅방 있음 && 상대방이 나를 차단 && 이미 퇴장한 채팅방")
    public void startChatroomByMemberIdFailsWhenBlockedAndExitedChatroomExists() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.BLOCKED_BY_CHAT_TARGET_CHAT_START_FAILED;
        // 기존 채팅방 먼저 생성
        ChatroomEnterDTO chatroomEnterDTO = chatCommandService.startChatroomByMemberId(
            member1.getId(), member2.getId());

        // member2 -> member1 차단
        Member blockerMember = blockService.blockMember(member2.getId(), member1.getId());

        // member1 채팅방 퇴장
        chatCommandService.exitChatroom(chatroomEnterDTO.getUuid(), member1.getId());

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByMemberId(member1.getId(), blockerMember.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());

    }


    @Test
    @Order(10)
    @DisplayName("10.특정 회원과 채팅 시작 - 실패 :: 기존 채팅방 있음 && 내가 상대방을 차단")
    public void startChatroomByMemberIdFailsWhenExistChatroomAndBlock() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.CHAT_TARGET_IS_BLOCKED_CHAT_START_FAILED;

        // 기존 채팅방 먼저 생성
        chatCommandService.startChatroomByMemberId(member1.getId(), member2.getId());

        Member blockerMember = blockService.blockMember(member1.getId(), member2.getId());

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByMemberId(blockerMember.getId(), member2.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());

    }

    @Test
    @Order(11)
    @DisplayName("11.특정 글을 보고 채팅 시작 - 성공 :: 기존 채팅방 없음")
    public void startChatroomByBoardIdSucceedsWhenNoExistingChatroom() throws Exception {
        // when
        ChatroomEnterDTO chatroomEnterDTO = chatCommandService.startChatroomByBoardId(
            member1.getId(), member2Board.getId());

        // then
        // 1. ChatroomEnterDTO의 값 검증
        assertNotNull(chatroomEnterDTO);
        assertEquals(member2.getId(), chatroomEnterDTO.getMemberId());
        assertEquals(chatroomEnterDTO.getSystem().getFlag(), 1);
        assertEquals(chatroomEnterDTO.getSystem().getBoardId(), member2Board.getId());

        // 2. 데이터베이스에서 채팅방이 실제로 생성되었는지 검증
        Optional<Chatroom> createdChatroom = chatroomRepository.findByUuid(
            chatroomEnterDTO.getUuid());
        assertTrue(createdChatroom.isPresent());

        // 3. MemberChatroom 엔티티가 각 회원에 대해 잘 생성되었는지 검증
        Optional<MemberChatroom> member1Chatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
            member1.getId(), createdChatroom.get().getId());
        Optional<MemberChatroom> member2Chatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
            member2.getId(), createdChatroom.get().getId());

        assertTrue(member1Chatroom.isPresent());
        assertTrue(member2Chatroom.isPresent());

    }

    @Test
    @Order(12)
    @DisplayName("12.특정 글을 보고 채팅 시작 - 성공 :: 기존 채팅방 있음 && 이미 입장한 채팅방")
    public void startChatroomByBoardIdSucceedsWhenExistingChatroomAndEntered() throws Exception {
        //given
        ChatroomEnterDTO beforeChatroomEnterDTO = chatCommandService.startChatroomByBoardId(
            member1.getId(), member2Board.getId());

        Chatroom beforeChatroom = chatroomRepository.findByUuid(
            beforeChatroomEnterDTO.getUuid()).get();

        MemberChatroom beforeMemberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
            member1.getId(), beforeChatroom.getId()).get();

        // 해당 채팅방 입장 처리
        beforeMemberChatroom.updateLastJoinDate(LocalDateTime.now());
        beforeMemberChatroom.updateLastViewDate(LocalDateTime.now());

        // when
        ChatroomEnterDTO afterChatroomEnterDTO = chatCommandService.startChatroomByBoardId(
            member1.getId(), member2Board.getId());

        // then
        // 1. ChatroomEnterDTO의 값 검증
        assertNotNull(afterChatroomEnterDTO);
        assertEquals(member2.getId(), afterChatroomEnterDTO.getMemberId());
        assertEquals(afterChatroomEnterDTO.getSystem().getFlag(), 2);
        assertEquals(afterChatroomEnterDTO.getSystem().getBoardId(), member2Board.getId());

        // 2. 기존에 존재하던 채팅방에 입장된 것인지 검증
        assertTrue(beforeChatroomEnterDTO.getUuid().equals(afterChatroomEnterDTO.getUuid()));

        // 3. chatroomRepository에 1개의 데이터만 있는지 검증
        assertTrue(chatroomRepository.count() == 1);

    }

    @Test
    @Order(13)
    @DisplayName("13.특정 글을 보고 채팅 시작 - 성공 :: 기존 채팅방 있음")
    public void startChatroomByBoardIdSucceedsWhenExistingChatroom() throws Exception {
        //given
        ChatroomEnterDTO beforeChatroomEnterDTO = chatCommandService.startChatroomByBoardId(
            member1.getId(), member2Board.getId());

        // when
        ChatroomEnterDTO afterChatroomEnterDTO = chatCommandService.startChatroomByBoardId(
            member1.getId(), member2Board.getId());

        // then
        // 1. ChatroomEnterDTO의 값 검증
        assertNotNull(afterChatroomEnterDTO);
        assertEquals(member2.getId(), afterChatroomEnterDTO.getMemberId());
        assertEquals(afterChatroomEnterDTO.getSystem().getFlag(), 1);
        assertEquals(afterChatroomEnterDTO.getSystem().getBoardId(), member2Board.getId());

        // 2. 기존에 존재하던 채팅방에 입장된 것인지 검증
        assertTrue(beforeChatroomEnterDTO.getUuid().equals(afterChatroomEnterDTO.getUuid()));

        // 3. chatroomRepository에 1개의 데이터만 있는지 검증
        assertTrue(chatroomRepository.count() == 1);

    }

    @Test
    @Order(14)
    @DisplayName("14.특정 글을 보고 채팅 시작 - 성공 :: 기존 채팅방 있음 && 상대방이 나를 차단 && 이미 입장한 채팅방")
    public void startChatroomByBoardIdSucceedsWhenExistChatroomAndBlockedAndEntered()
        throws Exception {
        // given
        // 기존 채팅방 생성
        ChatroomEnterDTO oldChatroomEnterDTO = chatCommandService.startChatroomByBoardId(
            member1.getId(), member2Board.getId());

        Chatroom beforeChatroom = chatroomRepository.findByUuid(
            oldChatroomEnterDTO.getUuid()).get();

        MemberChatroom beforeMemberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
            member1.getId(), beforeChatroom.getId()).get();

        // 해당 채팅방 입장 처리
        beforeMemberChatroom.updateLastJoinDate(LocalDateTime.now());
        beforeMemberChatroom.updateLastViewDate(LocalDateTime.now());

        LocalDateTime beforeLastViewDate = beforeMemberChatroom.getLastViewDate();

        // 상대방이 나를 차단
        Member blockerMember = blockService.blockMember(member2.getId(), member1.getId());

        // when
        ChatroomEnterDTO newChatroomEnterDTO = chatCommandService.startChatroomByBoardId(
            member1.getId(), member2Board.getId());

        // then
        // 1. ChatroomEnterDTO의 값 검증
        assertNotNull(newChatroomEnterDTO);
        assertEquals(member2.getId(), newChatroomEnterDTO.getMemberId());
        assertEquals(newChatroomEnterDTO.getBlocked(), true);
        assertNull(newChatroomEnterDTO.getSystem());

        // 2. MemberChatroom 엔티티가 잘 업데이트 되었는지 확인
        MemberChatroom afterMemberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
                member1.getId(), beforeChatroom.getId())
            .orElseThrow(() -> new IllegalStateException("MemberChatroom not found"));

        // 3. MemberChatroom의 lastViewDate가 업데이트 되었는지 검증
        assertTrue(beforeLastViewDate.isBefore(afterMemberChatroom.getLastViewDate()));
    }

    @Test
    @Order(15)
    @DisplayName("15.특정 글을 보고 채팅 시작 - 실패 :: 게시글 찾을 수 없음")
    public void startChatroomByBoardIdFailsWhenBoardNotFound() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.BOARD_NOT_FOUND;

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByBoardId(member1.getId(), 100L);
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());

    }

    @Test
    @Order(16)
    @DisplayName("16.특정 글을 보고 채팅 시작 - 실패 :: 게시글 작성자가 본인임")
    public void startChatroomByBoardIdFailsWhenBoardAuthorIsSelf() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.CHAT_TARGET_MEMBER_ID_INVALID;

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByBoardId(member2.getId(), member2Board.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());

    }

    @Test
    @Order(17)
    @DisplayName("17.특정 글을 보고 채팅 시작 - 실패 :: 게시글 작성자가 탈퇴함")
    public void startChatroomByBoardIdFailsWhenBoardAuthorIsBlind() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.USER_DEACTIVATED;
        member2.deactiveMember();

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByBoardId(member1.getId(), member2Board.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());

    }

    @Test
    @Order(18)
    @DisplayName("18.특정 글을 보고 채팅 시작 - 실패 :: 기존 채팅방 없음 && 내가 상대방을 차단")
    public void startChatroomByBoardIdFailsWhenNoExistingChatroomAndBlock()
        throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.CHAT_TARGET_IS_BLOCKED_CHAT_START_FAILED;
        blockService.blockMember(member1.getId(), member2.getId());

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByBoardId(member1.getId(), member2Board.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());

    }

    @Test
    @Order(19)
    @DisplayName("19.특정 글을 보고 채팅 시작 - 실패 :: 기존 채팅방 없음 && 상대방이 나를 차단")
    public void startChatroomByBoardIdFailsWhenNoExistingChatroomAndBlocked() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.BLOCKED_BY_CHAT_TARGET_CHAT_START_FAILED;
        blockService.blockMember(member2.getId(), member1.getId());

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByBoardId(member1.getId(), member2Board.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());

    }

    @Test
    @Order(20)
    @DisplayName("20.특정 글을 보고 채팅 시작 - 실패 :: 기존 채팅방 있음 && 상대방이 나를 차단 && 이미 퇴장한 채팅방")
    public void startChatroomByBoardIdFailsWhenBlockedAndExitedChatroomExists() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.BLOCKED_BY_CHAT_TARGET_CHAT_START_FAILED;
        // 기존 채팅방 먼저 생성
        ChatroomEnterDTO chatroomEnterDTO = chatCommandService.startChatroomByBoardId(
            member1.getId(), member2Board.getId());

        // member2 -> member1 차단
        Member blockerMember = blockService.blockMember(member2.getId(), member1.getId());

        // member1 채팅방 퇴장
        chatCommandService.exitChatroom(chatroomEnterDTO.getUuid(), member1.getId());

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByBoardId(member1.getId(), member2Board.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());

    }

    @Test
    @Order(21)
    @DisplayName("21.특정 글을 보고 채팅 시작 - 실패 :: 기존 채팅방 있음 && 내가 상대방을 차단")
    public void startChatroomByBoardIdFailsWhenExistingChatroomAndBlock() throws Exception {
        // given
        ErrorStatus expectedErrorCode = ErrorStatus.CHAT_TARGET_IS_BLOCKED_CHAT_START_FAILED;
        chatCommandService.startChatroomByBoardId(member1.getId(), member2Board.getId());
        blockService.blockMember(member1.getId(), member2.getId());

        // when
        GeneralException exception = assertThrows(GeneralException.class, () -> {
            chatCommandService.startChatroomByBoardId(member1.getId(), member2Board.getId());
        });

        // then
        assertEquals(expectedErrorCode, exception.getCode());

    }
}
