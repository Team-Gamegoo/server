package com.gamegoo.service.chat;

import com.gamegoo.apiPayload.code.status.ErrorStatus;
import com.gamegoo.apiPayload.exception.handler.BoardHandler;
import com.gamegoo.apiPayload.exception.handler.ChatHandler;
import com.gamegoo.converter.ChatConverter;
import com.gamegoo.domain.board.Board;
import com.gamegoo.domain.chat.Chat;
import com.gamegoo.domain.chat.Chatroom;
import com.gamegoo.domain.chat.MemberChatroom;
import com.gamegoo.domain.member.Member;
import com.gamegoo.dto.chat.ChatRequest;
import com.gamegoo.dto.chat.ChatRequest.SystemFlagRequest;
import com.gamegoo.dto.chat.ChatResponse;
import com.gamegoo.dto.chat.ChatResponse.ChatroomEnterDTO;
import com.gamegoo.repository.board.BoardRepository;
import com.gamegoo.repository.chat.ChatRepository;
import com.gamegoo.repository.chat.ChatroomRepository;
import com.gamegoo.repository.chat.MemberChatroomRepository;
import com.gamegoo.repository.member.MemberRepository;
import com.gamegoo.service.member.FriendService;
import com.gamegoo.service.member.ProfileService;
import com.gamegoo.service.socket.SocketService;
import com.gamegoo.util.MemberUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatCommandService {

    private final ProfileService profileService;
    private final FriendService friendService;
    private final SocketService socketService;
    private final MemberRepository memberRepository;
    private final MemberChatroomRepository memberChatroomRepository;
    private final ChatroomRepository chatroomRepository;
    private final ChatRepository chatRepository;
    private final BoardRepository boardRepository;

    private static final String POST_SYSTEM_MESSAGE_TO_MEMBER_INIT = "상대방이 게시한 글을 보고 말을 걸었어요. 대화를 시작해보세요~";
    private static final String POST_SYSTEM_MESSAGE_TO_MEMBER = "상대방이 게시한 글을 보고 말을 걸었어요.";
    private static final String POST_SYSTEM_MESSAGE_TO_TARGET_MEMBER = "내가 게시한 글을 보고 말을 걸어왔어요.";
    private static final String MATCHING_SYSTEM_MESSAGE = "상대방과 매칭이 이루어졌어요!";


    /**
     * 대상 회원과 채팅 시작: 기존에 채팅방이 있는 경우 채팅방 입장 처리, 기존에 채팅방이 없는 경우 새로운 채팅방 생성
     *
     * @param request
     * @param memberId
     * @return
     */
    public ChatResponse.ChatroomEnterDTO startChatroomByMemberId(Long memberId,
        Long targetMemberId) {
        Member member = profileService.findMember(memberId);

        // 채팅 대상 회원의 존재 여부 검증
        Member targetMember = memberRepository.findById(targetMemberId)
            .orElseThrow(() -> new ChatHandler(ErrorStatus.CHAT_TARGET_NOT_FOUND));

        // 채팅 대상으로 자기 자신을 요청한 경우
        if (member.equals(targetMember)) {
            throw new ChatHandler(ErrorStatus.CHAT_TARGET_MEMBER_ID_INVALID);
        }

        // 채팅 대상 회원이 탈퇴한 경우
        MemberUtils.checkBlind(targetMember);

        // 내가 채팅 대상 회원을 차단한 경우, 기존 채팅방 입장 및 새 채팅방 생성 모두 불가
        if (MemberUtils.isBlocked(member, targetMember)) {
            throw new ChatHandler(ErrorStatus.CHAT_TARGET_IS_BLOCKED_CHAT_START_FAILED);
        }

        // 채팅 대상 회원과의 chatroom 존재 여부 조회
        Optional<Chatroom> chatroom = chatroomRepository.findChatroomByMemberIds(
            member.getId(), targetMember.getId());

        // 기존에 채팅방이 존재하는 경우: 메시지 내역, 상대 회원 정보 조회 및 해당 채팅방 입장 처리
        if (chatroom.isPresent()) {
            MemberChatroom memberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
                    member.getId(), chatroom.get().getId())
                .orElseThrow(() -> new ChatHandler(ErrorStatus.CHATROOM_ACCESS_DENIED));

            // 채팅 대상 회원이 나를 차단함 && 내가 해당 채팅방을 퇴장한 상태인 경우
            if (MemberUtils.isBlocked(targetMember, member)
                && memberChatroom.getLastJoinDate() == null) {
                throw new ChatHandler(ErrorStatus.BLOCKED_BY_CHAT_TARGET_CHAT_START_FAILED);
            }

            // 최근 메시지 내역 조회
            Slice<Chat> recentChats = chatRepository.findRecentChats(chatroom.get().getId(),
                memberChatroom.getId(), member.getId());

            // 해당 채팅방의 lastViewDate 업데이트
            memberChatroom.updateLastViewDate(LocalDateTime.now());

            // ChatMessageListDTO 생성
            ChatResponse.ChatMessageListDTO chatMessageListDTO = ChatConverter.toChatMessageListDTO(
                recentChats);

            return ChatroomEnterDTO.builder()
                .uuid(chatroom.get().getUuid())
                .memberId(targetMember.getId())
                .gameName(targetMember.getGameName())
                .memberProfileImg(targetMember.getProfileImage())
                .friend(friendService.isFriend(member, targetMember))
                .blocked(MemberUtils.isBlocked(targetMember, member))
                .system(null)
                .chatMessageList(chatMessageListDTO)
                .build();
        } else {
            // 기존에 채팅방이 존재하지 않는 경우: 새 채팅방 생성

            // 채팅 상대 회원이 나를 차단한 경우, 새 채팅방 생성 불가
            if (MemberUtils.isBlocked(targetMember, member)) {
                throw new ChatHandler(ErrorStatus.BLOCKED_BY_CHAT_TARGET_CHAT_START_FAILED);
            }

            // chatroom 엔티티 생성
            String uuid = UUID.randomUUID().toString();
            Chatroom newChatroom = Chatroom.builder()
                .uuid(uuid)
                .startMember(member)
                .build();

            Chatroom savedChatroom = chatroomRepository.save(newChatroom);

            // MemberChatroom 엔티티 생성 및 연관관계 매핑
            // 나의 MemberChatroom 엔티티
            MemberChatroom memberChatroom = MemberChatroom.builder()
                .lastViewDate(null)
                .lastJoinDate(null)
                .chatroom(savedChatroom)
                .build();
            memberChatroom.setMember(member);
            memberChatroomRepository.save(memberChatroom);

            // 상대방의 MemberChatroom 엔티티
            MemberChatroom targetMemberChatroom = MemberChatroom.builder()
                .lastViewDate(null)
                .lastJoinDate(null)
                .chatroom(savedChatroom)
                .build();
            targetMemberChatroom.setMember(targetMember);
            memberChatroomRepository.save(targetMemberChatroom);

            return ChatroomEnterDTO.builder()
                .uuid(savedChatroom.getUuid())
                .memberId(targetMember.getId())
                .gameName(targetMember.getGameName())
                .memberProfileImg(targetMember.getProfileImage())
                .friend(friendService.isFriend(member, targetMember))
                .blocked(false)
                .system(null)
                .chatMessageList(null)
                .build();
        }

    }

    /**
     * 특정 글을 보고 채팅 시작: 기존에 채팅방이 있는 경우 채팅방 입장 처리, 기존에 채팅방이 없는 경우 새로운 채팅방 생성
     *
     * @param request
     * @param memberId
     * @return
     */
    public ChatResponse.ChatroomEnterDTO startChatroomByBoardId(Long memberId, Long boardId) {
        Member member = profileService.findMember(memberId);

        // 게시글 엔티티 조회
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new BoardHandler(ErrorStatus.BOARD_NOT_FOUND));

        // 채팅 대상 회원의 존재 여부 검증
        Member targetMember = memberRepository.findById(board.getMember().getId())
            .orElseThrow(() -> new ChatHandler(ErrorStatus.CHAT_TARGET_NOT_FOUND));

        // 채팅 대상 회원이 탈퇴한 경우
        MemberUtils.checkBlind(targetMember);

        // 내가 채팅 대상 회원을 차단한 경우, 기존 채팅방 입장 및 새 채팅방 생성 모두 불가
        if (MemberUtils.isBlocked(member, targetMember)) {
            throw new ChatHandler(ErrorStatus.CHAT_TARGET_IS_BLOCKED_CHAT_START_FAILED);
        }

        // 채팅 대상 회원과의 chatroom 존재 여부 조회
        Optional<Chatroom> chatroom = chatroomRepository.findChatroomByMemberIds(
            member.getId(), targetMember.getId());

        // 기존에 채팅방이 존재하는 경우: 메시지 내역, 상대 회원 정보 조회 및 해당 채팅방 입장 처리
        if (chatroom.isPresent()) {
            MemberChatroom memberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
                    member.getId(), chatroom.get().getId())
                .orElseThrow(() -> new ChatHandler(ErrorStatus.CHATROOM_ACCESS_DENIED));

            // 채팅 대상 회원이 나를 차단함 && 내가 해당 채팅방을 퇴장한 상태인 경우
            if (MemberUtils.isBlocked(targetMember, member)
                && memberChatroom.getLastJoinDate() == null) {
                throw new ChatHandler(ErrorStatus.BLOCKED_BY_CHAT_TARGET_CHAT_START_FAILED);
            }

            // 최근 메시지 내역 조회
            Slice<Chat> recentChats = chatRepository.findRecentChats(chatroom.get().getId(),
                memberChatroom.getId(), member.getId());

            // 해당 채팅방의 lastViewDate 업데이트
            memberChatroom.updateLastViewDate(LocalDateTime.now());

            // ChatMessageListDTO 생성
            ChatResponse.ChatMessageListDTO chatMessageListDTO = ChatConverter.toChatMessageListDTO(
                recentChats);

            // 시스템 메시지 기능을 위한 SystemFlagDTO 생성
            ChatResponse.SystemFlagDTO systemFlagDTO = null;

            // 내 lastJoinDate가 null인 경우
            if (memberChatroom.getLastJoinDate() == null) {
                systemFlagDTO = ChatResponse.SystemFlagDTO.builder()
                    .flag(1)
                    .boardId(boardId)
                    .build();
            } else { // 내 lastJoinDate가 null이 아닌 경우
                systemFlagDTO = ChatResponse.SystemFlagDTO.builder()
                    .flag(2)
                    .boardId(boardId)
                    .build();
            }

            return ChatroomEnterDTO.builder()
                .uuid(chatroom.get().getUuid())
                .memberId(targetMember.getId())
                .gameName(targetMember.getGameName())
                .memberProfileImg(targetMember.getProfileImage())
                .friend(friendService.isFriend(member, targetMember))
                .blocked(MemberUtils.isBlocked(targetMember, member))
                .system(systemFlagDTO)
                .chatMessageList(chatMessageListDTO)
                .build();
        } else {
            // 기존에 채팅방이 존재하지 않는 경우: 새 채팅방 생성

            // 채팅 상대 회원이 나를 차단한 경우, 새 채팅방 생성 불가
            if (MemberUtils.isBlocked(targetMember, member)) {
                throw new ChatHandler(ErrorStatus.BLOCKED_BY_CHAT_TARGET_CHAT_START_FAILED);
            }

            // chatroom 엔티티 생성
            String uuid = UUID.randomUUID().toString();
            Chatroom newChatroom = Chatroom.builder()
                .uuid(uuid)
                .startMember(member)
                .build();

            Chatroom savedChatroom = chatroomRepository.save(newChatroom);

            // MemberChatroom 엔티티 생성 및 연관관계 매핑
            // 나의 MemberChatroom 엔티티
            MemberChatroom memberChatroom = MemberChatroom.builder()
                .lastViewDate(null)
                .lastJoinDate(null)
                .chatroom(savedChatroom)
                .build();
            memberChatroom.setMember(member);
            memberChatroomRepository.save(memberChatroom);

            // 상대방의 MemberChatroom 엔티티
            MemberChatroom targetMemberChatroom = MemberChatroom.builder()
                .lastViewDate(null)
                .lastJoinDate(null)
                .chatroom(savedChatroom)
                .build();
            targetMemberChatroom.setMember(targetMember);
            memberChatroomRepository.save(targetMemberChatroom);

            // 시스템 메시지 기능을 위한 SystemFlagDTO 생성
            ChatResponse.SystemFlagDTO systemFlagDTO = null;

            // 내 lastJoinDate가 null인 경우
            if (memberChatroom.getLastJoinDate() == null) {
                systemFlagDTO = ChatResponse.SystemFlagDTO.builder()
                    .flag(1)
                    .boardId(boardId)
                    .build();
            } else { // 내 lastJoinDate가 null이 아닌 경우
                systemFlagDTO = ChatResponse.SystemFlagDTO.builder()
                    .flag(2)
                    .boardId(boardId)
                    .build();
            }

            return ChatroomEnterDTO.builder()
                .uuid(savedChatroom.getUuid())
                .memberId(targetMember.getId())
                .gameName(targetMember.getGameName())
                .memberProfileImg(targetMember.getProfileImage())
                .friend(friendService.isFriend(member, targetMember))
                .blocked(false)
                .system(systemFlagDTO)
                .chatMessageList(null)
                .build();
        }

    }

    /**
     * 대상 회원과의 채팅방 생성 (게시글 및 친구 목록을 통해 생성)
     *
     * @param request
     * @param memberId
     * @return
     */
    public Chatroom createChatroom(ChatRequest.ChatroomCreateRequest request, Long memberId) {
        Member member = profileService.findMember(memberId);

        // 채팅 대상 회원의 존재 여부 검증
        Member targetMember = memberRepository.findById(request.getTargetMemberId())
            .orElseThrow(() -> new ChatHandler(ErrorStatus.CHAT_TARGET_NOT_FOUND));

        // chatroom 엔티티 생성
        String uuid = UUID.randomUUID().toString();
        Chatroom chatroom = Chatroom.builder()
            .uuid(uuid)
            .startMember(member)
            .build();

        Chatroom savedChatroom = chatroomRepository.save(chatroom);

        // MemberChatroom 엔티티 생성 및 연관관계 매핑
        // 나의 MemberChatroom 엔티티
        MemberChatroom memberChatroom = MemberChatroom.builder()
            .lastViewDate(null)
            .lastJoinDate(null)
            .chatroom(chatroom)
            .build();
        memberChatroom.setMember(member);
        memberChatroomRepository.save(memberChatroom);

        // 상대방의 MemberChatroom 엔티티
        MemberChatroom targetMemberChatroom = MemberChatroom.builder()
            .lastViewDate(null)
            .lastJoinDate(null)
            .chatroom(chatroom)
            .build();
        targetMemberChatroom.setMember(targetMember);
        memberChatroomRepository.save(targetMemberChatroom);

        return savedChatroom;
    }

    /**
     * 매칭을 통해 새 채팅방을 생성
     *
     * @param request
     * @return
     */
    public Chatroom createChatroomByMatch(ChatRequest.ChatroomCreateByMatchRequest request) {
        if (request.getMemberList().size() != 2) {
            throw new ChatHandler(ErrorStatus._BAD_REQUEST);
        }
        Member member1 = profileService.findMember(request.getMemberList().get(0));
        Member member2 = profileService.findMember(request.getMemberList().get(1));

        // chatroom 엔티티 생성
        String uuid = UUID.randomUUID().toString();
        Chatroom chatroom = Chatroom.builder()
            .uuid(uuid)
            .startMember(null)
            .build();

        Chatroom savedChatroom = chatroomRepository.save(chatroom);

        LocalDateTime now = LocalDateTime.now();

        // MemberChatroom 엔티티 생성 및 연관관계 매핑
        // member1의 MemberChatroom 엔티티
        MemberChatroom memberChatroom1 = MemberChatroom.builder()
            .lastViewDate(null)
            .lastJoinDate(now)
            .chatroom(chatroom)
            .build();
        memberChatroom1.setMember(member1);
        memberChatroomRepository.save(memberChatroom1);

        // member2의 MemberChatroom 엔티티
        MemberChatroom memberChatroom2 = MemberChatroom.builder()
            .lastViewDate(null)
            .lastJoinDate(now)
            .chatroom(chatroom)
            .build();
        memberChatroom2.setMember(member2);
        memberChatroomRepository.save(memberChatroom2);

        return savedChatroom;
    }

    /**
     * chatroomUuid에 해당하는 채팅방에 입장 처리: 채팅 상대 회원 정보 조회, 메시지 내역 조회 및 lastViewDate 업데이트
     *
     * @param chatroomUuid
     * @param memberId
     * @return
     */
    @Transactional
    public ChatResponse.ChatroomEnterDTO enterChatroom(String chatroomUuid, Long memberId) {
        Member member = profileService.findMember(memberId);

        // chatroom 엔티티 조회 및 해당 회원의 채팅방이 맞는지 검증
        Chatroom chatroom = chatroomRepository.findByUuid(chatroomUuid)
            .orElseThrow(() -> new ChatHandler(ErrorStatus.CHATROOM_NOT_EXIST));

        MemberChatroom memberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
                memberId, chatroom.getId())
            .orElseThrow(() -> new ChatHandler(ErrorStatus.CHATROOM_ACCESS_DENIED));

        // 채팅 상대 회원 조회
        Member targetMember = memberChatroomRepository.findTargetMemberByChatroomIdAndMemberId(
            chatroom.getId(), memberId);

        // 내가 채팅 상대 회원을 차단한 경우
        if (MemberUtils.isBlocked(member, targetMember)) {
            throw new ChatHandler(ErrorStatus.CHAT_TARGET_IS_BLOCKED_CHAT_START_FAILED);
        }

        // 상대방이 나를 차단 && 내가 이 채팅방을 나간 상태인 경우
        if (MemberUtils.isBlocked(targetMember, member)
            && memberChatroom.getLastJoinDate() == null) {
            throw new ChatHandler(ErrorStatus.BLOCKED_BY_CHAT_TARGET_CHAT_START_FAILED);
        }

        // 최근 메시지 내역 조회
        Slice<Chat> recentChats = chatRepository.findRecentChats(chatroom.getId(),
            memberChatroom.getId(), member.getId());

        // 해당 채팅방의 lastViewDate 업데이트
        memberChatroom.updateLastViewDate(LocalDateTime.now());

        // ChatMessageListDTO 생성
        ChatResponse.ChatMessageListDTO chatMessageListDTO = ChatConverter.toChatMessageListDTO(
            recentChats);

        return ChatroomEnterDTO.builder()
            .uuid(chatroomUuid)
            .memberId(targetMember.getId())
            .gameName(targetMember.getGameName())
            .memberProfileImg(targetMember.getProfileImage())
            .friend(friendService.isFriend(member, targetMember))
            .blocked(MemberUtils.isBlocked(targetMember, member))
            .chatMessageList(chatMessageListDTO)
            .build();
    }

    /**
     * 채팅 등록 메소드
     *
     * @param request
     * @param memberId
     * @return
     */
    @Transactional
    public Chat addChat(ChatRequest.ChatCreateRequest request, String chatroomUuid, Long memberId) {
        Member member = profileService.findMember(memberId);

        // 채팅방 조회 및 존재 여부 검증
        Chatroom chatroom = chatroomRepository.findByUuid(chatroomUuid)
            .orElseThrow(() -> new ChatHandler(ErrorStatus.CHATROOM_NOT_EXIST));

        // 해당 채팅방이 회원의 것이 맞는지 검증
        MemberChatroom memberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
                memberId, chatroom.getId())
            .orElseThrow(() -> new ChatHandler(ErrorStatus.CHATROOM_ACCESS_DENIED));

        // 회원 간 차단 여부 검증
        // 대화 상대 회원 조회
        Member targetMember = memberChatroomRepository.findTargetMemberByChatroomIdAndMemberId(
            chatroom.getId(), memberId);
        if (MemberUtils.isBlocked(member, targetMember)) {
            throw new ChatHandler(ErrorStatus.CHAT_TARGET_IS_BLOCKED_SEND_CHAT_FAILED);
        }
        if (MemberUtils.isBlocked(targetMember, member)) {
            throw new ChatHandler(ErrorStatus.BLOCKED_BY_CHAT_TARGET_SEND_CHAT_FAILED);
        }

        // 등록해야 할 시스템 메시지가 있는 경우
        if (request.getSystemFlag() != null) {
            SystemFlagRequest systemFlag = request.getSystemFlag();
            Optional<Board> board = boardRepository.findById(systemFlag.getPostId());

            // 시스템 메시지 전송자 member 엔티티 조회
            Member systemMember = profileService.findMember(0L);

            // member 대상 시스템 메시지 생성
            String messageContent =
                systemFlag.getFlag().equals(1) ? POST_SYSTEM_MESSAGE_TO_MEMBER_INIT
                    : POST_SYSTEM_MESSAGE_TO_MEMBER;

            Chat systemChatToMember = Chat.builder()
                .contents(messageContent)
                .chatroom(chatroom)
                .fromMember(systemMember)
                .toMember(member)
                .sourceBoard(board.orElse(null))
                .build();

            // targetMember 대상 시스템 메시지 생성
            Chat systemChatToTargetMember = Chat.builder()
                .contents(POST_SYSTEM_MESSAGE_TO_TARGET_MEMBER)
                .chatroom(chatroom)
                .fromMember(systemMember)
                .toMember(targetMember)
                .sourceBoard(board.orElse(null))
                .build();

            Chat memberSystemChat = chatRepository.save(systemChatToMember);
            Chat targetMemberSystemChat = chatRepository.save(systemChatToTargetMember);
            chatRepository.flush();

            updateLastViewDateBySystemChat(memberChatroom, memberSystemChat.getCreatedAt(),
                targetMemberSystemChat.getCreatedAt());

        }

        // chat 엔티티 생성
        Chat chat = Chat.builder()
            .contents(request.getMessage())
            .chatroom(chatroom)
            .fromMember(member)
            .build();

        // MemberChatroom의 lastViewDate 업데이트
        Chat savedChat = chatRepository.save(chat);
        if (request.getSystem() == null) {
            updateLastViewDateByAddChat(memberChatroom, savedChat.getCreatedAt());
        }

        return savedChat;
    }

    /**
     * memberChatroom의 lastViewDate을 업데이트
     *
     * @param chatroomUuid
     * @param timestamp
     * @param memberId
     */
    @Transactional
    public void readChatMessages(String chatroomUuid, Long timestamp, Long memberId) {
        Member member = profileService.findMember(memberId);

        // chatroom 엔티티 조회 및 해당 회원의 채팅방이 맞는지 검증
        Chatroom chatroom = chatroomRepository.findByUuid(chatroomUuid)
            .orElseThrow(() -> new ChatHandler(ErrorStatus.CHATROOM_NOT_EXIST));

        MemberChatroom memberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
                member.getId(), chatroom.getId())
            .orElseThrow(() -> new ChatHandler(ErrorStatus.CHATROOM_ACCESS_DENIED));

        if (timestamp == null) { // timestamp 파라미터가 넘어오지 않은 경우, lastViewDate를 현재 시각으로 업데이트
            memberChatroom.updateLastViewDate(LocalDateTime.now());

        } else { // timestamp 파라미터가 넘어온 경우, lastViewDate를 해당 timestamp의 chat의 createdAt으로 업데이트
            Chat chat = chatRepository.findByChatroomAndTimestamp(chatroom, timestamp)
                .orElseThrow(() -> new ChatHandler(ErrorStatus.CHAT_MESSAGE_NOT_FOUND));
            memberChatroom.updateLastViewDate(chat.getCreatedAt());
        }
    }

    /**
     * 해당 채팅방 나가기
     *
     * @param chatroomUuid
     * @param memberId
     */
    @Transactional
    public void exitChatroom(String chatroomUuid, Long memberId) {
        Member member = profileService.findMember(memberId);

        // chatroom 엔티티 조회 및 해당 회원의 채팅방이 맞는지 검증
        Chatroom chatroom = chatroomRepository.findByUuid(chatroomUuid)
            .orElseThrow(() -> new ChatHandler(ErrorStatus.CHATROOM_NOT_EXIST));

        MemberChatroom memberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
                member.getId(), chatroom.getId())
            .orElseThrow(() -> new ChatHandler(ErrorStatus.CHATROOM_ACCESS_DENIED));

        memberChatroom.updateLastJoinDate(null);

    }

    /**
     * 채팅 등록 시 나와 상대방의 lastViewDate 업데이트
     *
     * @param memberChatroom
     * @param lastViewDate
     */
    private void updateLastViewDateByAddChat(MemberChatroom memberChatroom,
        LocalDateTime lastViewDate) {
        // lastJoinDate가 null인 경우
        if (memberChatroom.getLastJoinDate() == null) {
            // lastViewDate 업데이트
            memberChatroom.updateLastViewDate(lastViewDate);

            // lastJoinDate 업데이트
            memberChatroom.updateLastJoinDate(lastViewDate);

            // lastJoinDate 업데이트로 인해 socket room join API 요청
            socketService.joinSocketToChatroom(memberChatroom.getMember().getId(),
                memberChatroom.getChatroom().getUuid());

        } else {
            // lastViewDate 업데이트
            memberChatroom.updateLastViewDate(lastViewDate);

        }

        // 상대 회원의 memberChatroom의 latJoinDate가 null인 경우, 상대 회원의 lastJoinDate 업데이트
        Chatroom chatroom = memberChatroom.getChatroom();

        Member targetMember = memberChatroomRepository.findTargetMemberByChatroomIdAndMemberId(
            chatroom.getId(), memberChatroom.getMember().getId());
        MemberChatroom targetMemberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
            targetMember.getId(), chatroom.getId()).get();
        if (targetMemberChatroom.getLastJoinDate() == null) {
            targetMemberChatroom.updateLastJoinDate(lastViewDate);

            // lastJoinDate 업데이트로 인해 socket room join API 요청
            socketService.joinSocketToChatroom(targetMember.getId(),
                targetMemberChatroom.getChatroom().getUuid());
        }
    }

    /**
     * 시스템 메시지 등록 시 나와 상대방의 lastViewDate 업데이트
     *
     * @param memberChatroom
     * @param memberSystemChatCreatedAt
     * @param targetSystemChatCreatedAt
     */
    private void updateLastViewDateBySystemChat(MemberChatroom memberChatroom,
        LocalDateTime memberSystemChatCreatedAt, LocalDateTime targetSystemChatCreatedAt) {
        // lastJoinDate가 null인 경우
        if (memberChatroom.getLastJoinDate() == null) {
            // lastViewDate 업데이트
            memberChatroom.updateLastViewDate(memberSystemChatCreatedAt);

            // lastJoinDate 업데이트
            memberChatroom.updateLastJoinDate(memberSystemChatCreatedAt);

            // lastJoinDate 업데이트로 인해 socket room join API 요청
            socketService.joinSocketToChatroom(memberChatroom.getMember().getId(),
                memberChatroom.getChatroom().getUuid());

        } else {
            // lastViewDate 업데이트
            memberChatroom.updateLastViewDate(memberSystemChatCreatedAt);

        }

        // 상대 회원의 memberChatroom의 latJoinDate가 null인 경우, 상대 회원의 lastJoinDate 업데이트
        Chatroom chatroom = memberChatroom.getChatroom();

        Member targetMember = memberChatroomRepository.findTargetMemberByChatroomIdAndMemberId(
            chatroom.getId(), memberChatroom.getMember().getId());
        MemberChatroom targetMemberChatroom = memberChatroomRepository.findByMemberIdAndChatroomId(
            targetMember.getId(), chatroom.getId()).get();
        if (targetMemberChatroom.getLastJoinDate() == null) {
            targetMemberChatroom.updateLastJoinDate(targetSystemChatCreatedAt);

            // lastJoinDate 업데이트로 인해 socket room join API 요청
            socketService.joinSocketToChatroom(targetMember.getId(),
                targetMemberChatroom.getChatroom().getUuid());
        }
    }

}
