package com.gamegoo.service.member;

import com.gamegoo.apiPayload.code.status.ErrorStatus;
import com.gamegoo.apiPayload.exception.handler.FriendHandler;
import com.gamegoo.apiPayload.exception.handler.PageHandler;
import com.gamegoo.domain.friend.Friend;
import com.gamegoo.domain.friend.FriendRequestStatus;
import com.gamegoo.domain.friend.FriendRequests;
import com.gamegoo.domain.member.Member;
import com.gamegoo.domain.notification.NotificationTypeTitle;
import com.gamegoo.repository.friend.FriendRepository;
import com.gamegoo.repository.friend.FriendRequestsRepository;
import com.gamegoo.service.notification.NotificationService;
import com.gamegoo.util.MemberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendService {

    private final FriendRepository friendRepository;
    private final FriendRequestsRepository friendRequestsRepository;
    private final ProfileService profileService;
    private final NotificationService notificationService;

    private final static int PAGE_SIZE = 10;

    /**
     * memberId에 해당하는 회원의 친구 목록 조회, 오름차순 정렬 및 페이징 포함
     *
     * @param memberId
     * @return
     */
    @Transactional(readOnly = true)
    public Slice<Friend> getFriends(Long memberId, Long cursorId) {
        // 커서 값 검증
        if (cursorId!=null && cursorId < 0) {
            throw new PageHandler(ErrorStatus.CURSOR_INVALID);
        }

        Member member = profileService.findMember(memberId);

        return friendRepository.findFriendsByCursorAndOrdered(cursorId, member.getId(), PAGE_SIZE);
    }

    /**
     * query string으로 시작하는 소환사명을 갖는 모든 친구 검색
     *
     * @param memberId
     * @param queryString
     * @return
     */
    @Transactional(readOnly = true)
    public List<Friend> searchFriendByQueryString(Long memberId, String queryString) {
        if (queryString.length() > 100) {
            throw new FriendHandler(ErrorStatus.FRIEND_SEARCH_QUERY_BAD_REQUEST);
        }

        Member member = profileService.findMember(memberId);

        return friendRepository.findFriendsByQueryStringAndOrdered(queryString, member.getId());
    }

    /**
     * memberId에 해당하는 회원의 모든 친구 id 조회
     *
     * @param memberId
     * @return
     */
    public List<Long> getFriendIds(Long memberId) {
        Member member = profileService.findMember(memberId);

        List<Friend> friendList = friendRepository.findAllByFromMemberId(member.getId());

        return friendList
                .stream()
                .map(friend -> friend.getToMember().getId())
                .collect(Collectors.toList());
    }

    /**
     * targetMemberId에 해당하는 회원에게 친구 요청 전송
     *
     * @param memberId
     * @param targetMemberId
     * @return
     */
    public void sendFriendRequest(Long memberId, Long targetMemberId) {
        Member member = profileService.findMember(memberId);

        Member targetMember = profileService.findMember(targetMemberId);

        // targetMember로 나 자신을 요청한 경우
        if (member.equals(targetMember)) {
            throw new FriendHandler(ErrorStatus.FRIEND_BAD_REQUEST);
        }

        // 내가 상대방을 차단한 경우
        if (MemberUtils.isBlocked(member, targetMember)) {
            throw new FriendHandler(ErrorStatus.FRIEND_TARGET_IS_BLOCKED);
        }

        // 상대방이 나를 차단한 경우
        if (MemberUtils.isBlocked(targetMember, member)) {
            throw new FriendHandler(ErrorStatus.BLOCKED_BY_FRIEND_TARGET);
        }

        // 서로 이미 친구 관계인 경우
        friendRepository.findByFromMemberAndToMember(member, targetMember)
                .ifPresent(friend -> {
                    throw new FriendHandler(ErrorStatus.ALREADY_FRIEND);
                });

        // member -> targetMember로 보낸 친구 요청 중 PENDING 상태인 친구 요청이 존재하는 경우
        friendRequestsRepository.findByFromMemberAndToMemberAndStatus(member, targetMember, FriendRequestStatus.PENDING)
                .ifPresent(friendRequest -> {
                    throw new FriendHandler(ErrorStatus.MY_PENDING_FRIEND_REQUEST_EXIST);
                });

        // targetMember -> member에게 보낸 친구 요청 중 PENDING 상태인 친구 요청이 존재하는 경우
        friendRequestsRepository.findByFromMemberAndToMemberAndStatus(targetMember, member, FriendRequestStatus.PENDING)
                .ifPresent(friendRequests -> {
                    throw new FriendHandler(ErrorStatus.TARGET_PENDING_FRIEND_REQUEST_EXIST);
                });

        FriendRequests friendRequests = FriendRequests.builder()
                .status(FriendRequestStatus.PENDING)
                .fromMember(member)
                .toMember(targetMember)
                .build();

        friendRequestsRepository.save(friendRequests);

        // 친구 요청 알림 생성
        // member -> targetMember
        notificationService.createNotification(NotificationTypeTitle.FRIEND_REQUEST_SEND, null, targetMember, member);

        // targetMember -> member
        notificationService.createNotification(NotificationTypeTitle.FRIEND_REQUEST_RECEIVED, null, member,
                targetMember);
    }

    /**
     * member -> targetMember로 요청한 FriendRequest를 CANCELLED 처리
     *
     * @param memberId
     * @param targetMemberId
     */
    public void cancelFriendRequest(Long memberId, Long targetMemberId) {
        Member member = profileService.findMember(memberId);

        Member targetMember = profileService.findMember(targetMemberId);

        // targetMember로 나 자신을 요청한 경우
        if (member.equals(targetMember)) {
            throw new FriendHandler(ErrorStatus.FRIEND_BAD_REQUEST);
        }

        // 수락 대기 상태인 FriendRequest 엔티티 조회
        Optional<FriendRequests> pendingFriendRequest = friendRequestsRepository
                .findByFromMemberAndToMemberAndStatus(member, targetMember, FriendRequestStatus.PENDING);

        // 수락 대기 중인 친구 요청이 존재하지 않는 경우
        if (pendingFriendRequest.isEmpty()) {
            throw new FriendHandler(ErrorStatus.PENDING_FRIEND_REQUEST_NOT_EXIST);
        }

        // FriendRequest 엔티티 상태 변경
        pendingFriendRequest.get().updateStatus(FriendRequestStatus.CANCELLED);
    }

    /**
     * targetMember -> member로 요청한 FriendRequest를 ACCEPTED 처리
     *
     * @param memberId
     * @param targetMemberId
     * @return
     */
    public void acceptFriendRequest(Long memberId, Long targetMemberId) {
        Member member = profileService.findMember(memberId);

        Member targetMember = profileService.findMember(targetMemberId);

        // targetMember로 나 자신을 요청한 경우
        if (member.equals(targetMember)) {
            throw new FriendHandler(ErrorStatus.FRIEND_BAD_REQUEST);
        }

        // 수락 대기 상태인 FriendRequest 엔티티 조회
        Optional<FriendRequests> pendingFriendRequest = friendRequestsRepository
                .findByFromMemberAndToMemberAndStatus(targetMember, member, FriendRequestStatus.PENDING);

        // 수락 대기 중인 친구 요청이 존재하지 않는 경우
        if (pendingFriendRequest.isEmpty()) {
            throw new FriendHandler(ErrorStatus.PENDING_FRIEND_REQUEST_NOT_EXIST);
        }

        // FriendRequest 엔티티 상태 변경
        pendingFriendRequest.get().updateStatus(FriendRequestStatus.ACCEPTED);

        // 회원 간 Friend 엔티티 생성 및 저장
        Friend targetMemberFriend = Friend.builder()
                .fromMember(targetMember)
                .toMember(member)
                .isLiked(false)
                .build();

        Friend memberFriend = Friend.builder()
                .fromMember(member)
                .toMember(targetMember)
                .isLiked(false)
                .build();

        friendRepository.save(targetMemberFriend);
        friendRepository.save(memberFriend);

        // targetMember에게 친구 요청 수락 알림 생성
        notificationService.createNotification(NotificationTypeTitle.FRIEND_REQUEST_ACCEPTED, member.getGameName(),
                member, targetMember);
    }

    /**
     * targetMember -> member로 요청한 FriendRequest를 REJECTED 처리
     *
     * @param memberId
     * @param targetMemberId
     */
    public void rejectFriendRequest(Long memberId, Long targetMemberId) {
        Member member = profileService.findMember(memberId);

        Member targetMember = profileService.findMember(targetMemberId);

        // targetMember로 나 자신을 요청한 경우
        if (member.equals(targetMember)) {
            throw new FriendHandler(ErrorStatus.FRIEND_BAD_REQUEST);
        }

        // 수락 대기 상태인 FriendRequest 엔티티 조회
        Optional<FriendRequests> pendingFriendRequest = friendRequestsRepository
                .findByFromMemberAndToMemberAndStatus(targetMember, member, FriendRequestStatus.PENDING);

        // 수락 대기 중인 친구 요청이 존재하지 않는 경우
        if (pendingFriendRequest.isEmpty()) {
            throw new FriendHandler(ErrorStatus.PENDING_FRIEND_REQUEST_NOT_EXIST);
        }

        // FriendRequest 엔티티 상태 변경
        pendingFriendRequest.get().updateStatus(FriendRequestStatus.REJECTED);

        // targetMember에게 친구 요청 거절 알림 생성
        notificationService.createNotification(NotificationTypeTitle.FRIEND_REQUEST_REJECTED, member.getGameName(),
                member, targetMember);
    }

    /**
     * 친구 회원을 즐겨찾기 설정
     *
     * @param memberId
     * @param friendMemberId
     * @return
     */

    public Friend starFriend(Long memberId, Long friendMemberId) {
        Member member = profileService.findMember(memberId);
        Member friendMember = profileService.findMember(friendMemberId);

        // 본인의 id를 입력한 경우
        if (member.equals(friendMember)) {
            throw new FriendHandler(ErrorStatus.FRIEND_BAD_REQUEST);
        }

        Optional<Friend> friend = friendRepository.findByFromMemberAndToMember(member, friendMember);

        // 친구 회원의 탈퇴 여부 검증
        if (friendMember.getBlind()) {
            throw new FriendHandler(ErrorStatus.FRIEND_USER_DEACTIVATED);
        }

        // 두 회원이 친구 관계가 맞는지 검증
        if (friend.isEmpty()) {
            throw new FriendHandler(ErrorStatus.MEMBERS_NOT_FRIEND);
        }

        // 이미 즐겨찾기 되어있는지 검증
        if (friend.get().getIsLiked()) {
            throw new FriendHandler(ErrorStatus.ALREADY_STAR_FRIEND);
        }

        friend.get().updateIsLiked(true);

        return friend.get();
    }

    /**
     * 해당 친구 회원을 즐겨찾기 해제
     *
     * @param memberId
     * @param friendMemberId
     * @return
     */
    public Friend unstarFriend(Long memberId, Long friendMemberId) {
        Member member = profileService.findMember(memberId);
        Member friendMember = profileService.findMember(friendMemberId);

        // 본인의 id를 입력한 경우
        if (member.equals(friendMember)) {
            throw new FriendHandler(ErrorStatus.FRIEND_BAD_REQUEST);
        }

        Optional<Friend> friend = friendRepository.findByFromMemberAndToMember(member, friendMember);

        // 친구 회원의 탈퇴 여부 검증
        if (friendMember.getBlind()) {
            throw new FriendHandler(ErrorStatus.FRIEND_USER_DEACTIVATED);
        }

        // 두 회원이 친구 관계가 맞는지 검증
        if (friend.isEmpty()) {
            throw new FriendHandler(ErrorStatus.MEMBERS_NOT_FRIEND);
        }

        // 해당 회원이 즐겨찾기 되어있는지 검증
        if (!friend.get().getIsLiked()) {
            throw new FriendHandler(ErrorStatus.NOT_STAR_FRIEND);
        }

        friend.get().updateIsLiked(false);

        return friend.get();
    }

    /**
     * 두 회원 간 친구 관계 삭제
     *
     * @param memberId
     * @param friendMemberId
     */
    public void deleteFriend(Long memberId, Long friendMemberId) {
        Member member = profileService.findMember(memberId);
        Member friendMember = profileService.findMember(friendMemberId);

        if (member.equals(friendMember)) {
            throw new FriendHandler(ErrorStatus.FRIEND_BAD_REQUEST);
        }

        removeFriendshipIfPresent(member, friendMember);
    }

    /**
     * fromMember와 toMember가 서로 친구 관계이면, 친구 관계 끊기
     *
     * @param fromMember
     * @param toMember
     */
    public void removeFriendshipIfPresent(Member fromMember, Member toMember) {
        friendRepository.findByFromMemberAndToMember(fromMember, toMember)
                .ifPresent(friend -> {
                    friendRepository.deleteById(friend.getId());

                    friendRepository.findByFromMemberAndToMember(toMember, fromMember)
                            .ifPresent(reverseFriend ->
                                    friendRepository.deleteById(reverseFriend.getId())
                            );
                });
    }

    /**
     * fromMember -> toMember의 FriendRequest 중 PENDING 상태인 요청을 취소 처리
     *
     * @param fromMember
     * @param toMember
     */
    public void cancelPendingFriendRequests(Member fromMember, Member toMember) {
        friendRequestsRepository.findByFromMemberAndToMemberAndStatus(fromMember, toMember, FriendRequestStatus.PENDING)
                .ifPresent(friendRequests ->
                        friendRequests.updateStatus(FriendRequestStatus.CANCELLED)
                );
    }

    /**
     * 두 회원이 서로 친구 관계인지 여부 리턴
     *
     * @param member1
     * @param member2
     * @return
     */
    @Transactional(readOnly = true)
    public boolean isFriend(Member member1, Member member2) {
        List<Friend> friendList = friendRepository.findBothDirections(member1, member2);
        return (friendList.size()==2);
    }

    /**
     * 두 회원 사이 친구 요청이 존재하는 경우, 친구 요청을 보낸 회원의 id를 리턴
     *
     * @param member1
     * @param member2
     * @return
     */
    public Long getFriendRequestMemberId(Member member1, Member member2) {
        return friendRequestsRepository.findByFromMemberAndToMemberAndStatus(member1, member2,
                        FriendRequestStatus.PENDING)
                .map(friendRequests -> member1.getId())
                .or(() -> friendRequestsRepository.findByFromMemberAndToMemberAndStatus(member2,
                                member1, FriendRequestStatus.PENDING)
                        .map(friendRequests -> member2.getId()))
                .orElse(null); // 친구 요청이 없는 경우 null을 리턴
    }

}
