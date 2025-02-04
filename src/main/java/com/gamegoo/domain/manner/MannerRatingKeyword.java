package com.gamegoo.domain.manner;

import com.gamegoo.domain.common.BaseDateTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MannerRatingKeyword extends BaseDateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manner_rating_keyword_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manner_rating_id", nullable = false)
    private MannerRating mannerRating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manner_keyword_id", nullable = false)
    private MannerKeyword mannerKeyword;

    // 연관관계 메소드
    public void setMannerRating(MannerRating mannerRating) {
        if (this.mannerRating != null) {
            this.mannerRating.getMannerRatingKeywordList().remove(this);
        }
        this.mannerRating = mannerRating;
        if (mannerRating != null) {
            this.mannerRating.getMannerRatingKeywordList().add(this);
        }
    }

    public MannerRatingKeyword(MannerRating mannerRating, MannerKeyword mannerKeyword) {
        this.mannerRating = mannerRating;
        this.mannerKeyword = mannerKeyword;
    }

    public void setMannerKeyword(MannerKeyword mannerKeyword) {
        this.mannerKeyword = mannerKeyword;
    }

}
