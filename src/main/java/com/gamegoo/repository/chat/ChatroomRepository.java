package com.gamegoo.repository.chat;

import com.gamegoo.domain.chat.Chatroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long>, ChatroomRepositoryCustom {

    @Query("SELECT c.uuid FROM MemberChatroom mc JOIN mc.chatroom c WHERE mc.member.id = :memberId AND mc" +
            ".lastJoinDate IS NOT NULL")
    List<String> findActiveChatroomUuidsByMemberId(@Param("memberId") Long memberId);

    Optional<Chatroom> findByUuid(String uuid);

}
