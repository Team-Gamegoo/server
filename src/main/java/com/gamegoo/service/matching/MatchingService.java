package com.gamegoo.service.matching;

import com.gamegoo.apiPayload.code.status.ErrorStatus;
import com.gamegoo.apiPayload.exception.handler.MatchingHandler;
import com.gamegoo.apiPayload.exception.handler.MemberHandler;
import com.gamegoo.domain.MatchingRecord;
import com.gamegoo.domain.Member;
import com.gamegoo.dto.matching.MatchingRequest;
import com.gamegoo.repository.matching.MatchingRecordRepository;
import com.gamegoo.repository.member.MemberRepository;
import com.gamegoo.service.member.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class MatchingService {
    private final MemberRepository memberRepository;
    private final MatchingRecordRepository matchingRecordRepository;
    private final ProfileService profileService;

    /**
     * 매칭 정보 저장
     *
     * @param request
     * @param id
     */
    @Transactional
    public void save(MatchingRequest.SaveMatchingRequestDTO request, Long id) {
        // 회원 정보 불러오기
        Member member = memberRepository.findById(id).orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // 매칭 기록 저장
        MatchingRecord matchingRecord = MatchingRecord.builder()
                .mike(request.getMike())
                .tier(member.getTier())
                .rank(member.getRank())
                .matchingType(request.getMatchingType())
                .status("FAIL")
                .mainPosition(request.getMainP())
                .subPosition(request.getSubP())
                .wantPosition(request.getWantP())
                .winRate(member.getWinRate())
                .gameMode(request.getGameMode())
                .mannerLevel(request.getManner())
                .member(member)
                .build();

        // 매칭 기록에 따라 member 정보 변경하기
        member.updateMemberFromMatching(request.getMainP(), request.getSubP(), request.getMike());
        profileService.addMemberGameStyles(request.getGameStyleIdList(), member.getId());

        matchingRecordRepository.save(matchingRecord);
        memberRepository.save(member);
    }

    /**
     * 매칭 상태(status) 수정 : 회원의 가장 최신 매칭만 상태 변경 가능
     *
     * @param request
     * @param id
     */
    @Transactional
    public void modify(MatchingRequest.ModifyMatchingRequestDTO request, Long id) {
        // 회원 정보 불러오기
        Member member = memberRepository.findById(id).orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // 매칭 기록 불러오기
        MatchingRecord matchingRecord = matchingRecordRepository.findFirstByMemberOrderByUpdatedAtDesc(member)
                .orElseThrow(() -> new MatchingHandler(ErrorStatus.MATCHING_NOT_FOUND));

        String status = request.getStatus();

        // 매칭 status 값 확인
        if (status.equals("QUIT") || status.equals("SUCCESS")) {
            // status 값 변경
            matchingRecord.updateStatus(request.getStatus());
            matchingRecordRepository.save(matchingRecord);
        } else {
            // status 값이 이상할 경우 에러처리
            throw new MatchingHandler(ErrorStatus.MATCHING_STATUS_BAD_REQUEST);
        }

    }
}
