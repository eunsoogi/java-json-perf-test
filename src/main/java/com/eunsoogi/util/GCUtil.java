package com.eunsoogi.util;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * GC(Garbage Collection) 관련 유틸리티 클래스
 * 재사용 가능한 GC 성능 정보 수집 기능을 제공합니다.
 */
public class GCUtil {

    /**
     * 현재 GC 정보를 수집하여 반환
     * @return GC 정보가 담긴 GCInfo 객체
     */
    public static GCInfo getGcInfo() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        long youngGCCount = 0;
        long youngGCTime = 0;
        long fullGCCount = 0;
        long fullGCTime = 0;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            String name = gcBean.getName().toLowerCase();
            if (name.contains("young") || name.contains("scavenge") || name.contains("copy")) {
                youngGCCount += gcBean.getCollectionCount();
                youngGCTime += gcBean.getCollectionTime();
            } else if (name.contains("old") || name.contains("tenured") || name.contains("marksweep") ||
                      name.contains("concurrent") || name.contains("cms") || name.contains("g1")) {
                fullGCCount += gcBean.getCollectionCount();
                fullGCTime += gcBean.getCollectionTime();
            }
        }

        return new GCInfo(youngGCCount, youngGCTime, fullGCCount, fullGCTime);
    }


}
