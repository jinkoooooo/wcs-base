/**
 * 설비 타입 기본 SVG 아이콘(프론트 내장)
 * - Vite의 `?raw`로 SVG 원문을 가져와 Data URL로 변환한다.
 * - DB icon_data_2d(텍스트)에 저장하면, 에디터/대시보드 모두 같은 방식으로 렌더링 가능.
 */

import bcrSvg from './icons/bcr.svg?raw';
import bufferSvg from './icons/buffer.svg?raw';
import conveyorSvg from './icons/conveyor.svg?raw';
import craneSvg from './icons/crane.svg?raw';
import gateSvg from './icons/gate.svg?raw';
import lifterSvg from './icons/lifter.svg?raw';
import pillarSvg from './icons/pillar.svg?raw';
import rackSvg from './icons/rack.svg?raw';
import shuttleSvg from './icons/shuttle.svg?raw';
import stvSvg from './icons/stv.svg?raw';
import workstationSvg from './icons/workstation.svg?raw';

function svgToDataUrl(svg: string): string {
  // SVG를 안전하게 인코딩해서 data:image/svg+xml URL로 사용
  // (base64보다 용량/가독성 측면에서 유리)
  const encoded = encodeURIComponent(svg)
    .replace(/'/g, '%27')
    .replace(/"/g, '%22');
  return `data:image/svg+xml;charset=utf-8,${encoded}`;
}

export const defaultTypeIconDataUrl: Record<string, string> = {
  // 정적 설비 (MapEditor용)
  CONVEYOR: svgToDataUrl(conveyorSvg),
  LIFTER: svgToDataUrl(lifterSvg),
  BCR: svgToDataUrl(bcrSvg),
  RACK: svgToDataUrl(rackSvg),
  PILLAR: svgToDataUrl(pillarSvg),
  BUFFER: svgToDataUrl(bufferSvg),
  WORKSTATION: svgToDataUrl(workstationSvg),
  GATE: svgToDataUrl(gateSvg),
  // 동적 설비 (Dashboard용)
  SHUTTLE: svgToDataUrl(shuttleSvg),
  STV: svgToDataUrl(stvSvg),
  CRANE: svgToDataUrl(craneSvg),
};
