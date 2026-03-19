INSERT INTO product_category (category_name,use_yn,created_at,updated_at) VALUES
	 ('기타','Y','2025-10-16 22:01:50','2025-10-16 22:02:32'),
	 ('티셔츠/블라우스','Y','2025-10-16 22:01:50','2025-10-16 22:02:01'),
	 ('바지','Y','2025-10-16 22:01:50','2025-10-16 22:01:50'),
	 ('니트/스웨터','Y','2025-10-16 22:01:50','2025-10-16 22:01:50'),
	 ('조끼/가디건','Y','2025-10-16 22:01:50','2025-10-16 22:01:50'),
	 ('잠옷','Y','2025-10-16 22:01:50','2025-10-16 22:01:50');





INSERT INTO product
(product_name, price, sale_price, description, category_id, product_status, created_at, updated_at)
VALUES
('플라워 패턴 블라우스', 59000, 49000, '부드러운 소재의 여성 블라우스', 2, 'ACTIVE', NOW(), NOW()),
('면 100% 데일리 티셔츠', 29000, NULL, '편안한 착용감의 기본 티셔츠', 2, 'ACTIVE', NOW(), NOW()),
('허리밴딩 와이드 팬츠', 69000, 59000, '편안한 허리밴딩 팬츠', 3, 'ACTIVE', NOW(), NOW()),
('기모 일자 슬랙스', 79000, NULL, '겨울용 기모 슬랙스', 3, 'ACTIVE', NOW(), NOW()),
('라운드 니트 스웨터', 89000, 79000, '포근한 니트 스웨터', 4, 'ACTIVE', NOW(), NOW()),
('울 혼방 가디건', 99000, NULL, '가볍게 걸치기 좋은 가디건', 5, 'ACTIVE', NOW(), NOW()),
('니트 조끼 베스트', 69000, NULL, '레이어드용 니트 조끼', 5, 'ACTIVE', NOW(), NOW()),
('면 파자마 세트', 79000, 69000, '편안한 잠옷 세트', 6, 'ACTIVE', NOW(), NOW()),
('기본 홈웨어 상하의', 59000, NULL, '집에서 입기 좋은 홈웨어', 6, 'ACTIVE', NOW(), NOW()),
('생활 방수 앞치마', 39000, NULL, '주방용 생활 방수 앞치마', 1, 'ACTIVE', NOW(), NOW());



INSERT INTO product_option
(product_id, color, size, stock_quantity, created_at, updated_at)
VALUES
-- 1 블라우스
(1, '오프화이트', 'M', 30, NOW(), NOW()),
(1, '오프화이트', 'L', 25, NOW(), NOW()),

-- 2 티셔츠
(2, '화이트', 'FREE', 50, NOW(), NOW()),
(2, '블랙', 'FREE', 40, NOW(), NOW()),

-- 3 와이드 팬츠
(3, '베이지', 'M', 20, NOW(), NOW()),
(3, '베이지', 'L', 15, NOW(), NOW()),

-- 4 슬랙스
(4, '블랙', '28', 18, NOW(), NOW()),
(4, '블랙', '30', 15, NOW(), NOW()),

-- 5 니트
(5, '아이보리', 'FREE', 22, NOW(), NOW()),

-- 6 가디건
(6, '그레이', 'FREE', 17, NOW(), NOW()),

-- 7 조끼
(7, '네이비', 'FREE', 20, NOW(), NOW()),

-- 8 잠옷
(8, '핑크', 'FREE', 30, NOW(), NOW()),

-- 9 홈웨어
(9, '차콜', 'FREE', 25, NOW(), NOW()),

-- 10 앞치마
(10, '그린', 'FREE', 40, NOW(), NOW());




INSERT INTO product_image
(product_id, file_name, image_type, sort_order, use_yn, created_at)
VALUES
(1, '1_main.jpg', 'MAIN', 0, 'Y', NOW()),
(2, '2_main.jpg', 'MAIN', 0, 'Y', NOW()),
(3, '3_main.jpg', 'MAIN', 0, 'Y', NOW()),
(4, '4_main.jpg', 'MAIN', 0, 'Y', NOW()),
(5, '5_main.jpg', 'MAIN', 0, 'Y', NOW()),
(6, '6_main.jpg', 'MAIN', 0, 'Y', NOW()),
(7, '7_main.jpg', 'MAIN', 0, 'Y', NOW()),
(8, '8_main.jpg', 'MAIN', 0, 'Y', NOW()),
(9, '9_main.jpg', 'MAIN', 0, 'Y', NOW()),
(10,'10_main.jpg','MAIN', 0, 'Y', NOW());

INSERT INTO product_image
(product_id, file_name, image_type, sort_order, use_yn, created_at)
VALUES
(1, '1_detail_1.jpg', 'DETAIL', 1, 'Y', NOW()),
(1, '1_detail_2.jpg', 'DETAIL', 2, 'Y', NOW()),
(3, '3_detail_1.jpg', 'DETAIL', 1, 'Y', NOW()),
(5, '5_detail_1.jpg', 'DETAIL', 1, 'Y', NOW());
