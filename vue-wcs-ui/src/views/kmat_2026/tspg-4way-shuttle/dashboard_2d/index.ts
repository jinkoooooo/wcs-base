/**
 * 4-Way Shuttle WCS 모듈 인덱스
 */

// API
export * from './api/types';
export * from './api/shuttle';
export * from './api/websocket';

// Store
export { useShuttleStore } from './store/shuttleStore';

// 컴포넌트 Lazy Loading
export const MapEditor = () => import('./editor/MapEditor.vue');
export const Dashboard2D = () => import('./dashboard/Dashboard2D.vue');
