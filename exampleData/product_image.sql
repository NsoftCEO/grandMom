-- 상품 메인이미지의 경우 option값에 종속안되므로 null

INSERT INTO goot.product_image
(product_id, option_id, image_url, original_name, image_type, sort_order, use_yn, created_at)
VALUES
(1, NULL, '/images/product/1_main.jpg', 'blouse_main.jpg', 'MAIN', 0, 'Y', NOW()),
(2, NULL, '/images/product/2_main.jpg', 'tshirt_main.jpg', 'MAIN', 0, 'Y', NOW()),
(3, NULL, '/images/product/3_main.jpg', 'pants_main.jpg', 'MAIN', 0, 'Y', NOW()),
(4, NULL, '/images/product/4_main.jpg', 'slacks_main.jpg', 'MAIN', 0, 'Y', NOW()),
(5, NULL, '/images/product/5_main.jpg', 'knit_main.jpg', 'MAIN', 0, 'Y', NOW()),
(6, NULL, '/images/product/6_main.jpg', 'cardigan_main.jpg', 'MAIN', 0, 'Y', NOW()),
(7, NULL, '/images/product/7_main.jpg', 'vest_main.jpg', 'MAIN', 0, 'Y', NOW()),
(8, NULL, '/images/product/8_main.jpg', 'pajama_main.jpg', 'MAIN', 0, 'Y', NOW()),
(9, NULL, '/images/product/9_main.jpg', 'homewear_main.jpg', 'MAIN', 0, 'Y', NOW()),
(10, NULL, '/images/product/10_main.jpg', 'apron_main.jpg', 'MAIN', 0, 'Y', NOW()); 


INSERT INTO goot.product_image
(product_id, option_id, image_url, original_name, image_type, sort_order, use_yn, created_at)
VALUES
(1, NULL, '/images/product/1_detail_1.jpg', 'blouse_detail1.jpg', 'DETAIL', 1, 'Y', NOW()),
(1, NULL, '/images/product/1_detail_2.jpg', 'blouse_detail2.jpg', 'DETAIL', 2, 'Y', NOW()),

(3, NULL, '/images/product/3_detail_1.jpg', 'pants_detail1.jpg', 'DETAIL', 1, 'Y', NOW()),

(5, NULL, '/images/product/5_detail_1.jpg', 'knit_detail1.jpg', 'DETAIL', 1, 'Y', NOW());


INSERT INTO goot.product_image
(product_id, option_id, image_url, original_name, image_type, sort_order, use_yn, created_at)
VALUES
-- 블라우스 옵션 이미지
(1, 1, '/images/product/1_option_white.jpg', 'blouse_white.jpg', 'OPTION', 0, 'Y', NOW()),
(1, 2, '/images/product/1_option_white_l.jpg', 'blouse_white_l.jpg', 'OPTION', 0, 'Y', NOW()),

-- 팬츠 옵션 이미지
(3, 5, '/images/product/3_option_beige_m.jpg', 'pants_beige_m.jpg', 'OPTION', 0, 'Y', NOW()),
(3, 6, '/images/product/3_option_beige_l.jpg', 'pants_beige_l.jpg', 'OPTION', 0, 'Y', NOW());
