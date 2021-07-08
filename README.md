# 콜택시


- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW


# Table of contents

- [예제 - 콜택시](#---)
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

콜택시 커버하기

기능적/ 요구사항
1. 승객이 택시를 호출한다. 택시 호출할 때 목적지와 요금을 입력한다.
2. 택시를 호출하면 동기화로 payment(안전거래)로 cost=500 이 등록된다. (동기화)
3. 정상적으로 요금이 등록되면 tax driver 에게 호출정보를 등록한다.(비동기)
4. Call 정보가 등록되면 report 로 callId 별로 요금정보가 업데이트 된다. (CQRS)
5. tax driver 는 승객을 태우면, callId 별로 call 상태정보를 Accepted 로 변경하고 운행을 시작한다. 
   이 때 callId 별로 상태정보는 변경된다. (비동기)  callId별 상태정보가 변경되면 report로 callID별 요금정보가 업데이트된다 (CQRS)
6. 탑승이 완료되면 택시기사는 call상태정보를 Completed로 변경하고 요금을 정산받는다										
   이 때 상태정보가 Completed로 변경되면 payment(안전거래)에서 callId로 등록된 요금을 정산받는다 (동기화)

비기능적 요구사항 
1. call 게시판에서 call 별로 상태를 확인할 수 있어야 한다. 
2. driver 서비스 되지 않더라도 call 서비스는 지속된다. (장애격리)


# 체크포인트

- 분석 설계

  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?


# 분석/설계

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/GC1SWGUh8lSswBk8IBzQrKOwUxo2/mine/83801f8fcac356fe76550944be3e8284


### 이벤트 도출
### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/84304023/124904250-c776ed00-e01f-11eb-86c3-bc9c1d97fbd6.png)


    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
     

### 액터, 커맨드 부착하여 읽기 좋게
![image](https://user-images.githubusercontent.com/84304023/124904305-d3fb4580-e01f-11eb-9ef6-27fa7fdfb0cf.png)
### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/84304023/124904358-e37a8e80-e01f-11eb-8271-d476be987090.png)

   - command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌

### 바운디드 컨텍스트로 묶기
![image](https://user-images.githubusercontent.com/84304023/124904741-45d38f00-e020-11eb-8350-356ab0580db4.png)


### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)
![image](https://user-images.githubusercontent.com/84304023/124904812-5a178c00-e020-11eb-9023-e0438a038796.png)
### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)
![image](https://user-images.githubusercontent.com/84304023/124904848-63a0f400-e020-11eb-9c83-977e1310636f.png)

요구사항 점검 후 callist 빠진 것 확인 
![image](https://user-images.githubusercontent.com/84304023/124905523-17a27f00-e021-11eb-9ddd-a0a17f3a0636.png)


## MSAEz 결과
 - 아래와 같이 모델링한 후 소스를 생성함

![image](https://user-images.githubusercontent.com/84304023/124905145-b5497e80-e020-11eb-8c3b-83921a0ff52e.png)



## 헥사고날 아키텍처 다이어그램 도출
![image](https://user-images.githubusercontent.com/84304023/124906110-afa06880-e021-11eb-9b8d-58610f483cd0.png)



# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 
(각자의 포트넘버는 로컬에서는 8081 ~ 8084 임)

```
  mvn package -Dmaven.test.skip=trun
```

## CQRS

회사가 수익과 콜수를 점검할 수 있는 화면을 CQRS 로 구현한다.
비동기식으로 처리되어 발행된 이벤트 기반 카프카를 통해 수신/처리되어 별도 TABLE 에 관리한다. 

승객이 콜을 하면 승객 ID/ 금액/ 목적지 / 상태 = Called 가 업데이트 된다. 
운전기사가 수락을 하고 승객을 태우면 상태가 accetped 로 업데이트 된다. 
운행이 끝나고 운전기사가 종료처리 하면 정산 금액이 업데이트 되고 상태가 completed 로 변경된다.

테이블 모델링 (callList)
![image](https://user-images.githubusercontent.com/84304023/124918416-f85f1e00-e02f-11eb-8c47-553f5124b2b5.png)
 
- viewpage MSA ViewHandler 를 통해 구현 (이벤트 발생 시, Pub/Sub 기반으로 별도 callList 테이블에 저장)
- call 생성시 승객 id/금액/목적지/상태=called 가 생성됨
![image](https://user-images.githubusercontent.com/84304023/124919004-a9fe4f00-e030-11eb-992b-ea14c31d0700.png)
- 운전기사의 운행이 시작되어 상태가 accepted 로 업데이트 되면 뷰도 업데이트 됨
- 운전기사의 운행이 완료되어 상태를 completed 로 업데이트 되면 뷰의 상태값과 settle 필드도 업데이트  
![image](https://user-images.githubusercontent.com/84304023/124919111-c8644a80-e030-11eb-9363-ef01a922085c.png)



- 실제로 view 페이지를 조회에서 정보 확인이 가능하다

 ![image](https://user-images.githubusercontent.com/84304023/124941897-1f284f00-e046-11eb-8a6f-ab46ad5cdaf4.png)
![image](https://user-images.githubusercontent.com/84304023/124941920-23546c80-e046-11eb-8c08-26795bd3604e.png)
![image](https://user-images.githubusercontent.com/84304023/124941942-264f5d00-e046-11eb-99a2-ffa0e2caaf4d.png)

  

## API 게이트웨이
gateway 스프링부트 App을 추가 후 application.yaml내에 각 마이크로 서비스의 routes 를 추가하고 gateway 서버의 포트를 8080 으로 설정함
       
         
 1.  application.yml   
 ```  
spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: call
          uri: http://call:8080
          predicates:
            - Path=/calls/** 
        - id: driver
          uri: http://driver:8080
          predicates:
            - Path=/drivers/** 
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 
        - id: callList
          uri: http://callList:8080
          predicates:
            - Path= /callLists/**
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
	kubectl create deploy gateway --image 879772956301.dkr.ecr.eu-west-3.amazonaws.com/gateway:v1 -n taxi
	kubectl expose deploy gateway --type="LoadBalancer" --port=8080 --namespace=taxi

   ```     

![image](https://user-images.githubusercontent.com/84304023/124944396-3c5e1d00-e048-11eb-8c2b-75c3b01312c6.png)

    

# Correlation

승객이 택시를 호출했을 때, 운전기사가 호출을 수락하여 승객을 태울 때, 운행종료후 운전기사가 콜을 종료처리할 때 
calllist 상태가 변경된다. 

1) 승객이 택시를 호출한다

 http POST http://localhost:8081/calls customerId=1 destination=junja cost=500
 
![image](https://user-images.githubusercontent.com/84304023/124944723-86df9980-e048-11eb-9672-777804c3898a.png)

1-1) 택시를 호출하면 payment에 callId별로 요금 500이 등록되는데(동기) 이를 확인한다

http GET http://localhost:8083/payments/2

![image](https://user-images.githubusercontent.com/84304023/124944827-9b239680-e048-11eb-96cc-eb491c259e02.png)

1-2) driver정보도 확인한다

http GET http://localhost:8082/drivers/2

![image](https://user-images.githubusercontent.com/84304023/124946395-f43ffa00-e049-11eb-8c73-b1338b706a26.png)

1-3) callist 화면도 확인한다.

http GET http://localhost:8084/callLists/2

![image](https://user-images.githubusercontent.com/84304023/124944954-b7273800-e048-11eb-954f-9f30efabaa16.png)


2) 운전기사가 호출을 수락하여 승객을 태운다.

http PATCH http://localhost:8082/drivers/2 status=Accepted


![image](https://user-images.githubusercontent.com/84304023/124945032-c5755400-e048-11eb-90e4-f3fd580f55d4.png)


2-1)승객의 상태가 변경된다. 

http GET http://localhost:8081/calls/2


![image](https://user-images.githubusercontent.com/84304023/124945082-cefebc00-e048-11eb-9338-a1837678e131.png)


2-2)Calllist 상태가 변경된다.

http GET http://localhost:8084/callLists/2

![image](https://user-images.githubusercontent.com/84304023/124945134-d8882400-e048-11eb-8479-76b805cdc38d.png)


3) 탑승이 종료되면 운전기사가 상태를 변경한다.

http PATCH http://localhost:8082/drivers/2 status=Completed

![image](https://user-images.githubusercontent.com/84304023/124945204-e473e600-e048-11eb-861d-39ec707a1c1c.png)


3-1) 승객의 상태가 변경된다.

http GET http://localhost:8081/calls/2 

![image](https://user-images.githubusercontent.com/84304023/124945229-eb9af400-e048-11eb-9981-65deed563ae0.png)

3-2) callList 상태가 변경된다. 

http GET http://localhost:8084/callLists/2

![image](https://user-images.githubusercontent.com/84304023/124945278-f786b600-e048-11eb-8a97-7ec14c64d417.png)






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





## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

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





## 폴리그랏 퍼시스턴스 적용
```
Message Sevices : hsqldb사용
```
![image](https://user-images.githubusercontent.com/84304043/122845081-dda55d80-d33d-11eb-8d9f-a4e17735574e.png)
```
Message이외  Sevices : h2db사용
```
![image](https://user-images.githubusercontent.com/84304043/122845106-ed24a680-d33d-11eb-9124-aed5d9e7285b.png)

## Maven 빌드시스템 라이브러리 추가( pom.xml 설정변경 H2DB → HSQLDB) 
![image](https://user-images.githubusercontent.com/84304043/122845179-0fb6bf80-d33e-11eb-879a-1e6e8964ebb3.png)

# 운영


## CI/CD 설정

각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD는 buildspec.yml을 이용한 AWS codebuild를 사용하였습니다.

- CodeBuild 프로젝트를 생성하고 AWS_ACCOUNT_ID, KUBE_URL, KUBE_TOKEN 환경 변수 세팅을 한다
```
SA 생성
kubectl apply -f eks-admin-service-account.yml
```
![image](https://user-images.githubusercontent.com/84304043/122844500-c154f100-d33c-11eb-9ec0-5eb0fa3540d6.png)
```
Role 생성
kubectl apply -f eks-admin-cluster-role-binding.yml
```
![image](https://user-images.githubusercontent.com/84304043/122844538-d6ca1b00-d33c-11eb-818b-5a51404265c1.png)
```
Token 확인
kubectl -n kube-system get secret
kubectl -n kube-system describe secret $(kubectl -n kube-system get secret | grep eks-admin | awk '{print $1}')
```
![image](https://user-images.githubusercontent.com/86210580/122849832-34fbfb80-d347-11eb-9f6d-b1e379b3e1cf.png)

```
buildspec.yml 파일 
마이크로 서비스 storage의 yml 파일 이용하도록 세팅
```
![image](https://user-images.githubusercontent.com/84304043/122844673-201a6a80-d33d-11eb-8a52-a0fad02951d9.png)

- codebuild 실행
```
codebuild 프로젝트 및 빌드 이력
```
![image](https://user-images.githubusercontent.com/84304043/122846416-bdc36900-d340-11eb-9558-cad08d2615f2.png)
![image](https://user-images.githubusercontent.com/84304043/122861596-a2fdee00-d35a-11eb-9d73-7ff537c9e332.png)

- codebuild 빌드 내역 (Message 서비스 세부)

![image](https://user-images.githubusercontent.com/84304043/122846449-cd42b200-d340-11eb-8a33-aeff63915d61.png)

- codebuild 빌드 내역 (전체 이력 조회)

![image](https://user-images.githubusercontent.com/84304043/122846462-d5025680-d340-11eb-9914-b12b82a74ff5.png)



## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹: Hystrix 사용하여 구현함

시나리오는 예약(reservation)--> 창고(storage) 시의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 예약 요청이 과도할 경우 CB 를 통하여 장애격리.

![image](https://user-images.githubusercontent.com/84304043/122866912-b6618700-d363-11eb-8247-dae264aa6fdf.png)


* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인: siege 실행 하였으나 storagerent 부하가 생성되지 않음.

```
kubectl run siege --image=apexacme/siege-nginx -n storagerent
kubectl exec -it siege -c siege -n storagerent -- /bin/bash
```
- Jmeter 로 부하 테스트 하였으나 실패건이 3%로 나오는것확인 -> 하지만 Jmeter 테스트와 연결해서 CB결과를 보여줘야할지 모르겠음.

![image](https://user-images.githubusercontent.com/84304043/122867174-10fae300-d364-11eb-8ab4-a2dbc6395f75.png)
![image](https://user-images.githubusercontent.com/84304043/122867281-31c33880-d364-11eb-9854-587ebade3a5b.png)


### 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 

- storage deployment.yml 파일에 resources 설정을 추가한다

![image](https://user-images.githubusercontent.com/84304043/122850814-d6378180-d348-11eb-9cd2-eb0873f1c8d7.png)

- storage 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 50프로를 넘어서면 replica 를 10개까지 늘려준다. (X, 오류 발생)
```
kubectl autoscale deployment storage -n storagerent --cpu-percent=50 --min=1 --max=10
```

- 부하를 동시사용자 100명, 1분 동안 걸어준다.(X)
```
siege -c100 -t60S -v --content-type "application/json" 'http://storage:8080/storages POST {"desc": "BigStorage"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다 (X)
```
kubectl get deploy storage -w -n storagerent 
```
- 어느정도 시간이 흐른 후 (약 30초) 스케일 아웃이 벌어지는 것을 확인할 수 있다:(X)

- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. (X)


## 무정지 재배포

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함

```
kubectl delete destinationrules dr-storage -n storagerent
kubectl delete hpa storage -n storagerent
```

- seige 로 배포작업 직전에 워크로드를 모니터링 함(에러발생)
```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://storage:8080/storages POST {"desc": "BigStorage"}'
```

- 새버전으로의 배포 시작(X)
```
kubectl set image ...
```

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인(X)

```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://storage:8080/storages POST {"desc": "BigStorage"}'
```

- 배포기간중 Availability 가 평소 100%에서 87% 대로 떨어지는 것을 확인. 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함

```
# deployment.yaml 의 readiness probe 의 설정:
```

![image](https://user-images.githubusercontent.com/84304043/122858339-156bcf80-d355-11eb-9d1a-91da438ac905.png)


```
kubectl apply -f kubernetes/deployment.yml
```

- 동일한 시나리오로 재배포 한 후 Availability 확인(X)


# Self-healing (Liveness Probe)
- storage deployment.yml 파일 수정 
```
콘테이너 실행 후 /tmp/healthy 파일을 만들고 
90초 후 삭제
livenessProbe에 'cat /tmp/healthy'으로 검증하도록 함
```
![image](https://user-images.githubusercontent.com/84304043/122863309-80210900-d35d-11eb-8e07-8113c4ca6af9.png)

- kubectl describe pod storage -n storagerent 실행으로 확인(X)

