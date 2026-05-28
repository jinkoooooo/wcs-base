import { defineStore } from 'pinia';

export const useFetchStore = defineStore('main', {
  state: () => ({
    isUpdatingRows: false,
  }),
});