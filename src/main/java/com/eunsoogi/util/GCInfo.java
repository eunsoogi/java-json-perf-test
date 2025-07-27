package com.eunsoogi.util;

/**
 * GC 정보를 담는 클래스
 */
public class GCInfo {
    public final long youngGCCount;
    public final long youngGCTime;
    public final long fullGCCount;
    public final long fullGCTime;

    public GCInfo(long youngGCCount, long youngGCTime, long fullGCCount, long fullGCTime) {
        this.youngGCCount = youngGCCount;
        this.youngGCTime = youngGCTime;
        this.fullGCCount = fullGCCount;
        this.fullGCTime = fullGCTime;
    }

    /**
     * GC 정보의 차이를 계산
     * @param other 비교할 다른 GC 정보
     * @return 차이가 계산된 새로운 GCInfo 객체
     */
    public GCInfo getPerfDiff(GCInfo other) {
        return new GCInfo(
            this.youngGCCount - other.youngGCCount,
            this.youngGCTime - other.youngGCTime,
            this.fullGCCount - other.fullGCCount,
            this.fullGCTime - other.fullGCTime
        );
    }
}
