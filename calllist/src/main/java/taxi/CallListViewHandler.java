package taxi;

import taxi.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class CallListViewHandler {


    @Autowired
    private CallListRepository callListRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCalledTaxi_then_CREATE_1 (@Payload CalledTaxi calledTaxi) {
        try {

            if (!calledTaxi.validate()) return;

            // view 객체 생성
            CallList callList = new CallList();
            // view 객체에 이벤트의 Value 를 set 함
            callList.setId(calledTaxi.getId());
            callList.setCost(calledTaxi.getCost());
            callList.setCustomerId(calledTaxi.getCustomerId());
            callList.setDestination(calledTaxi.getDestination());
            callList.setStatus(calledTaxi.getStatus());
            // view 레파지 토리에 save
            callListRepository.save(callList);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenUpdatedStatus_then_UPDATE_1(@Payload UpdatedStatus updatedStatus) {
        try {
            if (!updatedStatus.validate()) return;
                // view 객체 조회
            Optional<CallList> callListOptional = callListRepository.findById(updatedStatus.getCallId());
            if( callListOptional.isPresent()) {
                CallList callList = callListOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                    callList.setStatus(updatedStatus.getStatus());
                    if( updatedStatus.getStatus().equals("Completed") ){
                        callList.setSettled(callList.getCost());
                    }
                    callList.setCost(updatedStatus.getCost());
                // view 레파지 토리에 save
                callListRepository.save(callList);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}