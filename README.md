# 🌽 Baro Farm -  백엔드
> 신선한 농산물을 생산자로부터 소비자에게 직접 연결하는 Farm-to-Table 기반 MSA 이커머스 플랫폼
>
> 이 프로젝트는 프로그래머스 데브코스에서 진행한 [기존 프로젝트](https://github.com/prgrms-be-adv-devcourse/beadv2_2_dogs_BE)를 고도화한 버전입니다.

</br>

## 📁 Directory

  <pre>                                                                                                                                                                                                                             
  baro-farm/   
  ├── .github/           # github actions script  
  ├── baro-ai/                                                                                                                                                                                                                      
  ├── baro-common/                                                                                                                                                                                                                  
  ├── ❌ <span style="color:red;"><s>baro-config/</s>       # k8s configmap으로 대체</span>                                                                                                                                                 
  ├── ❌ <span style="color:red;"><s>baro-eureka/</s>       # k8s service로 대체</span>                                                                                                                                          
  ├── baro-gateway/                                                                                                                                                                                                                 
  ├── baro-notification/                                                                                                                                                                                                            
  ├── baro-opa-bundle/                                                                                                                                                                                                              
  ├── baro-opa/                                                                                                                                                                                                                     
  ├── baro-order/                                                                                                                                                                                                                   
  ├── baro-payment/                                                                                                                                                                                                                 
  ├── baro-sample/       # 배포 테스트용 모듈                                                                                                                                                                                     
  ├── baro-settlement/                                                                                                                                                                                                              
  ├── baro-shopping
  ├── baro-user/                                                                                                                                                                                                                    
  ├── config/            # 전역 설정                                                                                                                                                                                             
  ├── docker/            # 도커 이미지                                                                                                                                                                                            
  ├── gradle/wrapper/                                                                                                                                                                                                               
  ├── k8s/                                                                                                                                                                                                                          
  │   ├── apps/                                                                                                                                                                                                                     
  │   ├── infra/                                                                                                                                                                                                                    
  │   └── monitoring/                                                                                                                                                                                                             
  └── scripts/           # 초기 더미데이터용 스크립트                                                                                                                                                                                                           
  </pre>

> 초기에는 docker compose로 배포하다가, k8s로 전환하면서 `baro-config`, `baro-eureka`를 각각 k8s의 ConfigMap, Service로 대체했습니다.

</br>

## ✨ 주요 기능

  <details>                                                                                                                                                                                                                         
  <summary><b>1. 로그인 & 회원가입</b></summary>                                                                                                                                                                                    

    사용자는 이메일과 비밀번호를 기반으로 회원가입과 로그인을 할 수 있습니다. Spring Security와 JWT 기반으로 인증·인가를 구현했습니다.

https://github.com/user-attachments/assets/14a194bb-a90a-4890-b7f0-ac58e8f4ce50

  </details>                                                                                                                                                                                                                        

  <details>                                                                                                                                                                                                                         
  <summary><b>2. 장바구니</b></summary>                                                                                                                                                                                        

    사용자는 상품을 장바구니에 담고 수량을 변경하거나 삭제하면서 주문 전까지 상품을 관리할 수 있습니다.

https://github.com/user-attachments/assets/a111920a-c97a-4462-8a8a-f3b5e83241fa
  </details>                                                                                                                                                                                                                        

  <details>                                                                                                                                                                                                                         
  <summary><b>3. 주문 & 결제</b></summary>                                                                                                                                                                                          

    사용자는 장바구니에 담긴 상품을 주문하고 결제할 수 있습니다. Toss Payments를 연동하여 결제 기능을 구현했습니다.

https://github.com/user-attachments/assets/726ec90c-4442-4ad0-9228-7a616878b582
  </details>                                                                                                                                                                                                                        

  <details>                                                                                                                                                                                                                         
  <summary><b>4. 검색</b></summary>                                                                                                                                                                                                 

    사용자는 상품을 검색할 수 있습니다. Elasticsearch를 활용해 상품 검색 기능을 구현했습니다.

https://github.com/user-attachments/assets/e1aed4ad-221f-4cc6-95a5-f7b7e89c5e3c
  </details>                                                                                                                                                                                                                        

  <details>                                                                                                                                                                                                                         
  <summary><b>5. 정산</b></summary>                                                                                                                                                                                                 

    판매자는 지정된 정산일에 해당 기간의 판매 금액에서 서비스 수수료를 제외한 금액을 정산받습니다. Spring Batch를 활용하여 정산 배치 프로세스를 구현했습니다.

https://github.com/user-attachments/assets/0c6a1007-55b5-4c7d-a172-f8b582ffb4b6
  </details>                                                                                                                                                                                                                        

  <details>                                                                                                                                                                                                                         
  <summary><b>6. 함께 보면 좋은 상품 추천</b></summary>                                                                                                                                                                             

    상품 상세 페이지에서 현재 상품과 함께 보면 좋은 유사 상품을 자동으로 노출합니다. Elasticsearch 기반 벡터 검색(kNN, HNSW)과 Diversity Service를 활용하여 유사도와 카테고리·브랜드 다양성을 함께 고려했습니다.

https://github.com/user-attachments/assets/87ebf8e0-7969-4567-b28c-ba221043292b
  </details>                                                                                                                                                                                                                        

  <details>                                                                                                                                                                                                                         
  <summary><b>7. 사용자 행동 로그 기반 개인화 추천</b></summary>                                                                                                                                                                    

    검색·장바구니·주문 등 사용자 행동 로그를 분석해 개인별 취향에 맞는 상품을 추천합니다. Kafka로 수집한 이벤트 로그를 Elasticsearch에 적재하고 사용자 프로필 벡터를 기반으로 kNN·코사인 유사도 검색으로 추천 결과를 생성했습니다.


https://github.com/user-attachments/assets/fffc63e3-bb12-45c7-8054-8530d53df406
  </details>                                                                                                                                                                                                                        

  <details>                                                                                                                                                                                                                         
  <summary><b>8. 장바구니 기반 레시피 추론 및 재료 추천</b></summary>                                                                                                                                                               

    장바구니에 담긴 재료로 만들 수 있는 레시피를 자동으로 제안하고 부족한 재료까지 함께 추천하는 기능입니다. LLM을 통해 레시피를 추론하고 Elasticsearch 상품 검색을 활용해 필요한 재료 상품을 찾아 연결했습니다.

https://github.com/user-attachments/assets/9dfcdc53-5dda-4ecb-9ed8-9fe5d4245879
  </details>                                                                                                                                                                                                                        

  <details>                                                                                                                                                                                                                         
  <summary><b>9. 리뷰 감정 분석 및 요약</b></summary>                                                                                                                                                                               

    사용자 리뷰의 전반적인 감정과 핵심 내용을 한눈에 볼 수 있도록 요약해줍니다. Kafka 기반으로 리뷰 이벤트를 수집한 뒤 감정 분석 엔진과 LLM 요약 작업을 스케줄러로 주기적으로 실행하여 대표 요약 리뷰를 생성했습니다.

https://github.com/user-attachments/assets/52e27271-3f67-4008-a606-eb086d7dea90
  </details>

</br>

## 🛠️ 기술 스택
**Backend**
<img width="899" height="146" alt="Image" src="https://github.com/user-attachments/assets/440fadea-bc58-43d2-a3de-58c90c6c5e03" />

**Infra**
<img width="1019" height="144" alt="Image" src="https://github.com/user-attachments/assets/ae301c9c-ad86-4ca5-af6d-ac584dc522e6" />

</br>

## 🔄 CI/CD
<img width="413" height="184" alt="Image" src="https://github.com/user-attachments/assets/1feaa85a-c425-4fd9-829e-a8ca53b8f4f8" />

</br>
</br>

## 🏗️ Architecture
<img width="757" height="417" alt="Image" src="https://github.com/user-attachments/assets/c50b086a-59b8-497e-9740-ac1edc57909c" />


</br>
</br>


## 🔗 API 경로

모든 API는 Gateway를 통해 접근합니다. Gateway Base URL 예시는 아래와 같습니다.

- `http://k8s-default-barogate-c698a3c89e-486320229.ap-northeast-2.elb.amazonaws.com`

Gateway 뒤에는 서비스별 Prefix와 실제 API 경로를 붙여 호출합니다.

| 서비스           | Gateway 경로 예시                         |                                                                                                                                                         
|---------------|---------------------------------------|                                                                                                                                                          
| User          | `/user-service/api/v1/**`             |                                                                                                                                                           
| Shopping      | `/shopping-service/api/v1/**`         |
| Order         | `/order-service/api/v1/**`            |                                                                                                                                                           
| Payment       | `/payment-service/api/v1/**`          |                                                                                                                                                           
| Notification  | `/notification-service/api/v1/**`     |                                                                                                                                                           
| AI            | `/ai-service/api/v1/**`               |                                                                                                                                                           
| Settlement    | `/settlement-service/api/v1/**`       |
| Sample (테스트용) | `/sample-service/api/v1/health`(헬스체크) |                                                                                                                                                           

예: Sample 서비스 헬스체크 api 호출                                                                                                                                                                                                         
`http://k8s-default-barogate-c698a3c89e-486320229.ap-northeast-2.elb.amazonaws.com/sample-service/api/v1/health`

</br>

## 👤 Member

<table align="left">
  <tr>
    <td align="center">Backend</td>
    <td align="center">Backend</td>
    <td align="center">Backend</td>
    <td align="center">Backend</td>
  </tr>
  <tr>
    <td>
      <a href="https://github.com/strangehoon">
        <img src="https://avatars.githubusercontent.com/u/117654450?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/sanghyunbang">
        <img src="https://avatars.githubusercontent.com/u/149238724?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/chhezue">
        <img src="https://avatars.githubusercontent.com/u/169384302?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/choiyoung69">
        <img src="https://avatars.githubusercontent.com/u/122353155?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
  <tr>
  <tr>
    <td align="center">
      <a href="https://github.com/strangehoon">이상훈</a>
    </td>
    <td align="center">
      <a href="https://github.com/sanghyunbang">방상현</a>
    </td>
    <td align="center">
      <a href="https://github.com/chhezue">손채연</a>
    </td>
    <td align="center">
      <a href="https://github.com/choiyoung69">최서영</a>
    </td>
  </tr>
</table>

</br>
