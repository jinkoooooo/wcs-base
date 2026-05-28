package operato.logis.inventory.service;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.OutboundCalculateStrategy;
import operato.logis.inventory.dto.ItemIdentifierDto;
import operato.logis.inventory.dto.OutboundStockRequestDto;
import operato.logis.inventory.entity.TbInventoryStock;
import operato.logis.inventory.query.InventoryQueryStore;
import org.springframework.stereotype.Service;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryOutboundStockService extends AbstractQueryService {

    private final InventoryQueryStore inventoryQueryStore;

    /**
     * 출고 재고 목록 산출
     * 요청 품목 정보 및 수량을 만족하기 위한 재고 목록을 반환합니다.
     * 출고 전략 목록
     * - FIFO: Inbound-Datetime 기준 선입선출
     * - FEFO: Expired-Datetime 기준 오름차순
     * - LIFO: Inbound-Datetime 기준 후입선출
     * - MIN_MOVEMENT: 조건 충족을 위한 최소 정렬 조합
     * - MIN_PALLET: 조건 충족을 위한 최소 재고 조합
     * - EXACT_MATCH: 조건 충족 후 잔여 재고 최소화
     * 
     * @param requestParam 출고 전략 / 요청 품목 정보 및 수량 / 특정 Lot 번호(옵션) / 특정 로케이션 Group(옵션)
     * @return 출고할 재고 목록 <출고 가능한 후보 재고에 대한 ROW는 DB LOCK이 걸립니다.>
     */
    public List<TbInventoryStock> calculateOutboundStock(OutboundStockRequestDto requestParam) {
        // 입력 파라미터에 대한 유효성 검사
        if (!validateOutboundStockRequest(requestParam)) throw new ElidomRuntimeException("입력 파라미터가 유효하지 않습니다.");

        // 데드락 방지를 위한 DB 락 획득 순서 강제 (itemCode 1차, itemOwner 2차 오름차순 정렬)
        requestParam.getItemList().sort(Comparator.comparing(ItemIdentifierDto::getItemCode)
                .thenComparing(ItemIdentifierDto::getItemOwner));

        // 정렬이 보장된 상태에서 배열 문자열 추출
        String itemCodeArrayStr = requestParam.getItemList().stream()
                .map(ItemIdentifierDto::getItemCode)
                .collect(Collectors.joining(",", "{", "}"));
        String itemOwnerArrayStr = requestParam.getItemList().stream()
                .map(ItemIdentifierDto::getItemOwner)
                .collect(Collectors.joining(",", "{", "}"));
        String itemQtyArrayStr = requestParam.getItemList().stream()
                .map(dto -> String.valueOf(dto.getItemQty()))
                .collect(Collectors.joining(",", "{", "}"));

        // 출고 가능한 재고 목록 조회
        String sql = inventoryQueryStore.getCalculateOutboundStockSql();
        Map<String, Object> param = ValueUtil.newMap("outboundCalculateStrategy,itemCodeList,itemOwnerList,itemQtyList,lotNo,locGroup",
                requestParam.getOutboundCalculateStrategy().value(), itemCodeArrayStr, itemOwnerArrayStr, itemQtyArrayStr, requestParam.getLotNo(), requestParam.getLocGroup());
        List<TbInventoryStock> candidates = this.queryManager.selectListBySql(sql, param, TbInventoryStock.class, 0, 0);

        // 출고 전략에 따른 로직 분기
        switch (requestParam.getOutboundCalculateStrategy()) {
            case FIFO:
            case FEFO:
            case LIFO:
                return applySequentialSelection(candidates, requestParam.getItemList(), requestParam.getOutboundCalculateStrategy());

            case MIN_MOVEMENT:
                return applyMinMovementSelection(candidates, requestParam.getItemList());

            case MIN_PALLET:
                return applyMinPalletSelection(candidates, requestParam.getItemList());

            case EXACT_MATCH:
                return applyExactMatchSelection(candidates, requestParam.getItemList());

            default:
                throw new ElidomRuntimeException("알 수 없는 값 : OutboundCalculateStrategy");
        }
    }

    /**
     * 단순 정렬로 계산 가능한 출고 전략
     */
    private List<TbInventoryStock> applySequentialSelection(List<TbInventoryStock> candidates, List<ItemIdentifierDto> itemList, OutboundCalculateStrategy strategy) {
        // 1. 출고 전략에 따른 정렬 기준(Comparator) 동적 생성
        Comparator<TbInventoryStock> comparator = Comparator.comparing(
                TbInventoryStock::getItemPriority, // 재고 고유의 우선순위 선반영
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        // 2. 출고 전략에 따른 후순위 정렬 기준(Comparator) 동적 체이닝
        switch (strategy) {
            case FIFO: // 입고일시 오름차순
                comparator = comparator
                        .thenComparing(TbInventoryStock::getInbDatetime, Comparator.nullsLast(Comparator.naturalOrder()));
                break;

            case FEFO: // 유통기한 오름차순 -> 선입선출
                comparator = comparator
                        .thenComparing(TbInventoryStock::getExpiredDatetime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TbInventoryStock::getInbDatetime, Comparator.nullsLast(Comparator.naturalOrder()));
                break;

            case LIFO: // 입고일시 내림차순
                comparator = comparator
                        .thenComparing(TbInventoryStock::getInbDatetime, Comparator.nullsLast(Comparator.reverseOrder()));
                break;

            default:
                throw new ElidomRuntimeException("지원하지 않는 순차 할당 전략입니다. : OutboundCalculateStrategy");
        }

        // 3. 조건에 맞게 전체 후보군 정렬
        candidates.sort(comparator);

        // 4. 요구 수량 트래킹을 위한 Map 생성 (Key: owner_code, Value: 남은 필요 수량)
        Map<String, Integer> remainingQtyMap = new HashMap<>();
        for (ItemIdentifierDto item : itemList) {
            String key = item.getItemOwner() + "_" + item.getItemCode();
            remainingQtyMap.put(key, remainingQtyMap.getOrDefault(key, 0) + item.getItemQty());
        }

        // 5. 순차 할당 진행
        List<TbInventoryStock> allocatedStocks = new ArrayList<>();

        for (TbInventoryStock stock : candidates) {
            String key = stock.getItemOwner() + "_" + stock.getItemCode();
            Integer remaining = remainingQtyMap.get(key);

            // 해당 품목의 요구 수량이 아직 남아있다면
            if (remaining != null && remaining > 0) {
                int availableQty = stock.getItemQty();
                int allocateQty = Math.min(availableQty, remaining); // 둘 중 작은 값을 할당

                // 트래킹 Map 수량 차감
                remainingQtyMap.put(key, remaining - allocateQty);

                // 할당된 수량만 담은 새로운 인스턴스를 생성하여 반환 리스트에 담습니다.
                TbInventoryStock allocated = copyStockForAllocation(stock, allocateQty);
                allocatedStocks.add(allocated);

                // 모든 품목의 요구 수량이 0 이하가 되었는지 확인
                boolean allSatisfied = remainingQtyMap.values().stream().allMatch(qty -> qty <= 0);
                if (allSatisfied) {
                    break;
                }
            }
        }

        // 6. 최종 검증: 재고 부족 예외 처리
        List<String> shortageItems = remainingQtyMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> entry.getKey() + "(부족: " + entry.getValue() + "개)")
                .collect(Collectors.toList());

        if (!shortageItems.isEmpty()) {
            throw new ElidomRuntimeException("출고 가능한 가용 재고가 부족합니다. " + String.join(", ", shortageItems));
        }

        return allocatedStocks;
    }

    /**
     * 요청 목록을 충족하기 위한 최소 정렬 작업 기준의 출고 전략
     */
    private List<TbInventoryStock> applyMinMovementSelection(List<TbInventoryStock> candidates, List<ItemIdentifierDto> itemList) {
        List<TbInventoryStock> allocatedStocks = new ArrayList<>();
        Set<String> usedStockIds = new HashSet<>(); // 타 품목 간 재고 중복 할당 방지

        for (ItemIdentifierDto item : itemList) {
            String targetOwner = item.getItemOwner();
            String targetCode = item.getItemCode();
            int targetQty = item.getItemQty();

            // 1. 해당 품목의 가용 재고 필터링
            List<TbInventoryStock> itemCandidates = candidates.stream()
                    .filter(s -> s.getItemOwner().equals(targetOwner) && s.getItemCode().equals(targetCode))
                    .filter(s -> !usedStockIds.contains(s.getId()))
                    .collect(Collectors.toList());

            // [최적화 1: DFS 가지치기 극대화를 위한 정렬]
            // 1순위: 장애물이 적은 것부터 (가장 이상적인 조합을 먼저 발견하기 위함)
            // 2순위: 수량이 많은 것부터 (적은 수의 파렛트로 타겟 수량에 빨리 도달하기 위함)
            itemCandidates.sort((a, b) -> {
                int obsA = a.getAttributeA() != null ? Integer.parseInt(a.getAttributeA()) : 0;
                int obsB = b.getAttributeA() != null ? Integer.parseInt(b.getAttributeA()) : 0;
                if (obsA != obsB) {
                    return Integer.compare(obsA, obsB);
                }
                return Integer.compare(b.getItemQty(), a.getItemQty()); // 수량 내림차순
            });

            BestMovementResult bestResult = new BestMovementResult();

            // 2. 조합 백트래킹(DFS) 탐색 시작
            findMinMovementDFS(itemCandidates, targetQty, 0, 0, 0, new ArrayList<>(), bestResult);

            // 탐색 실패 검증
            if (bestResult.bestCombination.isEmpty()) {
                throw new ElidomRuntimeException("품목 [" + targetCode + "] 의 출고 가능한 가용 재고가 부족합니다.");
            }

            // 3. 최적 조합 할당 적용
            int allocatedSum = 0;
            for (TbInventoryStock stock : bestResult.bestCombination) {
                usedStockIds.add(stock.getId());

                int allocateQty = Math.min(stock.getItemQty(), targetQty - allocatedSum);
                allocatedSum += allocateQty;
                allocatedStocks.add(copyStockForAllocation(stock, allocateQty));
            }
        }

        return allocatedStocks;
    }

    /**
     * 백트래킹(DFS)을 이용한 최적 동선(최소 장애물 합) 조합 탐색 알고리즘
     */
    private void findMinMovementDFS(List<TbInventoryStock> candidates, int targetQty, int currentIndex,
                                    int currentQtySum, int currentObstacleSum,
                                    List<TbInventoryStock> currentPath, BestMovementResult result) {

        // [최적화 2: 가지치기 Pruning]
        // 현재 조합의 장애물 합이 이미 발견된 '최소 장애물 합'보다 커졌다면,
        // 더 탐색해봐야 무조건 손해이므로 즉시 이 가지(Branch)를 자릅니다.
        if (currentObstacleSum > result.minObstacles) {
            return;
        }

        // 1. 목표 수량을 채웠을 경우 (조합 완성)
        if (currentQtySum >= targetQty) {

            // [갱신 조건]
            // 1순위: 장애물 합산이 기존보다 더 적은 경우
            // 2순위: 장애물 합은 같으나, 꺼내야 할 파렛트(조각) 수가 더 적은 경우
            if (currentObstacleSum < result.minObstacles ||
                    (currentObstacleSum == result.minObstacles && currentPath.size() < result.minPallets)) {

                result.minObstacles = currentObstacleSum;
                result.minPallets = currentPath.size();
                result.bestCombination = new ArrayList<>(currentPath);
            }

            // 수량을 100% 채웠으므로, 이 가지에서 더 이상 재고를 덧붙이는 탐색은 하지 않습니다.
            return;
        }

        // 2. 아직 목표 수량을 못 채웠다면 계속해서 조합을 만들어 나갑니다.
        for (int i = currentIndex; i < candidates.size(); i++) {
            TbInventoryStock stock = candidates.get(i);
            int obs = stock.getAttributeA() != null ? Integer.parseInt(stock.getAttributeA()) : 0;

            currentPath.add(stock); // 1. 후보를 경로에 추가

            findMinMovementDFS(candidates, targetQty, i + 1,
                    currentQtySum + stock.getItemQty(),
                    currentObstacleSum + obs,
                    currentPath, result); // 2. 누적 값들을 들고 더 깊이 탐색

            currentPath.remove(currentPath.size() - 1); // 3. 되돌아오면 경로에서 제거 (Backtrack)
        }
    }

    /**
     * 요청 목록을 충족하기 위한 최소 재고 산출 출고 전략
     */
    private List<TbInventoryStock> applyMinPalletSelection(List<TbInventoryStock> candidates, List<ItemIdentifierDto> itemList) {
        // 1. 요구 수량 트래킹 맵 초기화 (Key: owner_code, Value: 남은 필요 수량)
        Map<String, Integer> remainingQtyMap = new HashMap<>();
        for (ItemIdentifierDto item : itemList) {
            String key = item.getItemOwner() + "_" + item.getItemCode();
            remainingQtyMap.put(key, remainingQtyMap.getOrDefault(key, 0) + item.getItemQty());
        }

        // 2. 가용 재고들을 파렛트(stock_id) 단위로 그룹화
        Map<String, List<TbInventoryStock>> palletGroups = candidates.stream()
                .collect(Collectors.groupingBy(TbInventoryStock::getStockId));

        List<TbInventoryStock> allocatedStocks = new ArrayList<>();
        Set<String> usedPallets = new HashSet<>(); // 이미 선택된 파렛트 ID 트래킹

        // 3. 탐욕(Greedy) 알고리즘: 남은 요구 수량이 없어질 때까지 반복
        while (remainingQtyMap.values().stream().anyMatch(qty -> qty > 0)) {
            String bestPalletId = null;
            int maxScore = -1;
            int minWaste = Integer.MAX_VALUE; // 타이브레이커: 불필요한 딸림 재고 최소화

            // 현재 가용한 파렛트들을 순회하며 가장 효율이 좋은 파렛트를 찾음
            for (Map.Entry<String, List<TbInventoryStock>> entry : palletGroups.entrySet()) {
                String palletId = entry.getKey();
                if (usedPallets.contains(palletId)) continue; // 이미 뺀 파렛트는 패스

                int score = 0;              // 이 파렛트가 만족시킬 수 있는 요구 수량 합계
                int totalQtyOnPallet = 0;   // 이 파렛트에 적재된 전체 재고 수량

                for (TbInventoryStock stock : entry.getValue()) {
                    totalQtyOnPallet += stock.getItemQty();

                    String key = stock.getItemOwner() + "_" + stock.getItemCode();
                    int needed = remainingQtyMap.getOrDefault(key, 0);

                    if (needed > 0) {
                        // 이 파렛트가 해결해줄 수 있는 '유효 수량' 누적
                        score += Math.min(stock.getItemQty(), needed);
                    }
                }

                // 점수가 없으면(필요한 재고가 하나도 없으면) 패스
                if (score == 0) continue;

                // Waste(낭비): 딸려 나오지만 이번 출고에 쓰이지 않는 잉여 수량
                int waste = totalQtyOnPallet - score;

                // [최적 파렛트 갱신 조건]
                // 1순위: 해결 수량(score)이 가장 많은 것
                // 2순위: 해결 수량이 같다면, 불필요한 낭비(waste)가 적은 것
                if (score > maxScore || (score == maxScore && waste < minWaste)) {
                    maxScore = score;
                    minWaste = waste;
                    bestPalletId = palletId;
                }
            }

            // 더 이상 요구 수량을 채워줄 수 있는 파렛트가 없다면 루프 강제 종료 (재고 부족)
            if (bestPalletId == null) {
                break;
            }

            // 4. 찾은 최적의 파렛트를 채택하고 수량 차감
            usedPallets.add(bestPalletId);
            List<TbInventoryStock> bestPalletStocks = palletGroups.get(bestPalletId);

            for (TbInventoryStock stock : bestPalletStocks) {
                String key = stock.getItemOwner() + "_" + stock.getItemCode();
                int needed = remainingQtyMap.getOrDefault(key, 0);

                if (needed > 0) {
                    int allocateQty = Math.min(stock.getItemQty(), needed);
                    remainingQtyMap.put(key, needed - allocateQty);

                    // 필요한 만큼만 떼어내서 결과 리스트에 추가
                    TbInventoryStock allocated = copyStockForAllocation(stock, allocateQty);
                    allocatedStocks.add(allocated);
                }
            }
        }

        // 5. 요구 수량을 모두 만족했는지 최종 검증
        List<String> shortageItems = remainingQtyMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> entry.getKey() + "(부족: " + entry.getValue() + "개)")
                .collect(Collectors.toList());

        if (!shortageItems.isEmpty()) {
            throw new ElidomRuntimeException("최소 파렛트 탐색 결과, 출고 가능한 가용 재고가 부족합니다. " + String.join(", ", shortageItems));
        }

        return allocatedStocks;
    }

    /**
     * 요청 목록을 충족 후 잔여 재고가 최소한이 되도록 하는 출고 전략 (재입고 방지 전략)
     */
    private List<TbInventoryStock> applyExactMatchSelection(List<TbInventoryStock> candidates, List<ItemIdentifierDto> itemList) {
        List<TbInventoryStock> allocatedStocks = new ArrayList<>();
        Set<String> usedStockIds = new HashSet<>(); // 다른 품목 탐색 시 중복 선택 방지

        for (ItemIdentifierDto item : itemList) {
            String targetOwner = item.getItemOwner();
            String targetCode = item.getItemCode();
            int targetQty = item.getItemQty();

            // 1. 해당 품목의 가용 재고만 필터링
            List<TbInventoryStock> itemCandidates = candidates.stream()
                    .filter(s -> s.getItemOwner().equals(targetOwner) && s.getItemCode().equals(targetCode))
                    .filter(s -> !usedStockIds.contains(s.getId()))
                    .collect(Collectors.toList());

            // [최적화 1] 큰 수량부터 탐색하도록 내림차순 정렬
            itemCandidates.sort((a, b) -> Integer.compare(b.getItemQty(), a.getItemQty()));

            // 탐색 결과를 담을 객체 초기화
            BestMatchResult bestMatch = new BestMatchResult();

            // 2. 백트래킹(DFS) 탐색 시작
            findExactMatchDFS(itemCandidates, targetQty, 0, 0, new ArrayList<>(), bestMatch);

            // 3. 탐색 결과 적용
            if (bestMatch.bestCombination.isEmpty()) {
                throw new ElidomRuntimeException("품목 [" + targetCode + "] 의 출고 가능한 가용 재고가 없습니다.");
            }

            // 최적 조합으로 선정된 재고들을 할당 리스트에 추가하고 사용 처리
            int allocatedSum = 0;
            for (TbInventoryStock stock : bestMatch.bestCombination) {
                usedStockIds.add(stock.getId());

                // 실제 할당 수량 계산 (마지막 파렛트에서 수량이 초과될 경우 잘라내기 위함)
                int allocateQty = Math.min(stock.getItemQty(), targetQty - allocatedSum);
                if (allocateQty <= 0) allocateQty = stock.getItemQty(); // 오버피킹(Over-picking) 허용 시 그대로 반환

                allocatedSum += allocateQty;
                allocatedStocks.add(copyStockForAllocation(stock, allocateQty));
            }
        }

        return allocatedStocks;
    }

    /**
     * 백트래킹(DFS)을 이용한 최적 수량 조합 탐색 알고리즘
     */
    private void findExactMatchDFS(List<TbInventoryStock> candidates, int targetQty, int currentIndex,
                                   int currentSum, List<TbInventoryStock> currentPath, BestMatchResult result) {

        // 이미 완벽한 매칭(오차 0)을 찾았다면 불필요한 추가 연산을 중단 (조기 종료)
        if (result.foundExact) return;

        // 현재까지 조합된 수량과 목표 수량의 오차 계산
        int diff = Math.abs(currentSum - targetQty);

        // [최적 조합 갱신 조건]
        // 1순위: 오차가 더 적은 경우
        // 2순위: 오차가 같다면, 파렛트(조각) 수가 더 적은 경우 (15개짜리 1개가 5개짜리 3개보다 좋음)
        if (currentSum > 0) {
            if (diff < result.minDiff || (diff == result.minDiff && currentPath.size() < result.bestCombination.size())) {
                result.minDiff = diff;
                result.bestCombination = new ArrayList<>(currentPath);

                // 오차가 0이면 정확한 매칭을 찾은 것
                if (diff == 0) {
                    result.foundExact = true;
                    return;
                }
            }
        }

        // [최적화 2 - 가지치기 Pruning]
        // 현재 합계가 이미 타겟을 초과했는데, 그 초과한 오차가 이미 발견된 '최소 오차'보다 크다면
        // 이 가지(Branch)는 더 깊이 파봐야 손해이므로 즉시 탐색을 종료하고 돌아감
        if (currentSum > targetQty && (currentSum - targetQty) >= result.minDiff) {
            return;
        }

        // 조합 탐색 루프
        for (int i = currentIndex; i < candidates.size(); i++) {
            if (result.foundExact) break; // 완벽한 조합이 발견되면 형제 노드 탐색도 중지

            TbInventoryStock stock = candidates.get(i);

            currentPath.add(stock); // 1. 후보를 경로에 추가하고
            // 2. 더 깊은 곳으로 재귀 탐색
            findExactMatchDFS(candidates, targetQty, i + 1, currentSum + stock.getItemQty(), currentPath, result);
            currentPath.remove(currentPath.size() - 1); // 3. 탐색이 끝나면 후보를 경로에서 뺌 (Backtrack)
        }
    }

    // 객체 복사
    private TbInventoryStock copyStockForAllocation(TbInventoryStock original, int allocateQty) {
        TbInventoryStock copy = new TbInventoryStock();
        copy.setId(original.getId());
        copy.setStockId(original.getStockId());
        copy.setSku(original.getSku());
        copy.setItemOwner(original.getItemOwner());
        copy.setItemCode(original.getItemCode());
        copy.setDomainId(original.getDomainId());
        copy.setInbDatetime(original.getInbDatetime());
        copy.setExpiredDatetime(original.getExpiredDatetime());
        copy.setLotNo(original.getLotNo());
        copy.setAttributeA(original.getAttributeA());
        copy.setItemQty(allocateQty); // 이 객체의 수량은 창고에 있는 전체 수량이 아니라 '이번 출고에 할당된 수량'을 의미합니다.

        return copy;
    }

    private boolean validateOutboundStockRequest(OutboundStockRequestDto requestParam) {
        if (!validateItemIdentifier(requestParam.getItemList())) {
            return false;
        }

        if (ValueUtil.isEmpty(requestParam.getOutboundCalculateStrategy())) {
            return false;
        }

        return true;
    }

    private boolean validateItemIdentifier(List<ItemIdentifierDto> itemList) {
        if (ValueUtil.isEmpty(itemList) || itemList.isEmpty()) {
            return false;
        }

        for (ItemIdentifierDto item : itemList) {
            if (ValueUtil.isEmpty(item.getItemOwner()) || ValueUtil.isEmpty(item.getItemCode()) || ValueUtil.isEmpty(item.getItemQty())) {
                return false;
            }
        }

        return true;
    }

    // DFS 탐색 상태를 기록할 내부 클래스
    private static class BestMatchResult {
        List<TbInventoryStock> bestCombination = new ArrayList<>();
        int minDiff = Integer.MAX_VALUE;
        boolean foundExact = false;
    }

    // DFS 탐색 상태를 기록할 클래스
    private static class BestMovementResult {
        List<TbInventoryStock> bestCombination = new ArrayList<>();
        int minObstacles = Integer.MAX_VALUE; // 최소 장애물 수 합
        int minPallets = Integer.MAX_VALUE;   // 최소 파렛트 이동 수 (타이브레이커)
    }
}