# Resource Entity/Service Generator 가이드

## 목차
* [Resource Entity/Service Generator 가이드](#resource-entityservice-generator-가이드)
  * [목차](#목차)
  * [1. 설명](#1-설명)
  * [2. 엔티티(Entity) 생성 규칙](#2-엔티티entity-생성-규칙)
      * [2-1. 기본키(PK) 선정 <span style=color:red;>(⚠️ ID타입 옵션 중 Complex Key가 없어 추가필요)</span>](#2-1-기본키pk-선정-span-stylecolorred-id타입-옵션-중-complex-key가-없어-추가필요span)
      * [2-2. 기본키(PK) 생성 전략](#2-2-기본키pk-생성-전략)
      * [2-3. 상속(Inheritance) 및 컬럼(Column) 생성 규칙](#2-3-상속inheritance-및-컬럼column-생성-규칙)
      * [2-4. 참조 컬럼(Relation) 생성 규칙](#2-4-참조-컬럼relation-생성-규칙)
      * [2-5. 데이터 타입 매핑](#2-5-데이터-타입-매핑)
      * [2-6. 인덱스 생성](#2-6-인덱스-생성)
      * [2-7. 기본값(Default Values) 및 몀명규칙(Naming Convention)](#2-7-기본값default-values-및-몀명규칙naming-convention)
  * [3. 참조 엔티티(Ref Entity) 생성 규칙](#3-참조-엔티티ref-entity-생성-규칙)
      * [3-1. 기본키(PK) 선정 <span style=color:red;>(⚠️ ID타입 옵션 중 Complex Key가 없어 추가필요)</span>](#3-1-기본키pk-선정-span-stylecolorred-id타입-옵션-중-complex-key가-없어-추가필요span)
      * [3-2. 기본키(PK) 생성 전략](#3-2-기본키pk-생성-전략)
      * [3-3. 상속(Inheritance) 및 컬럼(Column) 생성 규칙](#3-3-상속inheritance-및-컬럼column-생성-규칙)
  * [4. 참조](#4-참조)
    * [A.번들](#a번들)
    * [B. ID 타입](#b-id-타입)
    * [A. 주요 설정 파라미터 (Velocity Context)](#a-주요-설정-파라미터-velocity-context)

--- 

## 1. 설명

본 문서는 테이블 정의 및 사용자 입력 설정을 기반으로 **Java Entity**와 **Ref Class**를 자동 생성하는 로직의 사용 지침을 담고 있습니다.

## 2. 엔티티(Entity) 생성 규칙

#### 2-1. 기본키(PK) 선정 <span style=color:red;>(⚠️ ID타입 옵션 중 Complex Key가 없어 추가필요)</span>

| 구분             | ID 필드 조건                    | ID 타입 조건    |   고유순위    | 매핑 결과 (PK) |
  |:---------------|:----------------------------|:------------|:---------:|:-----------|
| **기본 ID 방식**   | `Column.이름` 값이 `id`인 경우     | 상관없음        |   상관없음    | id 컬럼      |
| **커스텀 복합키 방식** | `Column.이름`이 `id`인 컬럼 없는 경우 | Complex Key | **0보다 큼** | 해당하는 컬럼 모두 |
| **식별자 없음**     | `Column.이름`이 `id`인 컬럼 없는 경우 | 그 외 조건      |   상관없음    | 미생성        |

#### 2-2. 기본키(PK) 생성 전략

  ```java
    // ======== 구분1. 기본 ID 방식 ========
    // Auto Increment
    @PrimaryKey
    @Sequence(name = "{tableName}_id_seq")
    
    // UUID, Meaningful ID
    @PrimaryKey
    
    // Complex key
    @Ignore
    
    
    // ======== 구분2. 커스텀 복합키 방식 ========
    // Complex Key
    @PrimaryKey
    
    // Auto Increment, UUID, Meaningful ID
      없음
   ```

#### 2-3. 상속(Inheritance) 및 컬럼(Column) 생성 규칙

- `Resource.부모ID`가 빈 값이면 **ElidomStampHook** 상속
  - **상속 컬럼** : `user_id`, `creator_id`, `updater_id`, `created_at`, `updated_at`, `domain_id`
- `Resource.부모ID`가 빈 값이 아니면 **UserTimeStamp** 상속
  - **상속 컬럼** : `user_id`, `creator_id`, `updater_id`, `created_at`, `updated_at`
- **중복 생성 제외**: 상속을 통해 생성된 컬럼명과 중복이면 무시
- **Virtual Field**: `virtual_field`가 `true`인 경우, DB 컬럼을 생성하지 않고 `@Ignore` 처리하여 화면 설정용으로만 사용
- **Nullable**: `@Column(nullable = true/false)` 설정

#### 2-4. 참조 컬럼(Relation) 생성 규칙

- 이름 : 언더바(`_`)가 포함된 필드명 (snake_case)
  > case1) 컬럼명 `test_ref_col` 입력 → 필드명 `testRef` 생성    
  case2) 컬럼명 `test` 입력 → 참조 컬럼 미생성

- 참조 타입 : `Entity`
- 참조 이름 : DB `entities` table `name`컬럼 값
  ```java
  // ex. 
  // column 이름 : col_ab_cd
  // 참조 타입 : Entity
  // 참조이름 : Message
  
  // entity.java
  @Relation(field = "col1AbCd") // camelCase
  private MessageRef colAb; // 마지막 _ 이전 값
  ```

#### 2-5. 데이터 타입 매핑

- `Integer` → **Integer**
- `String` / `Text` → **String** (필드명이 id로 끝나도 String 유지)
- `Float` → **Float**
- `Double` → **Long**
- `Boolean` → **Boolean**
- `Datetime` / `Date*` / `Timestamp` → **Date**
- `Binary` / `Decimal` / `Time` → **<span style="color:red;">에러</span>**

#### 2-6. 인덱스 생성
- 고유 번호가 부여된 유니크 필드들을 조합하여 하나의 인덱스로 작성
- 명칭 규칙: `{tableName}_{indexSeq}`
  ```java
  @Table(... 
    uniqueFields="col1,col2",
    indexes = { @Index(name = "ix_ad_test_0", columnList = "col1,col2", unique = true)}
  )

#### 2-7. 기본값(Default Values) 및 몀명규칙(Naming Convention)
- **snake_case** 항목 : `Resource.ID 필드`, `Resource.타이틀 필드`, `Column.이름`
- 입력값이 없을 경우 아래의 기본값 적용

  | 항목 | 기본값 | 비고                   |
  | :--- | :--- |:---------------------|
  | **순위 (Order)** | `20` | entity 컬럼 작성 순서      |
  | **용어 (Label)** | `label.null` | 다국어 처리용 키            |
  | **설명 (Description)** | `ID` | 필드명에 `id` 포함 시 자동 기입 |
  | **고유 순위** | `0` | -                    |
  | **컬럼 타입** | `String` | -                    |
  | **컬럼 사이즈** | `50` | -                    |
  | **Null 허용** | `false` | -                    |

## 3. 참조 엔티티(Ref Entity) 생성 규칙

#### 3-1. 기본키(PK) 선정 <span style=color:red;>(⚠️ ID타입 옵션 중 Complex Key가 없어 추가필요)</span>

| 구분             | `id` 컬럼 존재 여부 | ID 타입           |  고유순위   | Resource.ID필드 / Resource.타이틀 필드                                  | 매핑 결과 (PK)   |
|:---------------|:--------------|:----------------|:-------:|:-----------------------------------------------------------------|:-------------|
| **기본 ID 방식 1** | O (`id` 컬럼)   | 상관없음            | **> 0** | 상관없음                                                             | **id 컬럼**    |
| **기본 ID 방식 2** | O (`id` 컬럼)   | 상관없음            |  모두 0   | • 조건1 ID필드 or 타이틀 필드에 `id` 명시</br> • 조건2. Resource.타이틀이 빈값이 아닌 값 | **id 컬럼**    |
| **커스텀 복합키**    | X             | **Complex Key** | **> 0** | 상관없음                                                             | **해당 컬럼 모두** |
| **식별자 없음**     | 그 외 모든 조건     | 그 외 조건          |    -    | 그 외 조건                                                           | **미생성**      |

#### 3-2. 기본키(PK) 생성 전략

  ```java
    // ======== 구분1,2. 기본 ID 방식1,2 ========
// Auto Increment
@PrimaryKey
@Sequence(name = "{tableName}_id_seq")

// UUID, Meaningful ID, Complex key
@PrimaryKey

// ======== 구분3. 커스텀 복합키 방식 ========
// Complex Key
@PrimaryKey

// Auto Increment, UUID, Meaningful ID
없음
  ```

#### 3-3. 상속(Inheritance) 및 컬럼(Column) 생성 규칙

- **UniqueFieldsNumberIdRef 상속 조건 (2가지 모두 충족시)**
  1. **ID 타입**: `AUTO INCREMENT`
  2. **고유 순위**: 고유 순위가 `> 0` 인 컬럼이 **최소 하나 이상** 존재

| 우선순위    | 조건                                      | 컬럼 대상 필드                           |
|:--------|:----------------------------------------|:-----------------------------------|
| **1순위** | **고유 순위 > 0** 인 필드가 존재                  | 해당 필드 전체                           |
| **2순위** | 고유 순위가 모두 0이고, **Resource.title** 값이 존재 | `Resource.id`, `Resource.title` 필드 |

> **⚠️ 주의**: ID필드, 타이틀 필드 작성 시 **snake_case** 형식

## 4. 참조

<details style="background-color:#dcffe4; padding: 10px; font-weight: bold;"><summary>Resource 항목별 설명 [클릭]</summary>
 
| 항목               |    필수 여부     | 설명                                                                                                | 
|:-----------------|:------------:|:--------------------------------------------------------------------------------------------------|
| **번들**           | **Required** | `wcs-boot` 모듈 세부 패키지                                                                              | 
| **이름**           | **Required** | 엔티티/서비스의 논리적 명칭. 생성에 영향없는 값                                                                       |    
| **테이블 명**        | **Required** | 실제 매핑될 DB 테이블 명                                                                                   |    
| **Description**  |   Optional   | 상세 설명. 생성에 영향없는 값                                                                                 |    
| **URL**          |   Optional   | 데이터 조회 엔드포인트 (`search_url`)                                                                       |    
| **그리드 저장 URL**   |   Optional   | 데이터 일괄 저장 엔드포인트 (`multi_save_url`)                                                                |    
| **확장 엔티티**       |   Optional   | [⚠️검토필요] 확장된 엔티티 사용여부 (`ext_entity`)                                                              |    
| **ID 타입**        | **Required** | ID 생성 규칙 선택 <br>• `Auto Increment`: 자동 증가 <br>• `UUID`: UUID값 생성 <br>• `Meaningful ID`: unique필드들 |    
| **ID 필드**        |   Optional   | 참조엔티티에 관여하는 값                                                                                     |    
| **타이틀 필드**       |   Optional   | 참조엔티티에 관여하는 값                                                                                     |    
| **설명 필드**        |   Optional   | 상세 설명. 생성에 영향없는 값                                                                                 |    
| **부모**           |   Optional   | 참조엔티티에 관여하는 값                                                                                     |    
| **관계**           |   Optional   | [⚠️검토필요] 엔티티 간 연관 관계 정의 (1:N, 1:1)                                                                |    
| **참조 이름**        |   Optional   | [⚠️검토필요] 타 객체에서 참조 시 사용할 명칭                                                                       |   
| **데이터 프로퍼티**     |   Optional   | [⚠️검토필요] 데이터 모델의 속성값 정의                                                                           |    
| **Detail 삭제 전략** |   Optional   | [⚠️검토필요] 자식(Detail) 레코드 삭제 시 처리 방식                                                                |    
| **고정 컬럼수**       |   Optional   | 그리드 UI에서 고정할 컬럼의 개수                                                                               |    
| **활성화**          |   Optional   | [⚠️검토필요] 해당 설정의 사용 여부 (Enable/Disable)                                                            |    

### A.번들

- 파일 생성 위치 지정
- wcs-boot 모듈 > src > ...> xyz > elidom > [모듈별 상이] > entity 디렉토리
  - System : `sys` directory
  - Message : `msg` directory
  - Security : `sec` directory
  - Core : `core` directory
  - Base : `base` directory
  - Development : `dev` directory
  - Job : `job` directory
  - MQ : `rabbitmq` directory
- wcs-boot 모듈 > src > ... > xyz > anythings > [모듈별 상이] > entity 디렉토리
  - Gateway : `gw` directory
  - Logistics : `base` directory
- wcs-boot 모듈 > src > ... > operato > logis > [모듈별 상이] > entity 디렉토리
  - WCS : `wcs` directory
- 생성불가
  - DPS / DAS / INVENTORY ECS

### B. ID 타입
  - **Auto Increment**: 데이터베이스의 시퀀스나 Identity, Serial 타입의 컬럼 사용
  - **UUID**: 고유한 식별자를 자동으로 생성
  - **Meaningful ID**: 비즈니스 로직에 의미가 있는 복합키 등을 직접 지정
</details>


<details style="background-color:#d1e9ff; padding: 10px; font-weight: bold;"><summary>상세로직 [클릭]</summary>

### A. 주요 설정 파라미터 (Velocity Context)

파일 생성 시 템플릿 엔진(`velocityParamMap`)에 전달되는 주요 데이터

| 항목                   | 설명                                                              |
  |:---------------------|:----------------------------------------------------------------|
| `tableName`          | 생성 대상 DB 테이블 명                                                  |
| `entityName`         | 생성될 Resource/Entity 클래스 명                                       |
| `idStrategy`         | ID 생성 규칙 (`AUTO_INCREMENT`, `UUID` `meaningful`, `COMPLEX_KEY`) |
| `idDataType`         | ID 데이터 타입 (default: `Long`)                                     |
| `uniqueFields`       | 유니크 필드 목록 (고유 순위가 0이 아닌 컬럼)                                     |
| `refFieldList`       | 참조 필드 목록                                                        |
| `isDetail`           | 부모 ID 존재 여부 (자식 엔티티 여부)                                         |
| `isGenerateRefClass` | Ref 클래스 생성 여부 (부모 엔티티 & 참조필드 존재 시 true)                         |

</details>
