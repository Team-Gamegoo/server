package com.gamegoo.domain.member;

import com.gamegoo.domain.Block;
import com.gamegoo.domain.board.Board;
import com.gamegoo.domain.champion.MemberChampion;
import com.gamegoo.domain.chat.MemberChatroom;
import com.gamegoo.domain.common.BaseDateTimeEntity;
import com.gamegoo.domain.gamestyle.MemberGameStyle;
import com.gamegoo.domain.manner.MannerRating;
import com.gamegoo.domain.notification.Notification;
import com.gamegoo.domain.report.Report;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "Member")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member extends BaseDateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 500)
    private String password;

    @Column(name = "profile_image")
    private Integer profileImage;

    @Column(name = "manner_level")
    private Integer mannerLevel = 1;

    @Column(name = "manner_score")
    private Integer mannerScore;

    @Column(name = "blind", nullable = false)
    private Boolean blind = false;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)", nullable = false)
    private LoginType loginType;

    @Column(name = "gamename", length = 100)
    private String gameName;

    @Column(name = "tag", length = 100)
    private String tag;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier")
    private Tier tier;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "winrate")
    private Double winRate;

    @Column(name = "main_position")
    private Integer mainPosition = 0;

    @Column(name = "sub_position")
    private Integer subPosition = 0;

    @Column(name = "want_position")
    private Integer wantPosition = 0;

    @Column(name = "mike")
    private Boolean mike = false;

    @Column(name = "game_count")
    private Integer gameCount;

    @Column(name = "is_agree")
    private Boolean isAgree;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Board> boardList = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<MemberChampion> memberChampionList = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<MemberGameStyle> memberGameStyleList = new ArrayList<>();

    @OneToMany(mappedBy = "toMember", cascade = CascadeType.ALL)
    private List<MannerRating> mannerRatingList = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Notification> notificationList = new ArrayList<>();

    @OneToMany(mappedBy = "blockerMember", cascade = CascadeType.ALL)
    private List<Block> blockList = new ArrayList<>();

    @OneToMany(mappedBy = "reporter", cascade = CascadeType.ALL)
    private List<Report> reportList = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<MemberChatroom> memberChatroomList = new ArrayList<>();

    public void updateUpdatedAt() {
        // 현재 시간을 updatedAt으로 설정
        this.setUpdatedAt(LocalDateTime.now());
    }

    public void updateMike(Boolean isMike) {
        this.mike = isMike;
    }

    public void updatePosition(Integer mainPosition, Integer subPosition, Integer wantPosition) {
        this.mainPosition = mainPosition;
        this.subPosition = subPosition;
        this.wantPosition = wantPosition;
    }

    public void updateProfileImage(Integer profileImage) {
        this.profileImage = profileImage;
    }

    public void deactiveMember() {
        this.blind = true;
    }

    public void updateMemberFromMatching(Integer mainPosition, Integer subPosition, Integer wantPosition,
                                         Boolean mike) {
        this.mainPosition = mainPosition;
        this.subPosition = subPosition;
        this.wantPosition = wantPosition;
        this.mike = mike;
    }

    public void initializeMemberChampionList() {
        this.memberChampionList = new ArrayList<>();
    }

    public void updateRiotDetails(Tier tier, Integer rank, Double winRate, Integer gameCount) {

        this.tier = tier;
        this.rank = rank;
        this.winRate = winRate;
        this.gameCount = gameCount;
    }

    public void updateRiotBasic(String gameName, String tag) {
        this.gameName = gameName;
        this.tag = tag;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void setMannerScore(int mannerScore) {
        this.mannerScore = mannerScore;
    }

    public void setMannerLevel(int mannerLevel) {
        this.mannerLevel = mannerLevel;
    }

}

