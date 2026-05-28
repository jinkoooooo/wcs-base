<template>
  <div
    style="width: 100vw; height: 100vh; background-color: #1e1e1e; position: relative; overflow: hidden;"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @contextmenu.prevent
  >
    <!-- 랙 조회 조건 입력 UI -->
    <RackSearchPanel
      @search="handleSearchRacks"
    />

    <!-- 입고 로케이션 시뮬레이션 UI -->
    <InboundSimulationPanel
      @simulate="handleInboundSimulation"
    />

    <!-- 선택된 랙 UI -->
    <SelectedRackPanel
      :selected-codes="selectedCodes"
      @clear="clearSelection"
    />

    <!-- 선택된 랙들의 속성값 표시 및 수정 UI -->
    <RackPropertiesPanel
      :selected-racks="selectedRackObjects"
    />

    <div :style="selectionBoxStyle"></div>

    <TresCanvas clear-color="#202020" window-size>
      <!-- 카메라 -->
      <TresPerspectiveCamera ref="cameraRef" :position="[20, 15, 30]" :look-at="[10, 0, 10]" />
      <OrbitControls ref="controlsRef" :enable-pan="false" />

      <!-- 조명 -->
      <TresAmbientLight :intensity="1" />
      <TresDirectionalLight :position="[5, 5, 5]" :intensity="2" />
      <TresGridHelper :args="[50, 50, '#444444', '#222222']" />

      <!-- 3D 객체 -->
      <TresGroup v-if="isModelLoaded">
        <HighRack
          v-for="rack in rackList"
          :key="rack.code"
          :code="rack.code"
          :position="rack.position"
          :rotation-y="rack.rotationY"
          :isStock="rack.isStock"
          :box-model="rawBoxScene"
          :pallet-model="rawPalletScene"
          :is-selected="selectedCodes.includes(rack.code)"
          @select="handleSingleSelect"
        />
        <HighRackBackground
          v-for="rack in rackBackgroundList"
          :position="rack.position"
          :rotation-y="rack.rotationY"
          :high-rack-background-model="rawRackScene"
        />
        <AGF
          v-for="agf in agfList"
          :position="agf.position"
          :rotation-y="agf.rotationY"
          :agf-model="rawAgfScene"
        />
      </TresGroup>
    </TresCanvas>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, provide } from 'vue';
import { TresCanvas } from '@tresjs/core';
import { OrbitControls } from '@tresjs/cientos';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { getCommonGetApi, getCommonPostApi } from '/@/api/common/api';
import HighRack from './components/HighRack.vue';
import HighRackBackground from './components/HighRackBackground.vue';
import AGF from './components/AGF.vue';
import SelectedRackPanel from './components/SelectedRackPanel.vue';
import RackPropertiesPanel from './components/RackPropertiesPanel.vue';
import RackSearchPanel from './components/RackSearchPanel.vue';
import InboundSimulationPanel from './components/InboundSimulationPanel.vue';
import { useCameraMove } from './utils/camera';
import { useMultiSelection } from './utils/selection';

const fieldDefinitions = [
  { key: 'loc_group', label: 'Loc Group', type: 'string' },
  { key: 'loc_col', label: 'Column', type: 'number', readonly: true },
  { key: 'loc_row', label: 'Row', type: 'number', readonly: true },
  { key: 'loc_level', label: 'Level', type: 'number', readonly: true },
  { key: 'loc_deep', label: 'Deep', type: 'number', readonly: true },
  { key: 'loc_side', label: 'Side', type: 'string', readonly: true },
  { key: 'item_type', label: 'Item Type', type: 'string' },
  { key: 'item_group', label: 'Item Group', type: 'string' },
  { key: 'item_grade', label: 'Item Grade', type: 'number' },
  { key: 'max_height', label: 'Max Height', type: 'number' },
  { key: 'max_weight', label: 'Max Weight', type: 'number' },
  { key: 'is_enabled', label: 'Enabled', type: 'boolean' },
  { key: 'equip_type', label: 'Equip Type', type: 'string' },
  { key: 'equip_code', label: 'Equip Code', type: 'string' },
  { key: 'dest_node_code', label: 'Dest Node', type: 'string' },
  { key: 'is_path', label: 'Is Path', type: 'boolean' },
];
provide('rackFieldDefinitions', fieldDefinitions);

// 3D 모델
const rackList = ref([]); // 하이랙 Cell 렌더링 객체 목록
const rackBackgroundList = ref([]); // 하이랙 구조물 렌더링 객체 목록
const agfList = ref([]); // AGF 렌더링 객체 목록
const isModelLoaded = ref(false); // 3D 모델 로딩 완료 여부
let rawRackScene = null; // 하이랙 모델
let rawBoxScene = null; // 박스 모델
let rawPalletScene = null; // Pallet 모델
let rawAgfScene = null; // AGF 모델

// 카메라와 컨트롤 접근용 Ref
const cameraRef = ref(null);
const controlsRef = ref(null);

// 변경사항 추적을 위한 초기 데이터 스냅샷
let initialRacksMap = {};

// 카메라 조작 기능 연결
useCameraMove(cameraRef, controlsRef);

// 마우스를 사용한 객체 선택 기능 연결
const {
  selectedCodes,
  selectionBoxStyle,
  handleSingleSelect,
  onPointerDown,
  onPointerMove,
  onPointerUp,
  clearSelection
} = useMultiSelection(cameraRef, rackList);

const toCamelCase = (str) => {
  return str.replace(/_([a-z])/g, (g) => g[1].toUpperCase());
};

// 랙 조건문 조회 이벤트 처리 (하이랙 Cell 렌더링)
const handleSearchRacks = async (conditions) => {
  try {
    console.log('검색 조건:', conditions);

    let apiUrl = '/inventory_location/get_location_list_by_condition';

    // 검색 조건이 존재하면 URL에 파라미터로 붙임
    if (conditions && Object.keys(conditions).length > 0) {
      const camelCaseConditions = {};

      // snake_case -> camelCase 키값 변환
      for (const key in conditions) {
        camelCaseConditions[toCamelCase(key)] = conditions[key];
      }

      // 변환된 객체를 URL 쿼리 스트링
      const queryString = new URLSearchParams(camelCaseConditions).toString();
      apiUrl += `?${queryString}`;
    }

    // 데이터 조회 API
    const response = await getCommonGetApi(apiUrl, null);

    // 3D 모델 생성
    const mappedList = response.map(loc => {
      // Y축 회전값 계산
      // 혹시 랙이나 박스가 회전할 때 제자리가 아니라 큰 원을 그리면서 엉뚱한 곳으로 날아가는 현상이 있다면,
      // 이는 모델링 파일(.glb) 자체의 중심축(Pivot)이 정중앙(0, 0, 0)이 아니라 한쪽 구석으로 치우쳐 있어서 발생하는 문제입니다.
      // 이 경우에는 블렌더 같은 툴에서 중심축을 중앙으로 맞추거나, 코드 단에서 오프셋을 줘야 합니다.
      const degree = loc.rotation_cw || 0;
      const radianCW = -(degree * Math.PI / 180);

      // 스냅샷 찍을 때 렌더링용 참조값들은 제외하기 위해 객체 분리
      const businessProperties = {
        id: loc.id,
        loc_group: loc.loc_group,
        loc_col: loc.loc_col,
        loc_row: loc.loc_row,
        loc_level: loc.loc_level,
        loc_deep: loc.loc_deep,
        loc_side: loc.loc_side,
        item_type: loc.item_type,
        item_group: loc.item_group,
        item_grade: loc.item_grade,
        max_height: loc.max_height,
        max_weight: loc.max_weight,
        is_enabled: loc.is_enabled,
        equip_type: loc.equip_type,
        equip_code: loc.equip_code,
        dest_node_code: loc.dest_node_code,
        is_path: loc.is_path
      };

      return {
        ...businessProperties,
        code: loc.loc_code,
        position: [
          loc.position_x,
          loc.position_y,
          loc.position_z
        ],
        rotationY: radianCW,
        isStock: !loc.is_path
      }
    })

    // 반응성 배열에 입력
    rackList.value = mappedList;

    // 초기 스냅샷 보관
    initialRacksMap = {};
    mappedList.forEach(rack => {
      initialRacksMap[rack.id] = JSON.parse(JSON.stringify(rack));
    });

    // 재검색을 했으므로 기존에 선택된 객체들(초록 박스)을 초기화합니다.
    clearSelection();

  } catch (error) {
    console.error('검색 중 에러 발생:', error);
  }
};

// 입고 로케이션 시뮬레이션 이벤트 처리
const handleInboundSimulation = async (payload) => {
  try {
    // API 필수 항목
    const requestBody = {
      item_list: payload.itemList.map(item => ({
        item_owner: item.itemOwner,
        item_code: item.itemCode
      })),
      item_type: payload.itemType,
      total_height: payload.totalHeight,
      total_weight: payload.totalWeight
    };

    // API 선택 항목
    if (payload.locGroup) {
      requestBody.loc_group = payload.locGroup;
    }

    console.log('백엔드로 전송될 JSON Body:', requestBody);

    // 데이터 조회 API 호출
    const response = await getCommonPostApi('/inventory_location/calculate_inbound_location', requestBody);

    // 응답 검증 및 화면 처리
    if (response) {
      const targetLocCode = response.loc_code;

      if (targetLocCode) {
        clearSelection();
        selectedCodes.value = [targetLocCode];
      } else {
        alert('서버에서 응답을 받았으나, 코드를 식별할 수 없습니다.');
      }
    } else {
      alert('조건에 맞는 빈 로케이션을 찾을 수 없습니다.');
    }

  } catch (error) {
    console.error('시뮬레이션 중 에러 발생:', error);
    if (error.response && error.response.status === 404) {
      alert('할당 가능한 빈 로케이션이 없습니다. (재고 가득 참 또는 조건 불일치)');
    } else {
      alert('시뮬레이션 처리 중 서버 오류가 발생했습니다.');
    }
  }
};

// 전체 rackList에서 선택된 랙들의 원본 객체만 뽑아내는 Computed 속성
const selectedRackObjects = computed(() => {
  return rackList.value.filter(rack => selectedCodes.value.includes(rack.code));
});

// 저장 이벤트 처리
const saveAllModifiedProperties = async () => {
  const allUpdatedData = [];

  // 원본 데이터를 순회하며 초기 스냅샷과 비교
  rackList.value.forEach(currentRack => {
    const originalRack = initialRacksMap[currentRack.id];
    if (!originalRack) return;

    let hasChanged = false;
    const payload = { id: currentRack.id };

    fieldDefinitions.forEach(field => {
      const currentVal = currentRack[field.key];
      const originalVal = originalRack[field.key];
      payload[field.key] = currentVal;

      if (currentVal !== originalVal) {
        hasChanged = true;
      }
    });

    if (hasChanged) {
      allUpdatedData.push(payload);
    }
  });

  console.log('서버로 전송될 전체 수정된 데이터:', allUpdatedData);

  if (allUpdatedData.length === 0) {
    alert('변경된 내용이 없습니다.');
    return;
  }

  try {
    await getCommonPostApi('/inventory_location/set_location_properties', allUpdatedData);

    alert(`성공적으로 저장되었습니다!`);

    // 💡 저장 성공 후, 현재 상태를 새로운 스냅샷으로 갱신
    rackList.value.forEach(rack => {
      initialRacksMap[rack.id] = JSON.parse(JSON.stringify(rack));
    });

  } catch (error) {
    console.error('속성 저장 중 에러 발생:', error);
    alert('저장 중 오류가 발생했습니다.');
  }
};

// 자식에게 저장 함수 주입
provide('handleGlobalSave', saveAllModifiedProperties);

onMounted(async () => {
  try {
    // 3D 모델 로드
    const loader = new GLTFLoader();
    const [rackGLTF, boxGLTF, palletGLTF, agfGLTF] = await Promise.all([
      loader.loadAsync('/3d/HighRack.glb'),
      loader.loadAsync('/3d/Box.glb'),
      loader.loadAsync('/3d/Pallet.glb'),
      loader.loadAsync('/3d/AGF.glb')
    ])
    rawRackScene = rackGLTF.scene;
    rawBoxScene = boxGLTF.scene;
    rawPalletScene = palletGLTF.scene;
    rawAgfScene = agfGLTF.scene;

    // 3D 모델 로드 완료 (TresGroup Rendering 시작)
    isModelLoaded.value = true;

    // 필요 데이터 조회 API 호출
    const backgroundResponse = await getCommonGetApi('/inventory_equipment/get_equipment_list/HR', null);
    const equipmentResponse = await getCommonGetApi('/inventory_equipment/get_equipment_list/AGF', null);

    // 하이랙 Cell 렌더링
    await handleSearchRacks();

    // 하이랙 구조물 렌더링
    rackBackgroundList.value = backgroundResponse.map(comp => {
      const degree = comp.rotation_cw || 0;
      const radianCW = -(degree * Math.PI / 180);

      return {
        position: [
          comp.position_x,
          comp.position_y,
          comp.position_z
        ],
        rotationY: radianCW
      }
    })

    // AGF 렌더링
    agfList.value = equipmentResponse.map(comp => {
      const degree = comp.rotation_cw || 0;
      const radianCW = -(degree * Math.PI / 180);

      return {
        position: [
          comp.position_x,
          comp.position_y,
          comp.position_z
        ],
        rotationY: radianCW
      }
    })
  } catch (error) {
    console.error('로딩 또는 API 호출 중 에러 발생:', error);
  }
})
</script>
