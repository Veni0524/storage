# 배민라이더스 ( 배달대행 )


- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW


# Table of contents

- [배민라이더스( 배달대행 )](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오

배민라이더스 커버하기

기능적/ 요구사항
1. 음식점에서 배민파트너(배달원)을 호출한다. 배민파트너를 호출할 때 목적지와 배달대행비를 입력한다.
2. 배민파트너를 호출하면 동기화로 payment로 cost가 등록된다. (동기화)
3. 배민파트너를 호출하면 배민파트너가 호출정보를 받는다. (비동기)
4. 배민파트너를 호출하면 report로 deliveryId 별로 요금정보와 상태(Called)가 업데이트 된다. (CQRS)
5. 배민파트너는 음식을 픽업하면, callId 별로 call 상태정보를 Picked 로 변경하고 운행을 시작한다. 
   이 때 배달의 상태정보도 변경된다. (비동기)  deliveryId별 상태정보가 변경되면 report로 deliveryID별 상태정보가 업데이트된다 (CQRS)
6. 배달이 완료되면 배민파트너는 delivery상태정보를 Completed로 변경한다.									
   이 때 상태정보가 Completed로 변경되면 payment에서 deliveryId로 등록된 요금을 정산받는다 (동기화)
   deliverylist의 정산필드가 업데이트 되고, 상태값도 Completed 로 변경된다. 

비기능적 요구사항 
1. deliverylist 에 deliveryid 별로 상태정보가 없데이트 된다. 
2. driver 서비스 되지 않더라도 call 서비스는 지속된다. (장애격리)


# 체크포인트

# 분석/설계

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  https://labs.msaez.io/#/storming/FjUK5yZ3Lqh0BNV0qwgUEWoJ5L13/5f2edd182ef5b2792b07c9e880c82c64


### 이벤트 도출
### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/88864460/135231443-a988dbae-025d-47c2-ad9d-58d3f6c5fec8.png)



    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
     

### 액터, 커맨드 부착하여 읽기 좋게

![image](https://user-images.githubusercontent.com/88864460/135238814-3f95e89c-064c-4e1f-838d-a9d4a4ac542e.png)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/88864460/135238753-89b6eb7b-fd15-4e8b-b328-efb25ae5795c.png)

   - command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌
   

### 바운디드 컨텍스트로 묶기
![image](https://user-images.githubusercontent.com/88864460/135238864-26f0dde7-8a33-465f-aece-c484d8b4aac8.png)

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)
![image](https://user-images.githubusercontent.com/88864460/135238925-69e7d71c-1c61-4947-86a9-5e219bad576b.png)


### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)
![image](https://user-images.githubusercontent.com/88864460/135238983-b11dd354-a942-46b1-b518-f47c72954629.png)



요구사항 점검 후 deliverylist 빠진 것 확인 
![image](https://user-images.githubusercontent.com/88864460/135239087-4525c285-d7a4-4462-85cd-8b23f548ca6e.png)


## MSAEz 결과
 - 아래와 같이 모델링한 후 소스를 생성함

![image](https://user-images.githubusercontent.com/88864460/135239205-cbc59387-9e01-4dd8-a2db-5878fcd9c713.png)



## 헥사고날 아키텍처 다이어그램 도출
![image](https://user-images.githubusercontent.com/88864460/135233300-0ab4fb4d-d2d5-4192-b9fc-8061a949484e.png)



# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 
구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 
(각자의 포트넘버는 로컬에서는 8081 ~ 8084 임)

```
cd payment
mvn spring-boot:run

cd delivery
mvn spring-boot:run 

cd driver
mvn spring-boot:run  

cd deliverylist
mvn spring-boot:run 
 
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다.

```
package delivery;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Payments_table")
public class Payments {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long deliveryId;
    private String destination;
    private Integer cost;

    @PostPersist
    public void onPostPersist(){
        SettledCost settledCost = new SettledCost();
        BeanUtils.copyProperties(this, settledCost);
        settledCost.publishAfterCommit();

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
    }
    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }




}



```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package delivery;

@RepositoryRestResource(collectionResourceRel="payments", path="payments")
public interface PaymentsRepository extends PagingAndSortingRepository<Payments, Long>{

    Optional<Payments> findById(Long id);

```
- 적용 후 REST API 의 테스트
 
음식점에서 배달원을 요청하면 배달원(Driver)이 요청 확인 가능

http POST http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/deliveries customerId=1 destination=seoul cost=20000

http GET http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/deliveries

![image](https://user-images.githubusercontent.com/88864460/135445059-fd8d98a5-b7c6-4f21-a451-b0fa75ca98d5.png)
![image](https://user-images.githubusercontent.com/88864460/135445170-bd0769ed-698e-471a-8181-1123996b84c5.png)

## 동기식 호출(Sync) 과 Fallback 처리

- 음식점에서 배달원을 호출하면 payment 에 배달정보와 금액이 동기식으로 저장됨  
- Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출

# Payment.java (Entity)

 ```
 
    @PostPersist
    public void onPostPersist(){
        SettledCost settledCost = new SettledCost();
        BeanUtils.copyProperties(this, settledCost);
        settledCost.publishAfterCommit();  

```

- 가격 정보를 호출하기 위하여 stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현
- 
# PaymentService.java

```
package delivery.external;


@FeignClient(name="payments", url="${feign.client.url.paymentUrl}") //Insert
public interface PaymentsService {
    @RequestMapping(method= RequestMethod.GET, path="/payments")
    public void savePayments(@RequestBody Payments payments);


```

음식점에서 배달원 요청 후 payment 에 동기식으로 데이터 발생확인

![image](https://user-images.githubusercontent.com/88864460/135445059-fd8d98a5-b7c6-4f21-a451-b0fa75ca98d5.png)
![image](https://user-images.githubusercontent.com/88864460/135447785-ee013bb2-7dc1-42fe-9ff6-724347ba43f9.png)

## 비동기식 호출 

배달원이 음식을 싣고 deliveryId 별로 delivery 의 상태를 Accepted로 변경하고 운전을 시작한다.

```
# Driver.java
package taxi;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;

import java.util.List;
import java.util.Optional;
import java.util.Date;

@Entity
@Table(name="Driver_table")
public class Driver {

....

 @PostUpdate
    public void onPostUpdate() throws Exception {
    
    UpdateStatus updateStatus = new UpdateStatus();
        this.setDeliveryId(deliveryId);
        BeanUtils.copyProperties(this, updateStatus);
        updateStatus.publishAfterCommit();
	
 }	
    
    
# delivery  - PolicyHandler.java
package delivery;

import delivery.config.kafka.KafkaProcessor;

import java.util.Optional;

import delivery.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverUpdateStatus_UpdatedStatus(@Payload UpdateStatus updateStatus){

        if(!updateStatus.validate()) return;

        System.out.println("\n\n##### listener UpdatedStatus : " + updateStatus.toJson() + "\n\n");



        // Sample Logic //
        // Delivery delivery = new Delivery();
        // deliveryRepository.save(delivery);
        Optional<Delivery> deliveryOptional = deliveryRepository.findById(updateStatus.getDeliveryId());
        Delivery delivery = deliveryOptional.get();
        delivery.setStatus(updateStatus.getStatus());

        deliveryRepository.save(delivery);


    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}

```

http PATCH http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/drivers/1 status="Accepted"

![image](https://user-images.githubusercontent.com/88864460/135445729-02b7a388-9bd1-437d-b6b7-d004b522af55.png)

http GET http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/deliveries

![image](https://user-images.githubusercontent.com/88864460/135445900-f7cb994e-6919-4a4d-8c90-338983337ad0.png)

배달 시스템은 배달원 시스템과 완전히 분리되어 있으며, 이벤트 수신에 따라 처리되기 떄문에, 배달원(driver) 시스템이 유지보수로 인해 잠시 내려간 상태라도
배달주문을 등록하는데 문제가 없다.

```
# 배달원 서비스 (driver) 를 잠시 내려놓음

#배달등록
http POST http://localhost:8082/delivery customerId=1 cost=3500 status=called   #Success

#상품정보 확인
http GET http://localhost:8082/delivery/1     # 배달상태 called 확인

#드라이버 서비스 기동
cd driver
mvn spring-boot:run

#배달서비스의 상태 확인
http GET http://localhost:8081/products/1     # 배달상태 Accepted로 변경 확인

```

## CQRS

배달원이 수익과 콜수를 점검할 수 있는 화면을 CQRS 로 구현한다.
비동기식으로 처리되어 발행된 이벤트 기반 카프카를 통해 수신/처리되어 별도 TABLE 에 관리한다. 

음식점에서 콜을 하면 승객 ID/ 금액/ 목적지 / 상태 = Called 가 업데이트 된다. 
배달원이 수락을 하고 승객을 태우면 상태가 accetped 로 업데이트 된다. 
배달이 끝나고 배달원이 종료처리 하면 정산 금액이 업데이트 되고 상태가 completed 로 변경된다.
 
- viewpage MSA ViewHandler 를 통해 구현 (이벤트 발생 시, Pub/Sub 기반으로 별도 deliveryList 테이블에 저장)
- delivery 생성시 승객 id/금액/목적지/상태=called 가 생성됨
- 
![image](https://user-images.githubusercontent.com/88864460/135444043-c28b39b0-23ab-4794-95ed-4a1836c468ea.png)

- 배달원의 운행이 시작되어 상태가 accepted 로 업데이트 되면 뷰도 업데이트 됨
- 배달원의 운행이 완료되어 상태를 completed 로 업데이트 되면 뷰의 상태값과 settle 필드도 업데이트  

![image](https://user-images.githubusercontent.com/88864460/135444140-36449530-4282-4052-ae06-ed6d19e26415.png)



- 실제로 view 페이지를 조회에서 정보 확인이 가능하다

![image](https://user-images.githubusercontent.com/88864460/135444305-f07b3c1c-5cc6-43ae-a594-fda2c69fa4ce.png)

  

## 게이트웨이
gateway 스프링부트 App을 추가 후 application.yaml내에 각 마이크로 서비스의 routes 를 추가하고 gateway 서버의 포트를 8080 으로 설정함
       
         
 1.  application.yml   
 ```  
spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: delivery
          uri: http://delivery:8080
          predicates:
            - Path=/deliveries/** 
        - id: driver
          uri: http://driver:8080
          predicates:
            - Path=/drivers/** 
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 
        - id: deliveryList
          uri: http://deliveryList:8080
          predicates:
            - Path= /deliveryLists/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
        
 ``` 
 
  
 3.  gateway 생성하고 expose 시 type을 LoadBalancer 로 설정  
   
   ```
	kubectl create deploy gateway --image=879772956301.dkr.ecr.ap-northeast-1.amazonaws.com/user07-gateway:v1 -n delivery
        kubectl expose deploy gateway --type="LoadBalancer" --port=8080 --namespace=delivery

   ```     

![image](https://user-images.githubusercontent.com/88864460/135444684-d7ec95d7-624b-4fd8-a7fe-2a0ac293c972.png)

    

# Correlation

음식점에서 배달원을 호출했을 때, 배달원이 호출을 수락하여 음식을 픽업할 때, 운행종료후 배달원이 배달을 종료처리할 때 
deliverylist 상태가 변경된다. 

1) 음식점에서 배달원을 호출한다

 http POST http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/deliveries customerId=1 destination=seoul cost=20000
 
![image](https://user-images.githubusercontent.com/88864460/135445059-fd8d98a5-b7c6-4f21-a451-b0fa75ca98d5.png)

1-1) 배달원을 호출하면 payment에 deliveryId별로 요금 20000이 등록되는데(동기) 이를 확인한다

http GET http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/payments

![image](https://user-images.githubusercontent.com/88864460/135445170-bd0769ed-698e-471a-8181-1123996b84c5.png)

1-2) driver정보도 확인한다

http GET http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/drivers

![image](https://user-images.githubusercontent.com/88864460/135445449-523f6363-55ae-4872-bf7f-b4b41d2695f1.png)

1-3) deliveryist 화면도 확인한다.

http GET http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/deliveryLists

![image](https://user-images.githubusercontent.com/88864460/135445525-85b1bcb8-33ac-4b93-b104-d6e8bed5a524.png)


2) 배달원이 호출을 수락하여 음식을 픽업한다.

http PATCH http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/drivers/1 status="Accepted"


![image](https://user-images.githubusercontent.com/88864460/135445729-02b7a388-9bd1-437d-b6b7-d004b522af55.png)


2-1) 배달의 상태가 변경된다. 

http GET http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/deliveries


![image](https://user-images.githubusercontent.com/88864460/135445900-f7cb994e-6919-4a4d-8c90-338983337ad0.png)


2-2)deliverylist 상태가 변경된다.

http GET http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/deliveryLists

![image](https://user-images.githubusercontent.com/88864460/135446059-3ebdfeef-2609-4f14-944a-a08fa5013681.png)


3) 배달이 종료되면 배달원이 상태를 변경한다.

http PATCH http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/drivers/1 status="Completed"

![image](https://user-images.githubusercontent.com/88864460/135446177-031b06a8-889f-4046-9c3c-508c100beed4.png)


3-1) 배달의 상태가 변경된다.

http GET http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/deliveries

![image](https://user-images.githubusercontent.com/88864460/135446276-bb06e0d0-1875-4f16-95be-a9929d579b85.png)

3-2) deliveryList 상태가 변경된다. 

http GET http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/deliveryLists

![image](https://user-images.githubusercontent.com/88864460/135446418-7d96001c-ebef-45f8-b341-2655b9bf4bff.png)


# 운영

각각의 KUBECTL 명령어로 운영에 빌드하였습니다. 


1.AWS Configure 수행

2.EKS생성

eksctl create cluster --name user07-eks --version 1.19 --nodegroup-name standard-workers --node-type t3.medium --nodes 4 --nodes-min 1 --nodes-max 4

3.AWS 클러스터 토큰 가져오기
aws eks --region ap-northeast-1 update-kubeconfig --name user07-eks

4. 각 서비스 MVN
mvn package -Dmaven.test.skip=trun

5. ECR 에서 Repository 생성
![image](https://user-images.githubusercontent.com/88864460/135548077-94f8305c-5fc9-4dd9-bfd3-e37e7a788099.png)


6. AWS 컨테이너 레지스트리 로그인
docker login --username AWS -p $(aws ecr get-login-password --region ap-northeast-1) 879772956301.dkr.ecr.ap-northeast-1.amazonaws.com/


7. 네임스페이스 정의 
kubectl create ns delivery


8. 도커 이미지 생성 ( 각서비스별로 생성)

docker build -t 879772956301.dkr.ecr.ap-northeast-1.amazonaws.com/user07-payment:v1 .

9. 도커 이미지 푸쉬
docker push 879772956301.dkr.ecr.ap-northeast-1.amazonaws.com/user07-payment:v1

10. 이미지가 레파지토리에 있는지 확인 
![image](https://user-images.githubusercontent.com/88864460/135548223-2da4c5dd-acc7-4841-b295-04a7471605dc.png)


11.Pod deploy
kubectl create deploy payment --image=879772956301.dkr.ecr.ap-northeast-1.amazonaws.com/user07-payment:v1 -n delivery

12. Pod 확인 
kubectl get all -n delivery

13. payment 서비스 expose

kubectl expose deploy payment --type="ClusterIP" --port=8080 --namespace=delivery

14. 확인 

![image](https://user-images.githubusercontent.com/88864460/135444684-d7ec95d7-624b-4fd8-a7fe-2a0ac293c972.png)


## 셀프힐링 livenessProbe 설정
1. delivery deployment livenessProbe 

```
livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120  //초기delay시간
            timeoutSeconds: 1         //timeout시간내응답점검
            periodSeconds: 5          //점검주기
            failureThreshold: 5       //실패5번이후에는 RESTART
```
livenessProbe 기능 점검은 HPA적용되지 않은 상태에서 진행한다.

Pod 의 변화를 살펴보기 위하여 watch
```
kubectl get -n delivery po -w

    NAME                           READY   STATUS    RESTARTS   AGE
    pod/gateway-6449f7459-bcgz6    1/1     Running   0          31m
    pod/order-74f45d958f-qnnz5     1/1     Running   0          5m48s
    pod/product-698dd8fcc4-5frqp   1/1     Running   0          42m
    pod/report-86d9f7b89-knl6h     1/1     Running   0          140m
    pod/siege                      1/1     Running   0          119m
```

서비스를 다운시키기 위한 부하 발생
```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://ae8be8b4eed704a01abbfda9c2aaf74f-664416606.ap-northeast-1.elb.amazonaws.com:8080/deliveries POST {"customerId": "2"}'
```
delivery Pod의 liveness 조건 미충족에 의한 RESTARTS 횟수 증가 확인
```
kubectl get -n delivery po -w

    NAME                       READY   STATUS              RESTARTS   AGE
    delivery-74f45d958f-qnnz5     1/1     Running             0          2m6s
    delivery-74f45d958f-qnnz5     0/1     Running             1          9m28s
    delivery-74f45d958f-qnnz5     1/1     Running             1          11m
```


### 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 

- payment deployment.yml 파일에 resources 설정을 추가한다

![image](https://user-images.githubusercontent.com/84304023/125025988-a9ac9500-e0be-11eb-8cbd-9b264702788e.png)


- payment 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 20프로를 넘어서면 replica 를 3개까지 늘려준다. 
```
kubectl autoscale deployment payment -n taxi --cpu-percent=20 --min=1 --max=3
```

![image](https://user-images.githubusercontent.com/84304023/125026171-fd1ee300-e0be-11eb-9d1b-e4481e102d85.png)


- 부하를 동시사용자 100명, 1분 동안 걸어준다.(X)
```
siege -c100 -t60S -v http GET http://a61a63555c8e340cb8dd6b17be45597b-1845340017.eu-west-3.elb.amazonaws.com:8080/payments
```

![image](https://user-images.githubusercontent.com/84304023/125026409-79b1c180-e0bf-11eb-93c6-5cf7602b8eba.png)



- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다 (X)
```
kubectl get deploy payment -w -n taxi
```



...
