package com.gamegoo.converter;

import com.gamegoo.domain.Member;
import com.gamegoo.dto.member.MemberResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public class MemberConverter {

    public static MemberResponse.blockListDTO toBlockListDTO(Page<Member> blockList) {
        List<MemberResponse.blockedMemberDTO> blockedMemberDtoList = blockList.stream()
                .map(MemberConverter::toBlockedMemberDTO)
                .collect(Collectors.toList());

        return MemberResponse.blockListDTO.builder()
                .blocked_member_dto_list(blockedMemberDtoList)
                .list_size(blockedMemberDtoList.size())
                .total_page(blockList.getTotalPages())
                .total_elements(blockList.getTotalElements())
                .is_first(blockList.isFirst())
                .is_last(blockList.isLast())
                .build();
    }

    public static MemberResponse.blockedMemberDTO toBlockedMemberDTO(Member membr) {
        return MemberResponse.blockedMemberDTO.builder()
                .member_id(membr.getId())
                .profile_img(membr.getProfileImage())
                .email(membr.getEmail())
                .name(membr.getGameName())
                .build();
    }

    public static MemberResponse.myProfileMemberDTO toMyProfileDTO(Member member) {
        List<MemberResponse.GameStyleResponseDTO> dtoList = member.getMemberGameStyleList().stream().map(memberGameStyle -> MemberResponse.GameStyleResponseDTO.builder()
                .gameStyleId(memberGameStyle.getGameStyle().getId())
                .gameStyleName(memberGameStyle.getGameStyle().getStyleName())
                .build()).collect(Collectors.toList());

        return MemberResponse.myProfileMemberDTO.builder()
                .email(member.getEmail())
                .gamename(member.getGameName())
                .tag(member.getTag())
                .tier(member.getTier())
                .rank(member.getRank())
                .profile_img(member.getProfileImage())
                .updated_at(String.valueOf(member.getUpdatedAt()))
                .game_style_dto_list(dtoList)
                .build();
    }

}
