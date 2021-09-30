package delivery;

import delivery.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryListViewHandler {


    @Autowired
    private DeliveryListRepository deliveryListRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCalledDriver_then_CREATE_1 (@Payload CalledDriver calledDriver) {
        try {

            if (!calledDriver.validate()) return;

            // view 객체 생성
            DeliveryList deliveryList = new DeliveryList();
            // view 객체에 이벤트의 Value 를 set 함
            deliveryList.setId(calledDriver.getId());
            deliveryList.setCost(calledDriver.getCost());
            deliveryList.setCustomerId(calledDriver.getCustomerId());
            deliveryList.setDestination(calledDriver.getDestination());
            deliveryList.setStatus(calledDriver.getStatus());
            // view 레파지 토리에 save
            deliveryListRepository.save(deliveryList);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenUpdatedStatus_then_UPDATE_1(@Payload UpdateStatus updatedStatus) {
        try {
            if (!updatedStatus.validate()) return;
                // view 객체 조회
            Optional<DeliveryList> deliveryListOptional = deliveryListRepository.findById(updatedStatus.getDeliveryId());
            if( deliveryListOptional.isPresent()) {
                DeliveryList deliveryList = deliveryListOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                    deliveryList.setStatus(updatedStatus.getStatus());
                    if( updatedStatus.getStatus().equals("Completed") ){
                        deliveryList.setSettled(deliveryList.getCost());
                    }
                    deliveryList.setCost(updatedStatus.getCost());
                // view 레파지 토리에 save
                deliveryListRepository.save(deliveryList);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}

