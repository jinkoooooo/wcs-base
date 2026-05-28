<template>
  <PageWrapper>
    <div style="display: flex; flex-direction: column; height: calc(100vh - 100px)">
      <!-- 위쪽 그리드 -->
      <div :style="{ flex: props.masterRatio, overflow: 'auto' }">
        <CommonPage
          v-if="masterMetas"
          ref="masterRef"
          :metaUrl="masterMetaUrl"
          :metas="masterMetas"
          :limit="masterLimit"
          :fetchHandler="masterFetchHandler"
          @gridClicked="onGridClicked"
        >
          <slot name="masterButtons"></slot>
        </CommonPage>
      </div>

      <!-- 아래쪽 그리드 -->
      <div :style="{ flex: props.detailRatio, overflow: 'auto' }">
        <CommonPage
          ref="detailRef"
          :metaUrl="detailMetaUrl"
          :metas="detailMetas"
          :limit="detailLimit"
          :showSearchForm="false"
          :fetchHandler="detailFetchHandler"
          :menuMetaProp="detailMetas"
        >
          <slot name="detailButtons"></slot>
        </CommonPage>
      </div>
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { computed, ref, onMounted, toRaw, unref, markRaw } from 'vue';

  import { PageWrapper } from '/@/components/Page';
  import CommonPage from '/src/views/common/CommonPage.vue';
  import { getSearchList, getCommonGetListApi } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useRoute } from 'vue-router';

  const { createMessage } = useMessage();
  const route = useRoute();

  const props = defineProps({
    menuName: { type: String },

    masterMetaUrl: { type: String },
    masterFetchHandler: { type: Function },
    masterLimit: { type: Number },
    masterRatio: { type: Number, default: 6 },
    // master에서 노출할 컬럼만 지정
    masterVisibleFields: {
      type: Array as () => string[],
      default: () => [],
    },

    detailMetaUrl: { type: String },
    detailFetchHandler: { type: Function },
    detailLimit: { type: Number },
    detailRatio: { type: Number, default: 4 },
    // detail에서 노출할 컬럼만 지정
    detailVisibleFields: {
      type: Array as () => string[],
      default: () => [],
    },

    detailResourceUrl: { type: String },
    detailColName: { type: String, default: 'id' }, //조회 조건 컬럼
  });

  // 메타정보
  const masterMetas = ref<any>(null);
  const detailMetas = ref<any>(null);

  const masterRef = ref();
  const detailRef = ref();
  const record = ref<any>(null);

  // detail 데이터 조회 url 기본값
  const defaultDetailResourceUrl = computed(() => {
    return detailRef.value.resourceUrl.replace(':id', record.value);
  });

  // master Grid 클릭 시, detail Grid 조회 이벤트
  const onGridClicked = (clickedRow: any) => {
    record.value = clickedRow[props.detailColName];
    detailRef.value?.grid?.fetch();
  };

  // detail 데이터 조회 실제함수
  const detailFetchHandler = props.detailFetchHandler || defaultDetailFetchHandler;

  // detail 데이터 조회 기본함수
  async function defaultDetailFetchHandler(
    page: number,
    limit: number,
    sorters = [{ field: 'created_at', ascending: false }],
  ) {
    if (!record.value) {
      return {
        total: 0,
        records: [],
      };
    }

    const filterCols = [
      {
        name: props.detailColName,
        operator: 'eq',
        value: record.value,
        relation: false,
      },
    ];

    const requestParams = {
      query: JSON.stringify(filterCols),
      sort: JSON.stringify(sorters),
      page,
      limit,
    };

    const url = props.detailResourceUrl || defaultDetailResourceUrl.value;

    if (!url) {
      createMessage.error('상세 조회 URL이 없습니다.');
      return;
    }

    const response = await getSearchList(url, requestParams);
    console.log('vertical double detail response.items = ', response.items);
    return {
      total: response.total,
      records: response.items,
    };
  }

  // 사용할 메타 컬럼만 필터링
  function filterMetasColumns(metas: any, whitelist: string[]) {
    if (!metas || !whitelist) return metas;
    // Proxy 제거(원본 추출)
    const raw = toRaw(unref(metas));

    // 깊은 복제
    const clone =
      typeof structuredClone === 'function'
        ? structuredClone(raw)
        : JSON.parse(JSON.stringify(raw));

    if (!Array.isArray(whitelist) || whitelist.length === 0) {
      return markRaw(clone);
    }

    // 원하는 컬럼만 필터링
    clone.columns = (raw.columns || []).filter((col: any) => {
      const key = col.name;
      return whitelist.includes(key);
    });

    // 반응성 제외
    return markRaw(clone);
  }

  // 라이프사이클
  onMounted(async () => {
    // 메타 데이터 조회
    try {
      let metas: any;

      if (props.masterMetaUrl) {
        metas = await getCommonGetListApi(props.masterMetaUrl, null);
      } else {
        metas = await getCommonGetListApi(
          `/menus/${String(props.menuName ? props.menuName : route.name)}/named_meta`,
          null,
        );
      }
      masterMetas.value = filterMetasColumns(metas, props.masterVisibleFields);

      if (props.detailMetaUrl) {
        metas = await getCommonGetListApi(props.detailMetaUrl, null);
      } else {
        metas = await getCommonGetListApi(
          `/menus/${String(props.menuName ? props.menuName : route.name)}/named_meta`,
          null,
        );
      }
      detailMetas.value = filterMetasColumns(metas, props.detailVisibleFields);
    } catch (e) {
      createMessage.error('메타 정보 조회에 실패했습니다.');
    }
  });

  defineExpose({
    masterRef,
    detailRef,
  });
</script>
