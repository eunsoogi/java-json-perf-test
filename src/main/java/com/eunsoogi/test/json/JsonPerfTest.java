package com.eunsoogi.test.json;

import com.eunsoogi.util.GCUtil;
import com.eunsoogi.util.GCInfo;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.gson.Gson;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

public class JsonPerfTest {

    private static final Logger logger = LoggerFactory.getLogger(JsonPerfTest.class);

    /**
     * JSON 작업을 정의하는 함수형 인터페이스
     */
    @FunctionalInterface
    private interface JsonTask {
        void execute(AtomicLong[] counters, int requestId) throws Exception;
    }

    // Jackson JsonFactory - thread-safe하므로 재사용 가능
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    // 기본값 상수
    private static final int DEFAULT_FIELD_COUNT = 1000;  // JSON 객체당 필드 개수
    private static final int DEFAULT_TEST_DURATION_SECONDS = 10;  // 테스트 지속 시간
    private static final int DEFAULT_THREAD_POOL_SIZE = 5;  // 스레드 풀 크기

    // 런타임 설정 변수
    private static int fieldCount;
    private static int testDurationSeconds;
    private static int threadPoolSize;

    public static void main(String[] args) {
        // 런타임 인자 파싱
        parseArguments(args);

        logger.info("=== JSON 성능 비교 테스트 시작 ===");
        logger.info("테스트 조건: 필드 {}개, 지속시간 {}초, 스레드 풀 {} (최대 성능 측정)",
                    fieldCount, testDurationSeconds, threadPoolSize);

        // 1. JSONObject 테스트
        logger.info("1. JSONObject 성능 테스트 시작");
        testJSONObjectPerf();

        // 잠시 대기 (GC 정리를 위해)
        try {
            logger.info("테스트 간 대기 중... (GC 정리)");
            Thread.sleep(5000);
            System.gc();
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info(createSeparator(80));

        // 2. Jackson JsonGenerator 테스트
        logger.info("2. Jackson JsonGenerator 성능 테스트 시작");
        testJacksonPerf();

        // 잠시 대기 (GC 정리를 위해)
        try {
            logger.info("테스트 간 대기 중... (GC 정리)");
            Thread.sleep(5000);
            System.gc();
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info(createSeparator(80));

        // 3. Gson 테스트
        logger.info("3. Gson 성능 테스트 시작");
        testGsonPerf();

        logger.info("=== 테스트 완료 ===");
    }

    /**
     * 런타임 인자를 파싱하여 설정값 초기화
     * 사용법: java JsonPerfTest [fieldCount] [durationSeconds] [threadPoolSize]
     */
    private static void parseArguments(String[] args) {
        // 기본값 설정
        fieldCount = DEFAULT_FIELD_COUNT;
        testDurationSeconds = DEFAULT_TEST_DURATION_SECONDS;
        threadPoolSize = DEFAULT_THREAD_POOL_SIZE;

        // 인자 파싱
        try {
            if (args.length >= 1) {
                fieldCount = Integer.parseInt(args[0]);
                if (fieldCount <= 0) {
                    throw new IllegalArgumentException("필드 개수는 양수여야 합니다: " + fieldCount);
                }
            }

            if (args.length >= 2) {
                testDurationSeconds = Integer.parseInt(args[1]);
                if (testDurationSeconds <= 0) {
                    throw new IllegalArgumentException("테스트 지속 시간은 양수여야 합니다: " + testDurationSeconds);
                }
            }

            if (args.length >= 3) {
                threadPoolSize = Integer.parseInt(args[2]);
                if (threadPoolSize <= 0) {
                    throw new IllegalArgumentException("스레드 풀 크기는 양수여야 합니다: " + threadPoolSize);
                }
            }

            if (args.length > 3) {
                logger.warn("경고: 사용되지 않는 인자가 있습니다.");
                printUsage();
            }

        } catch (NumberFormatException e) {
            logger.error("에러: 잘못된 숫자 형식입니다 - {}", e.getMessage());
            printUsage();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.error("에러: {}", e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    /**
     * 사용법 출력
     */
    private static void printUsage() {
        logger.info("사용법: java JsonPerfTest [fieldCount] [durationSeconds] [threadPoolSize]");
        logger.info("  fieldCount: JSON 객체당 필드 개수 (기본값: {})", DEFAULT_FIELD_COUNT);
        logger.info("  durationSeconds: 테스트 지속 시간 (기본값: {})", DEFAULT_TEST_DURATION_SECONDS);
        logger.info("  threadPoolSize: 스레드 풀 크기 (기본값: {})", DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * JSONObject 성능 테스트
     */
    private static void testJSONObjectPerf() {
        runJsonPerfTest("JSONObject", (counters, requestId) -> {
            // JSONObject 생성 및 데이터 추가
            JSONObject jsonObject = new JSONObject();
            for (int j = 0; j < fieldCount; j++) {
                jsonObject.put("field" + j, "value" + j + "_" + requestId);
            }

            // JSON 문자열 생성
            String jsonString = jsonObject.toString();

            // 결과 검증 (간단히 길이만 확인)
            if (jsonString.length() > 0) {
                counters[0].incrementAndGet(); // successCount
            } else {
                counters[1].incrementAndGet(); // errorCount
            }
        });
    }

    /**
     * Jackson JsonGenerator 성능 테스트
     */
    private static void testJacksonPerf() {
        runJsonPerfTest("Jackson", (counters, requestId) -> {
            // StringWriter 생성 (각 요청마다 새로 생성)
            StringWriter writer = new StringWriter();

            // Jackson JsonGenerator 사용 (static factory 재사용)
            JsonGenerator generator = JSON_FACTORY.createGenerator(writer);

            generator.writeStartObject();
            for (int j = 0; j < fieldCount; j++) {
                generator.writeObjectField("field" + j, "value" + j + "_" + requestId);
            }
            generator.writeEndObject();
            generator.close();

            String jsonString = writer.toString();

            // 결과 검증 (간단히 길이만 확인)
            if (jsonString.length() > 0) {
                counters[0].incrementAndGet(); // successCount
            } else {
                counters[1].incrementAndGet(); // errorCount
            }
        });
    }

    /**
     * Gson 성능 테스트
     */
    private static void testGsonPerf() {
        runJsonPerfTest("Gson", (counters, requestId) -> {
            // Gson 객체 생성 (각 요청마다 새로 생성)
            Gson gson = new Gson();

            // Map을 사용하여 JSON 객체 생성
            java.util.Map<String, String> jsonMap = new java.util.HashMap<>();

            // 다수의 필드 추가
            for (int i = 0; i < fieldCount; i++) {
                jsonMap.put("field" + i, "value" + i + "_" + requestId);
            }

            // JSON 문자열 생성
            String jsonString = gson.toJson(jsonMap);

            // 결과 검증 (간단히 길이만 확인)
            if (jsonString.length() > 0) {
                counters[0].incrementAndGet(); // successCount
            } else {
                counters[1].incrementAndGet(); // errorCount
            }
        });
    }

    /**
     * 공통 JSON 성능 테스트 프레임워크
     * @param testName 테스트 이름
     * @param jsonTask 각 스레드에서 실행할 JSON 생성 작업 (counters, requestId)
     */
    private static void runJsonPerfTest(String testName, JsonTask jsonTask) {
        try {
            // 카운터 (successCount, errorCount, requestCount 순서)
            AtomicLong[] counters = new AtomicLong[]{
                new AtomicLong(0), // successCount
                new AtomicLong(0), // errorCount
                new AtomicLong(0)  // requestCount
            };
            AtomicBoolean testRunning = new AtomicBoolean(true);

            // GC 정보 수집 (시작)
            GCInfo initialGC = GCUtil.getGcInfo();

            // 테스트 시작 시간
            long testStartTime = System.currentTimeMillis();
            long testTargetEndTime = testStartTime + (testDurationSeconds * 1000L);

            // TPS 모니터링 스레드 시작
            Thread monitorThread = new Thread(() -> {
                long lastCount = 0;
                long lastTime = testStartTime;
                int second = 0;

                while (testRunning.get()) {
                    try {
                        Thread.sleep(1000);
                        second++;

                        long currentCount = counters[0].get(); // successCount
                        long currentTime = System.currentTimeMillis();

                        // 현재 초의 TPS 계산
                        double currentTPS = (double)(currentCount - lastCount) * 1000 / (currentTime - lastTime);

                        // 누적 평균 TPS 계산
                        double avgTPS = (double)currentCount * 1000 / (currentTime - testStartTime);

                        logger.info("[{}초] {} - 현재 TPS: {}, 평균 TPS: {}, 총 성공: {}",
                                   second, testName, String.format("%.2f", currentTPS), String.format("%.2f", avgTPS), currentCount);

                        lastCount = currentCount;
                        lastTime = currentTime;
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            monitorThread.start();

            // 스레드 생성 (최대 성능 측정)
            Thread[] threads = new Thread[threadPoolSize];
            for (int i = 0; i < threadPoolSize; i++) {
                final int threadIndex = i;
                threads[i] = new Thread(() -> {
                    int requestId = threadIndex; // 각 스레드별로 다른 시작 ID

                    while (testRunning.get() && System.currentTimeMillis() <= testTargetEndTime) {
                        try {
                            // JSON 생성 작업 실행
                            jsonTask.execute(counters, requestId);

                            counters[2].incrementAndGet(); // requestCount
                            requestId += threadPoolSize; // 다음 요청 ID (스레드 개수만큼 증가)

                        } catch (Exception e) {
                            counters[1].incrementAndGet(); // errorCount
                        }
                    }
                });
            }

            // 모든 스레드 시작
            for (Thread thread : threads) {
                thread.start();
            }

            // 테스트 종료 시간까지 대기
            Thread.sleep(testDurationSeconds * 1000L);
            testRunning.set(false);

            // 모니터링 스레드 종료
            monitorThread.interrupt();
            monitorThread.join(1000);

            // 모든 스레드 종료 대기
            for (Thread thread : threads) {
                thread.join(1000); // 최대 1초 대기
            }

            long actualTestEndTime = System.currentTimeMillis();

            // 결과 계산
            long totalDuration = actualTestEndTime - testStartTime;
            double actualTPS = (double) counters[0].get() * 1000 / totalDuration;

            // GC 정보 수집 (종료)
            GCInfo finalGC = GCUtil.getGcInfo();
            GCInfo gcDiff = finalGC.getPerfDiff(initialGC);

            // 결과 로그
            logger.info("=== {} 테스트 결과 ===", testName);
            logger.info("총 소요시간: {}ms", totalDuration);
            logger.info("성공 요청: {}", counters[0].get());
            logger.info("실패 요청: {}", counters[1].get());
            logger.info("실제 TPS: {}", String.format("%.2f", actualTPS));
            logger.info("총 요청 수: {}", counters[2].get());

            // GC 정보
            logger.info("=== GC 정보 ===");
            logger.info("Young GC 발생 횟수: {} -> {} (증가: {})",
                       initialGC.youngGCCount, finalGC.youngGCCount, gcDiff.youngGCCount);
            logger.info("Full GC 발생 횟수: {} -> {} (증가: {})",
                       initialGC.fullGCCount, finalGC.fullGCCount, gcDiff.fullGCCount);
            logger.info("Young GC 총 시간: {}ms -> {}ms (증가: {}ms)",
                       initialGC.youngGCTime, finalGC.youngGCTime, gcDiff.youngGCTime);
            logger.info("Full GC 총 시간: {}ms -> {}ms (증가: {}ms)",
                       initialGC.fullGCTime, finalGC.fullGCTime, gcDiff.fullGCTime);

        } catch (InterruptedException e) {
            logger.error("{} 테스트 중 인터럽트 발생", testName, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 구분선 생성
     */
    private static String createSeparator(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("=");
        }
        return sb.toString();
    }
}
