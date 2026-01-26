-- ============================================================
-- 카테고리 초기화 SQL 스크립트
-- 농가 직송 마켓 기준 카테고리 구조 (Level 0 ~ 2)
-- ============================================================
--
-- ⚠️ 기존 카테고리 데이터 삭제 (외래 키 제약 조건 해결)
-- ============================================================

-- 외래 키 체크 일시적으로 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 기존 카테고리 데이터 모두 삭제 (Level 2 → Level 1 → Level 0 순서)
DELETE FROM categories WHERE level = 2;
DELETE FROM categories WHERE level = 1;
DELETE FROM categories WHERE level = 0;

-- 외래 키 체크 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 카테고리 데이터 삽입
-- ============================================================
-- 
-- ✅ 카테고리 구조 요약
-- 
-- Level 0: 농가직송 농산물 (루트)
-- 
-- Level 1 (구매 관점 대분류):
--   - 과일
--   - 채소
--   - 곡물·잡곡
--   - 버섯·특용작물
--   - 견과·건과
--   - 산지 특산물
--   - 축산물
-- 
-- Level 2 (사용/조리/소비 맥락):
--   - 과일: 제철 과일, 저장 과일, 베리류
--   - 채소: 쌈·샐러드 채소, 국·찌개 채소, 볶음·구이 채소, 뿌리채소
--   - 축산물: 계란, 육류, 유제품
--   - 기타: 각 대분류별 기본 하위 카테고리
-- 
-- ⚠️ 실행 순서 중요: 1 → 2 → 3
-- 
-- ⚠️ 운영 규칙:
--   - 상품은 반드시 level=2 카테고리에만 연결
--   - level=0~1은 직접 상품 매핑 금지
--   - code는 절대 변경하지 않음 (name만 변경 가능)
-- 
-- ============================================================

-- ============================================================
-- 1️⃣ Level 0 — 루트
-- ============================================================

-- Level 0: Root
INSERT INTO categories (id, name, code, parent_id, level, sort_order)
VALUES (
  uuid_to_bin(UUID()),
  '농가직송 농산물',
  'AGRI_DIRECT',
  NULL,
  0,
  1
);

-- ============================================================
-- 2️⃣ Level 1 — 대분류 (구매 기준)
-- ============================================================

-- Level 1: 구매 기준 대분류
SET @root_id = (SELECT id FROM categories WHERE code = 'AGRI_DIRECT' LIMIT 1);

INSERT INTO categories (id, name, code, parent_id, level, sort_order)
VALUES
  (uuid_to_bin(UUID()), '과일', 'AGRI_FRUIT', @root_id, 1, 1),
  (uuid_to_bin(UUID()), '채소', 'AGRI_VEGETABLE', @root_id, 1, 2),
  (uuid_to_bin(UUID()), '곡물·잡곡', 'AGRI_GRAIN', @root_id, 1, 3),
  (uuid_to_bin(UUID()), '버섯·특용작물', 'AGRI_SPECIAL', @root_id, 1, 4),
  (uuid_to_bin(UUID()), '견과·건과', 'AGRI_NUT', @root_id, 1, 5),
  (uuid_to_bin(UUID()), '산지 특산물', 'AGRI_LOCAL_SPECIALTY', @root_id, 1, 6),
  (uuid_to_bin(UUID()), '축산물', 'AGRI_LIVESTOCK', @root_id, 1, 7);

-- ============================================================
-- 3️⃣ Level 2 — 사용 / 소비 맥락 기준
-- ============================================================

-- 🍎 Level 2: 과일 하위
SET @fruit_id = (SELECT id FROM categories WHERE code = 'AGRI_FRUIT' LIMIT 1);

INSERT INTO categories (id, name, code, parent_id, level, sort_order)
VALUES
  (uuid_to_bin(UUID()), '제철 과일', 'AGRI_FRUIT_SEASONAL', @fruit_id, 2, 1),
  (uuid_to_bin(UUID()), '저장 과일', 'AGRI_FRUIT_STORAGE', @fruit_id, 2, 2),
  (uuid_to_bin(UUID()), '베리류', 'AGRI_FRUIT_BERRY', @fruit_id, 2, 3);

-- 🥬 Level 2: 채소 하위
SET @vegetable_id = (SELECT id FROM categories WHERE code = 'AGRI_VEGETABLE' LIMIT 1);

INSERT INTO categories (id, name, code, parent_id, level, sort_order)
VALUES
  (uuid_to_bin(UUID()), '쌈·샐러드 채소', 'AGRI_VEG_SALAD', @vegetable_id, 2, 1),
  (uuid_to_bin(UUID()), '국·찌개 채소', 'AGRI_VEG_SOUP', @vegetable_id, 2, 2),
  (uuid_to_bin(UUID()), '볶음·구이 채소', 'AGRI_VEG_STIRFRY', @vegetable_id, 2, 3),
  (uuid_to_bin(UUID()), '뿌리채소', 'AGRI_VEG_ROOT', @vegetable_id, 2, 4);

-- 🌾 Level 2: 기타 대분류 기본 하위
SET @grain_id = (SELECT id FROM categories WHERE code = 'AGRI_GRAIN' LIMIT 1);
SET @special_id = (SELECT id FROM categories WHERE code = 'AGRI_SPECIAL' LIMIT 1);
SET @nut_id = (SELECT id FROM categories WHERE code = 'AGRI_NUT' LIMIT 1);
SET @local_specialty_id = (SELECT id FROM categories WHERE code = 'AGRI_LOCAL_SPECIALTY' LIMIT 1);

INSERT INTO categories (id, name, code, parent_id, level, sort_order)
VALUES
  (uuid_to_bin(UUID()), '식사용 곡물', 'AGRI_GRAIN_STAPLE', @grain_id, 2, 1),
  (uuid_to_bin(UUID()), '약용·산지 작물', 'AGRI_SPECIAL_MEDICINAL', @special_id, 2, 1),
  (uuid_to_bin(UUID()), '견과류', 'AGRI_NUT_RAW', @nut_id, 2, 1),
  (uuid_to_bin(UUID()), '지역 대표 농산물', 'AGRI_LOCAL_REPRESENT', @local_specialty_id, 2, 1);

-- 🥩 Level 2: 축산물 하위
SET @livestock_id = (SELECT id FROM categories WHERE code = 'AGRI_LIVESTOCK' LIMIT 1);

INSERT INTO categories (id, name, code, parent_id, level, sort_order)
VALUES
  (uuid_to_bin(UUID()), '계란', 'AGRI_LIVESTOCK_EGG', @livestock_id, 2, 1),
  (uuid_to_bin(UUID()), '육류', 'AGRI_LIVESTOCK_MEAT', @livestock_id, 2, 2),
  (uuid_to_bin(UUID()), '유제품', 'AGRI_LIVESTOCK_DAIRY', @livestock_id, 2, 3);

-- ============================================================
-- 구조의 장점
-- ============================================================
-- ✅ level 2까지만으로도 UX / 필터 / AI 충분
-- ✅ 상품은 level=2에 바로 매핑 가능
-- ✅ 나중에 필요하면 level 3(품목 세분화) 자연스럽게 추가 가능
-- ✅ 기타 같은 도메인 부채 없음
-- ✅ AI 프롬프트에서 의미가 명확함
-- ============================================================
