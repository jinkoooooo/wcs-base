<template>
  <div class="generator-overlay" @click.self="$emit('close')">
    <div class="generator-panel">
      <div class="panel-header">
        <div>
          <h3>설비 생성</h3>
          <p class="panel-subtitle">
            설비 그룹을 만들고, 그 안에 기본 설비를 등록한 뒤, 기본 설비 타입에 맞는 하위 설비만
            생성합니다.
          </p>
        </div>
        <button class="close-btn" @click="$emit('close')" aria-label="닫기">✕</button>
      </div>

      <div class="step-summary">
        <div class="step-chip" :class="{ active: activeTab === 'group' }">
          <span class="step-no">1</span>
          <div>
            <strong>설비 그룹</strong>
            <p>그룹 생성 / 조회 / 삭제</p>
          </div>
        </div>
        <div class="step-chip" :class="{ active: activeTab === 'equipment' }">
          <span class="step-no">2</span>
          <div>
            <strong>기본 설비</strong>
            <p>그룹 하위 기본 설비 등록</p>
          </div>
        </div>
        <div class="step-chip" :class="{ active: activeTab === 'children' }">
          <span class="step-no">3</span>
          <div>
            <strong>하위 설비 생성</strong>
            <p>기본 설비 타입별 생성</p>
          </div>
        </div>
      </div>

      <div class="selection-summary">
        <div class="summary-chip">
          <span class="summary-label">선택된 그룹</span>
          <strong>{{
            selectedEqGroup ? `${selectedEqGroup.id} · ${selectedEqGroup.name}` : '없음'
          }}</strong>
        </div>
        <div class="summary-chip">
          <span class="summary-label">선택된 기본 설비</span>
          <strong>{{
            selectedEqMst ? `${selectedEqMst.id} · ${selectedEqMst.name}` : '없음'
          }}</strong>
        </div>
        <div class="summary-chip">
          <span class="summary-label">기본 설비 타입</span>
          <strong>{{ selectedEqMst ? getEqTypeLabel(selectedEqMst.type) : '없음' }}</strong>
        </div>
      </div>

      <div class="tab-nav three-tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'group' }"
          @click="activeTab = 'group'"
        >
          <span class="tab-title">설비 그룹</span>
          <span class="tab-desc">작업 기준 만들기</span>
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'equipment' }"
          @click="activeTab = 'equipment'"
        >
          <span class="tab-title">기본 설비</span>
          <span class="tab-desc">eq_id 기준 설비 등록</span>
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'children' }"
          @click="activeTab = 'children'"
        >
          <span class="tab-title">하위 설비 생성</span>
          <span class="tab-desc">타입에 맞는 하위 설비만 표시</span>
        </button>
      </div>

      <div class="panel-body">
        <!-- 1. 설비 그룹 -->
        <div v-if="activeTab === 'group'" class="tab-content">
          <div class="content-grid">
            <section class="card" :class="{ 'card-edit-mode': eqGroupFormMode === 'edit' }">
              <div class="card-head">
                <div>
                  <h4>
                    {{ eqGroupFormMode === 'edit' ? '설비 그룹 수정' : '설비 그룹 생성' }}
                  </h4>
                  <p v-if="eqGroupFormMode === 'edit'">
                    ID <strong>{{ editingEqGroupId }}</strong> 의 정보를 수정합니다.
                  </p>
                  <p v-else>가장 먼저 작업 기준이 되는 설비 그룹을 등록합니다.</p>
                </div>
              </div>

              <div class="form-stack">
                <div class="form-row">
                  <label>그룹 ID</label>
                  <div v-if="eqGroupFormMode === 'edit'" class="field-readonly">
                    {{ editingEqGroupId }}
                  </div>
                  <input v-else v-model="newEqGroup.id" type="text" placeholder="예: tspg_1"/>
                </div>

                <div class="form-row">
                  <label>그룹명</label>
                  <input v-model="newEqGroup.name" type="text" placeholder="예: 상온창고" @keydown.stop />
                </div>

                <div class="form-row">
                  <label>그룹 타입</label>
                  <select v-model="newEqGroup.type">
                    <option v-for="opt in eqGroupTypeOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </option>
                  </select>
                </div>
              </div>

              <div class="action-footer edit-footer">
                <template v-if="eqGroupFormMode === 'edit'">
                  <button class="btn btn-secondary" @click="cancelEditEqGroup">취소</button>
                  <button
                    class="btn btn-primary"
                    :disabled="!newEqGroup.name.trim()"
                    @click="saveEqGroup"
                  >
                    저장
                  </button>
                </template>
                <button
                  v-else
                  class="btn btn-primary"
                  :disabled="!canCreateEqGroup"
                  @click="createEqGroup"
                >
                  설비 그룹 생성
                </button>
              </div>
            </section>

            <section class="card">
              <div class="card-head">
                <div>
                  <h4>기존 설비 그룹</h4>
                  <p>그룹을 선택하면 다음 단계에서 기본 설비를 바로 등록할 수 있습니다.</p>
                </div>
                <button class="btn btn-secondary btn-sm" @click="loadEqGroups">새로고침</button>
              </div>

              <div v-if="isLoading" class="state-box">로딩 중...</div>
              <div v-else-if="eqGroups.length === 0" class="state-box empty">
                등록된 설비 그룹이 없습니다.
              </div>

              <ul v-else class="list">
                <li
                  v-for="group in eqGroups"
                  :key="group.id"
                  class="list-item list-item-column"
                  :class="{ selected: selectedEqGroupId === group.id }"
                >
                  <div class="list-top" @click="selectEqGroup(group)">
                    <div class="item-main">
                      <span class="item-id">{{ group.id }}</span>
                      <span class="item-name">{{ group.name }}</span>
                    </div>
                    <span class="item-type">타입 {{ group.type }}</span>
                  </div>

                  <div class="item-actions">
                    <button class="tag-btn tag-btn-edit" @click="selectEqGroup(group)">선택 및 수정</button>
                    <button class="tag-btn" @click="goToEquipmentWithGroup(group.id)">
                      기본 설비로 이동
                    </button>
                    <button class="tag-btn danger" @click="deleteEqGroup(group.id)">삭제</button>
                  </div>
                </li>
              </ul>
            </section>
          </div>
        </div>

        <!-- 2. 기본 설비 -->
        <div v-if="activeTab === 'equipment'" class="tab-content">
          <div class="context-card">
            <div class="context-grid single">
              <div>
                <strong>안내</strong>
                <p>여기서 만든 기본 설비의 ID가 나중에 하위 설비의 <b>eq_id</b> 기준이 됩니다.</p>
              </div>
            </div>
          </div>

          <div class="content-grid">
            <section class="card" :class="{ 'card-edit-mode': eqMstFormMode === 'edit' }">
              <div class="card-head">
                <div>
                  <h4>
                    {{ eqMstFormMode === 'edit' ? '기본 설비 수정' : '기본 설비 생성' }}
                  </h4>
                  <p v-if="eqMstFormMode === 'edit'">
                    ID <strong>{{ editingEqMstId }}</strong> 의 정보를 수정합니다.
                  </p>
                  <p v-else>보관설비 / 이송설비 / 셔틀 카 중 하나를 선택해 등록합니다.</p>
                </div>
                <button v-if="eqMstFormMode === 'edit'" class="btn btn-secondary btn-sm" @click="cancelEditEqMst">
                  신규 생성으로 전환
                </button>
              </div>

              <div class="form-stack">
                <div class="form-row">
                  <label>설비 그룹</label>
                  <div v-if="eqMstFormMode === 'edit'" class="field-readonly">
                    {{ newEqMst.eqGroupId }}
                  </div>
                  <select v-else v-model="newEqMst.eqGroupId">
                    <option value="">선택하세요</option>
                    <option v-for="g in eqGroups" :key="g.id" :value="g.id">
                      {{ g.id }} ({{ g.name }})
                    </option>
                  </select>
                </div>

                <div class="form-row">
                  <label>설비 ID</label>
                  <div v-if="eqMstFormMode === 'edit'" class="field-readonly">
                    {{ editingEqMstId }}
                  </div>
                  <input v-else v-model="newEqMst.id" type="text" placeholder="예: rack_02" />
                </div>

                <div class="form-row">
                  <label>설비명</label>
                  <input v-model="newEqMst.name" type="text" placeholder="예: 1번 보관 랙" @keydown.stop />
                </div>

                <div class="form-row">
                  <label>설비 타입</label>
                  <div v-if="eqMstFormMode === 'edit'" class="field-readonly">
                    {{ getEqTypeLabel(newEqMst.type) }}
                  </div>
                  <select v-else v-model="newEqMst.type">
                    <option v-for="opt in eqTypeOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </option>
                  </select>
                </div>

                <template v-if="showPlcSection || (eqMstFormMode === 'edit' && editingEqMstHasPlc)">
                  <div class="section-divider"></div>

                  <div class="subsection-head">
                    <h5>PLC 설정</h5>
                    <p>이송설비 / 셔틀 카에서만 사용합니다. 비워두고 생성해도 됩니다.</p>
                  </div>

                  <div class="form-row">
                    <label>PLC ID</label>
                    <input
                      v-model="newEqMst.plcId"
                      type="text"
                      placeholder="비워도 가능 / 기본값은 설비 ID"
                      @keydown.stop
                    />
                  </div>

                  <div class="form-row">
                    <label>PLC 명</label>
                    <input v-model="newEqMst.plcName" type="text" placeholder="비워도 가능" @keydown.stop />
                  </div>

                  <div class="form-grid">
                    <div class="form-row">
                      <label>PLC IP</label>
                      <input v-model="newEqMst.plcIp" type="text" placeholder="비워도 가능" @keydown.stop />
                    </div>
                    <div class="form-row">
                      <label>PLC Port</label>
                      <input
                        v-model.number="newEqMst.plcPort"
                        type="number"
                        min="0"
                        placeholder="비워도 가능"
                      />
                    </div>
                  </div>

                  <div class="form-grid">
                    <div class="form-row">
                      <label>IF 타입</label>
                      <input
                        v-model="newEqMst.plcIfType"
                        type="text"
                        placeholder="예: MELSEC / 비워도 가능"
                        @keydown.stop
                      />
                    </div>
                    <div class="form-row">
                      <label>PLC EQ 타입</label>
                      <input
                        v-model.number="newEqMst.plcEqType"
                        type="number"
                        min="0"
                        placeholder="비워도 가능"
                      />
                    </div>
                  </div>

                  <div class="form-grid">
                    <div class="form-row">
                      <label>연결 여부</label>
                      <select v-model="newEqMst.connectYn">
                        <option :value="false">false</option>
                        <option :value="true">true</option>
                      </select>
                    </div>
                    <div class="form-row">
                      <label>사용 여부</label>
                      <select v-model="newEqMst.useYn">
                        <option :value="true">true</option>
                        <option :value="false">false</option>
                      </select>
                    </div>
                  </div>

                  <div class="readonly-info">
                    <div class="readonly-title">자동 기본값</div>
                    <div class="readonly-value">
                      PLC ID는 기본으로 현재 입력한 설비 ID와 동일하게 맞춰집니다. 필요하면 직접
                      수정할 수 있습니다.
                    </div>
                  </div>
                </template>
              </div>

              <div class="action-footer edit-footer">
                <template v-if="eqMstFormMode === 'edit'">
                  <button class="btn btn-secondary" @click="cancelEditEqMst">취소</button>
                  <button
                    class="btn btn-primary"
                    :disabled="!newEqMst.name.trim()"
                    @click="saveEqMst"
                  >
                    저장
                  </button>
                </template>
                <button v-else class="btn btn-primary" :disabled="!canCreateEqMst" @click="createEqMst">
                  기본 설비 생성
                </button>
              </div>
            </section>

            <section class="card">
              <div class="card-head">
                <div>
                  <h4>기본 설비 목록</h4>
                  <p>선택한 기본 설비의 타입에 따라 다음 단계에서 보이는 화면이 달라집니다.</p>
                </div>
                <button class="btn btn-primary btn-sm" @click="cancelEditEqMst">
                  + 신규 등록
                </button>
              </div>

              <div class="form-row">
                <label>설비 그룹</label>
                <select v-model="filterEqGroupId" @change="loadEqMstList">
                  <option value="">선택하세요</option>
                  <option v-for="g in eqGroups" :key="g.id" :value="g.id">
                    {{ g.id }} ({{ g.name }})
                  </option>
                </select>
              </div>

              <div v-if="isLoading" class="state-box">로딩 중...</div>
              <div v-else-if="!filterEqGroupId" class="state-box empty">
                먼저 설비 그룹을 선택하세요.
              </div>
              <div v-else-if="eqMstList.length === 0" class="state-box empty">
                등록된 기본 설비가 없습니다.
              </div>

              <ul v-else class="list">
                <li
                  v-for="eq in eqMstList"
                  :key="eq.id"
                  class="list-item list-item-column"
                  :class="{ selected: selectedEqMstId === eq.id }"
                >
                  <div class="list-top" @click="selectEqMst(eq)">
                    <div class="item-main">
                      <span class="item-id">{{ eq.id }}</span>
                      <span class="item-name">{{ eq.name }}</span>
                    </div>
                    <span class="item-type">{{ getEqTypeLabel(eq.type) }}</span>
                  </div>

                  <div class="sub-info">
                    <span>PLC ID: {{ eq.plcId || '-' }}</span>
                  </div>

                  <div class="item-actions">
                    <button class="tag-btn tag-btn-edit" @click="selectEqMst(eq)">선택 및 수정</button>
                    <button class="tag-btn" @click="goToChildrenWithEq(eq)">
                      하위 설비 생성으로 이동
                    </button>
                    <button class="tag-btn danger" @click="deleteEqMst(eq)">삭제</button>
                  </div>
                </li>
              </ul>
            </section>
          </div>
        </div>

        <!-- 3. 하위 설비 생성 -->
        <div v-if="activeTab === 'children'" class="tab-content">
          <div class="context-card">
            <div class="context-grid children-top">
              <div class="form-row compact">
                <label>설비 그룹</label>
                <select v-model="childrenEqGroupId" @change="onChildrenEqGroupChange">
                  <option value="">선택하세요</option>
                  <option v-for="g in eqGroups" :key="g.id" :value="g.id">
                    {{ g.id }} ({{ g.name }})
                  </option>
                </select>
              </div>

              <div class="form-row compact">
                <label>기본 설비</label>
                <select
                  v-model="selectedEqMstId"
                  :disabled="!childrenEqGroupId"
                  @change="onSelectedEqMstChange"
                >
                  <option value="">선택하세요</option>
                  <option v-for="eq in childrenEqMstList" :key="eq.id" :value="eq.id">
                    {{ eq.id }} ({{ eq.name }}) / {{ getEqTypeLabel(eq.type) }}
                  </option>
                </select>
              </div>

              <div class="readonly-panel">
                <div class="readonly-row">
                  <span class="readonly-label">현재 기준 eq_id</span>
                  <strong class="readonly-strong">{{
                    selectedEqMst ? selectedEqMst.id : '-'
                  }}</strong>
                </div>
                <div class="readonly-row">
                  <span class="readonly-label">현재 기본 설비 타입</span>
                  <strong class="readonly-strong">
                    {{ selectedEqMst ? getEqTypeLabel(selectedEqMst.type) : '-' }}
                  </strong>
                </div>
              </div>
            </div>
          </div>

          <div v-if="!selectedEqMst" class="state-box empty large-empty">
            먼저 설비 그룹과 기본 설비를 선택하세요.
          </div>

          <template v-else>
            <!-- 보관설비 -> 랙 셀 -->
            <div v-if="selectedEqMst.type === EqType.RACK" class="single-child-layout">
              <section class="card child-card">
                <div class="child-title-wrap">
                  <div>
                    <h4>랙 셀 생성</h4>
                    <p>보관설비를 선택했기 때문에 랙 셀 생성 화면만 표시됩니다.</p>
                  </div>
                  <span class="child-badge">eq_id = {{ selectedEqMst.id }}</span>
                </div>

                <div class="coordinate-info">
                  <div class="coord-diagram">
                    <div class="coord-axis-y">Row (Y)</div>
                    <div class="coord-grid">
                      <div class="coord-arrow-up">↑</div>
                      <div class="coord-origin">원점 기준</div>
                      <div class="coord-arrow-right">→</div>
                    </div>
                    <div class="coord-axis-x">Bay (X)</div>
                  </div>
                  <div class="coord-desc">
                    <strong>좌표 입력 기준</strong>
                    <ul>
                      <li>좌표계의 원점은 시스템 기준으로만 사용됩니다.</li>
                      <li>실제 랙 셀 입력값은 <b>Row/Bay 모두 1부터 시작</b>합니다.</li>
                      <li>Bay(X축)는 오른쪽으로 증가합니다.</li>
                      <li>Row(Y축)는 위쪽으로 증가합니다.</li>
                      <li>시작값이 끝값보다 크면 자동으로 보정됩니다.</li>
                    </ul>
                  </div>
                </div>

                <div class="readonly-strip">
                  <div class="readonly-mini">
                    <span>기준 기본 설비</span>
                    <strong>{{ selectedEqMst.id }}</strong>
                  </div>
                  <div class="readonly-mini">
                    <span>설비 타입</span>
                    <strong>{{ getEqTypeLabel(selectedEqMst.type) }}</strong>
                  </div>
                </div>

                <div class="form-stack">
                  <div class="form-grid">
                    <div class="form-row">
                      <label>시작 층(Level)</label>
                      <input
                        v-model.number="rackBulkRequest.startLevel"
                        type="number"
                        min="1"
                        placeholder="예: 1"
                      />
                    </div>
                    <div class="form-row">
                      <label>끝 층(Level)</label>
                      <input
                        v-model.number="rackBulkRequest.endLevel"
                        type="number"
                        min="1"
                        placeholder="예: 3"
                      />
                    </div>
                  </div>

                  <div class="inline-guide subtle">
                    ℹ️ 각 층마다 <b>별도의 2D 페이지</b>가 생성됩니다. 단일 층이면 시작과 끝을 같게 입력하세요.
                  </div>

                  <div class="form-grid">
                    <div class="form-row">
                      <label>시작 Row</label>
                      <input
                        v-model.number="rackBulkRequest.startRow"
                        type="number"
                        min="1"
                        placeholder="예: 1"
                      />
                    </div>
                    <div class="form-row">
                      <label>끝 Row</label>
                      <input
                        v-model.number="rackBulkRequest.endRow"
                        type="number"
                        min="1"
                        placeholder="예: 10"
                      />
                    </div>
                  </div>

                  <div class="form-grid">
                    <div class="form-row">
                      <label>시작 Bay</label>
                      <input
                        v-model.number="rackBulkRequest.startBay"
                        type="number"
                        min="1"
                        placeholder="예: 1"
                      />
                    </div>
                    <div class="form-row">
                      <label>끝 Bay</label>
                      <input
                        v-model.number="rackBulkRequest.endBay"
                        type="number"
                        min="1"
                        placeholder="예: 20"
                      />
                    </div>
                  </div>

                  <div class="form-row">
                    <label>기본 타입</label>
                    <select v-model="rackBulkRequest.rackType">
                      <option v-for="opt in rackTypeOptions" :key="opt.value" :value="opt.value">
                        {{ opt.label }}
                      </option>
                    </select>
                  </div>

                  <div class="form-row">
                    <label>주행 전용 Row</label>
                    <input v-model="driveOnlyRowsInput" type="text" placeholder="예: 2,6,8" @keydown.stop />
                  </div>

                  <div class="form-row">
                    <label>주행 전용 Bay</label>
                    <input v-model="driveOnlyBaysInput" type="text" placeholder="예: 3,10,15" @keydown.stop />
                  </div>

                  <div class="form-row checkbox-row">
                    <label class="checkbox-label">
                      <input
                        type="checkbox"
                        v-model="rackBulkRequest.create2dItems"
                        :disabled="!props.lcId"
                      />
                      <span>2D Dashboard 아이템 자동 생성</span>
                    </label>
                    <span v-if="!props.lcId" class="hint-text">(lcId 미설정)</span>
                  </div>
                </div>

                <div v-if="rackValidationMessage" class="inline-guide warning">
                  {{ rackValidationMessage }}
                </div>
                <div v-else class="inline-guide">
                  현재 범위로 <b>{{ estimatedCellCount }}</b>개의 랙 셀이
                  <b>{{ estimatedPageCount }}</b>개 층 페이지에 걸쳐 생성됩니다.
                </div>

                <div class="preview-box">
                  <div class="preview-item">
                    <span>예상 셀 수</span>
                    <strong>{{ estimatedCellCount }}개</strong>
                  </div>
                  <div class="preview-item">
                    <span>생성 페이지 수</span>
                    <strong>{{ estimatedPageCount }}개 (층별)</strong>
                  </div>
                  <div class="preview-item preview-item-wide">
                    <span>ID 범위</span>
                    <strong>{{ estimatedCellIdRange }}</strong>
                  </div>
                </div>

                <div class="action-footer">
                  <button
                    class="btn btn-secondary"
                    :disabled="!canRegenerate2d"
                    @click="regenerate2dFromExistingRacks"
                    title="이미 생성된 랙 셀을 기준으로 2D 페이지/아이템과 재고 로케이션만 역생성합니다."
                  >
                    기존 랙으로 2D + Location 재생성
                  </button>
                  <button
                    class="btn btn-primary"
                    :disabled="!canCreateRackCells"
                    @click="createRackCellsGrid"
                  >
                    랙 셀 생성
                  </button>
                </div>

                <div class="divider"></div>

                <div class="mini-head">
                  <strong>기존 랙 셀</strong>
                  <button
                    v-if="rackCells.length > 0"
                    class="btn btn-danger btn-sm"
                    @click="deleteAllRackCells"
                  >
                    전체 삭제
                  </button>
                </div>

                <div v-if="rackCells.length === 0" class="state-box empty mini-empty">
                  생성된 랙 셀이 없습니다.
                </div>
                <ul v-else class="list compact inner-list">
                  <li v-for="cell in displayedRackCells" :key="cell.id" class="list-item small">
                    <div class="item-main">
                      <span class="item-id">{{ cell.id }}</span>
                      <span class="item-pos"
                        >L{{ cell.level }} · R{{ cell.row }} · B{{ cell.bay }}</span
                      >
                    </div>
                    <button class="delete-btn small" @click="deleteRackCell(cell.id)">삭제</button>
                  </li>
                  <li v-if="rackCells.length > 20" class="more-indicator">
                    ... 외 {{ rackCells.length - 20 }}개
                  </li>
                </ul>
              </section>
            </div>

            <!-- 이송설비 -> 컨베이어 -->
            <div v-else-if="selectedEqMst.type === EqType.CONVEYOR" class="single-child-layout">
              <section class="card child-card">
                <div class="child-title-wrap">
                  <div>
                    <h4>컨베이어 / 리프터 생성</h4>
                    <p>이송설비를 선택했기 때문에 컨베이어 생성 화면만 표시됩니다.</p>
                  </div>
                  <span class="child-badge">eq_id = {{ selectedEqMst.id }}</span>
                </div>

                <div class="readonly-strip">
                  <div class="readonly-mini">
                    <span>기준 기본 설비</span>
                    <strong>{{ selectedEqMst.id }}</strong>
                  </div>
                  <div class="readonly-mini">
                    <span>설비 타입</span>
                    <strong>{{ getEqTypeLabel(selectedEqMst.type) }}</strong>
                  </div>
                </div>

                <div class="form-stack">
                  <div class="form-row">
                    <label>설비 ID</label>
                    <input v-model="newCvMst.id" type="text" placeholder="예: CV_001" @keydown.stop />
                  </div>

                  <div class="form-row">
                    <label>타입</label>
                    <select v-model="newCvMst.type">
                      <option
                        v-for="opt in conveyorTypeOptions"
                        :key="opt.value"
                        :value="opt.value"
                      >
                        {{ opt.label }}
                      </option>
                    </select>
                  </div>

                  <div class="form-row">
                    <label>층(Level)</label>
                    <input v-model.number="newCvMst.level" type="number" min="1" />
                  </div>
                </div>

                <div class="action-footer">
                  <button class="btn btn-primary" :disabled="!canCreateCvMst" @click="createCvMst">
                    컨베이어 생성
                  </button>
                </div>

                <div class="divider"></div>

                <div class="mini-head">
                  <strong>기존 컨베이어</strong>
                </div>

                <div v-if="cvMstList.length === 0" class="state-box empty mini-empty">
                  등록된 컨베이어/리프터가 없습니다.
                </div>
                <ul v-else class="list inner-list">
                  <li
                    v-for="cv in cvMstList"
                    :key="cv.id"
                    class="list-item list-item-column"
                    :class="{ selected: editingCvMstId === cv.id }"
                  >
                    <div class="list-top">
                      <div class="item-main">
                        <span class="item-id">{{ cv.id }}</span>
                        <span class="item-type">{{ getConveyorTypeLabel(cv.type) }}</span>
                        <span class="item-level">L{{ cv.level }}</span>
                      </div>
                      <div class="item-actions">
                        <button class="tag-btn tag-btn-edit" @click="startEditCvMst(cv)">수정</button>
                        <button class="tag-btn danger" @click="deleteCvMst(cv.id)">삭제</button>
                      </div>
                    </div>

                    <div v-if="editingCvMstId === cv.id" class="inline-edit-form">
                      <div class="form-grid">
                        <div class="form-row compact">
                          <label>타입</label>
                          <select v-model="editCvMst.type">
                            <option
                              v-for="opt in conveyorTypeOptions"
                              :key="opt.value"
                              :value="opt.value"
                            >
                              {{ opt.label }}
                            </option>
                          </select>
                        </div>
                        <div class="form-row compact">
                          <label>층(Level)</label>
                          <input v-model.number="editCvMst.level" type="number" min="1" />
                        </div>
                      </div>
                      <div class="form-grid">
                        <div class="form-row compact">
                          <label>자동 여부</label>
                          <select v-model="editCvMst.autoYn">
                            <option :value="true">true</option>
                            <option :value="false">false</option>
                          </select>
                        </div>
                        <div class="form-row compact">
                          <label>사용 여부</label>
                          <select v-model="editCvMst.useYn">
                            <option :value="true">true</option>
                            <option :value="false">false</option>
                          </select>
                        </div>
                      </div>
                      <div class="inline-edit-actions">
                        <button class="btn btn-secondary btn-sm" @click="cancelEditCvMst">취소</button>
                        <button class="btn btn-primary btn-sm" @click="saveEditCvMst(cv.id)">저장</button>
                      </div>
                    </div>
                  </li>
                </ul>
              </section>
            </div>

            <!-- 셔틀카 -->
            <div v-else-if="selectedEqMst.type === EqType.SHUTTLE_CAR" class="single-child-layout">
              <section class="card child-card">
                <div class="child-title-wrap">
                  <div>
                    <h4>셔틀카 생성</h4>
                    <p>셔틀 카 타입을 선택했기 때문에 셔틀카 생성 화면만 표시됩니다.</p>
                  </div>
                  <span class="child-badge">eq_id = {{ selectedEqMst.id }}</span>
                </div>

                <div class="readonly-strip">
                  <div class="readonly-mini">
                    <span>기준 기본 설비</span>
                    <strong>{{ selectedEqMst.id }}</strong>
                  </div>
                  <div class="readonly-mini">
                    <span>설비 타입</span>
                    <strong>{{ getEqTypeLabel(selectedEqMst.type) }}</strong>
                  </div>
                </div>

                <div class="form-stack">
                  <div class="form-row">
                    <label>셔틀카 ID</label>
                    <input
                      v-model="newCarMst.id"
                      type="text"
                      placeholder="기본값은 선택한 기본 설비 eq_id와 동일"
                      @keydown.stop
                    />
                  </div>

                  <div class="readonly-info">
                    <div class="readonly-title">자동 기본값</div>
                    <div class="readonly-value">
                      셔틀카 ID는 기본으로 현재 선택한 기본 설비의 eq_id와 같게 맞춰집니다.
                    </div>
                  </div>

                  <div class="form-row">
                    <label>차량 타입</label>
                    <select v-model="newCarMst.type">
                      <option v-for="opt in carTypeOptions" :key="opt.value" :value="opt.value">
                        {{ opt.label }}
                      </option>
                    </select>
                  </div>

                  <div class="form-grid">
                    <div class="form-row">
                      <label>Row</label>
                      <input v-model.number="newCarMst.row" type="number" min="0" />
                    </div>
                    <div class="form-row">
                      <label>Bay</label>
                      <input v-model.number="newCarMst.bay" type="number" min="0" />
                    </div>
                  </div>

                  <div class="form-grid">
                    <div class="form-row">
                      <label>Level</label>
                      <input v-model.number="newCarMst.level" type="number" min="1" />
                    </div>
                    <div class="form-row">
                      <label>Rack ID</label>
                      <input v-model="newCarMst.rackId" type="text" placeholder="선택 입력" @keydown.stop />
                    </div>
                  </div>

                  <div class="form-row">
                    <label>Rack EQ ID</label>
                    <input v-model="newCarMst.rackEqId" type="text" placeholder="선택 입력" @keydown.stop />
                  </div>

                  <div class="form-grid">
                    <div class="form-row">
                      <label>최소 Row</label>
                      <input v-model.number="newCarMst.minRow" type="number" min="0" />
                    </div>
                    <div class="form-row">
                      <label>최대 Row</label>
                      <input v-model.number="newCarMst.maxRow" type="number" min="0" />
                    </div>
                  </div>
                </div>

                <div class="action-footer">
                  <button
                    class="btn btn-primary"
                    :disabled="!canCreateCarMst"
                    @click="createCarMst"
                  >
                    셔틀카 생성
                  </button>
                </div>

                <div class="divider"></div>

                <div class="mini-head">
                  <strong>기존 셔틀카</strong>
                </div>

                <div v-if="carMstList.length === 0" class="state-box empty mini-empty">
                  등록된 셔틀카가 없습니다.
                </div>
                <ul v-else class="list inner-list">
                  <li
                    v-for="car in carMstList"
                    :key="car.id"
                    class="list-item list-item-column"
                    :class="{ selected: editingCarMstId === car.id }"
                  >
                    <div class="list-top">
                      <div class="item-main">
                        <span class="item-id">{{ car.id }}</span>
                        <span class="item-type">{{ car.type }}</span>
                        <span class="item-pos">L{{ car.level }} · R{{ car.row }} · B{{ car.bay }}</span>
                      </div>
                      <div class="item-actions">
                        <button class="tag-btn tag-btn-edit" @click="startEditCarMst(car)">수정</button>
                        <button class="tag-btn danger" @click="deleteCarMst(car.id)">삭제</button>
                      </div>
                    </div>

                    <div v-if="editingCarMstId === car.id" class="inline-edit-form">
                      <div class="form-grid">
                        <div class="form-row compact">
                          <label>차량 타입</label>
                          <select v-model="editCarMst.type">
                            <option v-for="opt in carTypeOptions" :key="opt.value" :value="opt.value">
                              {{ opt.label }}
                            </option>
                          </select>
                        </div>
                        <div class="form-row compact">
                          <label>Level</label>
                          <input v-model.number="editCarMst.level" type="number" min="1" />
                        </div>
                      </div>
                      <div class="form-grid">
                        <div class="form-row compact">
                          <label>Row</label>
                          <input v-model.number="editCarMst.row" type="number" min="0" />
                        </div>
                        <div class="form-row compact">
                          <label>Bay</label>
                          <input v-model.number="editCarMst.bay" type="number" min="0" />
                        </div>
                      </div>
                      <div class="form-grid">
                        <div class="form-row compact">
                          <label>최소 Row</label>
                          <input v-model.number="editCarMst.minRow" type="number" min="0" />
                        </div>
                        <div class="form-row compact">
                          <label>최대 Row</label>
                          <input v-model.number="editCarMst.maxRow" type="number" min="0" />
                        </div>
                      </div>
                      <div class="form-grid">
                        <div class="form-row compact">
                          <label>Rack ID</label>
                          <input v-model="editCarMst.rackId" type="text" placeholder="선택 입력" @keydown.stop />
                        </div>
                        <div class="form-row compact">
                          <label>Rack EQ ID</label>
                          <input v-model="editCarMst.rackEqId" type="text" placeholder="선택 입력" @keydown.stop />
                        </div>
                      </div>
                      <div class="form-grid">
                        <div class="form-row compact">
                          <label>자동 여부</label>
                          <select v-model="editCarMst.autoYn">
                            <option :value="true">true</option>
                            <option :value="false">false</option>
                          </select>
                        </div>
                        <div class="form-row compact">
                          <label>사용 여부</label>
                          <select v-model="editCarMst.useYn">
                            <option :value="true">true</option>
                            <option :value="false">false</option>
                          </select>
                        </div>
                      </div>
                      <div class="inline-edit-actions">
                        <button class="btn btn-secondary btn-sm" @click="cancelEditCarMst">취소</button>
                        <button class="btn btn-primary btn-sm" @click="saveEditCarMst(car.id)">저장</button>
                      </div>
                    </div>
                  </li>
                </ul>
              </section>
            </div>

            <div v-else class="state-box empty large-empty">
              지원하지 않는 기본 설비 타입입니다.
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, watch } from 'vue';
  import { generatorApi, type EqMstDetailResponse } from '../api/generatorApi';
  import {
    EqGroupType,
    EqGroupTypeOptions,
    EqType,
    EqTypeOptions,
    EqTypeLabels,
    RackType,
    RackTypeOptions,
    ConveyorType,
    ConveyorTypeOptions,
    ConveyorTypeLabels,
  } from '../../constants/EcsDBConsts';
  import { enumLabel } from '../../constants';

  const props = defineProps<{
    lcId?: string;
  }>();

  const emit = defineEmits<{
    (e: 'close'): void;
    (e: 'refresh'): void;
  }>();

  type TabKey = 'group' | 'equipment' | 'children';
  const activeTab = ref<TabKey>('group');
  const isLoading = ref(false);

  const eqGroupTypeOptions = EqGroupTypeOptions;
  const eqTypeOptions = EqTypeOptions;
  const rackTypeOptions = RackTypeOptions;
  const conveyorTypeOptions = ConveyorTypeOptions;
  const carTypeOptions = [{ value: 'SHUTTLE', label: '셔틀' }];

  const getEqTypeLabel = (type: number) => EqTypeLabels[type] || `타입 ${type}`;
  const getRackTypeLabel = (type: number) => enumLabel(RackType, type, `타입 ${type}`);
  const getConveyorTypeLabel = (type: number) => ConveyorTypeLabels[type] || `타입 ${type}`;

  const showPlcSection = computed(() => {
    return newEqMst.value.type === EqType.CONVEYOR || newEqMst.value.type === EqType.SHUTTLE_CAR;
  });

  // ============================================
  // 공통 헬퍼
  // ============================================

  /**
   * 현재 컨텍스트에서 활성 eqGroupId 를 반환
   * - selectedEqMst 우선, 없으면 childrenEqGroupId, 그다음 filterEqGroupId
   * - 없으면 빈 문자열
   */
  const resolveActiveEqGroupId = (): string => {
    return (
      selectedEqMst.value?.eqGroupId ||
      childrenEqGroupId.value ||
      filterEqGroupId.value ||
      ''
    );
  };

  // ============================================
  // 1. 설비 그룹
  // ============================================
  const eqGroups = ref<any[]>([]);
  const selectedEqGroupId = ref('');

  const selectedEqGroup = computed(
    () => eqGroups.value.find((g) => g.id === selectedEqGroupId.value) || null,
  );

  const newEqGroup = ref({
    id: '',
    name: '',
    type: EqGroupType.SHUTTLE_RACK_4WAY,
  });

  const canCreateEqGroup = computed(() => {
    return Boolean(newEqGroup.value.id.trim() && newEqGroup.value.name.trim());
  });

  const loadEqGroups = async () => {
    isLoading.value = true;
    try {
      eqGroups.value = await generatorApi.getEqGroups();
    } catch (e: any) {
      alert('설비 그룹 로드 실패: ' + e.message);
    } finally {
      isLoading.value = false;
    }
  };

  const createEqGroup = async () => {
    if (!canCreateEqGroup.value) return;
    const createdId = newEqGroup.value.id;

    try {
      await generatorApi.createEqGroup(newEqGroup.value);
      alert('설비 그룹이 생성되었습니다.');

      await loadEqGroups();

      const createdGroup = eqGroups.value.find((g) => g.id === createdId);
      if (createdGroup) {
        selectEqGroup(createdGroup);
        goToEquipmentWithGroup(createdId);
      }
      resetNewEqGroup();
    } catch (e: any) {
      alert('설비 그룹 생성 실패: ' + e.message);
    }
  };

  const resetNewEqGroup = () => {
    newEqGroup.value = { id: '', name: '', type: EqGroupType.SHUTTLE_RACK_4WAY };
  };

  const selectEqGroup = (group: any) => {
    selectedEqGroupId.value = group.id;

    editingEqGroupId.value = group.id;
    newEqGroup.value.name = group.name;
    newEqGroup.value.type = group.type;
    eqGroupFormMode.value = 'edit';
  };

  const deleteEqGroup = async (id: string) => {
    if (!confirm(`설비 그룹 "${id}"를 삭제하시겠습니까?`)) return;
    try {
      await generatorApi.deleteEqGroup(id);
      if (selectedEqGroupId.value === id) selectedEqGroupId.value = '';
      await loadEqGroups();
    } catch (e: any) {
      alert('삭제 실패: ' + e.message);
    }
  };

  const goToEquipmentWithGroup = async (groupId: string) => {
    selectedEqGroupId.value = groupId;
    newEqMst.value.eqGroupId = groupId;
    filterEqGroupId.value = groupId;
    activeTab.value = 'equipment';
    await loadEqMstList();
  };

  // ============================================
  // 2. 기본 설비
  // ============================================
  const eqMstList = ref<any[]>([]);
  const selectedEqMstId = ref('');
  const filterEqGroupId = ref('');

  const childrenEqGroupId = ref('');
  const childrenEqMstList = ref<any[]>([]);

  const selectedEqMst = computed(
    () =>
      eqMstList.value.find((eq) => eq.id === selectedEqMstId.value) ||
      childrenEqMstList.value.find((eq) => eq.id === selectedEqMstId.value) ||
      null,
  );

  const newEqMst = ref({
    id: '',
    eqGroupId: '',
    name: '',
    type: EqType.RACK,
    plcId: '',
    plcName: '',
    plcIp: '',
    plcPort: undefined as number | undefined,
    plcIfType: '',
    plcEqType: undefined as number | undefined,
    connectYn: false,
    useYn: true,
  });

  const canCreateEqMst = computed(() => {
    return Boolean(
      newEqMst.value.id.trim() && newEqMst.value.eqGroupId && newEqMst.value.name.trim(),
    );
  });

  const loadEqMstList = async () => {
    if (!filterEqGroupId.value) {
      eqMstList.value = [];
      return;
    }

    selectedEqGroupId.value = filterEqGroupId.value;
    newEqMst.value.eqGroupId = filterEqGroupId.value;

    isLoading.value = true;
    try {
      eqMstList.value = await generatorApi.getEqMstByGroup(filterEqGroupId.value);
    } catch (e: any) {
      alert('설비 로드 실패: ' + e.message);
    } finally {
      isLoading.value = false;
    }
  };

  const createEqMst = async () => {
    if (!canCreateEqMst.value) return;

    const createdGroupId = newEqMst.value.eqGroupId;

    try {
      await generatorApi.createEqMst({
        ...newEqMst.value,
        plcId: showPlcSection.value ? newEqMst.value.plcId || '' : '',
        plcName: showPlcSection.value ? newEqMst.value.plcName || '' : '',
        plcIp: showPlcSection.value ? newEqMst.value.plcIp || '' : '',
        plcPort: showPlcSection.value ? newEqMst.value.plcPort : undefined,
        plcIfType: showPlcSection.value ? newEqMst.value.plcIfType || '' : '',
        plcEqType: showPlcSection.value ? newEqMst.value.plcEqType : undefined,
        connectYn: showPlcSection.value ? newEqMst.value.connectYn : false,
        useYn: showPlcSection.value ? newEqMst.value.useYn : true,
      });

      alert('기본 설비가 생성되었습니다.');

      filterEqGroupId.value = createdGroupId;
      childrenEqGroupId.value = createdGroupId;
      selectedEqGroupId.value = createdGroupId;

      resetNewEqMst();
      await loadEqMstList();
      await loadChildrenEqMstList();
      emit('refresh');
    } catch (e: any) {
      alert('설비 생성 실패: ' + e.message);
    }
  };

  const resetNewEqMst = () => {
    newEqMst.value = {
      id: '',
      eqGroupId: '',
      name: '',
      type: EqType.RACK,
      plcId: '',
      plcName: '',
      plcIp: '',
      plcPort: undefined,
      plcIfType: '',
      plcEqType: undefined,
      connectYn: false,
      useYn: true,
    };
  };

  const clearPlcFields = () => {
    newEqMst.value.plcId = '';
    newEqMst.value.plcName = '';
    newEqMst.value.plcIp = '';
    newEqMst.value.plcPort = undefined;
    newEqMst.value.plcIfType = '';
    newEqMst.value.plcEqType = undefined;
    newEqMst.value.connectYn = false;
    newEqMst.value.useYn = true;
  };

  /**
   * ⭐ (eqGroupId + id) 쌍으로 상세 조회
   */
  const selectEqMst = async (eq: any) => {
    selectedEqMstId.value = eq.id;

    try {
      const groupId = eq.eqGroupId || filterEqGroupId.value;
      if (!groupId) {
        alert('설비 그룹 정보를 확인할 수 없습니다.');
        return;
      }

      const detail: EqMstDetailResponse = await generatorApi.getEqMstDetail(groupId, eq.id);
      editingEqMstId.value = detail.id;
      editingEqMstHasPlc.value = Boolean(detail.plcId);
      newEqMst.value = {
        id: detail.id,
        eqGroupId: detail.eqGroupId,
        name: detail.name,
        type: detail.type,
        plcId: detail.plcId || '',
        plcName: detail.plcName || '',
        plcIp: detail.plcIp || '',
        plcPort: detail.plcPort,
        plcIfType: detail.plcIfType || '',
        plcEqType: detail.plcEqType,
        connectYn: detail.connectYn ?? false,
        useYn: detail.useYn ?? true,
      };
      eqMstFormMode.value = 'edit';
    } catch (e: any) {
      alert('설비 정보 로드 실패: ' + e.message);
    }
  };

  /**
   * ⭐ (eqGroupId + id) 쌍으로 삭제
   */
  const deleteEqMst = async (eq: any) => {
    if (!confirm(`기본 설비 "${eq.id}"를 삭제하시겠습니까?`)) return;
    try {
      const groupId = eq.eqGroupId || filterEqGroupId.value;
      if (!groupId) {
        alert('설비 그룹 정보를 확인할 수 없습니다.');
        return;
      }

      await generatorApi.deleteEqMst(groupId, eq.id);
      if (selectedEqMstId.value === eq.id) selectedEqMstId.value = '';
      await loadEqMstList();
      await loadChildrenEqMstList();
    } catch (e: any) {
      alert('삭제 실패: ' + e.message);
    }
  };

  const goToChildrenWithEq = async (eq: any) => {
    selectedEqMstId.value = eq.id;
    childrenEqGroupId.value = eq.eqGroupId || filterEqGroupId.value;
    activeTab.value = 'children';
    await loadChildrenEqMstList();
    await syncChildrenData();
  };

  watch(
    () => newEqMst.value.id,
    (newId, oldId) => {
      if (!showPlcSection.value) return;

      if (!newId) {
        if (newEqMst.value.plcId === oldId) {
          newEqMst.value.plcId = '';
        }
        return;
      }

      if (!newEqMst.value.plcId || newEqMst.value.plcId === oldId) {
        newEqMst.value.plcId = newId;
      }
    },
  );

  watch(
    () => newEqMst.value.type,
    (newType, oldType) => {
      const wasPlcType = oldType === EqType.CONVEYOR || oldType === EqType.SHUTTLE_CAR;
      const isPlcType = newType === EqType.CONVEYOR || newType === EqType.SHUTTLE_CAR;

      if (isPlcType && !newEqMst.value.plcId) {
        newEqMst.value.plcId = newEqMst.value.id || '';
      }

      if (wasPlcType && !isPlcType) {
        clearPlcFields();
      }
    },
  );

  // ============================================
  // 3. 하위 설비 생성
  // ============================================
  const loadChildrenEqMstList = async () => {
    if (!childrenEqGroupId.value) {
      childrenEqMstList.value = [];
      selectedEqMstId.value = '';
      clearChildrenData();
      return;
    }

    isLoading.value = true;
    try {
      childrenEqMstList.value = await generatorApi.getEqMstByGroup(childrenEqGroupId.value);

      if (!childrenEqMstList.value.some((eq: any) => eq.id === selectedEqMstId.value)) {
        selectedEqMstId.value = '';
        clearChildrenData();
      }
    } catch (e: any) {
      childrenEqMstList.value = [];
      selectedEqMstId.value = '';
      clearChildrenData();
      alert('기본 설비 로드 실패: ' + e.message);
    } finally {
      isLoading.value = false;
    }
  };

  const onChildrenEqGroupChange = async () => {
    await loadChildrenEqMstList();
  };

  const onSelectedEqMstChange = async () => {
    await syncChildrenData();
  };

  const syncChildrenData = async () => {
    if (!selectedEqMst.value) {
      clearChildrenData();
      return;
    }

    // ⭐ 타입 전환 시 이전 타입의 state/리스트 초기화 (깨끗한 상태로)
    clearChildrenData();

    const eqId = selectedEqMst.value.id;
    const eqType = selectedEqMst.value.type;

    // ⭐ 선택된 기본 설비 타입에 해당하는 하위 설비만 처리
    if (eqType === EqType.RACK) {
      rackBulkRequest.value.eqId = eqId;
      filterRackEqId.value = eqId;
      await loadRackCells();
    } else if (eqType === EqType.CONVEYOR) {
      newCvMst.value.eqId = eqId;
      filterCvEqId.value = eqId;
      await loadCvMstList();
    } else if (eqType === EqType.SHUTTLE_CAR) {
      newCarMst.value.eqId = eqId;
      filterShuttleEqId.value = eqId;
      if (!newCarMst.value.id) {
        newCarMst.value.id = eqId;
      }
      await loadCarMstList();
    } else {
      console.warn(`[Generator] 지원하지 않는 설비 타입: ${eqType}`);
    }
  };

  const clearChildrenData = () => {
    rackBulkRequest.value.eqId = '';
    filterRackEqId.value = '';
    rackCells.value = [];
    driveOnlyRowsInput.value = '';
    driveOnlyBaysInput.value = '';

    newCvMst.value.eqId = '';
    filterCvEqId.value = '';
    cvMstList.value = [];

    newCarMst.value.eqId = '';
    filterShuttleEqId.value = '';
    carMstList.value = [];
  };

  // ============================================
  // 3-1. 랙 셀
  // ============================================
  const rackCells = ref<any[]>([]);
  const filterRackEqId = ref('');
  const driveOnlyRowsInput = ref('');
  const driveOnlyBaysInput = ref('');

  const rackBulkRequest = ref({
    eqId: '',
    startLevel: 1,
    endLevel: 1,
    startRow: 1,
    endRow: 1,
    startBay: 1,
    endBay: 1,
    rackType: RackType.CELL.code,
    create2dItems: true,
  });

  const normalizedRackRange = computed(() => {
    const startRow = Number(rackBulkRequest.value.startRow ?? 1);
    const endRow = Number(rackBulkRequest.value.endRow ?? 1);
    const startBay = Number(rackBulkRequest.value.startBay ?? 1);
    const endBay = Number(rackBulkRequest.value.endBay ?? 1);
    const startLevel = Number(rackBulkRequest.value.startLevel ?? 1);
    const endLevel = Number(rackBulkRequest.value.endLevel ?? 1);

    return {
      startRow: Math.min(startRow, endRow),
      endRow: Math.max(startRow, endRow),
      startBay: Math.min(startBay, endBay),
      endBay: Math.max(startBay, endBay),
      startLevel: Math.min(startLevel, endLevel),
      endLevel: Math.max(startLevel, endLevel),
    };
  });

  const rackValidationMessage = computed(() => {
    const { eqId } = rackBulkRequest.value;
    const { startRow, endRow, startBay, endBay, startLevel, endLevel } = normalizedRackRange.value;

    if (!eqId) return '먼저 기본 설비를 선택하세요.';
    if (
      !Number.isFinite(startLevel) ||
      !Number.isFinite(endLevel) ||
      startLevel < 1 ||
      endLevel < 1
    ) {
      return '층(Level)은 1 이상이어야 합니다.';
    }
    if ([startRow, endRow, startBay, endBay].some((v) => !Number.isFinite(v) || v < 1)) {
      return 'Row/Bay 값은 모두 1 이상의 숫자로 입력하세요.';
    }

    return '';
  });

  const estimatedCellCount = computed(() => {
    if (rackValidationMessage.value) return 0;

    const rows = normalizedRackRange.value.endRow - normalizedRackRange.value.startRow + 1;
    const bays = normalizedRackRange.value.endBay - normalizedRackRange.value.startBay + 1;
    const levels =
      normalizedRackRange.value.endLevel - normalizedRackRange.value.startLevel + 1;
    return rows * bays * levels;
  });

  /** 생성될 2D 페이지 수 (= 층 수) */
  const estimatedPageCount = computed(() => {
    if (rackValidationMessage.value) return 0;
    return normalizedRackRange.value.endLevel - normalizedRackRange.value.startLevel + 1;
  });

  const estimatedCellIdRange = computed(() => {
    if (rackValidationMessage.value) return '-';

    const { startRow, startBay, endRow, endBay, startLevel, endLevel } = normalizedRackRange.value;

    const startId = `${startLevel}${String(startRow).padStart(2, '0')}${String(startBay).padStart(2, '0')}`;
    const endId = `${endLevel}${String(endRow).padStart(2, '0')}${String(endBay).padStart(2, '0')}`;

    return `${startId} ~ ${endId}`;
  });

  const canCreateRackCells = computed(() => {
    return !rackValidationMessage.value && estimatedCellCount.value > 0;
  });

  const displayedRackCells = computed(() => {
    return [...rackCells.value]
      .sort((a, b) => {
        const levelDiff = (a.level ?? 0) - (b.level ?? 0);
        if (levelDiff !== 0) return levelDiff;

        const rowDiff = (a.row ?? 0) - (b.row ?? 0);
        if (rowDiff !== 0) return rowDiff;

        const bayDiff = (a.bay ?? 0) - (b.bay ?? 0);
        if (bayDiff !== 0) return bayDiff;

        return String(a.id ?? '').localeCompare(String(b.id ?? ''));
      })
      .slice(0, 20);
  });

  const parsePositiveNumberList = (input: string): number[] => {
    return [
      ...new Set(
        input
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean)
          .map((s) => Number(s))
          .filter((n) => Number.isInteger(n) && n >= 1),
      ),
    ].sort((a, b) => a - b);
  };

  /**
   * ⭐ (eqGroupId + eqId) 쌍으로 랙 셀 목록 조회
   */
  const loadRackCells = async () => {
    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId || !filterRackEqId.value) {
      rackCells.value = [];
      return;
    }
    console.log(`eqGroupId : ${eqGroupId}, filterRackEqId.value : ${filterRackEqId.value}`);

    try {
      rackCells.value = await generatorApi.getRackCellsByEqId(eqGroupId, filterRackEqId.value);
    } catch (e: any) {
      alert('랙 셀 로드 실패: ' + e.message);
    }
  };

  const createRackCellsGrid = async () => {
    if (!canCreateRackCells.value) return;

    const driveOnlyRows = parsePositiveNumberList(driveOnlyRowsInput.value);
    const driveOnlyBays = parsePositiveNumberList(driveOnlyBaysInput.value);

    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId) {
      alert('설비 그룹을 먼저 선택해 주세요.');
      return;
    }

    const shouldCreate2dItems = rackBulkRequest.value.create2dItems && Boolean(props.lcId);

    if (rackBulkRequest.value.create2dItems && !props.lcId) {
      console.warn('[Generator] lcId가 없어서 2D 아이템을 생성하지 않습니다.');
    }

    // ⭐ 여러 층 생성 시 사용자 확인 (각 층마다 페이지가 만들어지므로 신중)
    const { startLevel, endLevel } = normalizedRackRange.value;
    const levelCount = endLevel - startLevel + 1;

    if (levelCount > 1) {
      const confirmMsg =
        `${startLevel}층 ~ ${endLevel}층 총 ${levelCount}개 층에 각각 2D 페이지를 생성하고 ` +
        `${estimatedCellCount.value}개의 랙 셀을 생성합니다.\n\n계속하시겠습니까?`;
      if (!confirm(confirmMsg)) return;
    }

    try {
      const result = await generatorApi.createRackCellsGrid({
        ...rackBulkRequest.value,
        ...normalizedRackRange.value,
        driveOnlyRows: driveOnlyRows.length > 0 ? driveOnlyRows : undefined,
        driveOnlyBays: driveOnlyBays.length > 0 ? driveOnlyBays : undefined,
        createMode: 'GRID',
        lcId: shouldCreate2dItems ? props.lcId : undefined,
        eqGroupId,
        create2dItems: shouldCreate2dItems,
      });

      // ⭐ 성공 메시지에 층 범위 정보 포함
      const levelInfo =
        levelCount > 1 ? ` (${startLevel}층 ~ ${endLevel}층, ${levelCount}개 층)` : '';

      const message = shouldCreate2dItems
        ? `랙 셀 ${result.createdCount}개 및 2D 아이템이 생성되었습니다${levelInfo}.`
        : `랙 셀 ${result.createdCount}개가 생성되었습니다${levelInfo}.`;

      alert(message);
      await loadRackCells();
      emit('refresh');
    } catch (e: any) {
      alert('랙 셀 생성 실패: ' + e.message);
    }
  };

  /** 역생성 가능 조건: 그룹 + 기본 설비 + lcId 모두 있어야 함 */
  const canRegenerate2d = computed(() => {
    return Boolean(props.lcId && resolveActiveEqGroupId() && filterRackEqId.value);
  });

  /**
   * 이미 생성된 랙 셀 데이터를 기반으로
   * tb_ecs_2d_page + tb_ecs_2d_item + tb_inventory_location 만 역생성.
   * 기존 랙 셀은 건드리지 않음.
   */
  const regenerate2dFromExistingRacks = async () => {
    if (!props.lcId) {
      alert('lcId가 없어 역생성할 수 없습니다.');
      return;
    }

    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId || !filterRackEqId.value) {
      alert('설비 그룹과 기본 설비를 먼저 선택해 주세요.');
      return;
    }

    if (!confirm(
      `${eqGroupId} / ${filterRackEqId.value} 의 기존 랙 셀을 기준으로\n` +
      `2D 페이지·아이템과 재고 로케이션을 역생성합니다.\n\n` +
      `(이미 존재하는 항목은 그대로 유지됩니다)\n\n계속하시겠습니까?`
    )) return;

    try {
      const result = await generatorApi.generate2dFromExistingRacks(
        props.lcId,
        eqGroupId,
        filterRackEqId.value,
      );
      alert(
        `역생성 완료\n` +
        `- 새로 생성된 2D 아이템: ${result.createdItems}개\n` +
        `- 새로 생성된 재고 로케이션: ${result.createdLocations}개`
      );
      emit('refresh');
    } catch (e: any) {
      alert('역생성 실패: ' + e.message);
    }
  };

  const deleteRackCell = async (id: string) => {
    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId) {
      alert('설비 그룹을 먼저 선택해 주세요.');
      return;
    }

    try {
      await generatorApi.deleteRackCell(eqGroupId, filterRackEqId.value, id);
      await loadRackCells();
    } catch (e: any) {
      alert('삭제 실패: ' + e.message);
    }
  };

  const deleteAllRackCells = async () => {
    if (!filterRackEqId.value) return;
    if (!confirm(`${rackCells.value.length}개의 랙 셀을 모두 삭제하시겠습니까?`)) return;

    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId) {
      alert('설비 그룹을 먼저 선택해 주세요.');
      return;
    }

    try {
      await generatorApi.deleteRackCellsByEqId(eqGroupId, filterRackEqId.value);
      await loadRackCells();
    } catch (e: any) {
      alert('삭제 실패: ' + e.message);
    }
  };

  // ============================================
  // 3-2. 컨베이어
  // ============================================
  const cvMstList = ref<any[]>([]);
  const filterCvEqId = ref('');

  const newCvMst = ref({
    id: '',
    eqId: '',
    type: ConveyorType.GROUND,
    level: 1,
    useYn: true,
    autoYn: true,
  });

  const canCreateCvMst = computed(() => {
    return Boolean(newCvMst.value.id.trim() && newCvMst.value.eqId);
  });

  const loadCvMstList = async () => {
    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId || !filterCvEqId.value) {
      cvMstList.value = [];
      return;
    }

    try {
      cvMstList.value = await generatorApi.getCvMstByEqId(eqGroupId, filterCvEqId.value);
    } catch (e: any) {
      alert('컨베이어 로드 실패: ' + e.message);
    }
  };

  const createCvMst = async () => {
    if (!canCreateCvMst.value) return;

    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId) {
      alert('설비 그룹을 먼저 선택해 주세요.');
      return;
    }

    try {
      await generatorApi.createCvMst(eqGroupId, newCvMst.value);
      alert('컨베이어가 생성되었습니다.');
      resetNewCvMst();
      newCvMst.value.eqId = selectedEqMst.value?.id || '';
      await loadCvMstList();
      emit('refresh');
    } catch (e: any) {
      alert('생성 실패: ' + e.message);
    }
  };

  const resetNewCvMst = () => {
    newCvMst.value = {
      id: '',
      eqId: '',
      type: ConveyorType.GROUND,
      level: 1,
      useYn: true,
      autoYn: true,
    };
  };

  const deleteCvMst = async (id: string) => {
    if (!confirm(`컨베이어 "${id}"를 삭제하시겠습니까?`)) return;

    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId) {
      alert('설비 그룹을 먼저 선택해 주세요.');
      return;
    }

    try {
      await generatorApi.deleteCvMst(eqGroupId, filterCvEqId.value, id);
      await loadCvMstList();
    } catch (e: any) {
      alert('삭제 실패: ' + e.message);
    }
  };

  // ============================================
  // 3-3. 셔틀카
  // ============================================
  const carMstList = ref<any[]>([]);
  const filterShuttleEqId = ref('');

  const newCarMst = ref({
    id: '',
    eqId: '',
    type: 'SHUTTLE',
    row: 0,
    bay: 0,
    level: 1,
    rackId: '',
    rackEqId: '',
    minRow: 0,
    maxRow: 0,
    autoYn: true,
    useYn: true,
    cargoYn: false,
    completeYn: false,
    plcCmdId: 0,
    plcCompCmdId: 0,
    batteryStatus: 0,
  });

  const canCreateCarMst = computed(() => {
    return Boolean(
      newCarMst.value.id.trim() &&
        newCarMst.value.eqId &&
        newCarMst.value.type.trim() &&
        newCarMst.value.minRow <= newCarMst.value.maxRow,
    );
  });

  const loadCarMstList = async () => {
    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId || !filterShuttleEqId.value) {
      carMstList.value = [];
      return;
    }

    try {
      carMstList.value = await generatorApi.getCarMstByEqId(eqGroupId, filterShuttleEqId.value);
    } catch (e: any) {
      alert('셔틀카 로드 실패: ' + e.message);
    }
  };

  const createCarMst = async () => {
    if (!canCreateCarMst.value) return;

    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId) {
      alert('설비 그룹을 먼저 선택해 주세요.');
      return;
    }

    try {
      await generatorApi.createCarMst(eqGroupId, newCarMst.value);
      alert('셔틀카가 생성되었습니다.');
      resetNewCarMst();
      newCarMst.value.eqId = selectedEqMst.value?.id || '';
      newCarMst.value.id = selectedEqMst.value?.id || '';
      await loadCarMstList();
      emit('refresh');
    } catch (e: any) {
      alert('생성 실패: ' + e.message);
    }
  };

  const resetNewCarMst = () => {
    newCarMst.value = {
      id: '',
      eqId: '',
      type: 'SHUTTLE',
      row: 0,
      bay: 0,
      level: 1,
      rackId: '',
      rackEqId: '',
      minRow: 0,
      maxRow: 0,
      autoYn: true,
      useYn: true,
      cargoYn: false,
      completeYn: false,
      plcCmdId: 0,
      plcCompCmdId: 0,
      batteryStatus: 0,
    };
  };

  const deleteCarMst = async (id: string) => {
    if (!filterShuttleEqId.value) return;
    if (!confirm(`셔틀카 "${id}"를 삭제하시겠습니까?`)) return;

    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId) {
      alert('설비 그룹을 먼저 선택해 주세요.');
      return;
    }

    try {
      await generatorApi.deleteCarMst(eqGroupId, filterShuttleEqId.value, id);
      await loadCarMstList();
    } catch (e: any) {
      alert('삭제 실패: ' + e.message);
    }
  };

  watch(
    () => selectedEqMst.value?.id || '',
    (newEqId, oldEqId) => {
      newCvMst.value.eqId = newEqId;
      newCarMst.value.eqId = newEqId;

      if (!newCarMst.value.id || newCarMst.value.id === oldEqId) {
        newCarMst.value.id = newEqId;
      }
    },
  );

  onMounted(async () => {
    await loadEqGroups();
  });

  // ============================================
  // Edit mode — 설비 그룹
  // ============================================
  const eqGroupFormMode = ref<'create' | 'edit'>('create');
  const editingEqGroupId = ref<string | null>(null);

  const startEditEqGroup = (group: any) => {
    editingEqGroupId.value = group.id;
    newEqGroup.value.name = group.name;
    newEqGroup.value.type = group.type;
    eqGroupFormMode.value = 'edit';
  };

  const cancelEditEqGroup = () => {
    eqGroupFormMode.value = 'create';
    editingEqGroupId.value = null;
    resetNewEqGroup();
  };

  const saveEqGroup = async () => {
    if (!editingEqGroupId.value || !newEqGroup.value.name.trim()) return;
    const targetId = editingEqGroupId.value;

    try {
      await generatorApi.updateEqGroup(targetId, {
        name: newEqGroup.value.name,
        type: newEqGroup.value.type,
      });
      alert('설비 그룹이 수정되었습니다.');

      goToEquipmentWithGroup(targetId);
    } catch (e: any) {
      alert('수정 실패: ' + e.message);
    }
  };

  // ============================================
  // Edit mode — 기본 설비
  // ============================================
  const eqMstFormMode = ref<'create' | 'edit'>('create');
  const editingEqMstId = ref<string | null>(null);
  const editingEqMstHasPlc = ref(false);

  const cancelEditEqMst = () => {
    eqMstFormMode.value = 'create';
    editingEqMstId.value = null;
    editingEqMstHasPlc.value = false;
    resetNewEqMst();
  };

  /**
   * ⭐ (eqGroupId + id) 쌍으로 수정
   */
  const saveEqMst = async () => {
    if (!editingEqMstId.value || !newEqMst.value.name.trim()) return;
    const targetId = editingEqMstId.value;
    const targetGroupId = newEqMst.value.eqGroupId;

    if (!targetGroupId) {
      alert('설비 그룹 정보를 확인할 수 없습니다.');
      return;
    }

    try {
      await generatorApi.updateEqMst(targetGroupId, targetId, {
        name: newEqMst.value.name,
        plcName: editingEqMstHasPlc.value ? newEqMst.value.plcName : undefined,
        plcIp: editingEqMstHasPlc.value ? newEqMst.value.plcIp : undefined,
        plcPort: editingEqMstHasPlc.value ? newEqMst.value.plcPort : undefined,
        plcIfType: editingEqMstHasPlc.value ? newEqMst.value.plcIfType : undefined,
        connectYn: editingEqMstHasPlc.value ? newEqMst.value.connectYn : undefined,
        useYn: editingEqMstHasPlc.value ? newEqMst.value.useYn : undefined,
      });

      alert('기본 설비가 수정되었습니다.');

      await loadEqMstList();
      selectedEqMstId.value = targetId;

      await loadChildrenEqMstList();
      await syncChildrenData();

      emit('refresh');
    } catch (e: any) {
      alert('수정 실패: ' + e.message);
    }
  };

  // ============================================
  // Edit mode — 컨베이어 (inline)
  // ============================================
  const editingCvMstId = ref<string | null>(null);
  const editCvMst = ref({
    type: 0 as number,
    level: 1,
    autoYn: true,
    useYn: true,
  });

  const startEditCvMst = (cv: any) => {
    editingCvMstId.value = cv.id;
    editCvMst.value = { type: cv.type, level: cv.level, autoYn: cv.autoYn, useYn: cv.useYn };
  };

  const cancelEditCvMst = () => {
    editingCvMstId.value = null;
  };

  const saveEditCvMst = async (id: string) => {
    if (!filterCvEqId.value) return;

    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId) {
      alert('설비 그룹을 먼저 선택해 주세요.');
      return;
    }

    try {
      await generatorApi.updateCvMst(eqGroupId, filterCvEqId.value, id, {
        type: editCvMst.value.type as any,
        level: editCvMst.value.level,
        autoYn: editCvMst.value.autoYn,
        useYn: editCvMst.value.useYn,
      });
      alert('컨베이어가 수정되었습니다.');
      cancelEditCvMst();
      await loadCvMstList();
    } catch (e: any) {
      alert('수정 실패: ' + e.message);
    }
  };

  // ============================================
  // Edit mode — 셔틀카 (inline)
  // ============================================
  const editingCarMstId = ref<string | null>(null);
  const editCarMst = ref({
    type: 'SHUTTLE',
    row: 0,
    bay: 0,
    level: 1,
    rackId: '',
    rackEqId: '',
    minRow: 0,
    maxRow: 0,
    autoYn: true,
    useYn: true,
  });

  const startEditCarMst = (car: any) => {
    editingCarMstId.value = car.id;
    editCarMst.value = {
      type: car.type,
      row: car.row,
      bay: car.bay,
      level: car.level,
      rackId: car.rackId || '',
      rackEqId: car.rackEqId || '',
      minRow: car.minRow,
      maxRow: car.maxRow,
      autoYn: car.autoYn,
      useYn: car.useYn,
    };
  };

  const cancelEditCarMst = () => {
    editingCarMstId.value = null;
  };

  const saveEditCarMst = async (id: string) => {
    if (!filterShuttleEqId.value) return;

    const eqGroupId = resolveActiveEqGroupId();
    if (!eqGroupId) {
      alert('설비 그룹을 먼저 선택해 주세요.');
      return;
    }

    try {
      await generatorApi.updateCarMst(eqGroupId, filterShuttleEqId.value, id, {
        type: editCarMst.value.type,
        row: editCarMst.value.row,
        bay: editCarMst.value.bay,
        level: editCarMst.value.level,
        rackId: editCarMst.value.rackId,
        rackEqId: editCarMst.value.rackEqId,
        minRow: editCarMst.value.minRow,
        maxRow: editCarMst.value.maxRow,
        autoYn: editCarMst.value.autoYn,
        useYn: editCarMst.value.useYn,
      });
      alert('셔틀카가 수정되었습니다.');
      cancelEditCarMst();
      await loadCarMstList();
    } catch (e: any) {
      alert('수정 실패: ' + e.message);
    }
  };
</script>

<style scoped>
  .generator-overlay {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.45);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    z-index: 1000;
  }

  .generator-panel {
    width: min(1240px, 100%);
    max-height: calc(100vh - 48px);
    background: #ffffff;
    border-radius: 20px;
    box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    padding: 20px 24px 16px;
    border-bottom: 1px solid #e5e7eb;
    background: linear-gradient(180deg, #ffffff 0%, #fafcff 100%);
  }

  .panel-header h3 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: #111827;
  }

  .panel-subtitle {
    margin: 6px 0 0;
    font-size: 13px;
    color: #6b7280;
    line-height: 1.5;
  }

  .close-btn {
    width: 36px;
    height: 36px;
    border: 1px solid #e5e7eb;
    background: #f8fafc;
    border-radius: 10px;
    cursor: pointer;
    font-size: 14px;
    color: #6b7280;
    transition: 0.2s ease;
    flex-shrink: 0;
  }

  .close-btn:hover {
    background: #eef2ff;
    color: #1f2937;
    border-color: #c7d2fe;
  }

  .step-summary {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
    padding: 16px 24px 0;
  }

  .step-chip {
    display: flex;
    gap: 12px;
    align-items: center;
    padding: 14px 16px;
    border: 1px solid #e5e7eb;
    border-radius: 16px;
    background: #ffffff;
  }

  .step-chip.active {
    border-color: #6366f1;
    background: #eef2ff;
  }

  .step-no {
    width: 28px;
    height: 28px;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: #e5e7eb;
    color: #111827;
    font-size: 13px;
    font-weight: 700;
    flex-shrink: 0;
  }

  .step-chip.active .step-no {
    background: #6366f1;
    color: #ffffff;
  }

  .step-chip strong {
    display: block;
    color: #111827;
    font-size: 14px;
  }

  .step-chip p {
    margin: 3px 0 0;
    font-size: 12px;
    color: #6b7280;
  }

  .selection-summary {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
    padding: 16px 24px 0;
  }

  .summary-chip {
    border: 1px solid #e5e7eb;
    border-radius: 14px;
    padding: 12px 14px;
    background: #f8fafc;
  }

  .summary-label {
    display: block;
    font-size: 12px;
    color: #6b7280;
    margin-bottom: 4px;
  }

  .summary-chip strong {
    color: #111827;
    font-size: 14px;
    word-break: break-all;
  }

  .tab-nav {
    display: flex;
    gap: 10px;
    padding: 16px 24px;
    border-bottom: 1px solid #e5e7eb;
  }

  .tab-nav.three-tabs .tab-btn {
    flex: 1;
  }

  .tab-btn {
    border: 1px solid #dbe3f0;
    background: #f8fafc;
    color: #334155;
    border-radius: 14px;
    padding: 12px 14px;
    cursor: pointer;
    text-align: left;
    transition: 0.2s ease;
  }

  .tab-btn:hover {
    background: #eef2ff;
    border-color: #c7d2fe;
  }

  .tab-btn.active {
    background: #1e293b;
    color: #ffffff;
    border-color: #1e293b;
  }

  .tab-title {
    display: block;
    font-size: 14px;
    font-weight: 700;
  }

  .tab-desc {
    display: block;
    font-size: 12px;
    margin-top: 4px;
    opacity: 0.85;
  }

  .panel-body {
    overflow: auto;
    padding: 24px;
  }

  .tab-content {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .content-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  .card {
    border: 1px solid #e5e7eb;
    border-radius: 18px;
    background: #ffffff;
    padding: 18px;
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
  }

  .card-head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    align-items: flex-start;
    margin-bottom: 16px;
  }

  .card-head h4 {
    margin: 0;
    font-size: 18px;
    color: #111827;
  }

  .card-head p {
    margin: 6px 0 0;
    font-size: 13px;
    color: #6b7280;
    line-height: 1.5;
  }

  .form-stack {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .form-row {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .form-row.compact {
    min-width: 0;
  }

  .form-row label {
    font-size: 13px;
    font-weight: 600;
    color: #374151;
  }

  .form-row input,
  .form-row select {
    width: 100%;
    height: 42px;
    border-radius: 12px;
    border: 1px solid #d1d5db;
    background: #ffffff;
    padding: 0 12px;
    font-size: 14px;
    color: #111827;
    outline: none;
    transition: border-color 0.2s ease, box-shadow 0.2s ease;
    box-sizing: border-box;
  }

  .form-row input:focus,
  .form-row select:focus {
    border-color: #6366f1;
    box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.12);
  }

  .action-footer {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .btn {
    height: 40px;
    border-radius: 12px;
    border: none;
    padding: 0 16px;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    transition: 0.2s ease;
  }

  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .btn-primary {
    background: #4f46e5;
    color: #ffffff;
  }

  .btn-primary:hover:not(:disabled) {
    background: #4338ca;
  }

  .btn-secondary {
    background: #e5e7eb;
    color: #111827;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #d1d5db;
  }

  .btn-danger {
    background: #ef4444;
    color: #ffffff;
  }

  .btn-danger:hover:not(:disabled) {
    background: #dc2626;
  }

  .btn-sm {
    height: 34px;
    padding: 0 12px;
    font-size: 13px;
  }

  .state-box {
    border: 1px dashed #d1d5db;
    border-radius: 14px;
    padding: 18px;
    text-align: center;
    color: #6b7280;
    background: #f9fafb;
  }

  .state-box.empty {
    color: #94a3b8;
  }

  .large-empty {
    padding: 40px 20px;
  }

  .list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .list-item {
    border: 1px solid #e5e7eb;
    border-radius: 14px;
    padding: 12px 14px;
    background: #ffffff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .list-item.small {
    padding: 10px 12px;
  }

  .list-item-column {
    flex-direction: column;
    align-items: stretch;
  }

  .list-item.selected {
    border-color: #6366f1;
    box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.08);
    background: #f8faff;
  }

  .list-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    cursor: pointer;
  }

  .item-main {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
    flex-wrap: wrap;
  }

  .item-id {
    font-weight: 700;
    color: #111827;
    word-break: break-all;
  }

  .item-name,
  .item-type,
  .item-level,
  .item-pos {
    color: #475569;
    font-size: 13px;
  }

  .sub-info {
    font-size: 12px;
    color: #64748b;
  }

  .item-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .tag-btn {
    height: 32px;
    border: 1px solid #d1d5db;
    background: #ffffff;
    color: #334155;
    border-radius: 10px;
    padding: 0 10px;
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
  }

  .tag-btn:hover {
    background: #f8fafc;
  }

  .tag-btn.danger {
    border-color: #fecaca;
    color: #dc2626;
    background: #fff5f5;
  }

  .tag-btn.danger:hover {
    background: #fee2e2;
  }

  .context-card {
    border: 1px solid #e5e7eb;
    border-radius: 18px;
    background: #f8fafc;
    padding: 16px 18px;
  }

  .context-grid {
    display: grid;
    gap: 14px;
  }

  .context-grid.single {
    grid-template-columns: 1fr;
  }

  .children-top {
    grid-template-columns: minmax(220px, 1fr) minmax(260px, 1fr) minmax(280px, 1fr);
    align-items: end;
  }

  .readonly-panel {
    display: flex;
    flex-direction: column;
    gap: 10px;
    border: 1px solid #e5e7eb;
    background: #ffffff;
    border-radius: 14px;
    padding: 12px 14px;
  }

  .readonly-row {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    align-items: center;
  }

  .readonly-label {
    font-size: 12px;
    color: #64748b;
  }

  .readonly-strong {
    color: #111827;
    font-size: 14px;
    font-weight: 700;
    word-break: break-all;
  }

  .section-divider,
  .divider {
    height: 1px;
    background: #e5e7eb;
    margin: 4px 0 2px;
  }

  .subsection-head h5 {
    margin: 0;
    font-size: 15px;
    color: #111827;
  }

  .subsection-head p {
    margin: 4px 0 0;
    font-size: 12px;
    color: #6b7280;
  }

  .readonly-info {
    border: 1px solid #e0e7ff;
    background: #eef2ff;
    border-radius: 14px;
    padding: 12px 14px;
  }

  .readonly-title {
    font-size: 12px;
    font-weight: 700;
    color: #4338ca;
    margin-bottom: 4px;
  }

  .readonly-value {
    font-size: 13px;
    color: #3730a3;
    line-height: 1.5;
  }

  .single-child-layout {
    display: flex;
  }

  .child-card {
    width: 100%;
  }

  .child-title-wrap {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 16px;
  }

  .child-title-wrap h4 {
    margin: 0;
    font-size: 18px;
    color: #111827;
  }

  .child-title-wrap p {
    margin: 6px 0 0;
    font-size: 13px;
    color: #6b7280;
  }

  .child-badge {
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    height: 30px;
    padding: 0 10px;
    border-radius: 999px;
    background: #eef2ff;
    color: #4338ca;
    font-size: 12px;
    font-weight: 700;
    border: 1px solid #c7d2fe;
  }

  .coordinate-info {
    display: grid;
    grid-template-columns: 220px 1fr;
    gap: 16px;
    padding: 14px;
    border: 1px solid #e5e7eb;
    background: #f8fafc;
    border-radius: 16px;
    margin-bottom: 16px;
  }

  .coord-diagram {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 140px;
    border: 1px dashed #cbd5e1;
    border-radius: 14px;
    background: #ffffff;
  }

  .coord-axis-y,
  .coord-axis-x {
    font-size: 12px;
    font-weight: 700;
    color: #475569;
  }

  .coord-grid {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .coord-arrow-up,
  .coord-arrow-right {
    font-size: 16px;
    color: #4f46e5;
    font-weight: 700;
  }

  .coord-origin {
    font-size: 13px;
    font-weight: 700;
    color: #111827;
    padding: 8px 10px;
    border-radius: 10px;
    background: #eef2ff;
    border: 1px solid #c7d2fe;
  }

  .coord-desc strong {
    display: block;
    margin-bottom: 8px;
    color: #111827;
    font-size: 14px;
  }

  .coord-desc ul {
    margin: 0;
    padding-left: 18px;
    color: #475569;
    font-size: 13px;
    line-height: 1.6;
  }

  .readonly-strip {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 16px;
  }

  .readonly-mini {
    border: 1px solid #e5e7eb;
    border-radius: 14px;
    background: #ffffff;
    padding: 12px 14px;
  }

  .readonly-mini span {
    display: block;
    font-size: 12px;
    color: #64748b;
    margin-bottom: 4px;
  }

  .readonly-mini strong {
    color: #111827;
    font-size: 14px;
    word-break: break-all;
  }

  .checkbox-row {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 2px 0;
  }

  .checkbox-label {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    color: #374151;
    font-size: 14px;
    cursor: pointer;
  }

  .checkbox-label input {
    width: 16px;
    height: 16px;
    margin: 0;
  }

  .hint-text {
    font-size: 12px;
    color: #94a3b8;
  }

  .inline-guide {
    margin-top: 14px;
    padding: 12px 14px;
    border-radius: 12px;
    background: #f8fafc;
    border: 1px solid #e5e7eb;
    color: #334155;
    font-size: 13px;
    line-height: 1.5;
  }

  .inline-guide.warning {
    background: #fff7ed;
    border-color: #fdba74;
    color: #9a3412;
  }

  .inline-guide.subtle {
    background: #f0f9ff;
    border-color: #bae6fd;
    color: #0369a1;
    font-size: 12px;
    padding: 10px 12px;
    margin-top: 4px;
  }

  .preview-box {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-top: 14px;
  }

  .preview-item-wide {
    grid-column: 1 / -1;
  }

  .preview-item {
    border: 1px solid #e5e7eb;
    border-radius: 14px;
    background: #f8fafc;
    padding: 12px 14px;
  }

  .preview-item span {
    display: block;
    font-size: 12px;
    color: #64748b;
    margin-bottom: 4px;
  }

  .preview-item strong {
    color: #111827;
    font-size: 14px;
  }

  .mini-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin: 18px 0 12px;
  }

  .mini-head strong {
    color: #111827;
    font-size: 15px;
  }

  .mini-empty {
    padding: 20px;
  }

  .inner-list {
    margin-top: 0;
  }

  .delete-btn {
    height: 32px;
    border: 1px solid #fecaca;
    background: #fff5f5;
    color: #dc2626;
    border-radius: 10px;
    padding: 0 10px;
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
    flex-shrink: 0;
  }

  .delete-btn:hover {
    background: #fee2e2;
  }

  .delete-btn.small {
    height: 30px;
  }

  .more-indicator {
    text-align: center;
    font-size: 12px;
    color: #94a3b8;
    padding: 6px 0 0;
  }

  .card-edit-mode {
    border-color: #6366f1;
    box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.08);
  }

  .edit-footer {
    gap: 8px;
  }

  .field-readonly {
    height: 42px;
    border-radius: 12px;
    border: 1px solid #e5e7eb;
    background: #f8fafc;
    padding: 0 12px;
    font-size: 14px;
    color: #6b7280;
    display: flex;
    align-items: center;
    font-weight: 600;
  }

  .tag-btn-edit {
    border-color: #c7d2fe;
    color: #4338ca;
    background: #eef2ff;
  }

  .tag-btn-edit:hover {
    background: #e0e7ff;
  }

  .inline-edit-form {
    margin-top: 12px;
    padding: 14px;
    border: 1px solid #c7d2fe;
    border-radius: 14px;
    background: #f5f7ff;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .inline-edit-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding-top: 4px;
  }

  @media (max-width: 1100px) {
    .content-grid,
    .selection-summary,
    .step-summary,
    .readonly-strip,
    .preview-box {
      grid-template-columns: 1fr;
    }

    .children-top,
    .coordinate-info,
    .form-grid {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 768px) {
    .generator-overlay {
      padding: 12px;
    }

    .generator-panel {
      max-height: calc(100vh - 24px);
      border-radius: 16px;
    }

    .panel-header,
    .panel-body,
    .tab-nav,
    .step-summary,
    .selection-summary {
      padding-left: 16px;
      padding-right: 16px;
    }

    .card {
      padding: 16px;
    }

    .child-title-wrap,
    .card-head {
      flex-direction: column;
    }

    .checkbox-row {
      flex-direction: column;
      align-items: flex-start;
    }
  }
</style>
