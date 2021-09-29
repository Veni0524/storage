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
![image](https://user-images.githubusercontent.com/88864460/135233171-357c9456-2755-46a2-8cf8-36c0d6150eb9.png)



# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 
구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 
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
- 
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

