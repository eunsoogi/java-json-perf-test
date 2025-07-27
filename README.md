# Java JSON 성능 비교 테스트

JSONObject와 Jackson JsonGenerator의 성능을 비교하는 Maven 기반 테스트 프로젝트입니다.

## 📋 테스트 내용

1. **JSONObject vs Jackson JsonGenerator 성능 비교**
2. **각 JSON 객체당 1,000개 필드 생성**
3. **최대 성능 모드로 10초간 지속 테스트**
4. **GC(가비지 컬렉션) 성능 분석**
   - Young GC/Full GC 발생 횟수
   - GC 소요 시간
   - 각 구간별 처리 시간

## 🚀 주요 최적화 내용

### 1. 객체 재사용 최적화
- **JSONObject 최적화**: ThreadLocal을 사용한 JSONObject 재사용
- **Jackson 최적화**: ThreadLocal을 사용한 StringWriter 재사용
- **GC 압박 감소**: 메모리 할당 오버헤드 최소화

### 2. 코드 리팩토링
- **JsonTask 함수형 인터페이스** 도입
- **runJsonPerfTest() 공통 프레임워크** 생성
- **중복 코드 제거**: 약 200줄 → 50줄로 축소
- **유지보수성 향상**: 일관된 테스트 프레임워크

### 3. 성능 개선 결과
- **JSONObject**: 약 30배 성능 개선 (43 TPS → 1,275 TPS)
- **Jackson**: 약 23배 성능 개선 (2,100 TPS → 49,090 TPS)
- **최대 성능 모드**: TPS 제한 없이 최대 처리량 측정

## 🚀 실행 방법

### 사전 요구사항
- **JDK 8 이상** (JRE가 아닌 JDK 필요)
- **Maven 3.x**

### 기본 실행 (기본값 사용)
```bash
# 기본 설정으로 실행: 1000개 필드, 3000 TPS, 10초, 5개 스레드
mvn exec:java

# 또는 JAVA_HOME 지정 후 실행
export JAVA_HOME=/Users/사용자명/Downloads/jdk-8u431-macosx-x64/Contents/Home
mvn exec:java
```

### 런타임 인자 지정 실행
```bash
# 사용법: mvn exec:java -Dexec.args="[fieldCount] [durationSeconds] [threadPoolSize]"

# 예시 1: 500개 필드, 15초, 10개 스레드
mvn exec:java -Dexec.args="500 15 10"

# 예시 2: 2000개 필드, 5초, 8개 스레드
mvn exec:java -Dexec.args="2000 5 8"

# 예시 3: 필드 수만 변경 (500개)
mvn exec:java -Dexec.args="500"

# 예시 4: 필드 수와 지속 시간만 변경
mvn exec:java -Dexec.args="800 20"
```

### 런타임 인자 설명
- `fieldCount`: JSON 객체당 필드 개수 (기본값: 1000)
- `durationSeconds`: 테스트 지속 시간(초) (기본값: 10)
- `threadPoolSize`: 스레드 풀 크기 (기본값: 5)

**참고**: 목표 TPS 제한을 제거하고 최대 성능 모드로 동작합니다.

### 대체 실행 방법 (직접 Java 실행)
```bash
# Maven을 사용하지 않고 직접 Java 클래스 실행
export JAVA_HOME=/Users/사용자명/Downloads/jdk-8u431-macosx-x64/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
cd target/classes
# 기본 실행
java -cp ".:../../lib/*" com.eunsoogi.test.json.JsonPerfTest
# 런타임 인자 지정 실행
java -cp ".:../../lib/*" com.eunsoogi.test.json.JsonPerfTest 500 15 10
```

### 대안 실행 방법 (JAVA_HOME 임시 설정)
```bash
# JDK 경로를 임시로 설정하면서 실행
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_74.jdk/Contents/Home && mvn clean compile exec:java
```

## 📊 테스트 시나리오

각 테스트에서 **1,000개 필드**를 가진 JSON 객체를 생성합니다:
```json
{
  "field_0": "value_REQUEST_ID_0",
  "field_1": "value_REQUEST_ID_1",
  ...
  "field_999": "value_REQUEST_ID_999"
}
```

- **테스트 모드**: 최대 성능 모드 (TPS 제한 없음)
- **기본 테스트 시간**: 10초
- **기본 스레드 수**: 5개
- **로깅**: SLF4J + Logback (콘솔 + 파일)

## 🏁 성능 결과

### 최적화 후 성능 (최대 성능 모드)
- **JSONObject**: 약 1,275 TPS
- **Jackson**: 약 49,090 TPS
- **Jackson 우위**: 약 38배 더 빠른 처리 속도

### GC 성능 비교
- **JSONObject**: Young GC 257회, Full GC 0회
- **Jackson**: Young GC 285회, Full GC 0회
- **메모리 효율성**: Jackson이 더 효율적인 메모리 사용 패턴

## 📈 결과 분석 항목

1. **성능 지표**
   - 실제 달성 TPS
   - 개별 요청 처리 시간
   - 전체 테스트 소요 시간

2. **GC 성능**
   - Young GC 발생 횟수 및 시간
   - Full GC 발생 횟수 및 시간
   - 메모리 효율성 비교

3. **안정성**
   - 성공/실패 요청 수
   - 에러율

## 📁 프로젝트 구조

```
java-json-perf-test/
├── src/main/java/com/eunsoogi/
│   ├── test/json/
│   │   └── JsonPerfTest.java           # 메인 테스트 클래스
│   └── util/
│       ├── GCUtil.java                 # GC 성능 분석 유틸리티
│       └── GCInfo.java                 # GC 정보 데이터 클래스
├── src/main/resources/
│   └── logback.xml                     # 로깅 설정
├── pom.xml                             # Maven 설정
├── output.log                          # 테스트 결과 로그 파일
└── README.md
```

## 🔧 기술 스택

| 구분 | 라이브러리/도구 | 버전 |
|------|----------------|------|
| **JSON 처리** | org.json | 20180813 |
| **JSON 처리** | Jackson Core | 2.10.5 |
| **로깅** | SLF4J API | 1.7.30 |
| **로깅** | Logback Classic | 1.2.3 |
| **빌드** | Maven | 3.x |
| **Java** | JDK | 8+ |

## 📝 로그 출력

테스트 실행 시 다음과 같은 정보가 출력됩니다:
- **실시간 처리 현황**: 1초마다 현재 TPS 및 평균 TPS 출력
- **최종 성능 결과**: 전체 TPS, 총 처리 건수, 테스트 시간
- **GC 발생 횟수 및 소요 시간**: Young GC/Full GC 통계
- **로그 파일**: `json-performance-test.log`

### 출력 예시
```
[JSONObject 테스트] 현재 TPS: 1,280 | 평균 TPS: 1,275 | 총 처리: 12,759건
[Jackson 테스트] 현재 TPS: 49,500 | 평균 TPS: 49,090 | 총 처리: 490,906건
```

## 🏆 결론

**Jackson JsonGenerator**가 모든 측면에서 압도적으로 우수한 성능을 보입니다:
- **처리 속도**: 약 38배 빠른 JSON 생성 성능
- **메모리 효율성**: 더 효율적인 메모리 사용 패턴
- **안정성**: 긴 시간 동안 일관된 고성능 유지
- **확장성**: 대용량 JSON 처리에 더 적합

### 권장사항
고성능 JSON 처리가 필요한 프로덕션 환경에서는 **Jackson JsonGenerator** 사용을 강력히 권장합니다.
