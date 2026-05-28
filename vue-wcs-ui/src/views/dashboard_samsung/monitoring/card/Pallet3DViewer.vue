<template>
  <div ref="canvasContainer" class="three-container"></div>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref, watch } from 'vue';
  import * as THREE from 'three';
  import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';

  // 부모에게서 받을 데이터 (박스 수량 및 규격, 단위: mm)
  const props = defineProps({
    count: { type: Number, default: 0 },
    boxWidth: { type: Number, default: 300 },
    boxLength: { type: Number, default: 400 },
    boxHeight: { type: Number, default: 250 },
  });

  const canvasContainer = ref<HTMLDivElement | null>(null);

  let scene: THREE.Scene;
  let camera: THREE.PerspectiveCamera;
  let renderer: THREE.WebGLRenderer;
  let controls: OrbitControls;
  let animationId: number;
  const boxGroup = new THREE.Group();

  // EL18 팔레트 규격 (미터 단위)
  const PALLET_WIDTH = 1.8; // 1800mm
  const PALLET_DEPTH = 1.6; // 1600mm
  const PALLET_HEIGHT = 0.15;

  // [추가됨] 화면 크기 변경 대응 함수
  const onWindowResize = () => {
    if (!canvasContainer.value || !camera || !renderer) return;
    const width = canvasContainer.value.clientWidth;
    const height = canvasContainer.value.clientHeight;
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height);
  };

  const initThree = () => {
    if (!canvasContainer.value) return;

    // 캔버스 초기화 (안전장치)
    while (canvasContainer.value.firstChild) {
      canvasContainer.value.removeChild(canvasContainer.value.firstChild);
    }

    // 1. 장면
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x021a30);

    // 2. 카메라 설정
    const width = canvasContainer.value.clientWidth;
    const height = canvasContainer.value.clientHeight;
    camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 100);

    // [수정됨] 카메라 위치를 더 가깝게 조정 (Zoom In)
    // 기존 (4, 4, 4) -> (2.8, 3.2, 3.2)
    camera.position.set(2.8, 3.2, 3.2);
    camera.lookAt(0, 0, 0);

    // 3. 렌더러
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(width, height);
    renderer.setPixelRatio(window.devicePixelRatio); // 고해상도 대응
    renderer.shadowMap.enabled = true;
    canvasContainer.value.appendChild(renderer.domElement);

    // 4. 조명
    scene.add(new THREE.AmbientLight(0xffffff, 0.5));
    const dirLight = new THREE.DirectionalLight(0xaaccff, 1.5);
    dirLight.position.set(5, 10, 7);
    scene.add(dirLight);

    // 5. 팔레트 (EL18)
    const palletGeometry = new THREE.BoxGeometry(PALLET_WIDTH, PALLET_HEIGHT, PALLET_DEPTH);
    const palletMaterial = new THREE.MeshStandardMaterial({
      color: 0x1e90ff,
      roughness: 0.4,
      metalness: 0.6,
    });
    const pallet = new THREE.Mesh(palletGeometry, palletMaterial);
    pallet.position.y = -PALLET_HEIGHT / 2;
    scene.add(pallet);
    scene.add(boxGroup);

    // 6. 컨트롤
    controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.minDistance = 2; // 너무 가까이 줌인 방지
    controls.maxDistance = 10; // 너무 멀리 줌아웃 방지

    updateStacking();
    animate();

    // 리사이즈 이벤트 등록
    window.addEventListener('resize', onWindowResize);
  };

  const updateStacking = () => {
    while (boxGroup.children.length > 0) {
      const child = boxGroup.children[0];
      if (child instanceof THREE.Mesh) {
        child.geometry.dispose();
        if (Array.isArray(child.material)) child.material.forEach((m: any) => m.dispose());
        else child.material.dispose();
      }
      boxGroup.remove(child);
    }

    const boxW = props.boxWidth / 1000;
    const boxD = props.boxLength / 1000;
    const boxH = props.boxHeight / 1000;

    const cols = Math.floor(PALLET_WIDTH / boxW);
    const rows = Math.floor(PALLET_DEPTH / boxD);
    const boxesPerLayer = cols * rows;

    if (boxesPerLayer === 0) return;

    for (let i = 0; i < props.count; i++) {
      const layerIndex = Math.floor(i / boxesPerLayer);
      const indexInLayer = i % boxesPerLayer;
      const colIndex = indexInLayer % cols;
      const rowIndex = Math.floor(indexInLayer / cols);

      const startX = -PALLET_WIDTH / 2 + boxW / 2;
      const startZ = -PALLET_DEPTH / 2 + boxD / 2;

      const x = startX + colIndex * boxW;
      const y = layerIndex * boxH + boxH / 2;
      const z = startZ + rowIndex * boxD;

      createBox(x, y, z, boxW, boxH, boxD);
    }
  };

  const createBox = (x: number, y: number, z: number, w: number, h: number, d: number) => {
    const geometry = new THREE.BoxGeometry(w, h, d);
    const material = new THREE.MeshPhysicalMaterial({
      color: 0x87ceeb,
      transparent: true,
      opacity: 0.2,
      roughness: 0.1,
      metalness: 0.1,
      transmission: 0.5,
    });
    const box = new THREE.Mesh(geometry, material);
    box.position.set(x, y, z);

    const edges = new THREE.EdgesGeometry(geometry);
    const line = new THREE.LineSegments(edges, new THREE.LineBasicMaterial({ color: 0x00ffff }));
    box.add(line);

    boxGroup.add(box);
  };

  const animate = () => {
    animationId = requestAnimationFrame(animate);
    controls.update();
    renderer.render(scene, camera);
  };

  watch(
    () => [props.count, props.boxWidth, props.boxHeight, props.boxLength],
    () => {
      updateStacking();
    },
  );

  onMounted(() => {
    setTimeout(initThree, 100);
  });

  onUnmounted(() => {
    cancelAnimationFrame(animationId);
    window.removeEventListener('resize', onWindowResize);
    if (renderer) {
      renderer.dispose();
      renderer.forceContextLoss();
    }
  });
</script>

<style scoped>
  /* [수정됨] 컨테이너 높이를 600px로 확대 */
  .three-container {
    width: 100%;
    height: 600px;
    background: #021a30;
    border-radius: 8px;
    overflow: hidden;
  }
</style>
