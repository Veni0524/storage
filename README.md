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
  mvn package -Dmaven.test.skip=trun
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

  

## API 게이트웨이
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






## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다. (예시는 storage 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하려고 노력했다. 현실에서 발생가능한 이벤트에 의하여 마이크로 서비스들이 상호 작용하기 좋은 모델링으로 구현을 하였다.

```
package taxi;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Payment_table")
public class Payment {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long callId;
    private Integer cost;
    private String destination;

    @PostPersist
    public void onPostPersist(){
        RegisteredDestinationInfo registeredDestinationInfo = new RegisteredDestinationInfo();
        BeanUtils.copyProperties(this, registeredDestinationInfo);
        registeredDestinationInfo.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getCallId() {
        return callId;
    }

    public void setCallId(Long callId) {
        this.callId = callId;
    }
    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }
    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }




}



```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package taxi;

import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="payments", path="payments")
public interface PaymentRepository extends PagingAndSortingRepository<Payment, Long>{

    Optional<Payment> findById(Long id);

}

```
- 적용 후 REST API 의 테스트
 
 승객이 택시를 콜하면 운전기사(Driver)가 콜 확인 가능 
http POST http://a61a63555c8e340cb8dd6b17be45597b-1845340017.eu-west-3.elb.amazonaws.com:8080/calls customerId=10 destination=suji cost=100 

http GET http://a61a63555c8e340cb8dd6b17be45597b-1845340017.eu-west-3.elb.amazonaws.com:8080/drivers/3


![image](https://user-images.githubusercontent.com/84304023/124948366-944a5300-e04b-11eb-9f40-d6004465fe8f.png)
![image](https://user-images.githubusercontent.com/84304023/124948378-98767080-e04b-11eb-8484-ad9f03df28d2.png)




## 동기식 호출(Sync) 

- 승객이 택시를 콜하면 payment 에 콜정보와 금액이 동기식으로 저장됨  
- Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출

```
# PaymentService.java


package taxi.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

//import java.util.Date;

// @FeignClient(name="Payment", url="http://Payment:8083")         //Delete
@FeignClient(name="payment", url="${feign.client.url.paymentUrl}") //Insert
public interface PaymentService {

    @RequestMapping(method= RequestMethod.GET, path="/payments")
    public void savePayment(@RequestBody Payment payment);

}


```

- 

```
# Payment.java (Entity)

   
    @PostPersist
    public void onPostPersist(){
        RegisteredDestinationInfo registeredDestinationInfo = new RegisteredDestinationInfo();
        BeanUtils.copyProperties(this, registeredDestinationInfo);
        registeredDestinationInfo.publishAfterCommit();


    }

```

승객이 콜 호출 후 payment 에 동기식으로 데이터 발생확인

![image](https://user-images.githubusercontent.com/84304023/124963265-8c45df80-e05a-11eb-94ba-2f5ee7eaf0c2.png)
![image](https://user-images.githubusercontent.com/84304023/124963271-8fd96680-e05a-11eb-9050-d4a5311cb243.png)





## 비동기식 호출 

운전기사가 승객을 태우고 callId 별로 call 의 상태를 Accepted로 변경하고 운전을 시작한다.



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
    
    UpdatedStatus updatedStatus = new UpdatedStatus();
        this.setCallId(callId);
        BeanUtils.copyProperties(this, updatedStatus);
        updatedStatus.publishAfterCommit();
	
 }	
    
    
# call  - PolicyHandler.java
package taxi;

import taxi.config.kafka.KafkaProcessor;

import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired CallRepository callRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverUpdatedStatus_UpdateStatus(@Payload UpdatedStatus updatedStatus){

        if(!updatedStatus.validate()) return;

        System.out.println("\n\n##### listener UpdateStatus : " + updatedStatus.toJson() + "\n\n");

        // Sample Logic //
        // Call call = new Call();

        Optional<Call> callOptional =  callRepository.findById(updatedStatus.getCallId());
        Call call = callOptional.get();
        call.setStatus(updatedStatus.getStatus());

        callRepository.save(call);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}

```

운전기사가 승객을 태우고 callId 별로 call 의 상태를 Accepted로 변경하고 운전을 시작한다

http PATCH http://a61a63555c8e340cb8dd6b17be45597b-1845340017.eu-west-3.elb.amazonaws.com:8080/drivers/3 status=Accepted

![image](https://user-images.githubusercontent.com/84304023/124963665-0d9d7200-e05b-11eb-8621-4c3090cfab83.png)


http GET http://a61a63555c8e340cb8dd6b17be45597b-1845340017.eu-west-3.elb.amazonaws.com:8080/calls/6

![image](https://user-images.githubusercontent.com/84304023/124963677-11c98f80-e05b-11eb-88dd-4c8789e5da39.png)





# 운영

각각의 KUBECTL 명령어로 운영에 빌드하였습니다. 


1.AWS Configure 수행

2.ESK생성
eksctl create cluster --name user26-eks --version 1.17 --nodegroup-name standard-workers --node-type t3.medium --nodes 4 --nodes-min 1 --nodes-max 4

3.AWS 클러스터 토큰 가져오기
aws eks --region eu-west-3 update-kubeconfig --name user26-eks

4. 매트릭스 서버 설치
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.5.0/components.yaml

5. 각 서비스 MVN
mvn package -Dmaven.test.skip=trun

6. ECR 에서 Repository 생성
![image](https://user-images.githubusercontent.com/84304023/124966025-bc42b200-e05d-11eb-903e-a0373d6981f5.png)


7. AWS 컨테이너 레지스트리 로그인
docker login --username AWS -p $(aws ecr get-login-password --region eu-west-3) 879772956301.dkr.ecr.eu-west-3.amazonaws.com/

8. 이름 정의 
kubectl create ns taxi


9. 도커 이미지 생성 ( 각서비스별로 생성)

docker build -t 879772956301.dkr.ecr.eu-west-3.amazonaws.com/payment:v1 .

10. 도커 이미지 푸쉬
docker push 879772956301.dkr.ecr.eu-west-3.amazonaws.com/payment:v1

11. 이미지가 레파지토리에 있는지 확인 
![image](https://user-images.githubusercontent.com/84304023/124966413-42f78f00-e05e-11eb-9406-8c5d0c53d2f3.png)


12.앞서 생산한 taxi 에 pod 를 띄움
kubectl create deploy payment --image 879772956301.dkr.ecr.eu-west-3.amazonaws.com/payment:v1 -n taxi

13. 서비스 확인 
kubectl get all -n taxi

![image](https://user-images.githubusercontent.com/84304023/124966711-923dbf80-e05e-11eb-8f44-5b44c0ce183d.png)

14. payment 서비스를 올림 

kubectl expose deploy payment --type="ClusterIP" --port=8080 --namespace=taxi

![image](https://user-images.githubusercontent.com/84304023/124966795-ac779d80-e05e-11eb-8e54-27947774fd23.png)

15. 운영 반영 완료 후 서비스 

![image](https://user-images.githubusercontent.com/84304023/124966838-b6999c00-e05e-11eb-93ed-9ad9dd2387f3.png)





## 모니터링
1. 메트릭 서버 설치 및 설치확인

kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.5.0/components.yaml

![image](https://user-images.githubusercontent.com/84304023/125011799-16fefc80-e0a4-11eb-9fa3-1b5814630aaa.png)


2. 모니터링 
 
kubectl get nodes
kubectl describe nodes

![image](https://user-images.githubusercontent.com/84304023/125012209-ca67f100-e0a4-11eb-9622-ce3974fd777c.png)
![image](https://user-images.githubusercontent.com/84304023/125012265-dfdd1b00-e0a4-11eb-8090-5627568a7b3b.png)
....
![image](https://user-images.githubusercontent.com/84304023/125012313-f2575480-e0a4-11eb-9a3a-dcaa58d54812.png)






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



## Config map 

1.컨피그맵 생성

kubectl apply -f configmap.yml
![image](https://user-images.githubusercontent.com/84304023/125029729-0f038480-e0c5-11eb-9853-0daae2559779.png)

2.Deployment.yml
![image](https://user-images.githubusercontent.com/84304023/125030089-820cfb00-e0c5-11eb-8e6f-d0811cb905f2.png)

...

# Self-healing (Liveness Probe)


- payment deployment.yml 파일 수정 
```

/tmp/healthy 파일이 존재하는지 확인하는 설정파일이다.
livenessProbe에 'cat /tmp/healthy'으로 검증하도록 함
파일이 존재하지 않을 경우, 정상 작동에 문제가 있다고 판단되어 kubelet에 의해 자동으로 컨테이너가 재시작 된다

```
![image](https://user-images.githubusercontent.com/84304023/125010662-e9b14f00-e0a1-11eb-8f32-8fb33aee03d5.png)


- kubectl describe pod payment -n taxi  실행은 못함

