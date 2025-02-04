package com.gamegoo.domain.matching;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMatchingRecord is a Querydsl query type for MatchingRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchingRecord extends EntityPathBase<MatchingRecord> {

    private static final long serialVersionUID = 1978268795L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMatchingRecord matchingRecord = new QMatchingRecord("matchingRecord");

    public final com.gamegoo.domain.common.QBaseDateTimeEntity _super = new com.gamegoo.domain.common.QBaseDateTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> gameMode = createNumber("gameMode", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> mainPosition = createNumber("mainPosition", Integer.class);

    public final NumberPath<Integer> mannerLevel = createNumber("mannerLevel", Integer.class);

    public final BooleanPath mannerMessageSent = createBoolean("mannerMessageSent");

    public final EnumPath<MatchingType> matchingType = createEnum("matchingType", MatchingType.class);

    public final com.gamegoo.domain.member.QMember member;

    public final BooleanPath mike = createBoolean("mike");

    public final NumberPath<Integer> rank = createNumber("rank", Integer.class);

    public final EnumPath<MatchingStatus> status = createEnum("status", MatchingStatus.class);

    public final NumberPath<Integer> subPosition = createNumber("subPosition", Integer.class);

    public final com.gamegoo.domain.member.QMember targetMember;

    public final EnumPath<com.gamegoo.domain.member.Tier> tier = createEnum("tier", com.gamegoo.domain.member.Tier.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Integer> wantPosition = createNumber("wantPosition", Integer.class);

    public final NumberPath<Double> winRate = createNumber("winRate", Double.class);

    public QMatchingRecord(String variable) {
        this(MatchingRecord.class, forVariable(variable), INITS);
    }

    public QMatchingRecord(Path<? extends MatchingRecord> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMatchingRecord(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMatchingRecord(PathMetadata metadata, PathInits inits) {
        this(MatchingRecord.class, metadata, inits);
    }

    public QMatchingRecord(Class<? extends MatchingRecord> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.gamegoo.domain.member.QMember(forProperty("member")) : null;
        this.targetMember = inits.isInitialized("targetMember") ? new com.gamegoo.domain.member.QMember(forProperty("targetMember")) : null;
    }

}

