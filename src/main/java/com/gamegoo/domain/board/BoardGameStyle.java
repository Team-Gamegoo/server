package com.gamegoo.domain.board;

import com.gamegoo.domain.common.BaseDateTimeEntity;
import com.gamegoo.domain.gamestyle.GameStyle;
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
import javax.persistence.Table;

@Entity
@Table(name = "BoardGameStyle")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BoardGameStyle extends BaseDateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_game_style_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gamestyle_id", nullable = false)
    private GameStyle gameStyle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    // 연관관계 메소드
    public void setBoard(Board board) {
        if (this.board != null) {
            this.board.getBoardGameStyles().remove(this);
        }
        this.board = board;
        if (board != null) {
            this.board.getBoardGameStyles().add(this);
        }
    }

    public BoardGameStyle(GameStyle gameStyle, Board board) {
        this.gameStyle = gameStyle;
        this.board = board;
    }

    public void setGameStyle(GameStyle gameStyle) {
        this.gameStyle = gameStyle;
    }

}
