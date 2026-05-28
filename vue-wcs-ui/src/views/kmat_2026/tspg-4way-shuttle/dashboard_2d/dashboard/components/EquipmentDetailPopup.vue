<!-- src/views/tspg-4way-shuttle/dashboard_2d/components/EquipmentDetailPopup.vue -->
<!--
  [Phase 2 고도화] 단순 조회 팝업 → 종합 수동 제어 및 에러 복구 패널
  - 범용 설비 제어: Use/Disable 토글, 수동 Lock/Unlock
  - 재고 정합성 제어: 공출고(Empty Pick) / 이중입고(Double Entry) 경고 + 복구 액션
  - 작업 흐름 제어: Resume / Force Complete / Cancel
  - 수동 출고: 파렛트 단위 (전체 출고) - HOST_PENDING 예약 체계 연동
-->
<template>
  <transition name="slide-fade">
    <aside
      v-if="visible"
      ref="popupRef"
      class="detail-popup"
      :class="{ dragging: isDragging }"
      :style="popupStyle"
      @click.stop
      @wheel.stop
    >
      <div class="popup-header" @mousedown.left="startDrag">
        <div class="header-left">
          <span class="drag-handle" title="드래그하여 이동">⋮⋮</span>
          <h3>설비 상세 정보</h3>
          <span v-if="mergedEquipments.length > 1" class="selection-badge">
            {{ mergedEquipments.length }}개 선택
          </span>
        </div>
        <button class="close-btn" @click="$emit('close')">&times;</button>
      </div>

      <div class="popup-content">
        <div v-if="mergedEquipments.length === 0" class="empty-info"> 선택된 설비가 없습니다. </div>

        <template v-else>
          <!-- ────────────────────────────────────────────────────────────
               압축 메타 칩 — 코드·위치·타입·상태 + 잠금/금지 배지 (1줄)
               고정값을 한 줄로 응축하여 메인 콘텐츠가 즉시 보이도록.
               ──────────────────────────────────────────────────────────── -->
          <section class="meta-chip-bar">
            <span class="meta-chip code">{{ getEquipmentCode(activeBase) }}</span>
            <span v-if="isRackEquipment(activeBase)" class="meta-chip loc">{{
              getRackLocation(activeBase)
            }}</span>
            <span v-if="getEquipmentType(activeBase)" class="meta-chip type">{{
              getEquipmentType(activeBase)
            }}</span>
            <span
              v-if="activeEqStatus != null"
              class="meta-chip status"
              :class="getStatusClass(activeBase, activeEq)"
              >● {{ getStatusText(activeBase, activeEq) }}</span
            >
            <span v-if="activeBase?.realRackLocked" class="meta-chip-badge danger">잠금</span>
            <span v-if="activeBase?.realRackInboundForbidden" class="meta-chip-badge danger"
              >입고 금지</span
            >
            <span v-if="activeBase?.realRackOutboundForbidden" class="meta-chip-badge danger"
              >출고 금지</span
            >
            <span v-if="isPortLocked" class="meta-chip-badge danger">포트 락</span>
          </section>

          <!-- 겹친 설비 탭 — 다중 선택 시만 -->
          <section class="info-section" v-if="mergedEquipments.length > 1">
            <div class="section-label">겹친 설비 목록</div>
            <div class="equipment-tabs">
              <button
                v-for="(item, index) in mergedEquipments"
                :key="item.base.id"
                type="button"
                class="equipment-tab"
                :class="{
                  active: index === activeIndex,
                  error: isError(item.merged?.errorCode),
                }"
                @click="handleTabChange(index)"
              >
                <div class="tab-title">{{ getEquipmentCode(item.base) }}</div>
                <div class="tab-sub">{{ getEquipmentType(item.base) }}</div>
              </button>
            </div>
          </section>

          <!-- ────────────────────────────────────────────────────────────
               포트 모드 — 포트류 설비의 메인 영역
               ──────────────────────────────────────────────────────────── -->
          <section
            v-if="isPortEquipment(activeBase)"
            class="info-section port-mode-top"
            :class="{ 'port-mode-top--fixed': !isSwitchablePort(activeBase) }"
          >
            <div class="section-label">
              포트 모드
              <span class="port-mode-badge" :class="portModeBadgeClass">
                {{ currentPortModeLabel }}
              </span>
              <span v-if="!isSwitchablePort(activeBase)" class="port-fixed-tag">고정</span>
            </div>

            <div v-if="isDraining" class="port-mode-drain-panel">
              <div class="drain-icon">⏳</div>
              <div class="drain-text">
                <div class="drain-title"
                  >{{ drainingTarget === PortMode.INBOUND.code ? '입고' : '출고' }} 모드로 전환 대기
                  중</div
                >
                <div class="drain-desc">
                  진행중 작업 <strong>{{ controlInfo?.activeOrderKey ? '있음' : '검사중' }}</strong
                  >이 모두 완료되면 자동 전환됩니다.
                </div>
                <div v-if="controlInfo?.activeOrderKey" class="drain-current-job">
                  현재 작업: <code>{{ controlInfo.activeOrderKey }}</code>
                </div>
              </div>
              <button
                v-if="can('delete')"
                type="button"
                class="drain-cancel-btn"
                :disabled="controlLoading"
                @click="handleCancelDrain"
              >
                전환 취소
              </button>
            </div>

            <!-- 포트 락 holder + 강제 해제 -->
            <div v-if="isPortLocked" class="port-lock-panel">
              <div class="port-lock-header">
                <span class="port-lock-icon">🔒</span>
                <span class="port-lock-title">락 상태</span>
              </div>
              <div v-if="isDispatchLockSentinel" class="port-lock-row port-lock-sentinel">
                DISPATCH_LOCK · 선점 대기
              </div>
              <template v-else>
                <div class="port-lock-row">
                  <span class="port-lock-label">바코드</span>
                  <span class="port-lock-value mono">{{ portLockBarcode || '-' }}</span>
                </div>
                <div v-if="portLockOrderStatusLabel" class="port-lock-row">
                  <span class="port-lock-label">작업 상태</span>
                  <span class="port-lock-value">{{ portLockOrderStatusLabel }}</span>
                </div>
              </template>
              <div v-if="can('delete')" class="port-lock-actions">
                <button
                  type="button"
                  class="control-btn btn-danger"
                  :disabled="controlLoading"
                  @click="openForceUnlockModal"
                >
                  강제 해제
                </button>
              </div>
            </div>

            <div v-if="isSwitchablePort(activeBase) && can('update')" class="port-mode-grid">
              <button
                v-for="opt in PORT_MODE_OPTIONS"
                :key="opt.code"
                type="button"
                class="port-mode-btn"
                :class="{
                  active: currentPortMode === opt.code,
                  'pending-target': isDraining && drainingTarget === opt.code,
                }"
                :disabled="
                  !canSwitchPortMode ||
                  controlLoading ||
                  currentPortMode === opt.code ||
                  (isDraining && drainingTarget === opt.code)
                "
                :title="
                  isDraining && drainingTarget === opt.code
                    ? '이미 이 모드로 전환 중입니다'
                    : switchDisabledReason || ''
                "
                @click="handlePortModeChange(opt.code)"
              >
                <span class="port-mode-icon">{{ opt.icon }}</span>
                <span class="port-mode-text">{{ opt.label }}</span>
              </button>
            </div>

            <div v-if="isSwitchablePort(activeBase) && switchDisabledReason" class="port-mode-warn">
              ⚠ {{ switchDisabledReason }}
            </div>
          </section>

          <!-- ────────────────────────────────────────────────────────────
               STICKY 위급 영역 — 에러 + 정합성 경고
               메인 영역 최상단에 sticky 고정. 스크롤해도 떠 있음.
               ──────────────────────────────────────────────────────────── -->
          <div
            v-if="
              isError(activeEq?.errorCode) ||
              (isRackEquipment(activeBase) &&
                !isDriveOnly(activeBase) &&
                (isEmptyPickCondition || isDoubleEntryCondition))
            "
            class="alert-sticky-zone"
          >
            <!-- 에러 정보 -->
            <section v-if="isError(activeEq?.errorCode)" class="info-section error-section">
              <div class="error-badge">에러</div>
              <div class="info-row" v-if="activeEq?.errorCode || activeEq?.errorId">
                <span class="label">에러 코드</span>
                <span class="value status-error">{{
                  activeEq?.errorCode || activeEq?.errorId
                }}</span>
              </div>
              <div class="info-row" v-if="activeEq?.errorMessage || activeEq?.errorDesc">
                <span class="label">에러 메시지</span>
                <span class="value status-error">{{
                  activeEq?.errorMessage || activeEq?.errorDesc
                }}</span>
              </div>
            </section>

            <!-- 공출고 (Empty Pick) -->
            <section
              v-if="isRackEquipment(activeBase) && !isDriveOnly(activeBase) && isEmptyPickCondition"
              class="info-section alert-section alert-empty-pick"
            >
              <div class="alert-badge">공출고 감지</div>
              <p class="alert-desc">
                시스템에 재고가 있으나 실물이 없습니다.<br />
                수동 재고 삭제로 정합성을 맞추세요.
              </p>
              <button
                v-if="can('delete')"
                class="control-btn btn-danger"
                :disabled="controlLoading"
                @click="handleDeleteInventory"
              >
                수동 재고 삭제
              </button>
            </section>

            <!-- 이중입고 (Double Entry) -->
            <section
              v-if="
                isRackEquipment(activeBase) && !isDriveOnly(activeBase) && isDoubleEntryCondition
              "
              class="info-section alert-section alert-double-entry"
            >
              <div class="alert-badge">이중입고 감지</div>
              <p class="alert-desc">
                실물 화물이 있으나 시스템에 재고가 없습니다.<br />
                SKU와 LPN을 입력하여 재고를 수동 생성하세요.
              </p>
              <div class="inv-form">
                <input v-model="newSku" type="text" class="ctrl-input" placeholder="SKU 코드 *" />
                <input
                  v-model="newPalletId"
                  type="text"
                  class="ctrl-input"
                  placeholder="LPN (팔레트 ID)"
                />
                <input
                  v-model.number="newQty"
                  type="number"
                  class="ctrl-input"
                  placeholder="수량 *"
                  min="1"
                />
                <button
                  v-if="can('create')"
                  class="control-btn btn-warning"
                  :disabled="controlLoading || !newSku || newQty < 1"
                  @click="handleCreateInventory"
                >
                  수동 재고 생성
                </button>
              </div>
            </section>
          </div>

          <!-- ────────────────────────────────────────────────────────────
               메인 영역 — 설비 종류별 분기
                 • 랙   → 재고 상세 (즉시 노출)
                 • 셔틀 → 셔틀 제어 + 실시간 상태
                 • 리프터/컨베이어 → 실시간 상태
               ──────────────────────────────────────────────────────────── -->

          <!-- 셔틀 제어 (SHUTTLE 전용) -->
          <EquipmentShuttleSection
            v-if="isShuttleEquipment(activeBase)"
            :shuttle-code="getEquipmentCode(activeBase)"
            :lc-id="lcId"
            :shuttle-data="activeEq"
          />

          <!-- 비랙·비포트 설비의 실시간 상태 (메인) -->
          <section
            v-if="
              !isRackEquipment(activeBase) &&
              !isPortEquipment(activeBase) &&
              hasStatusIndicators(activeEq)
            "
            class="info-section"
          >
            <div class="section-label">실시간 상태</div>
            <div class="info-row" v-if="activeEq?.currentLevel != null">
              <span class="label">현재 층</span>
              <span class="value highlight">{{ activeEq.currentLevel }}F</span>
            </div>
            <div class="info-row" v-if="activeEq?.targetLevel != null">
              <span class="label">목표 층</span>
              <span class="value">{{ activeEq.targetLevel }}F</span>
            </div>
            <div class="status-grid">
              <div
                v-if="activeEq?.stopperOpenYn != null || activeEq?.stopperOpen != null"
                class="status-item"
                :class="{
                  active: activeEq?.stopperOpenYn ?? activeEq?.stopperOpen,
                  warning: activeEq?.stopperOpenYn ?? activeEq?.stopperOpen,
                }"
              >
                <span class="status-icon">🚧</span>
                <span class="status-label">스토퍼</span>
                <span
                  class="status-value"
                  :class="activeEq?.stopperOpenYn ?? activeEq?.stopperOpen ? 'open' : 'closed'"
                >
                  {{ activeEq?.stopperOpenYn ?? activeEq?.stopperOpen ? 'OPEN' : 'CLOSE' }}
                </span>
              </div>
              <div
                v-if="activeEq?.cargoYn != null || activeEq?.hasCargo != null"
                class="status-item"
                :class="{ active: activeEq?.cargoYn || activeEq?.hasCargo }"
              >
                <span class="status-icon">📦</span>
                <span class="status-label">화물</span>
                <span
                  class="status-value"
                  :class="activeEq?.cargoYn || activeEq?.hasCargo ? 'on' : 'off'"
                >
                  {{ activeEq?.cargoYn || activeEq?.hasCargo ? '있음' : '없음' }}
                </span>
              </div>
              <div
                v-if="activeEq?.hasShuttle != null"
                class="status-item"
                :class="{ active: activeEq?.hasShuttle }"
              >
                <span class="status-icon">🚗</span>
                <span class="status-label">셔틀</span>
                <span class="status-value" :class="activeEq?.hasShuttle ? 'on' : 'off'">
                  {{ activeEq?.hasShuttle ? '적재' : '없음' }}
                </span>
              </div>
              <div
                v-if="activeEq?.autoYn != null"
                class="status-item"
                :class="{ active: activeEq?.autoYn }"
              >
                <span class="status-icon">🤖</span>
                <span class="status-label">자동</span>
                <span class="status-value" :class="activeEq?.autoYn ? 'on' : 'off'">
                  {{ activeEq?.autoYn ? 'ON' : 'OFF' }}
                </span>
              </div>
            </div>
            <div
              class="info-row"
              v-if="
                activeEq?.runYn && activeEq?.targetLevel != null && activeEq?.currentLevel != null
              "
            >
              <span class="label">이동 방향</span>
              <span
                class="value"
                :class="activeEq.targetLevel > activeEq.currentLevel ? 'going-up' : 'going-down'"
              >
                {{ activeEq.targetLevel > activeEq.currentLevel ? '▲ 상승중' : '▼ 하강중' }}
              </span>
            </div>
            <div class="info-row" v-if="activeEq?.hasActiveJob">
              <span class="label">작업</span>
              <span class="value status-working">진행중</span>
            </div>
            <div class="info-row" v-if="activeEq?.currentOrderKey">
              <span class="label">작업키</span>
              <span class="value">{{ activeEq.currentOrderKey }}</span>
            </div>
            <div class="info-row" v-if="activeEq?.currentBarcode">
              <span class="label">바코드</span>
              <span class="value">{{ activeEq.currentBarcode }}</span>
            </div>
            <div class="info-row" v-if="activeEq?.currentFromLoc">
              <span class="label">출발지</span>
              <span class="value">{{ activeEq.currentFromLoc }}</span>
            </div>
            <div class="info-row" v-if="activeEq?.currentToLoc">
              <span class="label">목적지</span>
              <span class="value">{{ activeEq.currentToLoc }}</span>
            </div>
          </section>

          <!-- 랙 메인: 재고 상세 -->
          <section
            v-if="isRackEquipment(activeBase) && inventoryRows.length > 0"
            class="info-section"
          >
            <div class="section-label">
              재고 상세
              <span class="inv-count-badge">{{ inventoryRows.length }}건</span>
            </div>

            <div v-if="inventoryRows.length > 1 && isSamePallet" class="pallet-group-hint">
              <span class="pallet-icon">📦</span>
              <span
                >Pallet ID: {{ inventoryRows[0].palletId }} — 혼적
                {{ inventoryRows.length }}건</span
              >
            </div>

            <div
              v-for="(inv, idx) in inventoryRows"
              :key="inv.id || `inv-${idx}`"
              class="inventory-card"
              :class="{
                'inv-expired': isExpired(inv.expiredDate),
                'inv-hold': isHoldStatus(inv.stockStatus),
                'inv-reserved': inv.stockStatus === StockStatus.HOST_PENDING,
              }"
            >
              <!-- HOST_PENDING 예약 배너 -->
              <div v-if="inv.stockStatus === StockStatus.HOST_PENDING" class="reservation-banner">
                🔒 출고 지시 등록됨 — 작업 대기 중
              </div>

              <!-- 최상단 핵심 식별 배지: 카테고리 + 상태 -->
              <div v-if="inv.stockType || inv.stockStatus != null" class="inv-key-badges">
                <span v-if="inv.stockType" class="inv-key-badge inv-key-type">
                  <span class="inv-key-badge-label">카테고리</span>
                  <span class="inv-key-badge-value">{{ getStockTypeText(inv.stockType) }}</span>
                </span>
                <span
                  v-if="inv.stockStatus != null"
                  class="inv-key-badge"
                  :class="getStockStatusClass(inv.stockStatus)"
                >
                  <span class="inv-key-badge-label">상태</span>
                  <span class="inv-key-badge-value">{{ getStockStatusText(inv.stockStatus) }}</span>
                </span>
              </div>

              <div class="inv-header">
                <div class="inv-sku-block">
                  <span class="inv-sku">{{ inv.skuCode || inv.itemCode || '-' }}</span>
                  <span
                    v-if="inv.itemCode && inv.skuCode && inv.itemCode !== inv.skuCode"
                    class="inv-item-code"
                    >{{ inv.itemCode }}</span
                  >
                </div>
                <div class="inv-qty-block">
                  <span class="inv-qty">{{ inv.qty ?? 0 }}</span>
                  <span class="inv-qty-unit">EA</span>
                </div>
              </div>

              <!-- 액션 행 — 헤더 직후, 메타 그리드 위에 (텍스트 라벨만, 이모지 없음) -->
              <div v-if="inv.stockId && hasAnyAction(inv)" class="act-row">
                <button
                  v-if="canManualOutbound(inv) && can('update')"
                  type="button"
                  class="act-btn act-primary"
                  :disabled="actionBusy"
                  @click="openOutboundModal(inv)"
                >
                  수동 출고
                </button>
                <button
                  v-if="canSampleOutbound(inv) && can('update')"
                  type="button"
                  class="act-btn act-warn"
                  :disabled="actionBusy"
                  @click="openSampleOutboundModal(inv)"
                >
                  샘플 출고
                </button>
                <button
                  v-if="canTransitionToReturn(inv) && can('update')"
                  type="button"
                  class="act-btn act-warn"
                  :disabled="actionBusy"
                  @click="openTransitionModal(inv, 'RETURN', '반품으로 전환', '반품', 'warn')"
                >
                  반품
                </button>
                <button
                  v-if="canTransitionToDisposal(inv) && can('delete')"
                  type="button"
                  class="act-btn act-danger"
                  :disabled="actionBusy"
                  @click="openTransitionModal(inv, 'DISPOSAL', '폐기로 전환', '폐기', 'danger')"
                >
                  폐기
                </button>
                <button
                  v-if="canTransitionToNiaPending(inv) && can('update')"
                  type="button"
                  class="act-btn act-warn"
                  :disabled="actionBusy"
                  @click="
                    openTransitionModal(
                      inv,
                      'NIA_PENDING',
                      '국검 대기로 전환',
                      '국검 대기로 전환',
                      'warn',
                    )
                  "
                >
                  국검 대기
                </button>
                <button
                  v-if="canCorrectToQcPending(inv) && can('update')"
                  type="button"
                  class="act-btn act-warn"
                  :disabled="actionBusy"
                  @click="openQcCorrectionModal(inv)"
                >
                  QC 대기 보정
                </button>
                <button
                  v-if="canApproveNia(inv) && can('update')"
                  type="button"
                  class="act-btn act-success"
                  :disabled="actionBusy"
                  @click="openTransitionModal(inv, 'NORMAL', '국검 승인', '국검 승인', 'success')"
                >
                  국검 승인
                </button>
                <button
                  v-if="canApproveNia(inv) && can('delete')"
                  type="button"
                  class="act-btn act-danger"
                  :disabled="actionBusy"
                  @click="openTransitionModal(inv, 'NIA_FAIL', '국검 불승인', '불승인', 'danger')"
                >
                  불승인
                </button>
                <button
                  v-if="canAdjustQty(inv) && can('update')"
                  type="button"
                  class="act-btn act-warn"
                  :disabled="actionBusy"
                  @click="openAdjustQtyModal(inv)"
                >
                  수량 보정
                </button>
              </div>

              <div class="inv-grid">
                <div v-if="inv.lotNo" class="inv-cell">
                  <span class="inv-cell-label">Lot</span>
                  <span class="inv-cell-value">{{ inv.lotNo }}</span>
                </div>
                <div v-if="inv.itemOwner" class="inv-cell">
                  <span class="inv-cell-label">화주</span>
                  <span class="inv-cell-value">{{ inv.itemOwner }}</span>
                </div>
                <div v-if="inv.itemPriority != null" class="inv-cell">
                  <span class="inv-cell-label">우선순위</span>
                  <span class="inv-cell-value">{{ inv.itemPriority }}</span>
                </div>
                <div v-if="inv.inbDatetime" class="inv-cell">
                  <span class="inv-cell-label">입고</span>
                  <span class="inv-cell-value">{{ inv.inbDatetime }}</span>
                </div>
                <div v-if="inv.produceDate" class="inv-cell">
                  <span class="inv-cell-label">제조</span>
                  <span class="inv-cell-value">{{ inv.produceDate }}</span>
                </div>
                <div v-if="inv.expiredDate" class="inv-cell">
                  <span class="inv-cell-label">유통기한</span>
                  <span
                    class="inv-cell-value"
                    :class="{ 'inv-expired-text': isExpired(inv.expiredDate) }"
                  >
                    {{ inv.expiredDate }}
                    <span v-if="isExpired(inv.expiredDate)" class="expired-badge">만료</span>
                  </span>
                </div>
                <div v-if="inv.stockHeight != null" class="inv-cell">
                  <span class="inv-cell-label">높이</span>
                  <span class="inv-cell-value">{{ inv.stockHeight }}</span>
                </div>
                <div v-if="inv.attributeA" class="inv-cell">
                  <span class="inv-cell-label">Attr A</span>
                  <span class="inv-cell-value">{{ inv.attributeA }}</span>
                </div>
                <div v-if="inv.scannedBarcode" class="inv-cell inv-cell-wide">
                  <span class="inv-cell-label">BCR</span>
                  <span class="inv-cell-value inv-barcode">{{ inv.scannedBarcode }}</span>
                </div>
                <div v-if="inv.palletId" class="inv-cell inv-cell-wide">
                  <span class="inv-cell-label">Pallet ID</span>
                  <span class="inv-cell-value inv-monospace">{{ inv.palletId }}</span>
                </div>
              </div>
            </div>
          </section>

          <!-- 빈 랙 안내 -->
          <section
            v-else-if="isRackEquipment(activeBase) && controlInfo && inventoryRows.length === 0"
            class="info-section inv-empty-section"
          >
            <div class="section-label">재고 상세</div>
            <div class="inv-empty-msg">
              <span class="inv-empty-icon">📭</span>
              <span>빈 랙 — 재고 없음</span>
            </div>
          </section>

          <!-- ────────────────────────────────────────────────────────────
               상세 메타 — 접힘 토글 (default: 접힘)
                 • 설비 정보 (코드/타입/상태) — 풀버전
                 • 랙 정보 (rack-type-card + 위치/잠금/입출고 금지)
                 • 실시간 상태 (랙일 때만, 셔틀/리프터는 메인이라 제외)
               ──────────────────────────────────────────────────────────── -->
          <div class="details-toggle-wrap">
            <button
              type="button"
              class="details-toggle"
              @click="detailsExpanded = !detailsExpanded"
            >
              <span class="toggle-arrow" :class="{ open: detailsExpanded }">▾</span>
              {{ detailsExpanded ? '설비 상세 접기' : '설비 상세 보기' }}
            </button>
          </div>

          <div v-if="detailsExpanded" class="details-zone">
            <!-- 설비 기본 정보 (풀버전) -->
            <section class="info-section">
              <div class="section-label">설비 정보</div>
              <div class="info-row">
                <span class="label">설비 코드</span>
                <span class="value highlight">{{ getEquipmentCode(activeBase) }}</span>
              </div>
              <div class="info-row" v-if="getEquipmentType(activeBase)">
                <span class="label">타입</span>
                <span class="value">{{ getEquipmentType(activeBase) }}</span>
              </div>
              <div class="info-row" v-if="activeEqStatus != null">
                <span class="label">상태</span>
                <span class="value" :class="getStatusClass(activeBase, activeEq)">
                  {{ getStatusText(activeBase, activeEq) }}
                </span>
              </div>
            </section>

            <!-- 랙 상세 정보 -->
            <section class="info-section" v-if="isRackEquipment(activeBase)">
              <div class="section-label">랙 정보</div>
              <div class="rack-type-card" :class="getRackTypeClass(activeBase?.realRackType)">
                <span class="rack-type-icon">{{ getRackTypeIcon(activeBase?.realRackType) }}</span>
                <div class="rack-type-info">
                  <span class="rack-type-label">{{
                    getRackTypeLabel(activeBase?.realRackType)
                  }}</span>
                  <span class="rack-type-desc" v-if="activeBase?.realRackDriveOnlyYn"
                    >주행 전용 구역</span
                  >
                </div>
              </div>
              <div
                class="info-row"
                v-if="
                  activeBase?.realRackRow != null ||
                  activeBase?.realRackBay != null ||
                  activeBase?.realRackLevel != null
                "
              >
                <span class="label">위치</span>
                <span class="value location-value">{{ getRackLocation(activeBase) }}</span>
              </div>
              <div class="info-row" v-if="activeBase?.realRackStateCode">
                <span class="label">상태</span>
                <span class="value highlight">{{ getRackStateDisplay(activeBase) }}</span>
              </div>
              <div class="info-row" v-if="activeBase?.realRackLocked">
                <span class="label">잠금</span>
                <span class="value">사용금지 (LOCK)</span>
              </div>
              <div class="info-row" v-if="activeBase?.realRackInboundForbidden">
                <span class="label">입고</span>
                <span class="value">금지</span>
              </div>
              <div class="info-row" v-if="activeBase?.realRackOutboundForbidden">
                <span class="label">출고</span>
                <span class="value">금지</span>
              </div>
            </section>

            <!-- 랙의 실시간 상태 (있을 때만, 메인이 재고이므로 보조) -->
            <section
              v-if="isRackEquipment(activeBase) && hasStatusIndicators(activeEq)"
              class="info-section"
            >
              <div class="section-label">실시간 상태</div>
              <div class="info-row" v-if="activeEq?.hasActiveJob">
                <span class="label">작업</span>
                <span class="value status-working">진행중</span>
              </div>
              <div class="info-row" v-if="activeEq?.currentOrderKey">
                <span class="label">작업키</span>
                <span class="value">{{ activeEq.currentOrderKey }}</span>
              </div>
              <div class="info-row" v-if="activeEq?.currentBarcode">
                <span class="label">바코드</span>
                <span class="value">{{ activeEq.currentBarcode }}</span>
              </div>
              <div class="info-row" v-if="activeEq?.currentFromLoc">
                <span class="label">출발지</span>
                <span class="value">{{ activeEq.currentFromLoc }}</span>
              </div>
              <div class="info-row" v-if="activeEq?.currentToLoc">
                <span class="label">목적지</span>
                <span class="value">{{ activeEq.currentToLoc }}</span>
              </div>
            </section>
          </div>

          <!-- 작업 흐름 제어 -->
          <section class="info-section control-section" v-if="activeOrderKey">
            <div class="section-label">작업 제어</div>
            <div class="info-row">
              <span class="label">작업 키</span>
              <span class="value" style="color: #409eff; font-family: monospace; font-size: 11px">{{
                activeOrderKey
              }}</span>
            </div>
            <div class="info-row" v-if="activeOrderStatus != null">
              <span class="label">작업 상태</span>
              <span class="value" :class="getOrderStatusClass(activeOrderStatus)">
                {{ getOrderStatusText(activeOrderStatus) }}
              </span>
            </div>
            <div class="job-control-buttons">
              <button
                v-if="can('update')"
                class="control-btn btn-resume"
                :disabled="controlLoading || !isOrderError(activeOrderStatus)"
                @click="handleResumeOrder"
                title="에러 상태 오더를 SENT(10)으로 복원 → 스케줄러 재전송"
              >
                ▶ 재개
              </button>
              <button
                v-if="can('update')"
                class="control-btn btn-force-complete"
                :disabled="controlLoading || !isOrderActive(activeOrderStatus)"
                @click="handleForceComplete"
                title="실물 이동 완료 확인 후 WCS 상태 강제 완료 처리"
              >
                ✓ 강제완료
              </button>
              <button
                v-if="can('delete')"
                class="control-btn btn-cancel"
                :disabled="controlLoading || !isOrderActive(activeOrderStatus)"
                @click="handleCancelOrder"
                title="작업 취소 — 로케이션 Lock 해제 + 오더 CANCELLED"
              >
                ✕ 강제취소
              </button>
            </div>
          </section>

          <!-- 결과 토스트 -->
          <div v-if="inlineMsg" class="inline-toast" :class="inlineMsgType">
            {{ inlineMsg }}
          </div>
        </template>
      </div>
    </aside>
  </transition>

  <!-- 통합 액션 모달 — 반품/폐기/국검 승인/국검 미승인/국검 대기/수동 출고 공용 -->
  <ActionCommentModal
    :visible="actionModal.open"
    :title="actionModal.title"
    :confirm-label="actionModal.confirmLabel"
    :confirm-variant="actionModal.confirmVariant"
    :stock-info="{
      stockId: actionModal.inv?.stockId,
      sku: actionModal.inv?.skuCode || actionModal.inv?.itemCode,
      lot: actionModal.inv?.lotNo,
    }"
    :current-type="actionModal.currentType"
    :next-type="actionModal.nextType"
    :show-test-request-no="actionModal.showTestRequestNo"
    :show-port-code="actionModal.showPortCode"
    :show-qty="actionModal.kind === 'ADJUST_QTY'"
    :qty-default="actionModal.inv?.qty ?? 0"
    :extra-notice="actionModal.extraNotice"
    :busy="actionModal.busy"
    @cancel="closeActionModal"
    @confirm="onActionConfirm"
    @update:visible="
      (v) => {
        if (!v) closeActionModal();
      }
    "
  />

  <!-- 포트 강제 락 해제 모달 -->
  <ActionCommentModal
    :visible="forceUnlockModalVisible"
    title="포트 강제 락 해제"
    confirm-label="강제 해제"
    confirm-variant="danger"
    current-type="락 상태"
    next-type="해제"
    :stock-info="{
      sku: portLockBarcode || (isDispatchLockSentinel ? 'DISPATCH_LOCK' : '-'),
      lot: portLockOrderStatusLabel || '',
    }"
    extra-notice="진행중 작업의 정합성을 반드시 현장에서 확인하세요. tb_inventory_location.task_id 가 NULL 로 초기화됩니다."
    :busy="controlLoading"
    @cancel="closeForceUnlockModal"
    @confirm="handleForceUnlockConfirm"
    @update:visible="
      (v) => {
        if (!v) closeForceUnlockModal();
      }
    "
  />
</template>

<script setup lang="ts">
  import { computed, ref, watch, reactive, onMounted, onUnmounted, nextTick } from 'vue';
  import { getCommonPostApi } from '/@/api/common/api';
  import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import type { DashboardEquipmentData } from '../../api/types';
  import { isError, LayoutEquipmentType } from '../../api/types';
  import {
    // 도메인 상수
    RackType,
    WcsLocType,
    PortMode,
    WcsOrderTypeWorkingLabels,
    StockStatus,
    StockStatusLabels,
    StockTypeLabels,
    type PortModeValue,
    // enum lookup 헬퍼
    enumLabel,
    enumIcon,
    // 도메인 헬퍼
    isOrderError,
    isOrderActive,
    getOrderStatusText,
    getOrderStatusClass,
    isEmptyPick,
    isDoubleEntry,
    isPortDraining,
    getPortDrainTarget,
    getPortDrainOrigin,
    enumBadgeVariant,
  } from '../../../constants';
  import { useShuttleStore } from '../../store/shuttleStore';
  import {
    shuttleStateSpec,
    conveyorStateSpec,
  } from '@/views/kmat_2026/tspg-4way-shuttle/constants/legend/legend-spec';
  import EquipmentShuttleSection from './EquipmentShuttleSection.vue';
  import ActionCommentModal from './ActionCommentModal.vue';
  import {
    type DashboardControlInfo,
    type InventoryItem,
    getRackControlInfo,
    getEquipmentControlInfo,
    toggleLocUse,
    toggleEquipmentUse,
    manualLock,
    manualUnlock,
    deleteInventory,
    createInventory,
    adjustInventoryQty,
    forceCompleteOrder,
    cancelOrderWcs,
    resumeOrderWcs,
    changePortMode,
    forceUnlockPort,
  } from '../../api/equipmentControlApi';

  const { notification } = useMessage();

  const MENU = 'Dashboard2D';
  const { can } = usePermissionLocal(MENU);

  const store = useShuttleStore();

  const props = defineProps<{
    visible: boolean;
    equipments: DashboardEquipmentData[];
    lcId: string;
  }>();

  defineEmits<{ (e: 'close'): void }>();

  // ============================================================
  // 팝업 드래그 이동 — DashboardLegend 와 동일 방식.
  // 위치는 컴포넌트 인스턴스에 유지되므로 닫았다 셀 재클릭 시 같은 자리에 다시 뜸.
  // ============================================================
  const popupRef = ref<HTMLElement | null>(null);
  const isDragging = ref(false);
  const dragStartX = ref(0);
  const dragStartY = ref(0);
  const popupX = ref<number | null>(null);
  const popupY = ref<number | null>(null);

  // 위치 미지정(첫 오픈)이면 CSS 기본값(top/right) 사용, 드래그 후엔 좌표 고정.
  const popupStyle = computed(() => {
    if (popupX.value === null || popupY.value === null) return {};
    return {
      top: `${popupY.value}px`,
      left: `${popupX.value}px`,
      right: 'auto',
      bottom: 'auto',
    };
  });

  // 좌표 기준이 되는 부모(positioned ancestor).
  function getContainerEl(): HTMLElement | null {
    const panel = popupRef.value;
    if (!panel) return null;
    return panel.offsetParent as HTMLElement | null;
  }

  // 헤더 mousedown → 드래그 시작. 버튼/입력 위에서는 무시.
  function startDrag(e: MouseEvent) {
    console.log('[POPUP DRAG] mousedown', {
      button: e.button,
      target: (e.target as HTMLElement)?.className,
    });
    if (e.button !== 0) return;
    const target = e.target as HTMLElement;
    if (target.closest('button, a, input, textarea, select, .no-drag')) {
      console.log('[POPUP DRAG] bail: interactive child');
      return;
    }

    const panel = popupRef.value;
    const container = getContainerEl();
    console.log('[POPUP DRAG] refs', {
      panel: !!panel,
      container: !!container,
      containerClass: container?.className,
    });
    if (!panel || !container) return;

    const panelRect = panel.getBoundingClientRect();
    const containerRect = container.getBoundingClientRect();

    isDragging.value = true;
    popupX.value = panelRect.left - containerRect.left;
    popupY.value = panelRect.top - containerRect.top;
    dragStartX.value = e.clientX - panelRect.left;
    dragStartY.value = e.clientY - panelRect.top;

    window.addEventListener('mousemove', onDrag);
    window.addEventListener('mouseup', stopDrag);
    e.preventDefault();
    console.log('[POPUP DRAG] start ok', { x: popupX.value, y: popupY.value });
  }

  // 이동 중 좌표 갱신 + 컨테이너 경계 내 클램프.
  function onDrag(e: MouseEvent) {
    if (!isDragging.value) return;
    if (!(window as any).__popupDragLogged) {
      (window as any).__popupDragLogged = true;
      console.log('[POPUP DRAG] move firing');
    }
    const panel = popupRef.value;
    const container = getContainerEl();
    if (!panel || !container) return;

    const containerRect = container.getBoundingClientRect();
    const panelWidth = panel.offsetWidth;
    const panelHeight = panel.offsetHeight;

    let newX = e.clientX - containerRect.left - dragStartX.value;
    let newY = e.clientY - containerRect.top - dragStartY.value;

    const maxX = Math.max(0, container.clientWidth - panelWidth - 8);
    const maxY = Math.max(0, container.clientHeight - panelHeight - 8);
    newX = Math.max(8, Math.min(newX, maxX));
    newY = Math.max(8, Math.min(newY, maxY));

    popupX.value = newX;
    popupY.value = newY;
  }

  function stopDrag() {
    isDragging.value = false;
    window.removeEventListener('mousemove', onDrag);
    window.removeEventListener('mouseup', stopDrag);
  }

  // 저장된 좌표를 현재 컨테이너 크기에 맞게 보정(리사이즈·재오픈 시).
  function clampPopupToContainer() {
    const panel = popupRef.value;
    const container = getContainerEl();
    if (!panel || !container) return;
    if (popupX.value === null || popupY.value === null) return;

    const maxX = Math.max(0, container.clientWidth - panel.offsetWidth - 8);
    const maxY = Math.max(0, container.clientHeight - panel.offsetHeight - 8);
    popupX.value = Math.max(8, Math.min(popupX.value, maxX));
    popupY.value = Math.max(8, Math.min(popupY.value, maxY));
  }

  function handleResize() {
    nextTick(() => clampPopupToContainer());
  }

  onMounted(() => {
    window.addEventListener('resize', handleResize);
  });

  onUnmounted(() => {
    window.removeEventListener('mousemove', onDrag);
    window.removeEventListener('mouseup', stopDrag);
    window.removeEventListener('resize', handleResize);
  });

  // ============================================================
  // 기본 상태
  // ============================================================
  const activeIndex = ref(0);

  const mergedEquipments = computed(() =>
    (props.equipments || []).map((base) => ({
      base,
      merged: store.getMergedEquipmentState(base),
    })),
  );

  watch(
    () => props.equipments.map((e) => e.id).join(','),
    () => {
      activeIndex.value = 0;
    },
  );

  watch(
    () => mergedEquipments.value.length,
    (len) => {
      if (len === 0) {
        activeIndex.value = 0;
        return;
      }
      if (activeIndex.value >= len) activeIndex.value = 0;
    },
  );

  const activeItem = computed(() => mergedEquipments.value[activeIndex.value] ?? null);
  const activeBase = computed(() => activeItem.value?.base ?? null);
  const activeEq = computed(() => activeItem.value?.merged ?? null);
  const activeEqStatus = computed(
    () => activeEq.value?.status ?? activeEq.value?.currentStatus ?? null,
  );

  // ============================================================
  // 제어 상태
  // ============================================================
  const controlInfo = ref<DashboardControlInfo | null>(null);
  const controlLoading = ref(false);
  const inlineMsg = ref<string | null>(null);
  const inlineMsgType = ref<'success' | 'error'>('success');

  const newSku = ref('');
  const newPalletId = ref('');
  const newQty = ref(1);

  // 상세 메타(설비 정보 + 랙 정보 + 실시간 상태) 접힘 토글.
  // default: 접힘. 다른 설비 선택 시 watch에서 reset.
  const detailsExpanded = ref(false);

  // ============================================================
  // 탭 전환
  // ============================================================
  async function handleTabChange(index: number) {
    activeIndex.value = index;
    await fetchControlInfo();
  }

  // ============================================================
  // 팝업 가시성 / 선택 변경 시 제어 정보 조회
  // ============================================================
  watch(
    [() => props.visible, () => props.equipments.map((e) => e.id).join(',')],
    async ([visible]) => {
      if (!visible) {
        controlInfo.value = null;
        inlineMsg.value = null;
        newSku.value = '';
        newPalletId.value = '';
        newQty.value = 1;
        detailsExpanded.value = false;
        return;
      }
      detailsExpanded.value = false;
      nextTick(() => clampPopupToContainer());
      await fetchControlInfo();
    },
    { immediate: false },
  );

  /**
   * 현재 선택된 설비의 WCS 제어 정보 조회.
   * dashboardData(레이아웃+ECS)와 WCS 로케이션 상태(use_yn, lock_yn)는 별개 도메인이므로
   * WebSocket으로 실시간 갱신되지 않음 → 팝업 오픈/탭 전환 시 별도 조회.
   */
  async function fetchControlInfo() {
    const base = activeBase.value;
    if (!base?.realEqId) {
      controlInfo.value = null;
      return;
    }

    controlLoading.value = true;
    try {
      if (isRackEquipment(base)) {
        const currentGroupId = store.selectedEqGroupId;
        controlInfo.value = await getRackControlInfo(currentGroupId, base.realEqId);
      } else if (isNonRackControllable(base)) {
        controlInfo.value = await getEquipmentControlInfo(
          base.equipmentTypeCode ?? '',
          base.realEqId,
        );
      } else {
        controlInfo.value = null;
      }
    } catch (e) {
      console.warn('[EquipmentDetailPopup] fetchControlInfo failed:', e);
      controlInfo.value = null;
    } finally {
      controlLoading.value = false;
    }
  }

  // ============================================================
  // 재고 정합성 감지 (WcsLocStatus 헬퍼)
  // ============================================================
  const isEmptyPickCondition = computed(() => isEmptyPick(controlInfo.value?.locStatus));
  const isDoubleEntryCondition = computed(() => isDoubleEntry(controlInfo.value?.locStatus));

  // ============================================================
  // 설비 제어 상태
  // ============================================================
  const isCurrentlyEnabled = computed(() => {
    if (!controlInfo.value) return true;
    if (controlInfo.value.locUseYn != null) return controlInfo.value.locUseYn === 1;
    if (controlInfo.value.eqUseYn != null) return controlInfo.value.eqUseYn;
    return true;
  });

  const isCurrentlyLocked = computed(() => controlInfo.value?.locLockYn === 1);

  // ============================================================
  // 포트 락 holder 표시 / 강제 해제
  // ============================================================
  const portLockTaskId = computed(() => controlInfo.value?.portLockTaskId ?? null);
  const portLockBarcode = computed(() => controlInfo.value?.portLockBarcode ?? null);
  const portLockOrderStatus = computed(() => controlInfo.value?.portLockOrderStatus ?? null);
  const isPortLocked = computed(() => !!portLockTaskId.value);
  const isDispatchLockSentinel = computed(() => portLockTaskId.value === 'DISPATCH_LOCK');
  const portLockOrderStatusLabel = computed(() =>
    portLockOrderStatus.value != null ? getOrderStatusText(portLockOrderStatus.value) : null,
  );

  /** 강제 해제 모달 가시성 */
  const forceUnlockModalVisible = ref(false);

  function openForceUnlockModal() {
    forceUnlockModalVisible.value = true;
  }
  function closeForceUnlockModal() {
    forceUnlockModalVisible.value = false;
  }

  async function handleForceUnlockConfirm(payload: { comment: string }) {
    const info = controlInfo.value as any;
    const eqGroupId = info?.eqGroupId ?? store.selectedEqGroupId;
    const portCode = info?.locId ?? (activeBase.value as any)?.realEqId;
    if (!eqGroupId || !portCode) {
      showMsg('포트 정보가 없습니다.', 'error');
      return;
    }
    controlLoading.value = true;
    try {
      const res = await forceUnlockPort(eqGroupId, portCode, payload.comment);
      if (res.success) {
        if (controlInfo.value) {
          (controlInfo.value as any).portLockTaskId = null;
          (controlInfo.value as any).portLockBarcode = null;
          (controlInfo.value as any).portLockOrderStatus = null;
        }
        showMsg(res.message, 'success');
        await Promise.all([fetchControlInfo(), store.loadDashboardData()]).catch(() => {});
      } else {
        showMsg(res.message, 'error');
      }
    } catch (e: any) {
      showMsg('처리 중 오류: ' + (e?.message ?? ''), 'error');
    } finally {
      controlLoading.value = false;
      closeForceUnlockModal();
    }
  }

  /**
   * 현재 설비에 물려있는 작업 키.
   * ECS 실시간 currentOrderKey 우선 → 없으면 WCS controlInfo (RACK 셀 등 ECS 실시간이 없는 경우).
   */
  const activeOrderKey = computed(
    () => activeEq.value?.currentOrderKey ?? controlInfo.value?.activeOrderKey ?? null,
  );

  const activeOrderStatus = computed(() => controlInfo.value?.activeOrderStatus ?? null);

  /**
   * 작업 중인 랙의 상태 라벨 — order_type 우선 (입고/출고/이동/재입고),
   * 없으면 추상 상태 코드 fallback.
   */
  function getRackStateDisplay(base: any): string {
    const t = base?.realRackActiveOrderType;
    if (t) return WcsOrderTypeWorkingLabels[t] ?? t;
    return base?.realRackStateCode ?? '';
  }

  // ============================================================
  // 인라인 메시지
  // ============================================================
  function showMsg(msg: string, type: 'success' | 'error' = 'success') {
    inlineMsg.value = msg;
    inlineMsgType.value = type;
    setTimeout(() => {
      inlineMsg.value = null;
    }, 3500);
  }

  // ============================================================
  // 포트 모드 전환
  // ============================================================

  /** 포트류 설비(고정/겸용 포함) */
  function isPortEquipment(base: any): boolean {
    const w = base?.wcsLocType;
    const r = base?.realRackType;
    return (
      w === WcsLocType.INBOUND_PORT ||
      w === WcsLocType.OUTBOUND_PORT ||
      w === WcsLocType.IN_OUTBOUND_PORT ||
      r === RackType.INBOUND_PORT.code ||
      r === RackType.OUTBOUND_PORT.code ||
      r === RackType.IN_OUTBOUND_PORT.code
    );
  }

  /** 입출고 겸용(전환 가능) 포트 */
  function isSwitchablePort(base: any): boolean {
    return (
      base?.wcsLocType === WcsLocType.IN_OUTBOUND_PORT ||
      base?.realRackType === RackType.IN_OUTBOUND_PORT.code
    );
  }

  /** 사용자가 직접 전환 가능한 모드 (SWITCHING_* / OUTBOUND_PRIORITY 는 자동 진행 상태라 제외) */
  const PORT_MODE_OPTIONS = [PortMode.INBOUND, PortMode.OUTBOUND, PortMode.IDLE];

  const currentPortMode = computed<string | null>(
    () =>
      (controlInfo.value as any)?.portMode ??
      (activeBase.value as any)?.wcsPortMode ??
      (activeBase.value as any)?.portMode ??
      null,
  );

  /** 라벨/아이콘/배지 클래스 모두 PortMode 디스크립터에서 lookup */
  const currentPortModeLabel = computed(() => enumLabel(PortMode, currentPortMode.value, '-'));

  const portModeBadgeClass = computed(() => {
    const variant = enumBadgeVariant(PortMode, currentPortMode.value);
    return variant
      ? `badge-${variant === 'in' ? 'inStockType,bound' : variant === 'out' ? 'outbound' : variant}`
      : 'badge-unknown';
  });

  const isDraining = computed(() => isPortDraining(currentPortMode.value));
  const drainingTarget = computed(() => getPortDrainTarget(currentPortMode.value));

  const switchDisabledReason = computed<string | null>(() => {
    if (!isSwitchablePort(activeBase.value)) return '겸용 포트가 아닙니다';
    if (isCurrentlyLocked.value) return '포트 락 상태 — 강제 해제 후 전환하세요';
    return null;
  });

  const canSwitchPortMode = computed(() => !switchDisabledReason.value);

  async function handlePortModeChange(newMode: PortModeValue) {
    const info = controlInfo.value as any;
    const locCode = info?.locCode ?? (activeBase.value as any)?.realEqId;
    const eqGroupId = info?.eqGroupId ?? store.selectedEqGroupId;

    if (!locCode || !eqGroupId) {
      showMsg('포트 정보가 없습니다.', 'error');
      return;
    }
    if (currentPortMode.value === newMode) return;

    const label = PORT_MODE_OPTIONS.find((o) => o.code === newMode)?.label ?? newMode;
    const reason = prompt(
      `[${locCode}] 포트를 "${label}" 모드로 전환합니다.\n\n` +
        `• 진행중 작업이 있으면 자동 드레인 후 전환됩니다.\n` +
        `• 사유를 입력하세요 (감사 로그 기록).`,
      '',
    );
    if (reason === null) return;

    // ★ 낙관적 업데이트 — controlInfo.portMode를 즉시 새 모드로
    if (controlInfo.value) {
      (controlInfo.value as any).portMode = newMode;
    }
    controlLoading.value = true; // ← 이 플래그가 곧 "전환 중" 락 역할

    try {
      const res = await changePortMode(eqGroupId, locCode, newMode, reason.trim());
      if (res.success) {
        // 서버 확정값으로 갱신 (드레인 중이면 SWITCHING_* 가 올 수 있음)
        if (controlInfo.value && res.data?.currentMode) {
          (controlInfo.value as any).portMode = res.data.currentMode;
        }
        showMsg(res.message, 'success');
        store.loadDashboardData().catch(() => {});
        if (res.isDraining) {
          setTimeout(() => fetchControlInfo(), 5000);
        }
      } else {
        // 실패 시 롤백 — 직전 모드를 모르므로 서버에서 다시 받아옴
        showMsg(res.message ?? '전환 실패', 'error');
        await fetchControlInfo();
      }
    } catch (e: any) {
      showMsg('처리 중 오류: ' + (e?.message ?? ''), 'error');
      await fetchControlInfo();
    } finally {
      controlLoading.value = false;
    }
  }

  /** 드레인 취소 — 원래 모드로 복귀 */
  async function handleCancelDrain() {
    if (!isDraining.value) return;
    const restoreMode = getPortDrainOrigin(currentPortMode.value);
    if (!restoreMode) return;

    const info = controlInfo.value as any;
    const locCode = info?.locCode ?? (activeBase.value as any)?.realEqId;
    const eqGroupId = info?.eqGroupId ?? store.selectedEqGroupId;

    if (
      !confirm(
        `전환 대기를 취소하고 ${
          restoreMode === PortMode.INBOUND.code ? '입고' : '출고'
        } 모드로 복귀합니다.\n계속하시겠습니까?`,
      )
    )
      return;

    controlLoading.value = true;
    try {
      const res = await changePortMode(eqGroupId, locCode, restoreMode, '운영자 드레인 취소');
      if (res.success) {
        if (controlInfo.value && res.data?.currentMode) {
          (controlInfo.value as any).portMode = res.data.currentMode;
        }
        showMsg('전환 취소됨', 'success');
        store.loadDashboardData().catch(() => {});
      } else {
        showMsg(res.message ?? '취소 실패', 'error');
      }
    } catch (e: any) {
      showMsg('처리 중 오류: ' + (e?.message ?? ''), 'error');
    } finally {
      controlLoading.value = false;
    }
  }

  // ============================================================
  // 제어 핸들러 (Use/Lock/재고/오더)
  // ============================================================

  async function handleToggleUse() {
    const actionLabel = isCurrentlyEnabled.value ? '비가동' : '가동';
    const eqCode = getEquipmentCode(activeBase.value);
    if (!confirm(`[${eqCode}] 설비를 ${actionLabel} 처리하시겠습니까?`)) return;

    controlLoading.value = true;
    try {
      let result;
      const info = controlInfo.value!;
      if (isRackEquipment(activeBase.value) && info.locCode && info.eqGroupId) {
        result = await toggleLocUse(info.eqGroupId, info.locCode);
      } else if (activeBase.value?.realEqId && activeBase.value?.equipmentTypeCode) {
        result = await toggleEquipmentUse(
          activeBase.value.equipmentTypeCode,
          activeBase.value.realEqId,
        );
      } else {
        showMsg('제어 정보가 없습니다.', 'error');
        return;
      }

      if (result.success) {
        if (controlInfo.value) {
          if (result.data?.locUseYn != null) controlInfo.value.locUseYn = result.data.locUseYn;
          if (result.data?.locStatus != null) controlInfo.value.locStatus = result.data.locStatus;
          if (result.data?.eqUseYn != null) controlInfo.value.eqUseYn = result.data.eqUseYn;
        }
        showMsg(result.message, 'success');
        store.loadDashboardData().catch(() => {});
      } else {
        showMsg(result.message, 'error');
      }
    } catch (e: any) {
      showMsg('처리 중 오류가 발생했습니다: ' + (e?.message ?? ''), 'error');
    } finally {
      controlLoading.value = false;
    }
  }

  async function handleToggleLock() {
    const info = controlInfo.value;
    if (!info?.locCode || !info.eqGroupId) {
      showMsg('로케이션 정보가 없습니다.', 'error');
      return;
    }

    const actionLabel = isCurrentlyLocked.value ? '잠금 해제' : '수동 잠금';
    const eqCode = getEquipmentCode(activeBase.value);
    if (!confirm(`[${eqCode}] 로케이션을 ${actionLabel} 하시겠습니까?`)) return;

    controlLoading.value = true;
    try {
      const result = isCurrentlyLocked.value
        ? await manualUnlock(info.eqGroupId, info.locCode)
        : await manualLock(info.eqGroupId, info.locCode);

      if (result.success) {
        if (controlInfo.value) {
          controlInfo.value.locLockYn = result.data?.locLockYn ?? (isCurrentlyLocked.value ? 0 : 1);
          controlInfo.value.locLockBy = result.data?.locLockBy ?? null;
          if (result.data?.locStatus != null) controlInfo.value.locStatus = result.data.locStatus;
        }
        showMsg(result.message, 'success');
        store.loadDashboardData().catch(() => {});
      } else {
        showMsg(result.message, 'error');
      }
    } catch (e: any) {
      showMsg('처리 중 오류가 발생했습니다.', 'error');
    } finally {
      controlLoading.value = false;
    }
  }

  /** 공출고(Empty Pick) 복구 — 시스템 재고 전량 비활성(is_enabled=false, 되돌림 가능) */
  async function handleDeleteInventory() {
    const info = controlInfo.value;
    if (!info?.locCode || !info.eqGroupId) {
      showMsg('로케이션 정보가 없습니다. 재고 조회를 먼저 하세요.', 'error');
      return;
    }

    const eqCode = getEquipmentCode(activeBase.value);
    if (
      !confirm(
        `[${eqCode}] 공출고 복구\n\n` +
          `로케이션 [${info.locCode}]의 WCS 재고를 전량 비활성 처리합니다.\n` +
          `(물리 삭제가 아닌 is_enabled=false · 위치 EMPTY 복원, audit 기록)\n\n계속하시겠습니까?`,
      )
    )
      return;

    controlLoading.value = true;
    try {
      const result = await deleteInventory(info.eqGroupId, info.locCode);
      if (result.success) {
        if (controlInfo.value) controlInfo.value.inventory = [];
        showMsg(result.message, 'success');
        store.loadDashboardData().catch(() => {});
      } else {
        showMsg(result.message, 'error');
      }
    } catch (e: any) {
      showMsg('처리 중 오류가 발생했습니다.', 'error');
    } finally {
      controlLoading.value = false;
    }
  }

  /** 이중입고(Double Entry) 복구 — 수동 재고 생성 */
  async function handleCreateInventory() {
    const info = controlInfo.value;
    if (!info?.locCode || !info.eqGroupId) {
      showMsg('로케이션 정보가 없습니다.', 'error');
      return;
    }
    if (!newSku.value.trim() || newQty.value < 1) {
      showMsg('SKU 코드와 수량(1 이상)을 입력하세요.', 'error');
      return;
    }

    const eqCode = getEquipmentCode(activeBase.value);
    if (
      !confirm(
        `[${eqCode}] 이중입고 복구\n\n` +
          `로케이션: ${info.locCode}\n` +
          `SKU: ${newSku.value.trim()}\n` +
          `LPN: ${newPalletId.value.trim() || '(없음)'}\n` +
          `수량: ${newQty.value} EA\n\n` +
          `위 내용으로 재고를 생성하시겠습니까?`,
      )
    )
      return;

    controlLoading.value = true;
    try {
      const result = await createInventory(
        info.eqGroupId,
        info.locCode,
        newSku.value.trim(),
        newPalletId.value.trim(),
        newQty.value,
      );
      if (result.success) {
        newSku.value = '';
        newPalletId.value = '';
        newQty.value = 1;
        await Promise.all([fetchControlInfo(), store.loadDashboardData()]).catch(() => {});
        showMsg(result.message, 'success');
      } else {
        showMsg(result.message, 'error');
      }
    } catch (e: any) {
      showMsg('처리 중 오류가 발생했습니다.', 'error');
    } finally {
      controlLoading.value = false;
    }
  }

  async function handleResumeOrder() {
    if (!activeOrderKey.value) return;
    if (
      !confirm(
        `[${activeOrderKey.value}] 작업을 재개(Resume) 하시겠습니까?\n에러 상태 복구 후 사용하세요.`,
      )
    )
      return;

    controlLoading.value = true;
    try {
      const result = await resumeOrderWcs(activeOrderKey.value);
      showMsg(result.message, result.success ? 'success' : 'error');
      if (result.success) {
        store.loadDashboardData().catch(() => {});
      }
    } catch (e: any) {
      showMsg('처리 중 오류가 발생했습니다.', 'error');
    } finally {
      controlLoading.value = false;
    }
  }

  async function handleForceComplete() {
    if (!activeOrderKey.value) return;
    if (
      !confirm(
        `[${activeOrderKey.value}] 작업을 강제 완료 처리하시겠습니까?\n\n` +
          `⚠️ 주의: 실물 화물 이동이 실제로 완료된 것을 현장에서 반드시 확인하세요.\n` +
          `강제 완료 후 로케이션 Lock이 해제됩니다.`,
      )
    )
      return;

    controlLoading.value = true;
    try {
      const result = await forceCompleteOrder(activeOrderKey.value);
      showMsg(result.message, result.success ? 'success' : 'error');
      if (result.success) {
        await Promise.all([fetchControlInfo(), store.loadDashboardData()]).catch(() => {});
      }
    } catch (e: any) {
      showMsg('처리 중 오류가 발생했습니다.', 'error');
    } finally {
      controlLoading.value = false;
    }
  }

  async function handleCancelOrder() {
    if (!activeOrderKey.value) return;
    if (
      !confirm(
        `[${activeOrderKey.value}] 작업을 취소하시겠습니까?\n\n` +
          `취소된 작업은 복구할 수 없습니다.\n로케이션 Lock이 해제됩니다.`,
      )
    )
      return;

    controlLoading.value = true;
    try {
      const result = await cancelOrderWcs(activeOrderKey.value, '운영자 수동 취소');
      showMsg(result.message, result.success ? 'success' : 'error');
      if (result.success) {
        await Promise.all([fetchControlInfo(), store.loadDashboardData()]).catch(() => {});
      }
    } catch (e: any) {
      showMsg('처리 중 오류가 발생했습니다.', 'error');
    } finally {
      controlLoading.value = false;
    }
  }

  // ============================================================
  // 헬퍼 함수
  // ============================================================

  function getEquipmentCode(equipment: DashboardEquipmentData | null | undefined): string {
    return equipment?.realEqId || equipment?.equipmentCode || equipment?.id || '-';
  }

  function getEquipmentType(equipment: DashboardEquipmentData | null | undefined): string {
    return equipment?.equipmentTypeCode || equipment?.realEqType || '-';
  }

  function isRackEquipment(base: DashboardEquipmentData | null | undefined): boolean {
    return base?.equipmentTypeCode === LayoutEquipmentType.RACK.code;
  }

  function isShuttleEquipment(base: DashboardEquipmentData | null | undefined): boolean {
    return base?.equipmentTypeCode === LayoutEquipmentType.SHUTTLE.code;
  }

  /** CONVEYOR / LIFTER / SHUTTLE — use_yn 제어 가능한 비랙 타입 */
  function isNonRackControllable(base: DashboardEquipmentData | null | undefined): boolean {
    const type = base?.equipmentTypeCode?.toUpperCase();
    return (
      type === LayoutEquipmentType.CONVEYOR.code ||
      type === LayoutEquipmentType.LIFTER.code ||
      type === LayoutEquipmentType.SHUTTLE.code
    );
  }

  function isDriveOnly(base: DashboardEquipmentData | null | undefined): boolean {
    return base?.realRackDriveOnlyYn === true;
  }

  // ============================================================
  // 재고 상세
  // ============================================================
  const inventoryRows = computed<InventoryItem[]>(() => controlInfo.value?.inventory ?? []);

  /** 모든 재고가 같은 palletId면 혼적 팔레트 */
  const isSamePallet = computed(() => {
    const rows = inventoryRows.value;
    if (rows.length < 2) return false;
    const first = rows[0].palletId;
    if (!first) return false;
    return rows.every((r) => r.palletId === first);
  });

  /** 재고 상태 라벨 (StockStatus 기반) */
  function getStockStatusText(status: number | null | undefined): string {
    if (status == null) return '-';
    return StockStatusLabels[status] ?? `상태 ${status}`;
  }

  /** 재고 카테고리 라벨 (StockType 기반) */
  function getStockTypeText(type: string | null | undefined): string {
    if (!type) return '-';
    return StockTypeLabels[type] ?? type;
  }

  function getStockStatusClass(status: number | null | undefined): string {
    if (status == null) return '';
    if (status === StockStatus.IDLE) return 'stock-status-normal';
    if (status === StockStatus.HOST_PENDING) return 'stock-status-reserved';
    return 'stock-status-hold';
  }

  /** IDLE(0)이 아니면 hold */
  function isHoldStatus(status: number | null | undefined): boolean {
    return status != null && status !== StockStatus.IDLE && status !== StockStatus.HOST_PENDING;
  }

  function isExpired(expiredDate: string | null | undefined): boolean {
    if (!expiredDate) return false;
    const exp = new Date(expiredDate);
    if (isNaN(exp.getTime())) return false;
    return exp.getTime() < Date.now();
  }

  // ============================================================
  // 셀 클릭 액션 메뉴 — stock_type + stock_status 가시성 분기
  // ============================================================

  // ============================================================
  // 셀 클릭 액션 메뉴 — stock_type + stock_status 가시성 분기
  // ============================================================
  const actionBusy = ref(false);

  /**
   * 작업 진행 중인 재고면 어떤 액션도 차단.
   * - HOST_PENDING: 이미 출고 지시 등록됨 (중복 방지)
   * - INBOUND / OUTBOUND / RELOCATION / INBOUND_READY: 셔틀 단계 작업 중
   *
   * HOLD 는 통과 — 반품/폐기 격리 상태에서도 운영자 수동 출고, 카테고리 재전환 가능해야 함.
   * (HOLD 자체는 "자동 입출고 차단" 의미이지 운영자 액션 차단 의미가 아님)
   */
  function isStockAvailable(inv: InventoryItem): boolean {
    const s = inv.stockStatus;
    return s == null || s === StockStatus.IDLE || s === StockStatus.HOLD;
  }

  function canManualOutbound(inv: InventoryItem): boolean {
    if (!isStockAvailable(inv)) return false;
    const t = inv.stockType;
    return t === 'NORMAL' || t === 'RETURN' || t === 'DISPOSAL';
  }

  function canTransitionToDisposal(inv: InventoryItem): boolean {
    if (!isStockAvailable(inv)) return false;
    const t = inv.stockType;
    return t === 'NORMAL' || t === 'QC_FAIL' || t === 'NIA_FAIL';
  }

  function canTransitionToReturn(inv: InventoryItem): boolean {
    if (!isStockAvailable(inv)) return false;
    const t = inv.stockType;
    return t === 'NORMAL' || t === 'QC_FAIL' || t === 'NIA_FAIL';
  }

  function canApproveNia(inv: InventoryItem): boolean {
    if (!isStockAvailable(inv)) return false;
    return inv.stockType === 'NIA_PENDING';
  }

  /**
   * NORMAL/QC_FAIL → NIA_PENDING 전환 가능 여부.
   * 운영자 판단으로 재고를 국검 대기 상태로 돌릴 때 사용.
   */
  function canTransitionToNiaPending(inv: InventoryItem): boolean {
    if (!isStockAvailable(inv)) return false;
    const t = inv.stockType;
    return t === 'NORMAL' || t === 'QC_FAIL';
  }

  /**
   * NORMAL → QC 대기 보정 가능 여부 (QC 대상 누락 오기입 보정).
   * 재고+item+의뢰마스터+주문상태를 한 번에 정정한다.
   */
  function canCorrectToQcPending(inv: InventoryItem): boolean {
    if (!isStockAvailable(inv)) return false;
    return inv.stockType === 'NORMAL';
  }

  /**
   * 샘플 출고 (SAMPLE_OUT) 가능 여부.
   * 수량을 사후 확정하는 출고. 채취 / 시험 등 다양한 케이스 통칭.
   * 출고 → 워크스테이션 채취 → 남은 양 자동 재입고.
   */
  function canSampleOutbound(inv: InventoryItem): boolean {
    if (!isStockAvailable(inv)) return false;
    const t = inv.stockType;
    return t === 'NORMAL' || t === 'QC_PENDING' || t === 'QC_FAIL';
  }

  function hasAnyAction(inv: InventoryItem): boolean {
    return (
      (canManualOutbound(inv) && can('update')) ||
      (canSampleOutbound(inv) && can('update')) ||
      (canTransitionToDisposal(inv) && can('delete')) ||
      (canTransitionToReturn(inv) && can('update')) ||
      (canApproveNia(inv) && can('update')) ||
      (canTransitionToNiaPending(inv) && can('update')) ||
      (canCorrectToQcPending(inv) && can('update')) ||
      (canAdjustQty(inv) && can('update'))
    );
  }

  function parseError(e: any): string {
    if (!e) return '알 수 없는 오류';
    const data = e?.response?.data;
    if (data?.message) return String(data.message);
    if (data?.error) return String(data.error);
    if (e.message) return String(e.message);
    return '요청 실패';
  }

  // ─── 통합 액션 모달 (반품/폐기/국검 승인/국검 미승인/국검 대기/수동 출고/샘플 출고) ──
  // 모든 운영자 액션이 본 모달로 진입. comment 필수.
  type ActionKind =
    | 'TRANSITION'
    | 'MANUAL_OUTBOUND'
    | 'SAMPLE_OUTBOUND'
    | 'QC_CORRECTION'
    | 'ADJUST_QTY';
  type ActVariant = 'primary' | 'warn' | 'danger' | 'success';

  const actionModal = reactive({
    open: false,
    busy: false,
    kind: 'TRANSITION' as ActionKind,
    inv: null as InventoryItem | null,
    title: '',
    confirmLabel: '',
    confirmVariant: 'primary' as ActVariant,
    currentType: '',
    nextType: '',
    targetType: '', // TRANSITION 전용: 'RETURN' | 'DISPOSAL' | 'NORMAL' | 'NIA_PENDING'
    showTestRequestNo: false,
    showPortCode: false,
    extraNotice: '',
  });

  function openTransitionModal(
    inv: InventoryItem,
    to: string,
    title: string,
    confirmLabel: string,
    variant: ActVariant,
  ) {
    if (!inv?.stockId) return;
    actionModal.kind = 'TRANSITION';
    actionModal.inv = inv;
    actionModal.title = title;
    actionModal.confirmLabel = confirmLabel;
    actionModal.confirmVariant = variant;
    actionModal.currentType = inv.stockType || '-';
    actionModal.nextType = to;
    actionModal.targetType = to;
    actionModal.showTestRequestNo = to === 'NIA_PENDING';
    actionModal.showPortCode = false;
    actionModal.extraNotice =
      to === 'DISPOSAL' ? '폐기 처리는 되돌리기 어렵습니다. 사유를 명확히 기록하세요.' : '';
    actionModal.busy = false;
    actionModal.open = true;
  }

  /**
   * QC 누락 보정 모달 — NORMAL 재고를 QC 대기로 되돌림(재고+item+의뢰마스터+주문 재오픈).
   */
  function openQcCorrectionModal(inv: InventoryItem) {
    if (!inv?.stockId) return;
    actionModal.kind = 'QC_CORRECTION';
    actionModal.inv = inv;
    actionModal.title = 'QC 대기 보정';
    actionModal.confirmLabel = 'QC 대기 전환';
    actionModal.confirmVariant = 'warn';
    actionModal.currentType = inv.stockType || '-';
    actionModal.nextType = 'QC_PENDING';
    actionModal.targetType = 'QC_PENDING';
    actionModal.showTestRequestNo = false;
    actionModal.showPortCode = false;
    actionModal.extraNotice =
      'QC 대상 누락 보정: 시험 대기로 전환하고 의뢰를 생성합니다. 완료된 입고는 시험 미종결로 재오픈됩니다.';
    actionModal.busy = false;
    actionModal.open = true;
  }

  function openOutboundModal(inv: InventoryItem) {
    if (!inv?.stockId) return;
    if (!isStockAvailable(inv)) {
      notification.warning({
        message: '수동 출고 불가',
        description: '이미 다른 작업이 점유 중인 재고입니다.',
      });
      return;
    }
    const avail = Number(inv.qty) || 0;
    if (avail <= 0) {
      notification.warning({ message: '수동 출고', description: '출고 가능 수량이 없습니다.' });
      return;
    }
    actionModal.kind = 'MANUAL_OUTBOUND';
    actionModal.inv = inv;
    actionModal.title = '수동 출고 지시';
    actionModal.confirmLabel = '출고 지시';
    actionModal.confirmVariant = 'primary';
    actionModal.currentType = inv.stockType || '-';
    actionModal.nextType = '출고 (파렛트 전체)';
    actionModal.targetType = '';
    actionModal.showTestRequestNo = false;
    actionModal.showPortCode = true;
    actionModal.extraNotice = `해당 파렛트를 통째로 출고합니다. 부분 출고는 HOST 출고 지시를 사용하세요. (수량 ${avail} EA)`;
    actionModal.busy = false;
    actionModal.open = true;
  }

  /**
   * 샘플 출고 모달 — 채취 / 시험 등 수량을 사후 확정하는 출고.
   * 출고 → 워크스테이션 채취 → 남은 양 자동 재입고. 포트 lock 없음.
   */
  function openSampleOutboundModal(inv: InventoryItem) {
    if (!inv?.stockId) return;
    if (!isStockAvailable(inv)) {
      notification.warning({
        message: '샘플 출고 불가',
        description: '이미 다른 작업이 점유 중인 재고입니다.',
      });
      return;
    }
    actionModal.kind = 'SAMPLE_OUTBOUND';
    actionModal.inv = inv;
    actionModal.title = '샘플 출고';
    actionModal.confirmLabel = '샘플 출고';
    actionModal.confirmVariant = 'warn';
    actionModal.currentType = inv.stockType || '-';
    actionModal.nextType = '샘플 출고 (수량 사후 확정)';
    actionModal.targetType = '';
    actionModal.showTestRequestNo = false;
    actionModal.showPortCode = true;
    actionModal.extraNotice =
      '출고된 파렛트가 워크스테이션에 도착하면 작업자가 채취 수량을 입력합니다. 남은 양은 자동으로 재입고됩니다.';
    actionModal.busy = false;
    actionModal.open = true;
  }

  /** 수량 보정 가능 — 가용(IDLE) 또는 보류(HOLD) 재고만. 진행 중 재고는 백엔드에서도 거부. */
  function canAdjustQty(inv: InventoryItem): boolean {
    return !!inv.stockId && (isStockAvailable(inv) || isHoldStatus(inv.stockStatus));
  }

  /** 라인 단위 수량 보정 모달 — 채취/박스출고 오기입 보정. 0 입력 시 비활성. */
  function openAdjustQtyModal(inv: InventoryItem) {
    if (!inv?.stockId) return;
    actionModal.kind = 'ADJUST_QTY';
    actionModal.inv = inv;
    actionModal.title = '재고 수량 보정';
    actionModal.confirmLabel = '수량 보정';
    actionModal.confirmVariant = 'warn';
    actionModal.currentType = inv.stockType || '-';
    actionModal.nextType = '';
    actionModal.targetType = '';
    actionModal.showTestRequestNo = false;
    actionModal.showPortCode = false;
    const curQty = inv.qty ?? 0;
    actionModal.extraNotice = `채취/박스출고 오기입 보정. 0 입력 시 비활성 처리됩니다. (현재 ${curQty} EA)`;
    actionModal.busy = false;
    actionModal.open = true;
  }

  function closeActionModal() {
    if (actionModal.busy) return;
    actionModal.open = false;
    actionModal.inv = null;
  }

  async function onActionConfirm(payload: {
    comment: string;
    testRequestNo?: string;
    portCode?: string;
    qty?: number;
  }) {
    const inv = actionModal.inv;
    if (!inv?.stockId) return;
    if (actionModal.kind === 'TRANSITION') {
      await doTransition(inv, actionModal.targetType, actionModal.title, payload);
    } else if (actionModal.kind === 'QC_CORRECTION') {
      await doQcCorrection(inv, payload);
    } else if (actionModal.kind === 'ADJUST_QTY') {
      await doAdjustQty(inv, payload);
    } else if (actionModal.kind === 'SAMPLE_OUTBOUND') {
      await doSampleOutbound(inv, payload);
    } else {
      await doManualOutbound(inv, payload);
    }
  }

  /**
   * 라인 단위 수량 보정 실행 — 채취/박스출고 오기입 보정 (newQty=0 → 비활성).
   * 행 PK(inv.id) + 셀(locCode) 기준. 셔틀 이동 없음.
   */
  async function doAdjustQty(inv: InventoryItem, payload: { comment: string; qty?: number }) {
    console.log(`doAdjustQty 진입`);
    const info = controlInfo.value;
    console.log(`inv : ${JSON.stringify(inv,null,2)} \n info : ${JSON.stringify(info,null,2)}`);
    if (!inv?.id || !info?.eqGroupId || !info.locCode) return;
    const newQty = Number(payload.qty ?? 0);
    actionModal.busy = true;
    actionBusy.value = true;
    console.log(`doAdjustQty 실행`);
    try {
      const r: any = await adjustInventoryQty(
        info.eqGroupId,
        info.locCode,
        inv.id,
        newQty,
        payload.comment,
      );
      if (r?.success === false) {
        notification.error({
          message: '수량 보정 실패',
          description: r?.message || '요청 거부됨',
          duration: 5,
        });
        return;
      }
      notification.success({
        message: '수량 보정',
        description: `완료: ${newQty} EA`,
        duration: 3,
      });
      actionModal.open = false;
      actionModal.inv = null;
      await fetchControlInfo();
    } catch (e) {
      notification.error({ message: '수량 보정 실패', description: parseError(e), duration: 5 });
    } finally {
      actionBusy.value = false;
      actionModal.busy = false;
    }
  }

  async function doTransition(
    inv: InventoryItem,
    to: string,
    label: string,
    payload: { comment: string; testRequestNo?: string },
  ) {
    if (!inv?.stockId) return;
    actionModal.busy = true;
    actionBusy.value = true;
    try {
      const body: { to: string; comment: string; testRequestNo?: string } = {
        to,
        comment: payload.comment,
      };
      if (payload.testRequestNo) body.testRequestNo = payload.testRequestNo;
      await getCommonPostApi(`/admin/wcs/stock/${inv.stockId}/transition`, body);
      notification.success({ message: label, description: `완료: ${inv.stockId}`, duration: 3 });
      actionModal.open = false;
      actionModal.inv = null;
      await fetchControlInfo();
    } catch (e) {
      notification.error({ message: `${label} 실패`, description: parseError(e), duration: 5 });
    } finally {
      actionBusy.value = false;
      actionModal.busy = false;
    }
  }

  /**
   * QC 누락 보정 실행 — 전용 엔드포인트 호출(재고+item+의뢰마스터+주문 일괄 정정).
   */
  async function doQcCorrection(inv: InventoryItem, payload: { comment: string }) {
    console.log(`doQcCorrection 실행 stock_id 전`);

    if (!inv?.stockId) return;
    actionModal.busy = true;
    actionBusy.value = true;
    console.log(`doQcCorrection 실행`);
    try {
      await getCommonPostApi(`/wcs/qc-test/correction/stocks/${inv.stockId}/to-qc-pending`, {
        eqGroupId: controlInfo.value?.eqGroupId,
        comment: payload.comment,
      });
      notification.success({
        message: 'QC 대기 보정',
        description: `완료: ${inv.stockId}`,
        duration: 3,
      });
      actionModal.open = false;
      actionModal.inv = null;
      await fetchControlInfo();
    } catch (e) {
      notification.error({ message: 'QC 대기 보정 실패', description: parseError(e), duration: 5 });
    } finally {
      actionBusy.value = false;
      actionModal.busy = false;
    }
  }

  async function doManualOutbound(
    inv: InventoryItem,
    payload: { comment: string; portCode?: string },
  ) {
    if (!inv?.stockId) return;
    actionModal.busy = true;
    actionBusy.value = true;
    try {
      const r: any = await getCommonPostApi(`/admin/wcs/stock/${inv.stockId}/manual-outbound`, {
        portCode: payload.portCode || '',
        eqGroupId: controlInfo.value?.eqGroupId,
        comment: payload.comment,
      });
      if (r?.success === false) {
        const isAlreadyReserved =
          r?.errorCode === 'ALREADY_RESERVED' || r?.errorCode === 'STOCK_BUSY';
        notification.error({
          message: isAlreadyReserved ? '이미 출고 지시됨' : '수동 출고 실패',
          description: r?.message || '요청 거부됨',
          duration: 5,
        });
        if (isAlreadyReserved) {
          await fetchControlInfo();
        }
        return;
      }
      notification.success({
        message: '수동 출고 지시 등록',
        description: `host=${r?.hostOrderKey || ''} / status=${r?.hostOrderStatus ?? ''}`,
        duration: 3,
      });
      actionModal.open = false;
      actionModal.inv = null;
      await fetchControlInfo();
    } catch (e) {
      notification.error({ message: '수동 출고 실패', description: parseError(e), duration: 5 });
    } finally {
      actionBusy.value = false;
      actionModal.busy = false;
    }
  }

  /**
   * 운영자 샘플 출고 (SAMPLE_OUT) 호출.
   * POST /admin/wcs/stock/{stockId}/sample-outbound
   */
  async function doSampleOutbound(
    inv: InventoryItem,
    payload: { comment: string; portCode?: string },
  ) {
    if (!inv?.stockId) return;
    actionModal.busy = true;
    actionBusy.value = true;
    try {
      const r: any = await getCommonPostApi(`/admin/wcs/stock/${inv.stockId}/sample-outbound`, {
        portCode: payload.portCode || '',
        eqGroupId: controlInfo.value?.eqGroupId,
        comment: payload.comment,
      });
      if (r?.success === false) {
        notification.error({
          message: '샘플 출고 실패',
          description: r?.message || r?.error || '요청 거부됨',
          duration: 5,
        });
        await fetchControlInfo();
        return;
      }
      notification.success({
        message: '샘플 출고 지시 등록',
        description: `order=${r?.outboundOrderKey || ''} (채취 후 워크스테이션에서 재입고 트리거)`,
        duration: 4,
      });
      actionModal.open = false;
      actionModal.inv = null;
      await fetchControlInfo();
    } catch (e) {
      notification.error({ message: '샘플 출고 실패', description: parseError(e), duration: 5 });
    } finally {
      actionBusy.value = false;
      actionModal.busy = false;
    }
  }

  // ============================================================
  // 랙 표시
  // ============================================================
  // 라벨/아이콘은 RackType 디스크립터(label/icon) 에서 직접 lookup
  function getRackTypeLabel(type: number | null | undefined): string {
    if (type == null) return '-';
    return enumLabel(RackType, type, `타입 ${type}`);
  }

  function getRackTypeIcon(type: number | null | undefined): string {
    return enumIcon(RackType, type, '📍');
  }

  /** 랙 카드 배경 클래스만 popup-내부 의미라 별도 매핑 (in/out/inout/charge) */
  const RACK_CARD_CLASS: Record<number, string> = {
    [RackType.INBOUND_PORT.code]: 'rack-inbound',
    [RackType.OUTBOUND_PORT.code]: 'rack-outbound',
    [RackType.IN_OUTBOUND_PORT.code]: 'rack-inout',
    [RackType.CHARGE_PORT.code]: 'rack-charge',
    [RackType.CHARGE_ENTER_PORT.code]: 'rack-charge',
  };

  function getRackTypeClass(type: number | null | undefined): string {
    if (type == null) return '';
    return RACK_CARD_CLASS[type] ?? '';
  }

  function getRackLocation(base: DashboardEquipmentData | null | undefined): string {
    if (!base) return '-';
    return `R${base.realRackRow ?? '-'}-B${base.realRackBay ?? '-'}-L${base.realRackLevel ?? '-'}`;
  }

  function hasStatusIndicators(e: any): boolean {
    if (!e) return false;
    return (
      e.runYn != null ||
      e.cargoYn != null ||
      e.hasCargo != null ||
      e.hasShuttle != null ||
      e.stopperOpenYn != null ||
      e.stopperOpen != null ||
      e.autoYn != null ||
      e.currentLevel != null ||
      e.targetLevel != null ||
      e.hasActiveJob != null
    );
  }

  /** 셔틀/컨베이어 상태 라벨 — legend-spec 기반 */
  function getStatusText(base: DashboardEquipmentData | null | undefined, e: any): string {
    const type = base?.equipmentTypeCode;
    if (type === LayoutEquipmentType.SHUTTLE.code)
      return shuttleStateSpec(e?.shuttleState)?.label ?? '-';
    if (type === LayoutEquipmentType.CONVEYOR.code)
      return conveyorStateSpec(e?.conveyorState)?.label ?? '-';
    return '-';
  }

  function getStatusClass(base: DashboardEquipmentData | null | undefined, e: any): string {
    const type = base?.equipmentTypeCode;
    if (type === LayoutEquipmentType.SHUTTLE.code && e?.shuttleState)
      return `shuttle-state-${e.shuttleState}`;
    if (type === LayoutEquipmentType.CONVEYOR.code && e?.conveyorState)
      return `conveyor-state-${e.conveyorState}`;
    return '';
  }
</script>

<style scoped>
  /* ==================== 기본 팝업 레이아웃 ==================== */
  .detail-popup {
    position: absolute;
    top: 20px;
    right: 20px;
    width: 420px;
    max-height: calc(100% - 40px);
    background: rgba(30, 34, 45, 0.95);
    backdrop-filter: blur(10px);
    border-radius: 12px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
    z-index: 1000;
    display: flex;
    flex-direction: column;
    color: #e5eaf3;
    overflow: hidden;
  }

  /* 드래그 중 강조 */
  .detail-popup.dragging {
    box-shadow: 0 16px 48px rgba(0, 0, 0, 0.55);
  }
  .detail-popup.dragging .popup-header,
  .detail-popup.dragging .drag-handle {
    cursor: grabbing;
  }

  .popup-header {
    padding: 16px;
    background: rgba(255, 255, 255, 0.05);
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    cursor: grab;
    user-select: none;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  /* 드래그 핸들 글리프 */
  .drag-handle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    color: #606266;
    font-size: 10px;
    letter-spacing: -2px;
    cursor: grab;
    flex-shrink: 0;
  }

  .popup-header h3 {
    margin: 0;
    font-size: 1rem;
    font-weight: 600;
    color: #409eff;
  }

  .selection-badge {
    font-size: 12px;
    color: #cfe7ff;
    background: rgba(64, 158, 255, 0.16);
    border: 1px solid rgba(64, 158, 255, 0.35);
    border-radius: 999px;
    padding: 4px 10px;
  }

  .close-btn {
    background: none;
    border: none;
    color: #909399;
    font-size: 24px;
    cursor: pointer;
    line-height: 1;
    padding: 0;
  }
  .close-btn:hover {
    color: #c0c4cc;
  }

  .popup-content {
    padding: 16px;
    overflow-y: auto;
    flex: 1;
  }

  .info-section {
    margin-bottom: 12px;
  }

  .section-label {
    font-size: 0.7rem;
    color: #858585;
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  /* 포트모드 뱃지: 라벨과 baseline 정렬 보정 */
  .port-mode-top .section-label {
    font-size: 0.75rem;
  }

  .loading-dot {
    color: #409eff;
    font-size: 0.9rem;
  }

  .equipment-tabs {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    margin-bottom: 4px;
  }

  .equipment-tab {
    text-align: left;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.08);
    color: #d9e1ee;
    border-radius: 10px;
    padding: 10px 12px;
    cursor: pointer;
    transition: all 0.2s ease;
  }
  .equipment-tab:hover {
    border-color: rgba(64, 158, 255, 0.35);
    background: rgba(64, 158, 255, 0.08);
  }
  .equipment-tab.active {
    border-color: rgba(64, 158, 255, 0.55);
    background: rgba(64, 158, 255, 0.14);
  }
  .equipment-tab.error {
    border-color: rgba(245, 108, 108, 0.4);
  }
  .tab-title {
    font-size: 13px;
    font-weight: 600;
    color: #f2f5fb;
  }
  .tab-sub {
    margin-top: 2px;
    font-size: 11px;
    color: #9aa7bb;
  }

  .info-row {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 8px;
    font-size: 0.85rem;
  }
  .info-row .label {
    color: #909399;
    flex: 0 0 auto;
  }
  .info-row .value {
    font-weight: 500;
    text-align: right;
    word-break: break-word;
  }
  .info-row .value.highlight {
    color: #67c23a;
  }
  .info-row .value.status-normal {
    color: #67c23a;
  }
  .info-row .value.status-working {
    color: #e6a23c;
  }
  .info-row .value.status-error {
    color: #f56c6c;
  }

  .empty-info {
    color: #909399;
    font-size: 0.9rem;
    text-align: center;
    padding: 20px;
    background: rgba(255, 255, 255, 0.03);
    border-radius: 8px;
  }

  .divider {
    border: 0;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    margin: 12px 0;
  }

  .status-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
    margin: 12px 0;
  }
  .status-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 10px 6px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    transition: all 0.3s ease;
  }
  .status-item.active {
    background: rgba(64, 158, 255, 0.15);
    border-color: rgba(64, 158, 255, 0.4);
  }
  .status-item.warning {
    background: rgba(230, 162, 60, 0.15);
    border-color: rgba(230, 162, 60, 0.4);
  }
  .status-icon {
    font-size: 18px;
    margin-bottom: 4px;
  }
  .status-label {
    font-size: 10px;
    color: #909399;
    margin-bottom: 2px;
  }
  .status-value {
    font-size: 11px;
    font-weight: 600;
  }
  .status-value.on {
    color: #67c23a;
  }
  .status-value.off {
    color: #909399;
  }
  .status-value.open {
    color: #e6a23c;
  }
  .status-value.closed {
    color: #909399;
  }

  .going-up {
    color: #67c23a;
  }
  .going-down {
    color: #e6a23c;
  }

  .error-section {
    background: rgba(245, 108, 108, 0.1);
    border: 1px solid rgba(245, 108, 108, 0.3);
    border-radius: 8px;
    padding: 12px;
    margin-top: 12px;
  }
  .error-badge {
    font-size: 12px;
    font-weight: bold;
    color: #f56c6c;
    margin-bottom: 8px;
  }

  .status-idle {
    color: #909399;
  }

  /* ==================== 랙 정보 ==================== */
  .rack-type-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 10px;
    margin-bottom: 12px;
    transition: all 0.3s ease;
  }
  .rack-type-card.rack-inbound {
    background: rgba(103, 194, 58, 0.12);
    border-color: rgba(103, 194, 58, 0.4);
  }
  .rack-type-card.rack-outbound {
    background: rgba(230, 162, 60, 0.12);
    border-color: rgba(230, 162, 60, 0.4);
  }
  .rack-type-card.rack-inout {
    background: linear-gradient(135deg, rgba(103, 194, 58, 0.1), rgba(230, 162, 60, 0.1));
    border-color: rgba(64, 158, 255, 0.4);
  }
  .rack-type-card.rack-charge {
    background: rgba(155, 89, 182, 0.12);
    border-color: rgba(155, 89, 182, 0.4);
  }
  .rack-type-icon {
    font-size: 28px;
    flex-shrink: 0;
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.08);
    border-radius: 10px;
  }
  .rack-type-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    flex: 1;
  }
  .rack-type-label {
    font-size: 15px;
    font-weight: 600;
    color: #f2f5fb;
  }
  .rack-type-desc {
    font-size: 11px;
    color: #909399;
    display: flex;
    align-items: center;
    gap: 4px;
  }
  .rack-type-desc::before {
    content: '🚗';
    font-size: 10px;
  }
  .location-value {
    font-family: 'Roboto Mono', 'Consolas', monospace;
    font-weight: 600;
    color: #409eff !important;
    letter-spacing: 0.5px;
  }
  .has-cargo {
    color: #f0c14b !important;
    font-weight: 600;
  }
  .no-cargo {
    color: #909399;
  }

  /* ==================== 재고 정합성 경고 ==================== */
  .alert-section {
    border-radius: 10px;
    padding: 14px;
    margin-bottom: 10px;
  }
  .alert-empty-pick {
    background: rgba(245, 108, 108, 0.12);
    border: 1px solid rgba(245, 108, 108, 0.45);
  }
  .alert-double-entry {
    background: rgba(230, 162, 60, 0.12);
    border: 1px solid rgba(230, 162, 60, 0.45);
  }
  .alert-badge {
    font-size: 13px;
    font-weight: 700;
    margin-bottom: 6px;
  }
  .alert-empty-pick .alert-badge {
    color: #f56c6c;
  }
  .alert-double-entry .alert-badge {
    color: #e6a23c;
  }
  .alert-desc {
    font-size: 12px;
    color: #c0c4cc;
    margin: 0 0 10px 0;
    line-height: 1.5;
  }

  .inv-form {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .ctrl-input {
    width: 100%;
    padding: 8px 10px;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 6px;
    color: #e5eaf3;
    font-size: 0.82rem;
    box-sizing: border-box;
  }
  .ctrl-input::placeholder {
    color: #606266;
  }
  .ctrl-input:focus {
    outline: none;
    border-color: rgba(230, 162, 60, 0.6);
  }

  /* ==================== 제어 패널 ==================== */
  .control-section {
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(255, 255, 255, 0.07);
    border-radius: 10px;
    padding: 12px;
    margin-bottom: 10px;
  }

  .control-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 10px;
  }
  .control-row:last-child {
    margin-bottom: 0;
  }

  .ctrl-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
  }
  .ctrl-label {
    font-size: 0.82rem;
    color: #909399;
  }

  .ctrl-badge {
    font-size: 11px;
    font-weight: 600;
    padding: 3px 8px;
    border-radius: 999px;
  }
  .badge-on {
    background: rgba(103, 194, 58, 0.2);
    color: #67c23a;
    border: 1px solid rgba(103, 194, 58, 0.4);
  }
  .badge-off {
    background: rgba(144, 147, 153, 0.2);
    color: #909399;
    border: 1px solid rgba(144, 147, 153, 0.3);
  }
  .badge-locked {
    background: rgba(245, 108, 108, 0.2);
    color: #f56c6c;
    border: 1px solid rgba(245, 108, 108, 0.4);
  }
  .badge-unlocked {
    background: rgba(64, 158, 255, 0.15);
    color: #409eff;
    border: 1px solid rgba(64, 158, 255, 0.35);
  }

  .ctrl-lock-by {
    font-size: 10px;
    color: #606266;
  }

  .control-btn {
    padding: 8px 14px;
    border-radius: 7px;
    border: none;
    font-size: 0.8rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
  }
  .control-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }

  .btn-enable {
    background: rgba(103, 194, 58, 0.2);
    color: #67c23a;
    border: 1px solid rgba(103, 194, 58, 0.4);
  }
  .btn-enable:hover:not(:disabled) {
    background: rgba(103, 194, 58, 0.35);
  }

  .btn-disable {
    background: rgba(144, 147, 153, 0.2);
    color: #c0c4cc;
    border: 1px solid rgba(144, 147, 153, 0.3);
  }
  .btn-disable:hover:not(:disabled) {
    background: rgba(144, 147, 153, 0.35);
  }

  .btn-lock {
    background: rgba(245, 108, 108, 0.15);
    color: #f56c6c;
    border: 1px solid rgba(245, 108, 108, 0.35);
  }
  .btn-lock:hover:not(:disabled) {
    background: rgba(245, 108, 108, 0.3);
  }

  .btn-unlock {
    background: rgba(64, 158, 255, 0.15);
    color: #409eff;
    border: 1px solid rgba(64, 158, 255, 0.35);
  }
  .btn-unlock:hover:not(:disabled) {
    background: rgba(64, 158, 255, 0.3);
  }

  .btn-danger {
    width: 100%;
    background: rgba(245, 108, 108, 0.2);
    color: #f56c6c;
    border: 1px solid rgba(245, 108, 108, 0.4);
    padding: 9px;
  }
  .btn-danger:hover:not(:disabled) {
    background: rgba(245, 108, 108, 0.35);
  }

  .btn-warning {
    width: 100%;
    background: rgba(230, 162, 60, 0.2);
    color: #e6a23c;
    border: 1px solid rgba(230, 162, 60, 0.4);
    padding: 9px;
  }
  .btn-warning:hover:not(:disabled) {
    background: rgba(230, 162, 60, 0.35);
  }

  /* ==================== 작업 제어 버튼 ==================== */
  .job-control-buttons {
    display: flex;
    gap: 6px;
    margin-top: 8px;
  }

  .btn-resume {
    flex: 1;
    background: rgba(64, 158, 255, 0.15);
    color: #409eff;
    border: 1px solid rgba(64, 158, 255, 0.35);
  }
  .btn-resume:hover:not(:disabled) {
    background: rgba(64, 158, 255, 0.3);
  }

  .btn-force-complete {
    flex: 1;
    background: rgba(103, 194, 58, 0.15);
    color: #67c23a;
    border: 1px solid rgba(103, 194, 58, 0.35);
  }
  .btn-force-complete:hover:not(:disabled) {
    background: rgba(103, 194, 58, 0.3);
  }

  .btn-cancel {
    flex: 1;
    background: rgba(245, 108, 108, 0.15);
    color: #f56c6c;
    border: 1px solid rgba(245, 108, 108, 0.35);
  }
  .btn-cancel:hover:not(:disabled) {
    background: rgba(245, 108, 108, 0.3);
  }

  /* ==================== 인라인 토스트 ==================== */
  .inline-toast {
    margin-top: 10px;
    padding: 10px 12px;
    border-radius: 8px;
    font-size: 0.82rem;
    line-height: 1.4;
    animation: fadeIn 0.2s ease;
  }
  .inline-toast.success {
    background: rgba(103, 194, 58, 0.15);
    color: #67c23a;
    border: 1px solid rgba(103, 194, 58, 0.35);
  }
  .inline-toast.error {
    background: rgba(245, 108, 108, 0.15);
    color: #f56c6c;
    border: 1px solid rgba(245, 108, 108, 0.35);
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(4px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  /* ==================== 슬라이드 애니메이션 ==================== */
  .slide-fade-enter-active {
    transition: all 0.3s ease-out;
  }
  .slide-fade-leave-active {
    transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1);
  }
  .slide-fade-enter-from,
  .slide-fade-leave-to {
    transform: translateX(20px);
    opacity: 0;
  }

  /* ==================== 재고 상세 카드 ==================== */
  .inv-count-badge {
    margin-left: 6px;
    padding: 2px 8px;
    font-size: 10px;
    background: rgba(64, 158, 255, 0.2);
    color: #409eff;
    border-radius: 999px;
    font-weight: 600;
  }

  .pallet-group-hint {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 12px;
    margin-bottom: 8px;
    background: rgba(64, 158, 255, 0.08);
    border-left: 3px solid #409eff;
    border-radius: 4px;
    font-size: 11px;
    color: #409eff;
    font-weight: 500;
  }
  .pallet-icon {
    font-size: 14px;
  }

  .inventory-card {
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 10px;
    padding: 12px;
    margin-bottom: 8px;
    transition: all 0.2s;
  }
  .inventory-card:last-child {
    margin-bottom: 0;
  }

  .inventory-card.inv-expired {
    background: rgba(245, 108, 108, 0.08);
    border-color: rgba(245, 108, 108, 0.3);
  }
  .inventory-card.inv-hold {
    background: rgba(230, 162, 60, 0.08);
    border-color: rgba(230, 162, 60, 0.3);
  }

  /* HOST_PENDING — host_order 등록되었으나 셔틀 미생성 */
  .inventory-card.inv-reserved {
    background: rgba(64, 158, 255, 0.1);
    border-color: rgba(64, 158, 255, 0.45);
  }

  .reservation-banner {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 12px;
    margin-bottom: 10px;
    background: rgba(64, 158, 255, 0.16);
    border: 1px solid rgba(64, 158, 255, 0.4);
    border-radius: 8px;
    color: #cfe7ff;
    font-size: 11px;
    font-weight: 600;
  }

  /* 인벤토리 카드 최상단 핵심 식별 배지 — 카테고리/상태 즉시 인지 */
  .inv-key-badges {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 10px;
  }
  .inv-key-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 5px 12px;
    border-radius: 14px;
    font-size: 12px;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }
  .inv-key-badge-label {
    font-size: 10px;
    color: #909399;
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .inv-key-badge-value {
    font-size: 13px;
    font-weight: 700;
    color: #f2f5fb;
    letter-spacing: 0.2px;
  }
  .inv-key-type {
    background: rgba(64, 158, 255, 0.12);
    border-color: rgba(64, 158, 255, 0.4);
  }
  .inv-key-type .inv-key-badge-value {
    color: #79b8ff;
  }
  .inv-key-badge.stock-status-normal {
    background: rgba(103, 194, 58, 0.14);
    border-color: rgba(103, 194, 58, 0.4);
  }
  .inv-key-badge.stock-status-normal .inv-key-badge-value {
    color: #67c23a;
  }
  .inv-key-badge.stock-status-hold {
    background: rgba(230, 162, 60, 0.14);
    border-color: rgba(230, 162, 60, 0.4);
  }
  .inv-key-badge.stock-status-hold .inv-key-badge-value {
    color: #e6a23c;
  }
  .inv-key-badge.stock-status-reserved {
    background: rgba(64, 158, 255, 0.16);
    border-color: rgba(64, 158, 255, 0.5);
  }
  .inv-key-badge.stock-status-reserved .inv-key-badge-value {
    color: #409eff;
  }

  .inv-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 10px;
    padding-bottom: 8px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  .inv-sku-block {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    flex: 1;
  }
  .inv-sku {
    font-size: 13px;
    font-weight: 600;
    color: #f2f5fb;
    font-family: 'Roboto Mono', Consolas, monospace;
  }
  .inv-item-code {
    font-size: 11px;
    color: #909399;
    font-family: 'Roboto Mono', Consolas, monospace;
  }

  .inv-qty-block {
    display: flex;
    align-items: baseline;
    gap: 3px;
    flex-shrink: 0;
    margin-left: 12px;
  }
  .inv-qty {
    font-size: 18px;
    font-weight: 700;
    color: #67c23a;
    font-family: 'Roboto Mono', Consolas, monospace;
  }
  .inv-qty-unit {
    font-size: 11px;
    color: #909399;
  }

  .inv-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 6px 12px;
  }

  .inv-cell {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }
  .inv-cell-wide {
    grid-column: 1 / -1;
  }

  .inv-cell-label {
    font-size: 10px;
    color: #606266;
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .inv-cell-value {
    font-size: 12px;
    color: #d9e1ee;
    word-break: break-all;
  }
  .inv-cell-value.inv-monospace,
  .inv-cell-value.inv-barcode {
    font-family: 'Roboto Mono', Consolas, monospace;
    font-size: 11px;
    color: #409eff;
  }

  .inv-cell-value.stock-status-normal {
    color: #67c23a;
  }
  .inv-cell-value.stock-status-hold {
    color: #e6a23c;
  }
  .inv-cell-value.stock-status-ready-out {
    color: #409eff;
  }
  .inv-cell-value.stock-status-reserved {
    color: #409eff;
    font-weight: 600;
  }

  .inv-cell-value.inv-expired-text {
    color: #f56c6c;
    font-weight: 600;
  }
  .expired-badge {
    margin-left: 4px;
    padding: 1px 5px;
    font-size: 9px;
    background: rgba(245, 108, 108, 0.2);
    color: #f56c6c;
    border-radius: 3px;
    vertical-align: middle;
  }

  .inv-empty-section {
    padding: 16px 0;
  }
  .inv-empty-msg {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 24px;
    background: rgba(255, 255, 255, 0.02);
    border: 1px dashed rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    color: #606266;
    font-size: 12px;
  }
  .inv-empty-icon {
    font-size: 24px;
    opacity: 0.5;
  }

  /* ==================== 포트 모드 ==================== */
  .badge-switching {
    background: rgba(230, 162, 60, 0.2);
    color: #e6a23c;
    border: 1px solid rgba(230, 162, 60, 0.5);
    animation: switching-pulse 2s ease-in-out infinite;
  }

  @keyframes switching-pulse {
    0%,
    100% {
      opacity: 1;
    }
    50% {
      opacity: 0.6;
    }
  }

  .port-mode-drain-panel {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    margin-bottom: 12px;
    background: rgba(230, 162, 60, 0.1);
    border: 1px solid rgba(230, 162, 60, 0.4);
    border-radius: 8px;
  }

  .drain-icon {
    font-size: 28px;
    flex-shrink: 0;
  }

  .drain-text {
    flex: 1;
    min-width: 0;
  }

  .drain-title {
    font-size: 13px;
    font-weight: 700;
    color: #e6a23c;
    margin-bottom: 4px;
  }

  .drain-desc {
    font-size: 11px;
    color: #c0c4cc;
    line-height: 1.4;
  }

  .drain-current-job {
    margin-top: 4px;
    font-size: 11px;
    color: #909399;
  }
  .drain-current-job code {
    font-family: 'Roboto Mono', monospace;
    background: rgba(255, 255, 255, 0.06);
    padding: 1px 6px;
    border-radius: 3px;
    color: #409eff;
  }

  .drain-cancel-btn {
    flex-shrink: 0;
    padding: 6px 12px;
    background: rgba(245, 108, 108, 0.15);
    color: #f56c6c;
    border: 1px solid rgba(245, 108, 108, 0.4);
    border-radius: 6px;
    font-size: 11px;
    font-weight: 600;
    cursor: pointer;
  }
  .drain-cancel-btn:hover:not(:disabled) {
    background: rgba(245, 108, 108, 0.3);
  }
  .drain-cancel-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .port-mode-btn.pending-target {
    background: rgba(230, 162, 60, 0.18);
    border-color: rgba(230, 162, 60, 0.55);
    position: relative;
  }
  .port-mode-btn.pending-target::after {
    content: '⏳';
    position: absolute;
    top: 4px;
    right: 6px;
    font-size: 10px;
  }

  .port-mode-top {
    background: linear-gradient(135deg, rgba(64, 158, 255, 0.08), rgba(103, 194, 58, 0.04));
    border: 1px solid rgba(64, 158, 255, 0.25);
    border-radius: 10px;
    padding: 12px;
    margin-bottom: 12px;
  }
  .port-mode-top--fixed {
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(255, 255, 255, 0.08);
  }
  .port-mode-badge {
    margin-left: 0;
    padding: 3px 10px;
    font-size: 11px;
    font-weight: 700;
    border-radius: 999px;
    text-transform: none;
    letter-spacing: 0;
    line-height: 1.4;
    display: inline-flex;
    align-items: center;
  }
  .badge-inbound {
    background: rgba(103, 194, 58, 0.2);
    color: #67c23a;
    border: 1px solid rgba(103, 194, 58, 0.4);
  }
  .badge-outbound {
    background: rgba(230, 162, 60, 0.2);
    color: #e6a23c;
    border: 1px solid rgba(230, 162, 60, 0.4);
  }
  .badge-idle {
    background: rgba(144, 147, 153, 0.2);
    color: #c0c4cc;
    border: 1px solid rgba(144, 147, 153, 0.3);
  }
  .badge-unknown {
    background: rgba(64, 158, 255, 0.12);
    color: #409eff;
    border: 1px solid rgba(64, 158, 255, 0.3);
  }
  .port-fixed-tag {
    margin-left: 0;
    padding: 2px 8px;
    font-size: 10px;
    background: rgba(255, 255, 255, 0.05);
    color: #909399;
    border-radius: 4px;
    border: 1px solid rgba(255, 255, 255, 0.08);
    text-transform: none;
    letter-spacing: 0;
    line-height: 1.4;
  }
  .port-mode-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    margin-top: 10px;
  }
  .port-mode-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 12px 6px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    color: #d9e1ee;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }
  .port-mode-btn:hover:not(:disabled) {
    background: rgba(64, 158, 255, 0.15);
    border-color: rgba(64, 158, 255, 0.45);
    transform: translateY(-1px);
  }
  .port-mode-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    transform: none;
  }
  .port-mode-btn.active {
    background: rgba(64, 158, 255, 0.22);
    border-color: rgba(64, 158, 255, 0.6);
    color: #fff;
    box-shadow: 0 0 12px rgba(64, 158, 255, 0.25);
  }
  .port-mode-btn.active .port-mode-icon {
    filter: drop-shadow(0 0 4px rgba(64, 158, 255, 0.6));
  }
  .port-mode-icon {
    font-size: 20px;
    line-height: 1;
  }
  .port-mode-text {
    font-size: 11px;
  }

  .port-mode-warn {
    margin-top: 8px;
    padding: 6px 10px;
    background: rgba(230, 162, 60, 0.12);
    border: 1px solid rgba(230, 162, 60, 0.35);
    border-radius: 6px;
    color: #e6a23c;
    font-size: 11px;
  }

  /* ────────────────────────────────────────────────────────────
 * 셀 액션 버튼 — 텍스트 라벨, 색상 의도 명확
 * 컨테이너는 .act-row (재고 카드 헤더 직후) 참조.
 * ──────────────────────────────────────────────────────────── */
  .act-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    min-height: 38px;
    padding: 8px 12px;
    border: 1px solid transparent;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: transform 0.05s ease, box-shadow 0.15s ease, background 0.15s ease;
    background: rgba(255, 255, 255, 0.06);
    color: #cbd5e1;
  }
  .act-btn:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
  }
  .act-btn:active:not(:disabled) {
    transform: translateY(0);
  }
  .act-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
  .act-icon {
    font-size: 15px;
    line-height: 1;
  }
  .act-label {
    letter-spacing: -0.2px;
  }

  .act-primary {
    background: linear-gradient(135deg, #3182f6 0%, #1e6fe6 100%);
    color: #fff;
    border-color: #3182f6;
  }
  .act-success {
    background: linear-gradient(135deg, #16a34a 0%, #15803d 100%);
    color: #fff;
    border-color: #16a34a;
  }
  .act-warn {
    background: rgba(245, 158, 11, 0.18);
    color: #fbbf24;
    border-color: rgba(245, 158, 11, 0.45);
  }
  .act-danger {
    background: rgba(239, 68, 68, 0.18);
    color: #fca5a5;
    border-color: rgba(239, 68, 68, 0.45);
  }
  .act-danger:hover:not(:disabled) {
    background: rgba(239, 68, 68, 0.28);
    color: #fff;
  }

  /* ────────────────────────────────────────────────────────────
 * 카드 헤더 직후 액션 행 (재고 카드 액션 — 컴팩트 텍스트 라벨)
 * 메타 그리드 위에 위치, 이모지 없이 텍스트만.
 * ──────────────────────────────────────────────────────────── */
  .act-row {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin: 8px 0 10px 0;
    padding-bottom: 10px;
    border-bottom: 1px dashed rgba(255, 255, 255, 0.08);
  }
  .act-row .act-btn {
    min-height: 28px;
    padding: 4px 12px;
    font-size: 12px;
    gap: 4px;
    border-radius: 6px;
  }
  .act-row .act-btn:hover:not(:disabled) {
    transform: none;
    box-shadow: none;
    filter: brightness(1.12);
  }

  /* ────────────────────────────────────────────────────────────
 * 압축 메타 칩 (popup-content 최상단)
 * 코드·위치·타입·상태 한 줄 + 잠금/금지 텍스트 배지
 * ──────────────────────────────────────────────────────────── */
  .meta-chip-bar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    padding: 8px 10px;
    margin-bottom: 12px;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 8px;
  }
  .meta-chip {
    font-size: 12px;
    line-height: 1.4;
    padding: 2px 8px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.06);
    color: #cfd8e6;
    white-space: nowrap;
  }
  .meta-chip.code {
    background: rgba(64, 158, 255, 0.16);
    color: #67c23a;
    font-weight: 600;
  }
  .meta-chip.loc {
    font-family: 'Roboto Mono', 'Consolas', monospace;
    background: rgba(64, 158, 255, 0.12);
    color: #409eff;
    letter-spacing: 0.4px;
  }
  .meta-chip.type {
    background: rgba(255, 255, 255, 0.05);
    color: #9aa7bb;
  }
  .meta-chip.status {
    background: rgba(255, 255, 255, 0.05);
    color: #cfd8e6;
  }
  .meta-chip-badge {
    font-size: 10px;
    line-height: 1.4;
    padding: 2px 8px;
    border-radius: 999px;
    font-weight: 600;
    letter-spacing: 0.2px;
    white-space: nowrap;
  }
  .meta-chip-badge.danger {
    background: rgba(245, 108, 108, 0.12);
    color: #f56c6c;
    border: 1px solid rgba(245, 108, 108, 0.45);
  }

  /* ────────────────────────────────────────────────────────────
 * Sticky 위급 영역 — 에러 + 정합성 경고
 * popup-content 스크롤 시 상단 고정.
 * ──────────────────────────────────────────────────────────── */
  .alert-sticky-zone {
    position: sticky;
    top: 0;
    z-index: 3;
    background: rgba(30, 34, 45, 0.96);
    backdrop-filter: blur(6px);
    margin: 0 -16px 12px -16px;
    padding: 10px 16px 4px 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    max-height: 40vh;
    overflow-y: auto;
  }
  .alert-sticky-zone .info-section {
    margin-bottom: 8px;
  }
  .alert-sticky-zone .info-section:last-child {
    margin-bottom: 0;
  }

  /* ────────────────────────────────────────────────────────────
 * 설비 상세 접힘 토글 — 보조 메타정보(설비/랙/실시간 상태) 펼치기
 * ──────────────────────────────────────────────────────────── */
  .details-toggle-wrap {
    margin: 14px 0 10px 0;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
    padding-top: 10px;
  }
  .details-toggle {
    background: none;
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: #9aa7bb;
    font-size: 12px;
    padding: 6px 12px;
    border-radius: 6px;
    cursor: pointer;
    width: 100%;
    text-align: left;
    transition: all 0.15s ease;
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }
  .details-toggle:hover {
    background: rgba(255, 255, 255, 0.04);
    color: #cfd8e6;
    border-color: rgba(255, 255, 255, 0.16);
  }
  .toggle-arrow {
    display: inline-block;
    font-size: 10px;
    transition: transform 0.2s ease;
  }
  .toggle-arrow.open {
    transform: rotate(180deg);
  }
  .details-zone {
    margin-bottom: 10px;
  }

  /* ────────────────────────────────────────────────────────────
 * 수동 출고 모달
 * ──────────────────────────────────────────────────────────── */
  .mo-mask {
    position: fixed;
    inset: 0;
    z-index: 2000;
    background: rgba(0, 0, 0, 0.55);
    backdrop-filter: blur(2px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
  }
  .mo-dialog {
    width: 100%;
    max-width: 420px;
    background: #1f2937;
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 14px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5);
    color: #e2e8f0;
    overflow: hidden;
  }
  .mo-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 18px;
    background: linear-gradient(135deg, #3182f6 0%, #1e6fe6 100%);
    color: #fff;
  }
  .mo-title {
    font-size: 15px;
    font-weight: 700;
  }
  .mo-close {
    background: transparent;
    border: 0;
    color: #fff;
    font-size: 22px;
    line-height: 1;
    cursor: pointer;
    padding: 0 4px;
  }
  .mo-body {
    padding: 16px 18px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .mo-info-row {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
  }
  .mo-label {
    color: #94a3b8;
  }
  .mo-value {
    color: #e2e8f0;
  }
  .mo-hint {
    font-size: 11px;
    color: #94a3b8;
    font-weight: 400;
    margin-left: 4px;
  }
  .mo-notice {
    padding: 10px 12px;
    background: rgba(49, 130, 246, 0.12);
    border: 1px solid rgba(49, 130, 246, 0.35);
    border-radius: 6px;
    color: #93c5fd;
    font-size: 12px;
    line-height: 1.5;
  }
  .mo-notice b {
    color: #dbeafe;
  }
  .mo-input-section {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .mo-input-label {
    font-size: 12px;
    color: #94a3b8;
    font-weight: 600;
  }
  .mo-text-input {
    background: rgba(0, 0, 0, 0.25);
    border: 1px solid rgba(255, 255, 255, 0.12);
    color: #fff;
    font-size: 13px;
    padding: 8px 10px;
    border-radius: 8px;
  }
  .mo-footer {
    display: flex;
    gap: 8px;
    padding: 12px 18px;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
  }
  .mo-btn {
    flex: 1;
    min-height: 42px;
    border: 0;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
  }
  .mo-btn-cancel {
    background: rgba(255, 255, 255, 0.06);
    color: #cbd5e1;
  }
  .mo-btn-confirm {
    background: linear-gradient(135deg, #3182f6 0%, #1e6fe6 100%);
    color: #fff;
  }
  .mo-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .mono {
    font-family: 'Courier New', monospace;
  }

  .mo-modal-enter-from,
  .mo-modal-leave-to {
    opacity: 0;
  }
  .mo-modal-enter-active,
  .mo-modal-leave-active {
    transition: opacity 0.18s ease;
  }

  /* 포트 락 holder + 강제 해제 패널 */
  .port-lock-panel {
    margin: 8px 0 10px;
    padding: 10px 12px;
    border: 1px solid rgba(245, 108, 108, 0.55);
    border-radius: 8px;
    background: rgba(245, 108, 108, 0.1);
    color: #f5dada;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .port-lock-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 700;
    color: #f56c6c;
  }
  .port-lock-row {
    display: flex;
    gap: 8px;
    font-size: 12px;
  }
  .port-lock-label {
    min-width: 64px;
    color: #c0c4cc;
  }
  .port-lock-value.mono {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  }
  .port-lock-sentinel {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    color: #f5dada;
  }
  .port-lock-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 4px;
  }
</style>
