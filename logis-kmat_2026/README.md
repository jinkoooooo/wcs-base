# KMAT 2026 WCS 모듈

KMAT 2026 전시회 시나리오 자동화 WCS(Warehouse Control System) 모듈.
Sineva사 AGF/AMR ECS 모듈과 TSPG사 4way shuttle WCS 모듈을 통합하여 전시회 데모 시나리오를 실행한다.

---

## 목차

1. [모듈 구조](#1-모듈-구조)
2. [시나리오 개요 (비개발자용)](#2-시나리오-개요-비개발자용)
3. [시나리오 상세 흐름](#3-시나리오-상세-흐름)
4. [핵심 클래스 상세](#4-핵심-클래스-상세)
5. [DB 테이블 구조](#5-db-테이블-구조)
6. [상태 코드 정의](#6-상태-코드-정의)
7. [API 엔드포인트](#7-api-엔드포인트)
8. [개발자 가이드](#8-개발자-가이드)

---

## 1. 모듈 구조

```
logis-kmat_2026/src/main/java/operato/logis/kmat_2026/
├── biz/
│   ├── ecs/sineva/           # Sineva AGF/AMR ECS 모듈
│   │   ├── consts/           # 상수 정의 (CommandType, CbkStatus 등)
│   │   ├── event/            # 이벤트 처리
│   │   ├── processor/        # 작업 프로세서 (입고, 출고, 리필)
│   │   ├── service/          # 핵심 서비스
│   │   ├── support/          # 예외 처리
│   │   └── SinevaEcsFacade.java  # ECS 진입점
│   │
│   ├── wcs/tspg_4way_shuttle/    # TSPG 4way shuttle WCS 모듈
│   │   ├── consts/               # 상수 정의 (상태 코드, 에러 코드)
│   │   ├── dto/                  # 데이터 전송 객체
│   │   ├── facade/               # WCS 파사드
│   │   ├── handler/              # 주문 유형별 핸들러
│   │   ├── rest/                 # REST 컨트롤러
│   │   └── service/              # 핵심 서비스
│   │
│   └── wcs/kmat_2026/            # KMAT 2026 시나리오 통합 모듈
│       ├── consts/               # 로케이션 매핑
│       ├── dto/                  # 시나리오 컨텍스트
│       ├── rest/                 # 시나리오 컨트롤러
│       └── service/              # 시나리오 서비스
│
├── entity/                   # JPA 엔티티 (DB 테이블)
├── service/impl/             # 엔티티 CRUD 서비스
└── common/util/              # 공통 유틸리티
```

---

## 2. 시나리오 개요 (비개발자용)

### 2.1 시스템 구성

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                         KMAT 2026 전시회 시스템                                 │
├───────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                    WCS (본 모듈) - KMAT 2026 시나리오                      │ │
│  │  ┌──────────────────────┐    ┌──────────────────────┐                   │ │
│  │  │ KMat2026WcsFacade    │    │ KMat2026TspgCallback │                   │ │
│  │  │ (시나리오 제어)       │◀───│ Handler              │◀──┐               │ │
│  │  └──────────┬───────────┘    │ (콜백 수신)           │   │               │ │
│  │             │                └──────────────────────┘   │               │ │
│  │             │ Step 실행                                 │ TSPG 콜백     │ │
│  │             ▼                                           │ (완료/도착)   │ │
│  │  ┌──────────────────────┐    ┌──────────────────────┐   │               │ │
│  │  │ KMat2026Scenario     │───▶│ DirectWcsOrderService│───┼───┐           │ │
│  │  │ Service (Step 실행)   │    │ (WCS 명령 실행)       │   │   │           │ │
│  │  └──────────┬───────────┘    └──────────────────────┘   │   │           │ │
│  │             │                                           │   │           │ │
│  │             │ AGF 지시                                  │   │ TSPG 지시 │ │
│  │             ▼                                           │   ▼           │ │
│  │  ┌──────────────────────┐                    ┌──────────────────────┐   │ │
│  │  │ SinevaEcsFacade      │                    │ EcsCommandService    │   │ │
│  │  │ (AGF ECS 진입점)      │                    │ (TSPG 명령 전송)      │   │ │
│  │  └──────────┬───────────┘                    └──────────┬───────────┘   │ │
│  └─────────────┼────────────────────────────────────────────┼──────────────┘ │
│                │                                            │                │
│                │ AGF 지시                                   │ 셔틀 지시       │
│                ▼                                            ▼                │
│  ┌──────────────────────┐                    ┌──────────────────────────┐   │
│  │ Sineva AGF/AMR       │                    │ TSPG 4Way Shuttle WCS    │   │
│  │ (파렛트 운반 로봇)     │                    │ (파렛트 이동 설비)         │   │
│  └──────────┬───────────┘                    └──────────┬───────────────┘   │
│             │                                           │                   │
│             │ AGF 콜백                                   │ 셔틀 콜백         │
│             │ (END/SUCCESS)                             │ (COMPLETE/        │
│             ▼                                           │  RACK_CONVEYOR_   │
│  ┌──────────────────────┐                               │  ARRIVED)         │
│  │ EquipCallback        │                               │                   │
│  │ RoutingService       │──────────────┐                │                   │
│  │ (AGF 콜백 라우팅)     │              │                │                   │
│  └──────────────────────┘              │                │                   │
│                                        ▼                ▼                   │
│                              ┌───────────────────────────────────┐          │
│                              │ EcsCallbackProcessor              │          │
│                              │ (TSPG 콜백 처리)                   │          │
│                              │ → KMat2026TspgCallbackHandler 호출 │          │
│                              └───────────────────────────────────┘          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 콜백 흐름 상세

```
[TSPG 콜백 흐름]
TSPG 4Way Shuttle
    │ HTTP POST (EcsCallbackRequest)
    ▼
EcsCallbackController.receiveCallback()
    │ 경로: /wcs/tspg/callback
    ▼
EcsCallbackProcessor.process()
    │ status별 분기 처리
    ├─ COMPLETE → handler.handleCompletion() + KMat2026TspgCallbackHandler.handleComplete()
    └─ RACK_CONVEYOR_ARRIVED → handler.handleRackConveyorArrived() + KMat2026TspgCallbackHandler.handleConveyorArrived()

[AGF 콜백 흐름]
Sineva AGF/AMR
    │ AGF 콜백 수신
    ▼
SinevaCallbackReceiveService
    │
    ▼
EquipCallbackRoutingService.route()
    │ cbkStatus별 분기 처리
    ├─ END → commandType별 분기
    │        ├─ K_MAT_TSPG_CONVEYOR_INBOUND → SinevaEcsFacade.handleTspgConveyorInboundRefillCallback()
    │        └─ K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND → SinevaEcsFacade.handleTspgConveyorOutboundCallback()
    │
    ▼
SinevaTaskReportEvent 발행
    │
    ▼
KMat2026SinevaTaskReportEventListener (이벤트 수신 후 추가 처리)
```

### 2.3 물리적 구조

```
                    2층 (입고 목적지)
    ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
    │20401│20301│20201│20402│20302│20202│20403│20303│20203│
    └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┘
       │     │     │     │     │     │     │     │     │
       │     │     │     │   엘리베이터/셔틀 이동      │
       │     │     │     │     │     │     │     │     │
    ┌──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┐
    │10401│10301│10201│10402│10302│10202│10403│10303│10203│
    └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
                    1층 (출고 소스)

    ┌────────────────────────┐     ┌────────────────────────┐
    │ 출고단 10601           │     │ 출고단 10602           │
    │ (ECS: TSPG_CONV_OUT_01)│     │ (ECS: TSPG_CONV_OUT_02)│
    │ → AGF가 픽업하는 위치   │     │ → AGF가 픽업하는 위치   │
    └────────────────────────┘     └────────────────────────┘

    ┌────────────────────────┐     ┌────────────────────────┐
    │ 입고단                 │     │ POD 버퍼               │
    │ (ECS: TSPG_CONV_IN_01) │     │ (ECS: POD_BUFFER_01)   │
    │ → AGF가 파렛트 내려놓음 │     │ → 입고단 대기 파렛트    │
    │ → TSPG가 2층으로 입고   │     │ → AGF가 입고단으로 이동 │
    └────────────────────────┘     └────────────────────────┘
```

### 2.4 시나리오 동작 설명 (1 사이클 상세)

**1 라운드 = 3 사이클, 1 사이클 = 파렛트 2개 순환**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ [1사이클 전체 흐름 - 파렛트 2개 처리]                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│ ① Step1: 4개 MOVE 지시 동시 생성 (WCS → TSPG)                               │
│    ├─ 출고1: 1층(10401) → 출고단(10601)    [outbound1OrderKey]              │
│    ├─ 출고2: 1층(10301) → 출고단(10602)    [outbound2OrderKey]              │
│    ├─ 이동1: 2층(20401) → 1층(10401)       [move1OrderKey]                  │
│    └─ 이동2: 2층(20301) → 1층(10301)       [move2OrderKey]                  │
│                                                                             │
│ ② Step2: 출고1 완료 콜백 (TSPG → WCS)                                       │
│    ├─ TSPG 콜백: outbound1OrderKey 완료 (status=90)                         │
│    └─ WCS → AGF 지시: 출고단1(TSPG_CONV_OUT_01) → 입고단(TSPG_CONV_IN_01)   │
│       └─ 입고단이 비어있으므로 입고단으로 이동                                │
│                                                                             │
│ ③ Step3: 출고2 완료 콜백 (TSPG → WCS)                                       │
│    ├─ TSPG 콜백: outbound2OrderKey 완료 (status=90)                         │
│    └─ WCS → AGF 지시: 출고단2(TSPG_CONV_OUT_02) → POD버퍼(POD_BUFFER_01)    │
│       └─ 입고단이 사용중이므로 POD 버퍼로 이동                                │
│                                                                             │
│ ④ 2층 이동 완료 + 렉단 컨베이어 도착 콜백 (TSPG → WCS)                       │
│    ├─ 이동1 완료: move1OrderKey 완료 (status=90)                             │
│    │   └─ 2층 출발지(20401)를 빈자리 풀에 추가                               │
│    │                                                                        │
│    ├─ 이동2 렉단 컨베이어 도착: move2OrderKey (status=RACK_CONVEYOR_ARRIVED) │
│    │   └─ "마지막 파렛트가 렉단 컨베이어에 도착" = 입고 시작 트리거           │
│    │                                                                        │
│    └─ Step4: 입고단 파렛트 입고 지시 (WCS → TSPG)                            │
│         ├─ AGF가 입고단에 파렛트 내려놓음 완료 확인                          │
│         ├─ TSPG 입고 명령: 입고단 → 2층 빈자리(20401) [inbound1OrderKey]     │
│         └─ ECS 입고단 상태 EMPTY로 업데이트 (다음 파렛트 받을 준비)          │
│                                                                             │
│ ⑤ Step5: 입고1 완료 + POD 버퍼 → 입고단 이동 (AGF 리필)                      │
│    ├─ TSPG 콜백: inbound1OrderKey 완료 (status=90)                          │
│    ├─ WCS → AGF 지시: POD버퍼(POD_BUFFER_01) → 입고단(TSPG_CONV_IN_01)      │
│    │   └─ TspgConveyorInboundRefillProcessor가 버퍼에서 파렛트 당겨옴        │
│    │                                                                        │
│    └─ AGF 도착 후 → Step6: 입고2 지시 (WCS → TSPG)                          │
│         ├─ AGF 콜백: END (버퍼→입고단 이동 완료)                             │
│         ├─ ECS 입고단 상태 FULL로 업데이트                                   │
│         └─ TSPG 입고 명령: 입고단 → 2층 빈자리(20301) [inbound2OrderKey]     │
│                                                                             │
│ ⑥ Step6: 입고2 완료 → 다음 사이클/라운드 전환                                │
│    ├─ TSPG 콜백: inbound2OrderKey 완료 (status=90)                          │
│    │                                                                        │
│    └─ if (마지막 사이클):                                                    │
│           라운드 번호 증가 → DB 재조회 → 새 라운드 시작                      │
│       else:                                                                  │
│           다음 사이클로 전환 → Step1 재실행                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.5 콜백 트리거 요약

| 콜백 소스 | 콜백 유형 | 트리거 조건 | 후속 동작 |
| --- | --- | --- | --- |
| TSPG | COMPLETE (status=90) | outbound1 완료 | AGF 지시: 출고단1 → 입고단 |
| TSPG | COMPLETE (status=90) | outbound2 완료 | AGF 지시: 출고단2 → POD 버퍼 |
| TSPG | COMPLETE (status=90) | move1 완료 | 2층 빈자리 풀에 추가 |
| TSPG | RACK_CONVEYOR_ARRIVED | move2 렉단 도착 | 입고1 지시 생성 |
| TSPG | COMPLETE (status=90) | inbound1 완료 | AGF 리필 (버퍼→입고단) |
| AGF | END | 버퍼→입고단 이동 완료 | 입고2 지시 생성 |
| TSPG | COMPLETE (status=90) | inbound2 완료 | 사이클/라운드 전환 |

### 2.6 입고 트리거 조건 상세

**입고1 시작 조건 (렉단 컨베이어 도착 콜백)**:
```
move2OrderKey의 RACK_CONVEYOR_ARRIVED 콜백 수신 시:
  1. 2층에서 1층으로 내려오는 마지막 파렛트가 렉단 컨베이어에 도착
  2. 이 시점에 AGF가 입고단에 파렛트를 내려놓을 준비가 됨
  3. WCS가 TSPG에 입고 명령 생성 (입고단 → 2층 빈자리)
  4. ECS 입고단 상태를 EMPTY로 업데이트 (AGF 파렛트 수신 가능)
```

**입고2 시작 조건 (AGF 리필 완료)**:
```
inbound1OrderKey 완료 + AGF 리필 완료 시:
  1. inbound1 완료 → ECS 입고단 EMPTY 상태
  2. AGF가 POD 버퍼 파렛트를 입고단으로 이동
  3. AGF END 콜백 수신 → ECS 입고단 FULL 상태
  4. WCS가 TSPG에 입고 명령 생성 (입고단 → 2층 빈자리)
```

---

## 3. 시나리오 상세 흐름

### 3.1 라운드/사이클 구조

```
시나리오 시작
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ 라운드 1 (DB 조회 → CyclePoint 3개 생성)                     │
│  ├─ 사이클 0: [Step1 → Step2 → Step3 → Step4 → Step5 → Step6]│
│  ├─ 사이클 1: [Step1 → Step2 → Step3 → Step4 → Step5 → Step6]│
│  └─ 사이클 2: [Step1 → Step2 → Step3 → Step4 → Step5 → Step6]│
│                    └─ 마지막 사이클 완료 → 다음 라운드       │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ 라운드 2 (DB 재조회 → 새 CyclePoint 3개 생성)               │
│  └─ ... 반복                                                 │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 CyclePoint 구성

각 사이클에서 처리할 위치 정보:

| 필드           | 설명                           | 예시        |
| --- | --- | --- |
| outbound1Loc  | 출고1 출발지 (1층 파렛트 위치)  | 10401      |
| outbound2Loc  | 출고2 출발지 (1층 파렛트 위치)  | 10301      |
| move1From     | 이동1 출발지 (2층 파렛트 위치)  | 20401      |
| move1To       | 이동1 목적지 (1층 빈 위치)      | 10401 (동적) |
| move2From     | 이동2 출발지 (2층 파렛트 위치)  | 20301      |
| move2To       | 이동2 목적지 (1층 빈 위치)      | 10301 (동적) |
| inbound1ToLoc | 입고1 목적지 (2층 빈 위치)      | 20401 (동적) |
| inbound2ToLoc | 입고2 목적지 (2층 빈 위치)      | 20301 (동적) |

### 3.3 Step별 상세 (클래스/메서드 매핑 포함)

#### Step1: 4개 MOVE 지시 동시 생성 (WCS → TSPG)
```
┌──────────────────────────────────────────────────────────────────────────────┐
│ Step1 시작                                                                    │
│                                                                              │
│ [호출 흐름]                                                                   │
│ KMat2026WcsFacade.startScenario()                                            │
│   └→ KMat2026ScenarioService.executeStep1(ctx)                               │
│       └→ DirectWcsOrderService.execute(WcsOrderCommand)  x 4번               │
│           └→ MoveOrderHandler.createShuttleOrder()                           │
│               └→ EcsCommandService.sendCommand()  // TSPG로 전송             │
│                                                                              │
│ [생성되는 작업]                                                               │
│   1. outbound1Loc → 10601 (MOVE)  → outbound1OrderKey 생성                   │
│   2. outbound2Loc → 10602 (MOVE)  → outbound2OrderKey 생성                   │
│   3. move1From → move1To (MOVE)   → move1OrderKey 생성                       │
│   4. move2From → move2To (MOVE)   → move2OrderKey 생성                       │
│                                                                              │
│ [DB 저장]                                                                     │
│   - tb_wcs_shuttle_order: 4개 레코드 INSERT                                  │
│   - tb_wcs_shuttle_order_item: 4개 레코드 INSERT                             │
│   - tb_wcs_loc_mst: 관련 로케이션 lock_yn=1로 UPDATE                         │
│                                                                              │
│ → ctx.setCurrentStep(STEP1_DONE)                                             │
│ → TSPG 콜백 대기                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Step2: outbound1 완료 콜백 → AGF 지시 (TSPG → WCS → AGF)
```
┌──────────────────────────────────────────────────────────────────────────────┐
│ [콜백 수신 흐름]                                                              │
│ TSPG 4Way Shuttle                                                            │
│   │ HTTP POST /wcs/tspg/callback                                             │
│   │ { "orderKey": "outbound1OrderKey", "status": "COMPLETE" }                │
│   ▼                                                                          │
│ EcsCallbackController.receiveCallback()                                       │
│   └→ EcsCallbackProcessor.process()                                          │
│       ├→ MoveOrderHandler.handleCompletion()  // 재고 이동, 로케이션 해제     │
│       └→ KMat2026TspgCallbackHandler.handleComplete(shuttleOrder)            │
│           └→ routeOutbound1Complete()                                        │
│               └→ KMat2026ScenarioService.executeStep2(ctx)                   │
│                                                                              │
│ [Step2 실행]                                                                  │
│ KMat2026ScenarioService.executeStep2(ctx)                                    │
│   ├→ ctx.addFloor1EmptyLoc(outbound1Loc)  // 1층 빈자리 풀에 추가            │
│   ├→ KMat2026LocationService.updateEcsOutboundLocToFull("10601")             │
│   │   └→ tb_ecs_loc_mst UPDATE: TSPG_CONV_OUT_01 → FULL                      │
│   │                                                                          │
│   └→ SinevaEcsFacade.handleTspgConveyorOutboundExecute("TSPG_CONV_OUT_01")   │
│       └→ TspgConveyorOutboundProcessor.execute()                             │
│           ├→ 입고단 상태 확인: EMPTY → 입고단으로 이동                        │
│           └→ AGF 명령 생성: from=TSPG_CONV_OUT_01, to=TSPG_CONV_IN_01        │
│              └→ tb_wcs_order INSERT (commandType=K_MAT_TSPG_CONVEYOR_INBOUND)│
│                                                                              │
│ → ctx.setCurrentStep(STEP2_DONE)                                             │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Step3: outbound2 완료 콜백 → AGF 지시 (TSPG → WCS → AGF)
```
┌──────────────────────────────────────────────────────────────────────────────┐
│ [콜백 수신 흐름]                                                              │
│ TSPG 4Way Shuttle                                                            │
│   │ HTTP POST /wcs/tspg/callback                                             │
│   │ { "orderKey": "outbound2OrderKey", "status": "COMPLETE" }                │
│   ▼                                                                          │
│ EcsCallbackController.receiveCallback()                                       │
│   └→ EcsCallbackProcessor.process()                                          │
│       └→ KMat2026TspgCallbackHandler.handleComplete(shuttleOrder)            │
│           └→ routeOutbound2Complete()                                        │
│               └→ KMat2026ScenarioService.executeStep3(ctx)                   │
│                                                                              │
│ [Step3 실행]                                                                  │
│ KMat2026ScenarioService.executeStep3(ctx)                                    │
│   ├→ ctx.addFloor1EmptyLoc(outbound2Loc)  // 1층 빈자리 풀에 추가            │
│   ├→ KMat2026LocationService.updateEcsOutboundLocToFull("10602")             │
│   │   └→ tb_ecs_loc_mst UPDATE: TSPG_CONV_OUT_02 → FULL                      │
│   │                                                                          │
│   └→ SinevaEcsFacade.handleTspgConveyorOutboundExecute("TSPG_CONV_OUT_02")   │
│       └→ TspgConveyorOutboundProcessor.execute()                             │
│           ├→ 입고단 상태 확인: FULL (Step2의 AGF가 사용중)                    │
│           └→ AGF 명령 생성: from=TSPG_CONV_OUT_02, to=POD_BUFFER_01          │
│              └→ tb_wcs_order INSERT (commandType=K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND)
│                                                                              │
│ → ctx.setCurrentStep(STEP3_DONE)                                             │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Step4: move2 렉단 컨베이어 도착 콜백 → 입고1 지시 생성 (TSPG → WCS → TSPG)
```
┌──────────────────────────────────────────────────────────────────────────────┐
│ [콜백 수신 흐름 - 렉단 컨베이어 도착]                                          │
│ TSPG 4Way Shuttle                                                            │
│   │ HTTP POST /wcs/tspg/callback                                             │
│   │ { "orderKey": "move2OrderKey", "status": "RACK_CONVEYOR_ARRIVED" }       │
│   ▼                                                                          │
│ EcsCallbackController.receiveCallback()                                       │
│   └→ EcsCallbackProcessor.process()                                          │
│       ├→ MoveOrderHandler.handleRackConveyorArrived()  // 현재 빈 처리       │
│       └→ KMat2026TspgCallbackHandler.handleConveyorArrived(orderKey)         │
│           └→ routeMove2ConveyorArrived()                                     │
│               └→ KMat2026ScenarioService.executeStep4(ctx)                   │
│                                                                              │
│ [Step4 실행 - 입고1 지시 생성]                                                │
│ KMat2026ScenarioService.executeStep4(ctx)                                    │
│   ├→ ctx.addFloor2EmptyLoc(move2From)  // 2층 빈자리 풀에 추가               │
│   │                                                                          │
│   ├→ KMat2026LocationService.updateEcsInboundLocToEmpty()                    │
│   │   └→ tb_ecs_loc_mst UPDATE: TSPG_CONV_IN_01 → EMPTY (AGF 수신 준비)      │
│   │                                                                          │
│   └→ 입고1 지시 생성                                                          │
│       ├→ inbound1ToLoc = ctx.pollFloor2EmptyLoc()  // 2층 빈자리에서 꺼내기  │
│       └→ DirectWcsOrderService.execute(INBOUND: 입고단 → inbound1ToLoc)      │
│           └→ InboundOrderHandler.createShuttleOrder()                        │
│               └→ EcsCommandService.sendCommand()  // TSPG로 입고 명령        │
│                  └→ tb_wcs_shuttle_order INSERT (orderType=INBOUND)          │
│                  └→ ctx.setInbound1OrderKey(...)                             │
│                                                                              │
│ → ctx.setCurrentStep(STEP4_DONE)                                             │
│ → TSPG 입고1 완료 콜백 대기                                                   │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Step5: inbound1 완료 콜백 → AGF 리필 → 입고2 지시 생성
```
┌──────────────────────────────────────────────────────────────────────────────┐
│ [콜백 수신 흐름 - 입고1 완료]                                                  │
│ TSPG 4Way Shuttle                                                            │
│   │ HTTP POST /wcs/tspg/callback                                             │
│   │ { "orderKey": "inbound1OrderKey", "status": "COMPLETE" }                 │
│   ▼                                                                          │
│ EcsCallbackController.receiveCallback()                                       │
│   └→ EcsCallbackProcessor.process()                                          │
│       ├→ InboundOrderHandler.handleCompletion()  // 재고 생성, 로케이션 해제  │
│       └→ KMat2026TspgCallbackHandler.handleComplete(shuttleOrder)            │
│           └→ routeInbound1Complete()                                         │
│               └→ KMat2026ScenarioService.executeStep5(ctx)                   │
│                                                                              │
│ [Step5 실행 - AGF 리필 트리거]                                                │
│ KMat2026ScenarioService.executeStep5(ctx)                                    │
│   └→ KMat2026LocationService.updateEcsInboundLocToEmpty()                    │
│       └→ tb_ecs_loc_mst UPDATE: TSPG_CONV_IN_01 → EMPTY                      │
│                                                                              │
│ [AGF 리필 처리 - POD 버퍼 → 입고단]                                           │
│ (입고단 EMPTY 트리거 또는 직접 호출)                                          │
│ SinevaEcsFacade.handleTspgConveyorInboundRefillExecute()                     │
│   └→ TspgConveyorInboundRefillProcessor.execute()                            │
│       ├→ 소스 선택: POD_BUFFER_01 (버퍼 우선) 또는 출고단 (location_seq 순)  │
│       └→ AGF 명령 생성: from=POD_BUFFER_01, to=TSPG_CONV_IN_01               │
│          └→ tb_wcs_order INSERT (commandType=K_MAT_TSPG_BUFFER_TO_CONVEYOR_INBOUND)
│                                                                              │
│ → ctx.setCurrentStep(STEP5_DONE)                                             │
│ → AGF END 콜백 대기                                                           │
│                                                                              │
├──────────────────────────────────────────────────────────────────────────────┤
│ [AGF 콜백 수신 - 리필 완료 → 입고2 지시 생성]                                  │
│ Sineva AGF                                                                   │
│   │ AGF 콜백 (cbkStatus=END)                                                 │
│   ▼                                                                          │
│ EquipCallbackRoutingService.route()                                          │
│   └→ handleTaskEnd()                                                         │
│       └→ SinevaEcsFacade.handleTspgConveyorInboundRefillCallback(order)      │
│           ├→ tb_ecs_loc_mst UPDATE: TSPG_CONV_IN_01 → FULL (파렛트 도착)     │
│           │                                                                  │
│           └→ 입고2 지시 생성                                                  │
│               ├→ inbound2ToLoc = ctx.pollFloor2EmptyLoc()                    │
│               └→ DirectWcsOrderService.execute(INBOUND: 입고단 → inbound2ToLoc)
│                   └→ InboundOrderHandler.createShuttleOrder()                │
│                       └→ EcsCommandService.sendCommand()                     │
│                          └→ ctx.setInbound2OrderKey(...)                     │
│                                                                              │
│ → TSPG 입고2 완료 콜백 대기                                                   │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Step6: inbound2 완료 콜백 → 사이클/라운드 전환
```
┌──────────────────────────────────────────────────────────────────────────────┐
│ [콜백 수신 흐름 - 입고2 완료]                                                  │
│ TSPG 4Way Shuttle                                                            │
│   │ HTTP POST /wcs/tspg/callback                                             │
│   │ { "orderKey": "inbound2OrderKey", "status": "COMPLETE" }                 │
│   ▼                                                                          │
│ EcsCallbackController.receiveCallback()                                       │
│   └→ EcsCallbackProcessor.process()                                          │
│       ├→ InboundOrderHandler.handleCompletion()  // 재고 생성                │
│       └→ KMat2026TspgCallbackHandler.handleComplete(shuttleOrder)            │
│           └→ routeInbound2Complete()                                         │
│               └→ KMat2026ScenarioService.executeStep6(ctx)                   │
│                                                                              │
│ [Step6 실행 - 사이클/라운드 전환]                                             │
│ KMat2026ScenarioService.executeStep6(ctx)                                    │
│   │                                                                          │
│   ├─ if (cycleIndexInRound < CYCLES_PER_ROUND - 1):                          │
│   │     // 다음 사이클로 전환                                                 │
│   │     ctx.setCycleIndexInRound(cycleIndexInRound + 1)                      │
│   │     ctx.clearOrderKeys()  // 주문키 초기화                                │
│   │     executeStep1(ctx)     // Step1 재실행                                 │
│   │                                                                          │
│   └─ else:                                                                   │
│         // 마지막 사이클 완료 → 다음 라운드                                   │
│         ctx.setRoundNumber(roundNumber + 1)                                  │
│         ctx.setCycleIndexInRound(0)                                          │
│         KMat2026RoundPlanService.buildRoundPlan(roundNumber + 1)             │
│           └→ DB 재조회 → 새 CyclePoint 3개 생성                              │
│         executeStep1(ctx)                                                    │
│                                                                              │
│ → ctx.setCurrentStep(STEP6_DONE) 후 즉시 STEP1_DONE으로 전환                  │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 핵심 클래스 상세

### 4.1 KMAT 2026 시나리오 모듈

#### KMat2026WcsFacade.java
**경로**: `biz/wcs/kmat_2026/service/KMat2026WcsFacade.java`

시나리오의 최상위 진입점.

| 메서드                         | 설명                                    |
| --- | --- |
| `startScenario()`             | 시나리오 시작. 컨텍스트 생성 후 Step1 실행 |
| `stopScenario()`              | 시나리오 정지 (PAUSED 상태)              |
| `resumeScenario()`            | PAUSED → STEP1_DONE 복원               |
| `resetScenario()`             | 시나리오 완전 초기화                     |
| `restartCurrentRound(ctx)`    | 현재 라운드 재시작                       |
| `getContext()`                | 현재 시나리오 컨텍스트 반환               |
| `getStatus()`                 | 시나리오 상태 조회                       |
| `getLocationStatus()`         | 로케이션 상태 조회                       |
| `saveToDb()`                  | 현재 상태 DB 저장                        |

**초기화 흐름**:
```java
@PostConstruct
public void init() {
    loadFromDb();  // DB에서 이전 상태 복원
}
```

---

#### KMat2026ScenarioContext.java
**경로**: `biz/wcs/kmat_2026/dto/KMat2026ScenarioContext.java`

시나리오 실행 상태를 메모리에 보관.

| 필드                    | 타입                          | 설명                  |
| --- | --- | --- |
| scenarioId             | String                        | 시나리오 고유 ID       |
| currentStep            | ScenarioStep (enum)           | 현재 스텝 상태         |
| roundNumber            | int                           | 현재 라운드 번호       |
| cycleIndexInRound      | int                           | 라운드 내 사이클 인덱스 |
| roundCyclePoints       | List\<CyclePoint\>            | 라운드 내 3개 CyclePoint |
| outbound1OrderKey      | String                        | 출고1 주문키           |
| outbound2OrderKey      | String                        | 출고2 주문키           |
| move1OrderKey          | String                        | 이동1 주문키           |
| move2OrderKey          | String                        | 이동2 주문키           |
| inbound1OrderKey       | String                        | 입고1 주문키           |
| inbound2OrderKey       | String                        | 입고2 주문키           |
| floor1EmptyPool        | Queue\<String\>               | 1층 빈 위치 풀         |
| floor2EmptyPool        | Queue\<String\>               | 2층 빈 위치 풀         |

**ScenarioStep 열거형**:
| 값             | 설명              |
| --- | --- |
| INITIALIZED   | 초기화 완료        |
| STEP1_RUNNING | Step1 실행 중     |
| STEP1_DONE    | Step1 완료, 대기   |
| STEP2_DONE    | Step2 완료        |
| STEP3_DONE    | Step3 완료        |
| STEP4_DONE    | Step4 완료        |
| STEP5_DONE    | Step5 완료        |
| STEP6_DONE    | Step6 완료        |
| PAUSED        | 일시 정지          |
| ERROR         | 에러 발생          |

---

#### KMat2026ScenarioService.java
**경로**: `biz/wcs/kmat_2026/service/KMat2026ScenarioService.java`

각 Step의 실제 실행 로직.

| 메서드                         | 설명                                    |
| --- | --- |
| `executeStep1(ctx)`           | 4개 MOVE 지시 생성                      |
| `executeStep2(ctx)`           | AGF 출고 지시 (10601→입고단)            |
| `executeStep3(ctx)`           | AGF 출고 지시 (10602→POD버퍼)           |
| `executeStep4(ctx)`           | ECS 입고단 EMPTY 처리                   |
| `executeStep5(ctx)`           | ECS 입고단 EMPTY 처리                   |
| `executeAgfInboundRefill()`   | AGF 입고 리필 실행                      |
| `createInboundOrder(from,to)` | TSPG 입고 지시 생성                     |

---

#### KMat2026TspgCallbackHandler.java
**경로**: `biz/wcs/kmat_2026/service/KMat2026TspgCallbackHandler.java`

TSPG 셔틀 WCS에서 콜백을 수신하여 KMAT 시나리오의 다음 Step을 실행.

**호출 경로**:
```
TSPG 4Way Shuttle (HTTP POST /wcs/tspg/callback)
    ↓
EcsCallbackController.receiveCallback()
    ↓
EcsCallbackProcessor.process()
    ├─ COMPLETE → handler.handleCompletion() + KMat2026TspgCallbackHandler.handleComplete()
    └─ RACK_CONVEYOR_ARRIVED → handler.handleRackConveyorArrived() + KMat2026TspgCallbackHandler.handleConveyorArrived()
```

| 메서드                              | 설명                              |
| --- | --- |
| `handleComplete(order)`            | 셔틀 오더 완료 콜백 처리 (status=90) |
| `handleConveyorArrived(orderKey)`  | 렉단 컨베이어 도착 콜백 처리 (RACK_CONVEYOR_ARRIVED) |

**콜백 라우팅 규칙 상세**:

| 오더키 | 콜백 유형 | 후속 처리 |
| --- | --- | --- |
| outbound1OrderKey | COMPLETE | routeOutbound1Complete() → executeStep2() |
| outbound2OrderKey | COMPLETE | routeOutbound2Complete() → executeStep3() |
| move1OrderKey | COMPLETE | 2층 EMPTY 풀에 추가만 |
| move2OrderKey | RACK_CONVEYOR_ARRIVED | routeMove2ConveyorArrived() → executeStep4() |
| inbound1OrderKey | COMPLETE | routeInbound1Complete() → executeStep5() |
| inbound2OrderKey | COMPLETE | routeInbound2Complete() → executeStep6() |

**핵심 코드 위치**:
```java
// EcsCallbackProcessor.java:96-100
try {
        kmat2026CallbackHandler.handleComplete(shuttleOrder);  // 여기서 호출
} catch (Exception e) {
        logger.warn("KMAT 2026 callback handler error: {}", e.getMessage());
        }

// EcsCallbackProcessor.java:113-117
        try {
        kmat2026CallbackHandler.handleConveyorArrived(shuttleOrder.getOrderKey());  // 여기서 호출
        } catch (Exception e) {
        logger.warn("KMAT 2026 callback handler error: {}", e.getMessage());
        }
```

---

#### KMat2026RoundPlanService.java
**경로**: `biz/wcs/kmat_2026/service/KMat2026RoundPlanService.java`

DB 조회로 라운드 계획 수립.

| 메서드                         | 설명                                    |
| --- | --- |
| `buildRoundPlan(roundNumber)` | DB 조회 → CyclePoint 3개 + EMPTY 풀 반환 |

**반환 DTO (RoundPlan)**:
| 필드            | 설명                 |
| --- | --- |
| cyclePoints    | CyclePoint 3개 리스트 |
| floor1EmptyPool| 1층 빈 위치 리스트    |
| floor2EmptyPool| 2층 빈 위치 리스트    |

---

#### KMat2026LocationMapping.java
**경로**: `biz/wcs/kmat_2026/consts/KMat2026LocationMapping.java`

로케이션 코드 매핑 상수.

| 상수                     | 값                  | 설명                |
| --- | --- | --- |
| EQ_GROUP_ID             | K_MAT_TSPG          | 설비 그룹 ID        |
| OUTBOUND_PORT_1         | 10601               | 출고단 1            |
| OUTBOUND_PORT_2         | 10602               | 출고단 2            |
| INBOUND_PORT            | INBOUND_01          | 입고단              |
| POD_BUFFER              | POD_BUFFER_01       | POD 버퍼            |
| CYCLES_PER_ROUND        | 3                   | 라운드당 사이클 수   |
| PALLETS_PER_CYCLE       | 2                   | 사이클당 파렛트 수   |

**WCS ↔ ECS 매핑**:
| WCS 코드       | ECS 코드           |
| --- | --- |
| 10601         | TSPG_CONV_OUT_01  |
| 10602         | TSPG_CONV_OUT_02  |
| INBOUND_01    | TSPG_CONV_IN_01   |

---

### 4.2 Sineva ECS 모듈 (AGF/AMR 제어)

#### SinevaEcsFacade.java
**경로**: `biz/ecs/sineva/SinevaEcsFacade.java`

AGF 지시의 최상위 진입점.

| 메서드                                      | 설명                    |
| --- | --- |
| `handleTspgConveyorInboundExecute(loc)`    | AGF→TSPG 입고 실행      |
| `handleTspgConveyorInboundCallback(order)` | AGF→TSPG 입고 콜백      |
| `handleTspgConveyorOutboundExecute(loc)`   | TSPG→AGF 출고 실행      |
| `handleTspgConveyorOutboundCallback(order)`| TSPG→AGF 출고 콜백      |
| `handleTspgConveyorInboundRefillExecute()` | AGF 입고 리필 실행      |
| `handleTspgConveyorInboundRefillCallback(order)` | AGF 입고 리필 콜백 (→ 입고2 생성) |
| `handleCancelTaskExecute(orderId)`         | 작업 취소               |

---

#### TspgConveyorOutboundProcessor.java
**경로**: `biz/ecs/sineva/processor/TspgConveyorOutboundProcessor.java`

출고단에서 입고단/버퍼로 AGF 이동 처리.

**호출 경로**:
```
KMat2026ScenarioService.executeStep2/3()
    ↓
SinevaEcsFacade.handleTspgConveyorOutboundExecute(ecsOutboundLoc)
    ↓
TspgConveyorOutboundProcessor.execute()
    ↓
tb_wcs_order INSERT (AGF 명령 생성)
```

**라우팅 정책**:
| 입고단 상태 | 목적지 | CommandType |
| --- | --- | --- |
| EMPTY | TSPG_CONV_IN_01 (입고단) | K_MAT_TSPG_CONVEYOR_INBOUND |
| FULL | POD_BUFFER_01 (버퍼) | K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND |

---

#### TspgConveyorInboundRefillProcessor.java
**경로**: `biz/ecs/sineva/processor/TspgConveyorInboundRefillProcessor.java`

입고단 비워지면 다음 파렛트를 당겨오는 처리.

**호출 경로**:
```
KMat2026ScenarioService.executeStep5() (inbound1 완료 후)
    ↓
SinevaEcsFacade.handleTspgConveyorInboundRefillExecute()
    ↓
TspgConveyorInboundRefillProcessor.execute()
    ↓
tb_wcs_order INSERT (AGF 리필 명령 생성)
```

**소스 선택 우선순위**:
| 순위 | 소스 | CommandType |
| --- | --- | --- |
| 1 | POD_BUFFER_01 (버퍼단 FULL) | K_MAT_TSPG_BUFFER_TO_CONVEYOR_INBOUND |
| 2 | TSPG_CONV_OUT_xx (출고단 FULL, location_seq ASC) | K_MAT_TSPG_CONVEYOR_INBOUND |

---

#### EquipCallbackRoutingService.java
**경로**: `biz/ecs/sineva/service/EquipCallbackRoutingService.java`

AGF가 콜백을 보내면 WCS (본 모듈)에서 처리.

**AGF 콜백 수신 경로**:
```
Sineva AGF/AMR (콜백 전송)
    ↓
SinevaCallbackReceiveService (콜백 수신)
    ↓
EquipCallbackRoutingService.route(order, errorCode)
    ├─ cbkStatus별 분기 처리
    │   ├─ END → handleTaskEnd() → commandType별 분기
    │   │         ├─ K_MAT_TSPG_CONVEYOR_INBOUND → SinevaEcsFacade.handleTspgConveyorInboundCallback()
    │   │         ├─ K_MAT_TSPG_BUFFER_TO_CONVEYOR_INBOUND → SinevaEcsFacade.handleTspgConveyorInboundRefillCallback()
    │   │         └─ K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND → SinevaEcsFacade.handleTspgConveyorOutboundCallback()
    │   └─ SUCCESS, ERROR 등 → 해당 핸들러
    │
    └─ SinevaTaskReportEvent 발행 (이벤트 기반 처리)
        └─ KMat2026SinevaTaskReportEventListener (이벤트 수신)
```

**콜백 상태별 처리**:

| CbkStatus | 처리 |
|---|---|
| SUCCESS | 완료 처리, 로케이션 상태 업데이트 |
| END | commandType별 분기 처리 (아래 상세) |
| IN_PROGRESS | 로봇 작업 시작 처리 |
| FINISH_LOADING | 출발지 로딩 완료 처리 |
| ERROR | 에러 처리 |
| ERROR_RECOVERY | 에러 복구 처리 |

**END 상태 commandType별 처리**:

| CommandType | 처리 |
|---|---|
| K_MAT_TSPG_CONVEYOR_INBOUND | AGF가 출고단 → 입고단 이동 완료 |
| K_MAT_TSPG_BUFFER_TO_CONVEYOR_INBOUND | AGF가 버퍼 → 입고단 이동 완료 → 입고2 지시 생성 |
| K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND | AGF가 출고단 → 버퍼 이동 완료 |
---

#### KMat2026SinevaTaskReportEventListener.java
**경로**: `biz/wcs/kmat_2026/service/KMat2026SinevaTaskReportEventListener.java`

AGF 콜백 이벤트를 수신하여 KMAT 시나리오 추가 처리.

**이벤트 발행/수신 흐름**:
```
EquipCallbackRoutingService.publishSinevaTaskReportEvent(order, cbkStatus, errorCode)
    ↓
ApplicationEventPublisher.publishEvent(SinevaTaskReportEvent)
    ↓
KMat2026SinevaTaskReportEventListener.onSinevaTaskReport(event)
    └─ 필요시 추가 시나리오 로직 실행
```

---

### 4.3 TSPG 4Way Shuttle WCS 모듈

#### EcsCallbackController.java
**경로**: `biz/wcs/tspg_4way_shuttle/rest/EcsCallbackController.java`

TSPG 셔틀 WCS로부터 콜백을 수신하는 REST 컨트롤러.

**API 엔드포인트**:
```
POST /wcs/tspg/callback
Content-Type: application/json

{
  "orderKey": "ORDER_KEY_xxx",
  "status": "COMPLETE",          // STARTED, COMPLETE, RACK_CONVEYOR_ARRIVED, ERROR, CANCELLED
  "errorCode": null,
  "message": null
}
```

**콜백 status 종류**:

| status | 설명 | 후속 처리 |
|---|---|---|
| STARTED | 작업 시작 | orderStatus → RUNNING |
| ACCEPTED | 명령 수락 | 로그만 |
| IN_PROGRESS | 진행 중 | 로그만 |
| RACK_CONVEYOR_ARRIVED | 렉단 컨베이어 도착 | 입고 트리거 (Step4) |
| COMPLETE | 작업 완료 | 재고 처리 + 로케이션 해제 + 다음 Step |
| ERROR | 에러 발생 | 예약 해제 + 로케이션 해제 |
| CANCELLED | 취소됨 | 예약 해제 + 로케이션 해제 |

---

#### DirectWcsOrderService.java
**경로**: `biz/wcs/tspg_4way_shuttle/service/DirectWcsOrderService.java`

KMAT 시나리오에서 TSPG로 작업 지시를 보내는 핵심 서비스.

**호출 경로 (MOVE 지시)**:
```
KMat2026ScenarioService.executeStep1()
    ↓
DirectWcsOrderService.execute(WcsOrderCommand)
    ↓
MoveOrderHandler.createShuttleOrder()
    ↓
EcsCommandService.sendCommand()
    ↓
TSPG 4Way Shuttle WCS (HTTP 또는 TCP)
```

**호출 경로 (INBOUND 지시)**:
```
KMat2026ScenarioService.executeStep4/5() 또는 SinevaEcsFacade.handleTspgConveyorInboundRefillCallback()
    ↓
DirectWcsOrderService.execute(WcsOrderCommand)
    ↓
InboundOrderHandler.createShuttleOrder()
    ↓
EcsCommandService.sendCommand()
    ↓
TSPG 4Way Shuttle WCS
```

**처리 흐름**:
```
1. 명령 검증 (validationService.validate)
2. (옵션) 가상 HOST 주문 생성
3. Handler 조회 (orderType 기준)
4. 로케이션 할당 (handler.allocateLocation)
5. 로케이션 잠금 (lockLocations)
6. 재고 예약 (handler.reserveInventory)
7. Shuttle Order 생성 (handler.createShuttleOrder) → tb_wcs_shuttle_order INSERT
8. Shuttle Order Item 생성 (handler.createShuttleOrderItems) → tb_wcs_shuttle_order_item INSERT
9. ECS 전송 (ecsCommandService.sendCommand)
10. 응답 반환 (orderKey 포함)
```

---

#### WcsOrderHandler.java (인터페이스)
**경로**: `biz/wcs/tspg_4way_shuttle/handler/WcsOrderHandler.java`

주문 유형별 처리 전략 인터페이스.

| 메서드                           | 설명                    |
| --- | --- |
| `supports(orderType)`           | 해당 주문 유형 지원 여부  |
| `allocateLocation(command)`     | 로케이션 할당            |
| `reserveInventory(command,alloc)`| 재고 예약               |
| `createShuttleOrder(command,alloc)` | Shuttle Order 생성   |
| `createShuttleOrderItems(command,order)` | Item 생성       |
| `handleCompletion(order,items)` | 완료 후처리              |
| `handleFailure(order,code,desc)`| 실패 후처리              |
| `handleCancellation(order)`     | 취소 후처리              |
| `handleRackConveyorArrived(order)` | 렉단 도착 후처리       |

**구현체**:
- `InboundOrderHandler`: 입고 처리
- `OutboundOrderHandler`: 출고 처리
- `MoveOrderHandler`: 이동 처리

---

#### EcsCallbackProcessor.java
**경로**: `biz/wcs/tspg_4way_shuttle/service/EcsCallbackProcessor.java`

TSPG 콜백을 받아서 상태별로 처리하고, KMAT 시나리오 핸들러를 호출.

**전체 콜백 처리 흐름**:
```
EcsCallbackController.receiveCallback()
    │ EcsCallbackRequest { orderKey, status, errorCode, message }
    ▼
Tspg4WayShuttleWcsFacade.handleCallback(request)
    │
    ▼
EcsCallbackProcessor.process(request, shuttleOrder, items, handler)
    │
    ├─ status 분기
    │   ├─ STARTED → handleStarted()
    │   │   └─ orderStatus = RUNNING
    │   │
    │   ├─ COMPLETE → handleCompleted()
    │   │   ├─ handler.handleCompletion()  // 재고 처리, 로케이션 해제
    │   │   └─ kmat2026CallbackHandler.handleComplete(shuttleOrder)  // ★ KMAT 시나리오 연계
    │   │
    │   ├─ RACK_CONVEYOR_ARRIVED → handleRackConveyorArrived()
    │   │   ├─ handler.handleRackConveyorArrived()
    │   │   └─ kmat2026CallbackHandler.handleConveyorArrived(orderKey)  // ★ 입고 트리거
    │   │
    │   ├─ ERROR → handleFailed()
    │   │   └─ handler.handleFailure()  // 예약 해제, 로케이션 해제
    │   │
    │   └─ CANCELLED → handleCancelled()
    │       └─ handler.handleCancellation()
    │
    ▼
EcsCallbackResponse 반환
```

**KMAT 시나리오 연계 핵심 코드**:
```java
// EcsCallbackProcessor.java - handleCompleted()
private EcsCallbackResponse handleCompleted(TbWcsShuttleOrder shuttleOrder, ...) {
    // 1. 기본 완료 처리 (재고, 로케이션)
    handler.handleCompletion(shuttleOrder, items);

    // 2. KMAT 시나리오 콜백 호출 (★ 여기서 다음 Step 실행)
    try {
        kmat2026CallbackHandler.handleComplete(shuttleOrder);
    } catch (Exception e) {
        logger.warn("KMAT 2026 callback handler error: {}", e.getMessage());
    }

    return EcsCallbackResponse.success("Order completed");
}

// EcsCallbackProcessor.java - handleRackConveyorArrived()
private EcsCallbackResponse handleRackConveyorArrived(TbWcsShuttleOrder shuttleOrder, ...) {
    handler.handleRackConveyorArrived(shuttleOrder);

    // ★ 렉단 컨베이어 도착 → 입고 트리거 (Step4)
    try {
        kmat2026CallbackHandler.handleConveyorArrived(shuttleOrder.getOrderKey());
    } catch (Exception e) {
        logger.warn("KMAT 2026 callback handler error: {}", e.getMessage());
    }

    return EcsCallbackResponse.success("Order Rack Conveyor Arrived Completed");
}
```

| EcsCallbackStatus  | 처리                           | KMAT 연계 |
| --- | --- | --- |
| STARTED           | orderStatus → RUNNING          | 없음 |
| COMPLETE          | handler.handleCompletion 호출  | handleComplete() → 다음 Step |
| RACK_CONVEYOR_ARRIVED | handler.handleRackConveyorArrived | handleConveyorArrived() → 입고 트리거 |
| ERROR             | handler.handleFailure 호출     | 없음 |
| CANCELLED         | handler.handleCancellation 호출| 없음 |
| ACCEPTED/IN_PROGRESS | 로그만 남김                  | 없음 |

---

#### EcsCommandService.java
**경로**: `biz/wcs/tspg_4way_shuttle/service/EcsCommandService.java`

WCS에서 TSPG ECS로 명령 전송.

| 메서드                            | 설명                    |
| --- | --- |
| `sendCommand(order,items)`       | 작업 지시 전송           |
| `sendCancelCommand(order)`       | 취소 명령 전송           |
| `sendRetryCommand(order,items)`  | 재시도 명령 전송         |
| `sendInboundCommand(from,to)`    | HTTP 입고 명령 전송      |

---

#### InternalEcsCallbackService.java
**경로**: `biz/wcs/tspg_4way_shuttle/service/InternalEcsCallbackService.java`

동일 시스템 내부에서 ECS 작업 상태를 WCS로 전달 (REST API 없이 메서드 호출).

**사용 예**:
```java
// 렉단 컨베이어 도착 알림 (→ 입고 트리거)
internalEcsCallbackService.conveyorArrived(orderKey);

// 작업 완료 알림
internalEcsCallbackService.complete(orderKey);
```

| 메서드 | 설명 |
| --- | --- |
| `started(orderKey)` | 작업 시작 |
| `inProgress(orderKey)` | 작업 진행중 |
| `complete(orderKey)` | 작업 완료 |
| `error(orderKey, errorCode, message)` | 작업 실패 |
| `cancelled(orderKey)` | 작업 취소 |
| `conveyorArrived(orderKey)` | 렉단 컨베이어 도착 (★ 입고 트리거) |

**conveyorArrived() 상세**:
```
InternalEcsCallbackService.conveyorArrived(orderKey)
    ├─ kmat2026CallbackHandler.handleConveyorArrived(orderKey)  // 입고 트리거
    └─ wcsFacade.processEcsCallback(RACK_CONVEYOR_ARRIVED)
```

---

### 4.4 Handler 상세

#### InboundOrderHandler.java
**경로**: `biz/wcs/tspg_4way_shuttle/handler/InboundOrderHandler.java`

| 처리           | 동작                              |
| --- | --- |
| allocate      | 입고 가능한 빈 로케이션 선택        |
| reserve       | 재고 예약 불필요 (신규 생성이므로)  |
| complete      | 재고 생성 + 로케이션 잠금 해제      |
| failure/cancel| 로케이션 잠금 해제 (EMPTY 복원)    |

---

#### MoveOrderHandler.java
**경로**: `biz/wcs/tspg_4way_shuttle/handler/MoveOrderHandler.java`

| 처리           | 동작                                |
| --- | --- |
| allocate      | from/to 로케이션 확정                |
| reserve       | 출발지 + 목적지 재고 예약            |
| complete      | from 재고 감소 + to 재고 증가 + 잠금 해제 |
| failure/cancel| 예약 해제 + 양쪽 잠금 해제           |

---

## 5. DB 테이블 구조

### 5.1 WCS 로케이션 마스터 (tb_wcs_loc_mst)

TSPG 셔틀 로케이션 관리.

| 컬럼명              | 타입         | 설명                |
| --- | --- | --- |
| id                 | VARCHAR(50) | PK (UUID)           |
| loc_code           | VARCHAR(64) | 로케이션 코드        |
| loc_type           | VARCHAR(16) | 로케이션 유형        |
| eq_group_id        | VARCHAR(20) | 설비 그룹 ID         |
| row_no             | INTEGER     | 열 번호              |
| bay_no             | INTEGER     | 베이 번호            |
| level_no           | INTEGER     | 레벨 번호            |
| capacity           | INTEGER     | 용량                 |
| lock_yn            | INTEGER     | 잠금 여부 (0/1)      |
| use_yn             | INTEGER     | 사용 여부 (0/1)      |
| status             | INTEGER     | 상태 코드            |
| loc_seq            | VARCHAR     | 우선순위 정렬용       |

**상태 코드 (`status`)**

| 값 | 상수 | 설명 |
|---|---|---|
| 0 | `EMPTY` | 비어있음 |
| 1 | `OCCUPIED` | 재고 있음 |
| 2 | `LOCKED` | 잠김 |

---

### 5.2 ECS 로케이션 마스터 (tb_ecs_loc_mst)

AGF 작업용 로케이션 관리.

| 컬럼명              | 타입         | 설명                |
| --- | --- | --- |
| id                 | VARCHAR(40) | PK (UUID)           |
| lc_id              | VARCHAR(20) | 물류센터 ID          |
| group_cd           | VARCHAR(50) | 그룹 코드 (TSPG_CONV_OUT 등) |
| location_cd        | VARCHAR(50) | 로케이션 코드        |
| location_status    | VARCHAR(50) | 상태 (EMPTY/FULL 등) |
| location_use_yn    | INTEGER     | 사용 여부            |
| locked_yn          | INTEGER     | 잠금 여부            |
| lock_order_id      | VARCHAR(50) | 잠금한 주문 ID       |
| location_seq       | INTEGER     | 우선순위             |
| pod_cd             | VARCHAR(30) | POD 코드             |
| equip_id           | VARCHAR(10) | 할당된 장비 ID       |

**그룹 코드**

| 코드 | 설명 |
|---|---|
| `TSPG_CONV_OUT` | 출고 컨베이어 |
| `TSPG_CONV_IN` | 입고 컨베이어 |
| `TSPG_CONV_BUF` | 버퍼 |

---

### 5.3 WCS 작업 지시 (tb_wcs_order)

AGF 작업 지시 이력.

| 컬럼명              | 타입         | 설명                |
| --- | --- | --- |
| id                 | VARCHAR(40) | PK (UUID)           |
| lc_id              | VARCHAR(20) | 물류센터 ID          |
| order_id           | VARCHAR(30) | 주문 ID              |
| task_id            | VARCHAR(30) | 태스크 ID            |
| command_type       | VARCHAR(50) | 명령 유형            |
| from_position_cod  | VARCHAR(50) | 출발 위치            |
| to_position_cod    | VARCHAR(50) | 도착 위치            |
| process_status     | INTEGER     | 처리 상태            |
| cbk_status         | VARCHAR(3)  | 콜백 상태            |
| equip_id           | VARCHAR(50) | 장비 ID              |
| accept_datetime    | DATETIME    | 접수 시간            |
| comp_datetime      | DATETIME    | 완료 시간            |

---

### 5.4 셔틀 작업 지시 (tb_wcs_shuttle_order)

TSPG 셔틀 작업 지시.

| 컬럼명              | 타입         | 설명                |
| --- | --- | --- |
| id                 | VARCHAR(50) | PK (UUID)           |
| order_key          | VARCHAR(100)| 주문 키              |
| order_type         | VARCHAR(30) | INBOUND/OUTBOUND/MOVE |
| order_status       | INTEGER     | 상태 코드            |
| priority           | INTEGER     | 우선순위             |
| from_loc_code      | VARCHAR(50) | 출발 로케이션        |
| to_loc_code        | VARCHAR(50) | 도착 로케이션        |
| ecs_if_status      | INTEGER     | ECS 인터페이스 상태  |
| eq_group_id        | VARCHAR(20) | 설비 그룹 ID         |
| owner_code         | VARCHAR(20) | 화주 코드            |
| barcode            | VARCHAR(100)| 바코드               |

**order_status 코드**

| 값 | 상수 | 설명 |
|---|---|---|
| 0 | `CREATED` | 생성됨 |
| 10 | `RUNNING` | 실행 중 |
| 90 | `COMPLETED` | 완료 |
| 91 | `ERROR` | 에러 |
| 92 | `CANCELLED` | 취소됨 |

---

### 5.5 셔틀 작업 품목 (tb_wcs_shuttle_order_item)

셔틀 작업의 품목 상세.

| 컬럼명              | 타입         | 설명                |
| --- | --- | --- |
| id                 | VARCHAR(50) | PK (UUID)           |
| order_key          | VARCHAR(64) | 상위 주문 키         |
| line_no            | INTEGER     | 라인 번호            |
| sku_code           | VARCHAR(64) | SKU 코드             |
| lot_no             | VARCHAR(40) | LOT 번호             |
| qty                | INTEGER     | 수량                 |
| uom                | VARCHAR(10) | 단위                 |
| line_status        | INTEGER     | 라인 상태            |

---

### 5.6 HOST 주문 (tb_wcs_host_order)

외부 시스템 주문 이력.

| 컬럼명              | 타입           | 설명                |
| --- | --- | --- |
| id                 | VARCHAR(50)   | PK (UUID)           |
| host_system_code   | VARCHAR(20)   | 호스트 시스템 코드   |
| host_order_key     | VARCHAR(64)   | 호스트 주문 키       |
| order_type         | VARCHAR(16)   | 주문 유형            |
| order_status       | INTEGER       | 상태 코드            |
| wcs_order_key      | VARCHAR(64)   | WCS 주문 키 (연결)   |
| requested_at       | TIMESTAMP     | 요청 시간            |
| error_code         | VARCHAR(32)   | 에러 코드            |
| error_desc         | VARCHAR(255)  | 에러 설명            |

---

### 5.7 재고 (tb_wcs_inventory)

로케이션별 재고 관리.

| 컬럼명              | 타입         | 설명                |
| --- | --- | --- |
| id                 | VARCHAR(50) | PK (UUID)           |
| loc_code           | VARCHAR(64) | 로케이션 코드        |
| eq_group_id        | VARCHAR(20) | 설비 그룹 ID         |
| owner_code         | VARCHAR(20) | 화주 코드            |
| sku_code           | VARCHAR(64) | SKU 코드             |
| lot_no             | VARCHAR(40) | LOT 번호             |
| qty                | INTEGER     | 현재 수량            |
| alloc_qty          | INTEGER     | 예약 수량            |
| stock_status       | INTEGER     | 재고 상태            |
| lock_yn            | INTEGER     | 잠금 여부            |

**재고 로직**:
- 가용 수량 = qty - alloc_qty
- 출고/이동 시 alloc_qty 증가 (예약)
- 완료 시 qty 감소 + alloc_qty 감소

---

## 6. 상태 코드 정의

### 6.1 ShuttleOrderStatusEnumCode

| 코드 | 상수명     | 설명      |
| --- | --- | --- |
| 0   | CREATED   | 생성됨    |
| 10  | RUNNING   | 실행 중   |
| 90  | COMPLETED | 완료      |
| 91  | ERROR     | 에러      |
| 92  | CANCELLED | 취소됨    |

### 6.2 EcsIfStatusEnumCode

| 코드 | 상수명  | 설명        |
| --- | --- | --- |
| 0   | READY  | 전송 대기    |
| 1   | SENDING| 전송 중     |
| 2   | SENT   | 전송 완료   |
| 9   | FAIL   | 전송 실패   |

### 6.3 LocStatusEnumCode

| 코드 | 상수명    | 설명      |
| --- | --- | --- |
| 0   | EMPTY   | 비어있음   |
| 1   | OCCUPIED| 재고 있음  |
| 2   | LOCKED  | 잠김      |

### 6.4 CbkStatus (AGF 콜백)

| 코드 | 상수 | 설명 |
|---|---|---|
| `-1` | `UNKNOWN` | 알 수 없음 |
| `0` | `ERROR` | 오류 발생 |
| `1` | `IN_PROGRESS` | 작업 시작 |
| `2` | `FINISH_LOADING` | 로딩 완료(fromSide) |
| `3` | `SUCCESS` | 중간 포인트 도착 |
| `4` | `END` | 모든 작업 완료 |
| `5` | `ERROR_RECOVERY` | Error 해제 |

### 6.5 CommandType (AGF 명령)

| 코드                             | 설명                    |
| --- | --- |
| K_MAT_TSPG_CONVEYOR_INBOUND     | 출고단→입고단 이동       |
| K_MAT_TSPG_BUFFER_TO_CONVEYOR_INBOUND | 버퍼→입고단 이동   |
| K_MAT_TSPG_CONVEYOR_OUTBOUND    | 출고 처리               |
| K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND | 출고단→버퍼 이동    |
| FREIGHT_MOVE                    | 화물 운송               |

---

## 7. API 엔드포인트

### 7.1 시나리오 제어 API

**컨트롤러**: `biz/wcs/kmat_2026/rest/KMat2026ScenarioController.java`

| Method | Path                        | 설명               |
| --- | --- | --- |
| POST   | /kmat2026/scenario/start    | 시나리오 시작       |
| POST   | /kmat2026/scenario/stop     | 시나리오 정지       |
| POST   | /kmat2026/scenario/resume   | 시나리오 재개       |
| POST   | /kmat2026/scenario/reset    | 시나리오 초기화     |
| GET    | /kmat2026/scenario/status   | 상태 조회          |
| GET    | /kmat2026/scenario/location | 로케이션 상태 조회  |

### 7.2 TSPG 콜백 API

**컨트롤러**: `biz/wcs/tspg_4way_shuttle/rest/EcsCallbackController.java`

| Method | Path                    | 설명                |
| --- | --- | --- |
| POST   | /wcs/tspg/callback      | ECS 콜백 수신       |

**요청 Body (EcsCallbackRequest)**:
```json
{
  "orderKey": "ORDER_KEY_xxx",
  "status": "COMPLETE",
  "errorCode": null,
  "message": null
}
```

---

## 8. 개발자 가이드

### 8.1 시나리오 수정 방법

#### 사이클 수 변경
```java
// KMat2026LocationMapping.java
public static final int CYCLES_PER_ROUND = 3;  // 원하는 값으로 변경
```

#### 로케이션 우선순위 변경
```java
// KMat2026LocationMapping.java
public static final List<String> FLOOR_1_PRIORITY = Arrays.asList(
    "10401", "10301", "10201",  // 순서 변경
    ...
);
```

#### Step 로직 추가
```java
// KMat2026ScenarioService.java
@Transactional
public void executeStepN(KMat2026ScenarioContext ctx) {
    // 새 Step 로직
    ctx.setCurrentStep(ScenarioStep.STEPN_DONE);
}
```

#### 콜백 라우팅 추가
```java
// KMat2026TspgCallbackHandler.java
private void routeXxxComplete(String orderKey, KMat2026ScenarioContext ctx) {
    // 새 콜백 라우팅 로직
}
```

### 8.2 새 주문 유형 추가

1. `OrderTypeEnumCode`에 새 유형 추가
2. `WcsOrderHandler` 구현체 생성
3. `supports()` 메서드에서 새 유형 반환
4. 필요시 새 `LocationAllocator` 구현

### 8.3 테스트 방법

```bash
# 시나리오 시작
curl -X POST http://localhost:8080/kmat2026/scenario/start

# 상태 확인
curl http://localhost:8080/kmat2026/scenario/status

# 시나리오 정지
curl -X POST http://localhost:8080/kmat2026/scenario/stop
```

### 8.4 로그 확인

```properties
# application.properties
logging.level.operato.logis.kmat_2026=DEBUG
```

주요 로그 패턴:
- `[KMat2026WcsFacade]` - 시나리오 제어
- `[KMat2026ScenarioService]` - Step 실행
- `[KMat2026TspgCallbackHandler]` - 콜백 처리
- `[TspgConveyorOutboundProcessor]` - AGF 출고
- `[DirectWcsOrderService]` - WCS 주문 실행

### 8.5 에러 대응

| 에러 상황                    | 확인 사항                    |
| --- | --- |
| 시나리오 시작 실패           | DB 재고 상태 확인            |
| Step 진행 안됨              | TSPG 콜백 수신 확인          |
| AGF 지시 실패               | ECS 연결 상태 확인           |
| 로케이션 잠금 충돌           | lock_yn=1인 로케이션 확인    |

---

## 변경 이력

| 버전   | 날짜         | 내용              |
| --- | --- | --- |
| 1.0.0 | 2026-03-17  | 최초 작성          |
